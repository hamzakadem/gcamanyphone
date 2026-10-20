package com.gcam.compatible.camera.data

import android.hardware.camera2.CameraCharacteristics

/**
 * Enumerates Camera2 hardware support levels with user-friendly descriptions.
 */
enum class CameraHardwareLevel(val displayName: String, val rawSupportedByDefault: Boolean) {
    LEGACY("LEGACY", false),
    LIMITED("LIMITED", false),
    FULL("FULL", true),
    LEVEL_3("LEVEL_3", true),
    EXTERNAL("EXTERNAL", false),
    UNKNOWN("UNKNOWN", false);

    companion object {
        fun fromInt(level: Int?): CameraHardwareLevel {
            return when (level) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> LEGACY
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> LIMITED
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> FULL
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> LEVEL_3
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> EXTERNAL
                else -> UNKNOWN
            }
        }
    }
}
