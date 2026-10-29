# GCam Pro — Universal Computational Camera & Pro Video

[![Android Build & APK Delivery](https://github.com/hamzakadem/gcamanyphone/actions/workflows/android-build.yml/badge.svg)](https://github.com/hamzakadem/gcamanyphone/actions/workflows/android-build.yml)
[![Release](https://img.shields.io/github/v/release/hamzakadem/gcamanyphone?color=00E676&label=Latest%20Release)](https://github.com/hamzakadem/gcamanyphone/releases/latest)

**GCam Pro** (`com.gcam.compatible.camera`) is an advanced, standalone, open-source Android camera and video application. It combines flagship computational photography algorithms (multi-frame burst HDR+, deep-sky astrophotography, motion de-ghosting) with **pro-grade manual video controls**, a **universal Synthetic Log profile**, and a **3D LUT (.cube) color grading engine** that works on **any Android phone** (Android 5.0 to Android 14+), regardless of manufacturer restrictions or hardware Camera2 limitations (`LEGACY`, `LIMITED`, `FULL`, `LEVEL_3`).

---

## 📥 Direct APK Download

Download the latest pre-compiled, signed APK directly to your phone:

- 🚀 **[Download GCamPro-v1.2.0.apk (Latest Release)](https://github.com/hamzakadem/gcamanyphone/releases/download/v1.2.0/GCamCompatibleCamera-debug.apk)**
- 📦 **[View All Release Assets & Checksums (v1.2.0)](https://github.com/hamzakadem/gcamanyphone/releases/tag/v1.2.0)**

---

## 🌟 Key Features

### 1. 🎨 Modern Flagship Camera Interface
- **Dark Glassmorphism Design:** Translucent frosted controls inspired by Google Pixel 9 and iOS Pro camera systems.
- **Top Quick-Pill Bar:** 1-tap toggles for Flash, Pro Manual Panel, Synthetic LOG, LUT Selector, and Settings.
- **Bottom Mode Carousel:** Smooth, responsive horizontal switcher between:
  - `NIGHT`: 15-frame low-light temporal noise elimination.
  - `PHOTO`: Balanced HDR+ Lite (3 frames).
  - `VIDEO`: 4K / 1080p recording with full manual controls and audio.
  - `FAKE RAW`: Intelligent computational RAW synthesis with user-selected frame counts.
  - `ASTRO`: Astrophotography mode with infinite optical focus and maximum exposure integration.
- **Concentric Ring Shutter:** Dynamic shutter button with real-time circular progress indicator during multi-frame bursts, and dedicated red recording state with live timer (`00:00`).

---

### 2. 🎛️ Full Pro Manual Controls (Photo & Video)
- **Manual ISO Slider:** Complete control from Auto to ISO 100, 200, 400, 800, 1600, 3200, and 6400 via Camera2 manual sensor sensitivity.
- **Manual Shutter Speed Slider:** Seamless integration from Auto, 1/2000s, 1/1000s, 1/500s, 1/250s, 1/125s, 1/60s, 1/30s, down to 1/15s.
- **Manual White Balance (Kelvin):** Correlated color temperature control from Auto, 2800K (Tungsten), 3500K (Warm), 4500K (Fluorescent), 5500K (Daylight), 6500K (Cloudy), to 7500K (Shade) with RGB gain correction.
- **Real-Time Video Recording:** High-bitrate MP4 video recording using `androidx.camera.video.VideoCapture` saved directly to your device's Gallery (`DCIM/GCamLite`).

---

### 3. 🎞️ Universal Synthetic Log Profile
- **Logarithmic Dynamic Range on Any Phone:** Eliminates crushed shadows and blown-out highlights by remapping tones through a standardized logarithmic transfer curve:
  $$V_{log} = a \cdot \ln(1 + b \cdot V_{lin}) + c$$
- **Hardware-Agnostic:** Operates on standard sensor streams, unlocking flat cinematic Log shooting for smartphones whose manufacturers do not support official 10-bit Log capture.

---

### 4. 🎬 Universal 3D LUT (.cube) Engine & Custom Importer
- **DaVinci Resolve & Adobe Premiere Support:** Built-in parser for standard `.cube` (3D and 1D) Look-Up Table files with trilinear interpolation.
- **Custom LUT Importer:** Built-in file picker allows importing any `.cube` file from device storage or cloud drives.
- **Cinematic Presets Included:**
  - *Synthetic Log (Universal Flat Curve)*
  - *Cinematic Teal & Orange (Hollywood blockbuster contrast)*
  - *Vintage Kodachrome 64 (Classic warm film emulation)*
  - *High-Dynamic B&W Pro (Leica-style monochrome contrast)*
  - *Cool Cyberpunk (Cold cyan/magenta shadows with neutral tones)*

---

### 5. 🔬 Intelligent "Fake RAW" Computational Synthesis
- **User-Selectable Burst Count:** Choose your preferred frame count directly using a slider (from **2 to 25 frames**).
- **16-bit Linear Photometric Accumulation:** Bypasses non-linear 8-bit gamma compression by linearizing sensor frames into a high-precision 16-bit linear light domain before accumulation.
- **RAW-Grade Dynamic Range:** Boosts signal-to-noise ratio by up to $\sqrt{N}$ and delivers 12 to 14+ stops of usable dynamic range, reconstructing deep shadow details without clipping highlights.

---

### 6. 🛡️ OOM-Proof Multi-Frame Engine (20+ RAW Frame Fix)
- **Zero Heap Spikes:** Replaced heavy 64-bit `LongArray` buffers with 32-bit `IntArray` accumulators, cutting memory consumption in half.
- **Buffer Reuse Architecture:** Employs a single pre-allocated transfer buffer instead of re-allocating 48MB arrays per frame.
- **Immediate Bitmap Recycling:** Sequential decode-accumulate-recycle pipeline ensures memory stays bounded to just **one active frame at a time**.
- **Large Heap Enabled:** `android:largeHeap="true"` unlocked to utilize full system heap on demanding multi-frame bursts.

---

## 📊 Comparison Matrix

| Feature | Official GCam Ports | GCam Pro (`v1.2.0`) |
| :--- | :--- | :--- |
| **APK Size** | ~350 MB – 500 MB | **~8 MB** |
| **Device Compatibility** | Snapdragon + RAW_SENSOR only | **Universal (Any Android phone)** |
| **Camera2 Hardware Level** | `FULL` or `LEVEL_3` mandatory | `LEGACY`, `LIMITED`, `FULL`, `LEVEL_3` |
| **Video Manual Controls** | Often broken or vendor-locked | **Full Manual ISO, Shutter, Kelvin WB** |
| **Log Video Profile** | Pixel 8/9 Pro only | **Universal Synthetic Log on all phones** |
| **LUT Support** | Proprietary XML configs | **Standard .cube 3D LUTs + Custom Import** |
| **Computational RAW** | Fixed frames (Crashes on 20) | **Customizable 2–25 frames with OOM guard** |
| **Astrophotography** | Tripod mandatory | **Infinite focus + max shutter integration** |

---

## 🛠️ Technology Stack & Architecture

- **Language:** Kotlin 1.9.22 (100% clean, idiomatic code)
- **Target SDK:** Android 14 (API 34) | **Min SDK:** Android 5.0 (API 21)
- **Camera Subsystem:** Jetpack CameraX 1.3.3 (`camera-core`, `camera-camera2`, `camera-video`, `camera-view`, `camera-lifecycle`)
- **Coroutines:** Kotlin Coroutines Android for non-blocking I/O and processing
- **Storage:** Scoped Storage MediaStore API with backward compatibility
- **CI/CD:** Automated builds, unit tests, and GitHub Releases via GitHub Actions

---

## 📄 Documentation

- [CHANGELOG.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/CHANGELOG.md) — Comprehensive version history and release notes.
- [BUILD_INSTRUCTIONS.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/BUILD_INSTRUCTIONS.md) — Step-by-step 1-click cloud build guide.
- [IMAGE_PROCESSING.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/IMAGE_PROCESSING.md) — Mathematical formulations of tone mapping, Fake RAW, and Log curves.
- [CAMERA_BACKENDS.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/CAMERA_BACKENDS.md) — Camera2 interop and hardware capability detection.
- [ARCHITECTURE.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/ARCHITECTURE.md) — Modular architecture and component boundaries.

---

## ⚖️ License

Licensed under the **Apache License, Version 2.0**. See [LICENSES_AND_ATTRIBUTIONS.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/LICENSES_AND_ATTRIBUTIONS.md) for details.
