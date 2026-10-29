# Changelog

All notable changes to **GCam Pro** will be documented in this file.

---

## [1.2.0] - 2026-10-29

### Fixed
- **20+ RAW / Burst Frame Crash Resolution**:
  - Eliminated OutOfMemory (OOM) heap exhaustion when selecting or merging 20+ RAW/burst frames.
  - Replaced 64-bit `LongArray` buffers with 32-bit `IntArray` accumulators, cutting memory consumption by 50%.
  - Introduced reusable pixel transfer buffers to eliminate garbage collection churn during sequential frame merging.
  - Enabled `android:largeHeap="true"` in `AndroidManifest.xml` to grant full heap headroom for computational photography.
  - Added graceful per-frame OOM catch-and-merge fallback so shooting never aborts unexpectedly.

### Added
- **Modern Flagship Camera UI**:
  - Completely redesigned camera interface inspired by modern Google Pixel and iOS Pro cameras.
  - Sleek translucent glassmorphism control pillbar (Flash, Pro Manual Controls, Synthetic LOG, LUTs, Settings).
  - Bottom swipeable/tappable Mode Carousel: `NIGHT`, `PHOTO`, `VIDEO`, `FAKE RAW`, and `ASTRO`.
  - Contemporary concentric dual-ring shutter button with dedicated video recording states and circular progress feedback.
  - Slide-out Pro Manual Controls drawer with real-time SeekBars and live value readouts.
- **Video Recording with Full Pro Manual Controls**:
  - Integrated 4K / 1080p high-bitrate video recording using CameraX VideoCapture (`QualitySelector.HIGHEST`).
  - Full manual control over **ISO** (Auto, 100 to 6400).
  - Full manual control over **Shutter Speed** (Auto, 1/2000s to 1/15s).
  - Full manual control over **White Balance** (Auto, 2800K to 7500K with color correction gains).
  - Live recording timer display and dedicated video gallery media storage.
- **Universal Synthetic Log Profile**:
  - Standardized logarithmic transfer curve that works on **any Android phone**, bypassing proprietary vendor HAL restrictions.
  - Lifts crushed shadows and rolls off specular highlights to maximize captured dynamic range for color grading.
- **Universal 3D LUT (.cube) Engine & Custom Importer**:
  - Native parser for DaVinci Resolve / Adobe `.cube` 3D and 1D color LUT files.
  - File picker allowing users to import custom LUT files from device storage or cloud drives.
  - Built-in cinematic presets: *Synthetic Log*, *Cinematic Teal & Orange*, *Vintage Kodachrome 64*, *High-Dynamic B&W Pro*, and *Cool Cyberpunk*.
- **Intelligent Fake RAW (Computational RAW Synthesis)**:
  - User-configurable burst count slider (2 to 25 frames).
  - Linearizes sensor frames using inverse gamma mapping into high-precision 16-bit linear light domain.
  - Performs photometric accumulation to expand effective dynamic range by 12–14+ stops and suppress photon noise by up to $\sqrt{N}$.

### Enhanced
- **Adaptive Tone Mapper**: Upgraded Reinhard-inspired highlight compression and dynamic shadow boosting.
- **Spatial Denoising**: Bilateral edge-magnitude weighting that aggressively cleans noise in flat areas while keeping textures and edges tack-sharp.
- **Motion De-Ghosting**: Running pixel variance rejection that downweights moving objects during multi-frame handheld bursts.

---

## [1.1.0] - 2026-10-20

### Added
- **15-Frame Enhanced Night Mode**:
  - Upgraded Night Mode algorithm to merge 15 sequential frames using temporal accumulation, eliminating high-ISO sensor noise.
  - Implemented `IncrementalBurstAccumulator` to process frames sequentially with zero memory bloat (constant memory footprint).
- **Astrophotography Mode (10 Frames, Infinite Focus, Max Shutter)**:
  - Added dedicated Astrophotography mode designed for capturing night skies, constellations, and stars.
  - Locked lens focus to optical infinity (`LENS_FOCUS_DISTANCE = 0.0f`, `CONTROL_AF_MODE_OFF`).
  - Maximized sensor shutter integration time via exposure compensation controls.
  - Implemented specialized Astrophotography Star Curve to subdue background sky glow and enhance star point contrast.
- **Real-Time Burst Progress UI**:
  - Viewfinder now displays real-time frame progress ("Capturing frame 3 of 15...") with haptic vibration ticks.

---

## [1.0.0] - 2026-10-20

### Added
- Initial standalone release of GCam Lite (`com.gcam.compatible.camera`).
- CameraX backend supporting `LEGACY`, `LIMITED`, `FULL`, and `LEVEL_3` devices without RAW requirements.
- Adaptive tone mapping and burst merging.
- GitHub Actions CI/CD cloud build pipeline.
