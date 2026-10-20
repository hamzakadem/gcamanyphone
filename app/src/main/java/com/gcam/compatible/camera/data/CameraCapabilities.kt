package com.gcam.compatible.camera.data

import android.util.Size

/**
 * Data model encapsulating the probed hardware characteristics of a camera sensor.
 */
data class CameraCapabilities(
    val cameraId: String,
    val isFrontFacing: Boolean,
    val hardwareLevel: CameraHardwareLevel,
    val supportsRawSensor: Boolean,
    val hasFlash: Boolean,
    val maxResolution: Size?,
    val supportedPreviewResolutions: List<Size>,
    val supportedCaptureResolutions: List<Size>,
    val isLowEndDevice: Boolean
) {
    fun toDiagnosticString(): String {
        val facing = if (isFrontFacing) "Front" else "Rear"
        val raw = if (supportsRawSensor) "Supported" else "Not Supported (JPEG fallback)"
        val flash = if (hasFlash) "Available" else "None"
        val maxRes = maxResolution?.let { "${it.width}x${it.height}" } ?: "Unknown"
        return """
            Camera: ID $cameraId ($facing)
            Level: ${hardwareLevel.displayName}
            RAW Capture: $raw
            Flash: $flash
            Max Resolution: $maxRes
            Low-End Tier: ${if (isLowEndDevice) "Yes (Optimized)" else "Standard"}
        """.trimIndent()
    }
}
