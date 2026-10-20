# Build & Toolchain Audit Report

## 1. Toolchain & Build Environment

The build configuration is engineered to guarantee clean, deterministic cloud builds in GitHub Actions without local dependencies:

| Component | Selected Version | Rationale & Compatibility |
| :--- | :--- | :--- |
| **Gradle** | `8.7` | Official release, fully compatible with JDK 17 and AGP 8.4.1. |
| **Android Gradle Plugin (AGP)** | `8.4.1` | Stable release with full Android 14 (API 34) compilation support. |
| **Java Development Kit (JDK)** | `17` (Eclipse Temurin) | Standard LTS Java release for modern Android builds. |
| **Compile SDK** | `34` (Android 14) | Up-to-date platform APIs and security requirements. |
| **Target SDK** | `34` (Android 14) | Compliant with Google Play and modern Android permission policies. |
| **Min SDK** | `21` (Android 5.0 Lollipop) | Maximum backward compatibility spanning $>99\%$ of active Android devices. |
| **Build Tools** | `34.0.0` | Exact build tools matching compile SDK. |
| **Kotlin** | `1.9.24` | Stable compiler version compatible with AGP 8.4.1. |

---

## 2. Dependency Audit

All dependencies are sourced exclusively from Google Maven (`dl.google.com/dl/android/maven2`) and Maven Central:

- `androidx.core:core-ktx:1.13.1` (AndroidX Kotlin extensions)
- `androidx.appcompat:appcompat:1.6.1` (Backward compatible UI components)
- `com.google.android.material:material:1.12.0` (Material Design components)
- `androidx.constraintlayout:constraintlayout:2.1.4` (Flexible responsive UI layout)
- `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0` (Coroutines lifecycle integration)
- `androidx.camera:camera-core:1.3.3` (CameraX pipeline core)
- `androidx.camera:camera-camera2:1.3.3` (CameraX Camera2 backend implementation)
- `androidx.camera:camera-lifecycle:1.3.3` (CameraX lifecycle binding)
- `androidx.camera:camera-view:1.3.3` (CameraX PreviewView)
- `androidx.exifinterface:exifinterface:1.3.7` (Robust EXIF metadata reader/writer)
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0` (Asynchronous image processing)

---

## 3. Artifact Integrity & Packaging

When the cloud workflow completes, three distinct artifacts are exposed:

1. **`GCamCompatibleCamera-debug-apk`**:
   - Path: `output/GCamCompatibleCamera-debug.apk`
   - Form: Uncompressed Android Package signed with standard debug keystore.
   - Accompanying: `output/SHA256SUMS.txt` cryptographic verification file.
2. **`GCamCompatibleCamera-source`**:
   - Path: `output/GCamCompatibleCamera-Source.zip`
   - Form: Clean archive containing complete source code, build scripts, Gradle wrapper, and documentation.
   - Excludes: Temporary build outputs, `.git` internals, IDE caches, and reference APKs.
3. **`GCamCompatibleCamera-test-reports`**:
   - Path: `app/build/reports/`
   - Contains: JUnit test reports and Android Lint HTML diagnostics.
