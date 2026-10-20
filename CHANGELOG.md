# Changelog

All notable changes to **GCam Lite** will be documented in this file.

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
