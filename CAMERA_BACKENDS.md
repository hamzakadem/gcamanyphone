# Camera Backends & Hardware Compatibility

## 1. Camera2 Hardware Support Levels

Android Camera2 classifies device hardware implementations into 4 standardized levels:

| Level | Characteristics & Low-End Implications | Official GCam Behavior | GCam Lite Behavior |
| :--- | :--- | :--- | :--- |
| **`LEGACY`** | Camera1 emulation layer on top of old HAL. No per-frame controls, no manual exposure, no RAW. Found on budget MediaTek/Unisoc and older phones. | **Crashes immediately** (`IllegalArgumentException` or native crash in `libgcastartup.so`). | **Fully Supported**. Gracefully delivers preview, auto-exposure, auto-focus, and high-quality JPEG capture. |
| **`LIMITED`** | Supports basic Camera2 features, but cannot handle high-speed bursts or simultaneous RAW + YUV streams. | **Fails or freezes** during multi-frame HDR capture. | **Fully Supported**. Uses single or balanced frame bursts tailored to limited HAL bandwidth. |
| **`FULL`** | Guaranteed 30 FPS at full sensor resolution, per-frame manual controls, RAW sensor streams, post-raw sensitivity boost. | Works if device ID matches Google whitelist. | **Fully Supported**. Unlocks full computational pipeline. |
| **`LEVEL_3`** | Premium level with YUV re-processing, RAW sensor re-processing, and additional physical camera streams (Pixel, Galaxy S, Xiaomi Ultra). | Supported. | **Fully Supported**. High-speed capture and advanced processing active. |

---

## 2. Why CameraX Was Chosen Over Raw Camera2

Writing raw Camera2 code requires managing `CameraDevice`, `CameraCaptureSession`, `CaptureRequest`, `ImageReader`, and dozens of threading primitives manually. More importantly, hundreds of Android devices exhibit vendor-specific HAL bugs, including:
- Inverted preview sensors on front-facing cameras.
- Aspect ratio distortion when preview and capture aspect ratios mismatch.
- Failure to close capture sessions properly on app pause, locking the camera hardware until reboot.
- Premature memory exhaustion when allocating multiple YUV surfaces.

**GCam Lite utilizes AndroidX CameraX**, which includes the Google Android team's comprehensive device quirk database (`androidx.camera.camera2.internal.compat.quirk`). It automatically resolves:
- Surface texture buffer formats.
- Safe preview-to-sensor aspect ratio matching.
- Automatic session re-establishment on camera disconnection.

---

## 3. Runtime Capability Probing

`CameraCapabilityDetector` inspects the device at runtime to detect:

```kotlin
val chars = cameraManager.getCameraCharacteristics(id)
val level = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
val rawSupported = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)
val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
```

The application uses these findings to:
1. Dynamically render the hardware level badge in the UI (e.g. `Camera2: LIMITED | GCam Lite (JPEG/YUV)`).
2. Recommend the safe processing profile (`Fast Single` on memory-constrained devices).
3. Populate the **Hardware Capabilities** diagnostic report inside the Settings modal.
