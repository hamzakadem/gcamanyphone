# Changelog

All notable changes to **GCam Lite** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-10-20

### Added
- **Complete Kotlin Project**: Initial standalone implementation of GCam Lite (`com.gcam.compatible.camera`).
- **Resilient CameraX Backend**: Support across Camera2 `LEGACY`, `LIMITED`, `FULL`, and `LEVEL_3` hardware levels.
- **Computational Photography Engine**:
  - `AdaptiveToneMapper`: Look-Up Table (LUT) driven luminance mapping with Reinhard highlight roll-off and shadow lifting.
  - `BurstAccumulator`: Multi-frame exposure temporal averaging for noise suppression.
  - `EdgePreservingDenoise`: Spatial bilateral filtering and unsharp masking.
  - `MemoryGuard`: Proactive runtime heap monitoring to eliminate OOM crashes on low-end devices.
- **Camera Features**:
  - Live viewfinder with tap-to-focus and metering animation.
  - Flash modes (Auto, On, Off, Torch).
  - Front / rear camera switching with animated rotation.
  - Hardware capability readout badge and detailed settings dialog.
  - Photo review thumbnail opening the system gallery viewer.
- **Modern Storage Integration**:
  - MediaStore Scoped Storage support for Android 10+ (API 29–34) with `IS_PENDING`.
  - Legacy DCIM direct storage fallback for Android 5.0–9.0 (API 21–28).
  - EXIF software metadata tagging.
- **CI/CD Cloud Build**:
  - GitHub Actions workflow (`.github/workflows/android-build.yml`).
  - Automated JDK 17, Android SDK 34, and Gradle 8.7 setup.
  - Unit tests and Android lint execution.
  - Automatic debug APK compilation, verification, SHA-256 checksum generation, source ZIP packaging, and artifact uploading.
  - Tag-triggered GitHub Releases publication.
- **Comprehensive Documentation**: Complete architecture, reverse engineering compatibility report, troubleshooting guide, test reports, and licenses.
