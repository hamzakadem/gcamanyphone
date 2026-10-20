package com.gcam.compatible.camera.camera

import android.app.ActivityManager
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Log
import android.util.Size
import com.gcam.compatible.camera.data.CameraCapabilities
import com.gcam.compatible.camera.data.CameraHardwareLevel

/**
 * Utility responsible for probing device camera hardware capabilities at runtime.
 */
class CameraCapabilityDetector(private val context: Context) {

    companion object {
        private const val TAG = "CameraCapabilityDetect"
    }

    /**
     * Determines whether the current device is considered a low-end device
     * based on RAM size and system low-RAM flags.
     */
    fun isLowEndDevice(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager?.isLowRamDevice == true) {
                return true
            }
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            // Less than 3GB total RAM is classified as memory-constrained
            val totalRamGb = memoryInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
            totalRamGb < 3.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inspect system memory; defaulting to low-end safeguards", e)
            true
        }
    }

    /**
     * Probes all available cameras on the device and returns a map of camera ID to capabilities.
     */
    fun probeAllCameras(): Map<String, CameraCapabilities> {
        val result = mutableMapOf<String, CameraCapabilities>()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return result

        val lowEnd = isLowEndDevice()

        try {
            for (id in cameraManager.cameraIdList) {
                try {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    val isFront = facing == CameraCharacteristics.LENS_FACING_FRONT

                    val levelInt = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hwLevel = CameraHardwareLevel.fromInt(levelInt)

                    // Check RAW_SENSOR capability
                    val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                    val rawSupported = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)

                    val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

                    // Resolutions
                    val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val captureSizes = map?.getOutputSizes(ImageFormat.JPEG)?.toList() ?: emptyList()
                    val previewSizes = map?.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.toList() ?: emptyList()
                    val maxRes = captureSizes.maxByOrNull { it.width * it.height }

                    val caps = CameraCapabilities(
                        cameraId = id,
                        isFrontFacing = isFront,
                        hardwareLevel = hwLevel,
                        supportsRawSensor = rawSupported,
                        hasFlash = flashAvailable,
                        maxResolution = maxRes,
                        supportedPreviewResolutions = previewSizes,
                        supportedCaptureResolutions = captureSizes,
                        isLowEndDevice = lowEnd
                    )
                    result[id] = caps
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to probe camera $id", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list camera IDs", e)
        }

        return result
    }

    /**
     * Returns the primary rear camera capabilities, or fallback to any available camera.
     */
    fun getPrimaryCameraCapabilities(): CameraCapabilities {
        val all = probeAllCameras()
        val rear = all.values.firstOrNull { !it.isFrontFacing }
        if (rear != null) return rear

        val front = all.values.firstOrNull { it.isFrontFacing }
        if (front != null) return front

        return CameraCapabilities(
            cameraId = "0",
            isFrontFacing = false,
            hardwareLevel = CameraHardwareLevel.UNKNOWN,
            supportsRawSensor = false,
            hasFlash = false,
            maxResolution = Size(1920, 1080),
            supportedPreviewResolutions = emptyList(),
            supportedCaptureResolutions = emptyList(),
            isLowEndDevice = isLowEndDevice()
        )
    }
}
