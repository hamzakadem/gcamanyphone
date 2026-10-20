# Test & Verification Report

## 1. Overview

This document details the testing architecture, automated test suites, and physical verification methodology established for **GCam Lite** (`com.gcam.compatible.camera`).

---

## 2. Testing Matrix

| Test Suite | Target Component | Execution Environment | Status |
| :--- | :--- | :--- | :--- |
| **`CameraHardwareLevelTest`** | `CameraHardwareLevel.fromInt()` & defaults | Local JVM / GitHub Actions | **Verified** |
| **`MemoryGuardTest`** | InSampleSize calculation & burst count clamps | Robolectric SDK 33 / GitHub Actions | **Verified** |
| **`AdaptiveToneMapperTest`** | LUT boundary limits, monotonicity, luminance estimation | Local JVM / GitHub Actions | **Verified** |
| **`BurstAccumulatorTest`** | Frame accumulation math & dimension handling | Robolectric SDK 33 / GitHub Actions | **Verified** |
| **`ImageProcessingProfileTest`** | Profile progression, parameters, cycle integrity | Local JVM / GitHub Actions | **Verified** |
| **`PermissionHelperTest`** | Permission set consistency across API tiers | Robolectric SDK 33 / GitHub Actions | **Verified** |
| **`CaptureResultDataTest`** | Success / failure state evaluation | Robolectric SDK 33 / GitHub Actions | **Verified** |

---

## 3. Detailed Test Specifications

### 3.1 Tone Mapping Monotonicity & Boundary Invariants
- **Objective**: Ensure `buildToneCurveLut()` never inverts pixel gradients (which would cause unsightly visual solarization or posterization) and strictly restricts values to the valid $[0, 255]$ range.
- **Assertion**: For all $i \in [1, 255]$, $\text{LUT}[i] \ge \text{LUT}[i-1]$.

### 3.2 Memory Guard Heap Calculations
- **Objective**: Ensure that large bitmap dimensions ($8000 \times 6000$) on memory-constrained systems result in an `inSampleSize \ge 2`, preventing crash loops.
- **Assertion**: `calculateInSampleSize()` returns a power of two ensuring image dimensions stay within safe processing bounds.

### 3.3 Burst Accumulation Math
- **Objective**: Verify that combining frame $A$ (value 100) and frame $B$ (value 200) yields an exact average of 150 without precision drift or byte overflow.
- **Assertion**: `accumulate([b1, b2])` returns 150 for all RGB color channels.

---

## 4. Test Execution in GitHub Actions

The cloud build workflow automatically executes all unit and Robolectric tests on clean Ubuntu runners via:

```bash
./gradlew testDebugUnitTest --continue --stacktrace
```

The resulting XML and HTML test reports are preserved and uploaded as the workflow artifact **`GCamCompatibleCamera-test-reports`** for developer review.

---

## 5. Physical Device Verification Criteria

Physical testing should be conducted across the following test configurations:

1. **Ultra-Low-End Android Device (1GB–2GB RAM, Android Go, Camera2 `LEGACY` / `LIMITED`)**:
   - Verify app launch without `OutOfMemoryError`.
   - Verify camera preview starts smoothly.
   - Verify photo capture in `Fast Single` mode completes in $< 1$ second.
   - Verify photo is stored in the gallery and viewable.
2. **Mid-Range Android Device (4GB–6GB RAM, Camera2 `FULL`)**:
   - Verify `HDR+ Lite` 3-frame capture.
   - Verify dynamic range enhancement in backlit scenes (bright sky + dark foreground).
   - Verify tap-to-focus and metering accuracy.
3. **Modern Flagship Android Device (8GB+ RAM, Android 13/14, Camera2 `LEVEL_3`)**:
   - Verify `Night Lite` 5-frame exposure blending.
   - Verify front/rear camera switching and zoom.
   - Verify scoped storage compliance under Android 14 restrictions.
