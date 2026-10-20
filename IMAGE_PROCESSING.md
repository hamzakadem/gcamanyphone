# Computational Photography & Image Processing

## 1. Algorithmic Principles

In traditional digital photography, a single exposure captured by a small smartphone sensor suffers from:
1. **Photon shot noise** in low-light environments due to limited quantum efficiency.
2. **Dynamic range clipping**, causing blown highlights (sky, lights) or crushed shadows.
3. **Sensor read noise** caused by analog-to-digital conversion circuits.

**GCam Lite** applies a three-stage computational photography pipeline designed to run efficiently on standard mobile CPUs without requiring proprietary DSP hardware:

```
[Raw Capture Frame] 
       │
       ▼
[Stage 1: Multi-Frame Temporal Accumulation]  --> Suppresses random shot & read noise
       │
       ▼
[Stage 2: Adaptive Local Tone Mapping]       --> Lifts dark shadows & rolls off highlights
       │
       ▼
[Stage 3: Spatial Filtering & Unsharp Mask]   --> Enhances fine edges & textures
       │
       ▼
[High-Quality JPEG Output]
```

---

## 2. Multi-Frame Burst Accumulation

When multiple frames $I_1, I_2, \dots, I_N$ of the same static scene are merged, the independent random noise components are averaged. 

The signal adds constructively:

$$S_{\text{merged}} = \sum_{k=1}^N S_k = N \cdot S$$

While the noise variance adds quadratically:

$$\sigma_{\text{merged}}^2 = \sum_{k=1}^N \sigma_k^2 = N \cdot \sigma^2 \implies \sigma_{\text{merged}} = \sqrt{N} \cdot \sigma$$

The resulting Signal-to-Noise Ratio (SNR) improves by:

$$\text{SNR}_{\text{improvement}} = \frac{N \cdot S}{\sqrt{N} \cdot \sigma} = \sqrt{N} \cdot \text{SNR}_{\text{single}}$$

- **Fast Single ($N=1$)**: 0ms merge overhead, ideal for moving subjects.
- **HDR+ Lite ($N=3$)**: Improves SNR by $\approx 1.73\times$ ($\approx 4.8\text{ dB}$).
- **Night Lite ($N=5$)**: Improves SNR by $\approx 2.24\times$ ($\approx 7.0\text{ dB}$).

Implementation in `BurstAccumulator.kt` uses 64-bit integer accumulators (`LongArray`) to prevent arithmetic overflow on 32-bit and 64-bit platforms alike.

---

## 3. Adaptive Local Tone Mapping

Smartphone scenes often exhibit dynamic ranges far exceeding the 8-bit display capabilities ($0\dots 255$). 

`AdaptiveToneMapper` dynamically computes the average scene luminance $L_{\text{avg}}$ from a subsampled grid:

$$L = 0.299 \cdot R + 0.587 \cdot G + 0.114 \cdot B$$

If the scene is dark ($L_{\text{avg}} < 0.35$), the shadow boost factor is scaled dynamically:

$$\gamma_{\text{effective}} = \frac{1}{\beta_{\text{shadow}} \cdot (1.0 + (0.35 - L_{\text{avg}}))}$$

A 256-entry Look-Up Table (LUT) is generated using a Reinhard-inspired soft compression operator:

$$\text{LUT}[i] = 255 \cdot \frac{x^\gamma}{1.0 + (1.0 - \kappa_{\text{highlight}}) \cdot x^\gamma}, \quad x = \frac{i}{255}$$

Because the transformation is mapped via a precomputed 256-integer array, the per-pixel execution is an $O(1)$ direct array index lookup with zero CPU branching.

---

## 4. Edge-Preserving Sharpening (Unsharp Masking)

Tone mapping and multi-frame averaging can slightly soften micro-contrast. `EdgePreservingDenoise` applies a high-frequency enhancement:

$$I_{\text{out}}(x, y) = I_{\text{in}}(x, y) + \lambda \cdot (I_{\text{in}}(x, y) - I_{\text{blurred}}(x, y))$$

Where:
- $I_{\text{blurred}}$ is a spatial $3\times 3$ box-filter convolution.
- $\lambda$ is the sharpening strength parameter ($0.25\dots 0.40$).
- Pixel values are clamped to $[0, 255]$ with border protection.

---

## 5. Memory Guard & OOM Prevention

On Android devices with 1GB to 2GB of RAM, allocating two uncompressed 12-megapixel $4000\times 3000$ ARGB_8888 bitmaps requires:

$$4000 \times 3000 \times 4 \text{ bytes} = 48\text{ MB per bitmap}$$

Two bitmaps plus processing buffers exceed 100MB, triggering an instantaneous `java.lang.OutOfMemoryError` on phones with a 128MB or 192MB JVM heap limit.

`MemoryGuard` evaluates heap health before every operation:

```kotlin
val runtime = Runtime.getRuntime()
val availableHeap = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
```

If `availableHeap` minus estimated memory is $< 48\text{ MB}$:
1. It automatically increases `BitmapFactory.Options.inSampleSize` to $2$ or $4$.
2. It clamps the burst frame count to $1$ (`FAST_SINGLE_FRAME`).
3. It frees intermediate buffers immediately using `.recycle()`.
