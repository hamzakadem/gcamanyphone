# Computational Photography & Image Processing

## 1. Algorithmic Principles

In traditional digital photography, a single exposure captured by a small smartphone sensor suffers from:
1. **Photon shot noise** in low-light environments due to limited quantum efficiency.
2. **Dynamic range clipping**, causing blown highlights or crushed shadows.
3. **Sensor read noise** and thermal noise in night scenes.

**GCam Lite** applies a memory-safe computational photography pipeline designed to run efficiently on standard mobile CPUs without requiring proprietary DSP hardware:

```
[Raw Capture Frame Stream] 
       │
       ▼
[Stage 1: Incremental Multi-Frame Accumulation]  --> Suppresses random shot & thermal noise
       │
       ▼
[Stage 2: Adaptive Local Tone Mapping]          --> Lifts dark shadows / Astrophotography Star Curve
       │
       ▼
[Stage 3: Spatial Filtering & Unsharp Mask]      --> Enhances fine edges & stars
       │
       ▼
[High-Quality JPEG Output]
```

---

## 2. Multi-Frame Burst Accumulation (15-Frame Night Mode)

When $N$ independent frames $I_1, I_2, \dots, I_N$ of a scene are merged, the random noise components cancel out while the coherent light signal adds constructively.

The Signal-to-Noise Ratio (SNR) improves by:

$$\text{SNR}_{\text{improvement}} = \sqrt{N} \cdot \text{SNR}_{\text{single}}$$

- **Fast Single ($N=1$)**: Instantaneous capture, 0ms merge latency.
- **HDR+ Lite ($N=3$)**: Improves SNR by $\approx 1.73\times$ ($\approx 4.8\text{ dB}$).
- **Astrophotography ($N=10$)**: Improves SNR by $\approx 3.16\times$ ($\approx 10.0\text{ dB}$) with locked infinite focus and maximum shutter.
- **Night Mode ($N=15$)**: Improves SNR by $\approx 3.87\times$ ($\approx 11.8\text{ dB}$), eliminating high-ISO sensor grain.

### Incremental Memory-Safe Accumulation
Holding 15 uncompressed 12MP bitmaps in RAM simultaneously would demand over $720\text{ MB}$, causing immediate OOM on budget devices.

`IncrementalBurstAccumulator` solves this by maintaining a single 64-bit sum buffer (`LongArray`). Each incoming frame is added to the running sum and immediately recycled (`autoRecycle = true`), keeping the active heap consumption under $35\text{ MB}$ regardless of frame count.

---

## 3. Astrophotography Mode (10 Frames, Infinite Focus, Max Shutter)

Astrophotography requires specialized hardware controls and astronomical image processing:

1. **Infinite Optical Focus**:
   In optics, focusing at infinity corresponds to $0.0\text{ diopters}$ ($d = \frac{1}{\infty} = 0$).
   Via Camera2Interop, auto-focus is disabled and lens distance is locked:
   $$\text{CONTROL\_AF\_MODE} = \text{OFF}, \quad \text{LENS\_FOCUS\_DISTANCE} = 0.0\text{f}$$
2. **Maximum Shutter Integration**:
   Sets exposure compensation to maximum (`exposureCompensationRange.upper`) to force the sensor to use the longest integration time.
3. **Astrophotography Star Curve**:
   Subdues background atmospheric glow and sensor thermal noise in the lowest $8\%$ of luminance values while dramatically boosting faint point light sources (stars, constellations):
   $$\text{LUT}[x] = \begin{cases} 
   0.45 \cdot x & x < 0.08 \\ 
   0.036 + 1.55 \cdot (x - 0.08) & 0.08 \le x < 0.45 \\ 
   0.61 + 0.70 \cdot (x - 0.45) & x \ge 0.45 
   \end{cases}$$
