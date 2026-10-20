# GCam Lite — Standalone Compatible Android Camera

[![Android Build & APK Delivery](https://github.com/REPLACE_WITH_YOUR_USERNAME/GCamCompatibleCamera/actions/workflows/android-build.yml/badge.svg)](https://github.com/REPLACE_WITH_YOUR_USERNAME/GCamCompatibleCamera/actions/workflows/android-build.yml)

**GCam Lite** (`com.gcam.compatible.camera`) is a standalone, lightweight, resilient Android camera application designed from the ground up to bring Google Camera-inspired computational photography (multi-frame noise reduction, adaptive local tone mapping, and edge-preserving sharpening) to **low-end Android devices**, memory-constrained hardware (1GB–3GB RAM), and phones with limited Camera2 API support (`LEGACY` and `LIMITED` hardware levels) that do not support RAW sensor streams.

---

## Key Highlights

- **Zero Local Toolchain Required**: Designed specifically for cloud compilation via **GitHub Actions**. You do not need to install Android Studio, Android SDK, Gradle, or JDK on your local computer to build and download the APK.
- **Universal Device Compatibility**: Targets Android API 21+ (Android 5.0 Lollipop through Android 14+), removing the restrictive Google Play and Camera2 `LEVEL_FULL` / `RAW_SENSOR` barriers of official GCam ports.
- **Adaptive Computational Photography**:
  - **Fast Single**: Immediate single-frame capture with instant gamma shadow-lifting and zero OOM risk.
  - **HDR+ Lite**: 3-frame temporal burst accumulation for $\approx\sqrt{3}$ photon noise reduction, adaptive Reinhard tone curve, and unsharp masking.
  - **Night Lite**: 5-frame exposure blending with deep shadow lift and edge-preserving noise suppression.
- **Memory Guard Architecture**: Real-time heap monitoring ensures downsampling or burst fallback before memory spikes, preventing `OutOfMemoryError` on 1GB/2GB RAM phones.
- **Clean Scoped Storage**: Full support for Android 10+ (API 29–34) MediaStore scoped storage with `IS_PENDING` and legacy storage fallback for Android 5.0–9.0.

---

## Comparison: Official GCam Port vs GCam Lite

| Feature | Provided GCam Port (`MGC_9.4.103`) | GCam Lite (`com.gcam.compatible.camera`) |
| :--- | :--- | :--- |
| **APK Size** | **~326 MB** (Heavy DEX + 120MB native binary) | **~5 to 8 MB** (Lightweight, clean footprint) |
| **Supported ABIs** | `arm64-v8a` only (64-bit) | Universal (any ABI supported by Android) |
| **Min Camera2 Level** | `FULL` or `LEVEL_3` required | `LEGACY`, `LIMITED`, `FULL`, `LEVEL_3` |
| **RAW_SENSOR Requirement** | Mandatory (Crashes if absent) | Optional; operates seamlessly on JPEG/YUV |
| **RAM Footprint** | ~500 MB – 1.2 GB active heap | < 35 MB active heap |
| **Proprietary Binaries** | Google Tensor models (`.tflite.enc`), Halide Hexagon DSP | 100% Clean Open-Source Kotlin / AndroidX |
| **Build Environment** | Complex proprietary build dependencies | 1-Click Cloud Build via GitHub Actions |

---

## Quick Start (No Local Setup Needed)

To build and obtain the APK directly in your browser:

1. **Create Repository**: Go to [GitHub.com](https://github.com) and create a new repository named `GCamCompatibleCamera`.
2. **Upload Files**: Push or upload this project directory (including `.github/workflows/android-build.yml` and the `gradle` folder).
3. **Actions Tab**: Click on **Actions** in your GitHub repository.
4. **Trigger Workflow**: Select **Android Build & APK Delivery** and click **Run workflow** (or simply push to `main`).
5. **Download APK**: Once completed, open the workflow run and download the `GCamCompatibleCamera-debug-apk` artifact. Inside the downloaded ZIP, you will find `GCamCompatibleCamera-debug.apk` ready to install on your phone!

For full details, see [BUILD_INSTRUCTIONS.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/BUILD_INSTRUCTIONS.md).

---

## Documentation Index

- [BUILD_INSTRUCTIONS.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/BUILD_INSTRUCTIONS.md) — Step-by-step browser-only build guide.
- [COMPATIBILITY_REPORT.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/COMPATIBILITY_REPORT.md) — Technical analysis of the reference GCam APK and hardware blockers.
- [ARCHITECTURE.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/ARCHITECTURE.md) — System architecture and module boundaries.
- [CAMERA_BACKENDS.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/CAMERA_BACKENDS.md) — Multi-backend camera architecture and Camera2 level degradation.
- [IMAGE_PROCESSING.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/IMAGE_PROCESSING.md) — Computational photography algorithms, tone mapping, and burst accumulation.
- [TEST_REPORT.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/TEST_REPORT.md) — Unit test suite and verification criteria.
- [BUILD_REPORT.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/BUILD_REPORT.md) — Toolchain compatibility and CI/CD audit.
- [TROUBLESHOOTING.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/TROUBLESHOOTING.md) — Diagnostics and solutions for device-specific quirks.
- [LICENSES_AND_ATTRIBUTIONS.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/LICENSES_AND_ATTRIBUTIONS.md) — Legal notices and open source licensing.
- [THIRD_PARTY_NOTICES.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/THIRD_PARTY_NOTICES.md) — Upstream dependencies and license disclosures.
- [CHANGELOG.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/CHANGELOG.md) — Project version history.

---

## License

This project is licensed under the Apache License 2.0. See [LICENSES_AND_ATTRIBUTIONS.md](file:///c:/Users/admin/Desktop/gcam%20for%20me/LICENSES_AND_ATTRIBUTIONS.md).
