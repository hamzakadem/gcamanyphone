package com.gcam.compatible.camera

import android.hardware.camera2.CameraCharacteristics
import com.gcam.compatible.camera.data.CameraHardwareLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraHardwareLevelTest {

    @Test
    fun testFromIntMapping() {
        assertEquals(
            CameraHardwareLevel.LEGACY,
            CameraHardwareLevel.fromInt(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        )
        assertEquals(
            CameraHardwareLevel.LIMITED,
            CameraHardwareLevel.fromInt(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        )
        assertEquals(
            CameraHardwareLevel.FULL,
            CameraHardwareLevel.fromInt(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        )
        assertEquals(
            CameraHardwareLevel.LEVEL_3,
            CameraHardwareLevel.fromInt(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        )
        assertEquals(
            CameraHardwareLevel.EXTERNAL,
            CameraHardwareLevel.fromInt(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL)
        )
        assertEquals(
            CameraHardwareLevel.UNKNOWN,
            CameraHardwareLevel.fromInt(-1)
        )
        assertEquals(
            CameraHardwareLevel.UNKNOWN,
            CameraHardwareLevel.fromInt(null)
        )
    }

    @Test
    fun testRawSupportDefaults() {
        assertFalse(CameraHardwareLevel.LEGACY.rawSupportedByDefault)
        assertFalse(CameraHardwareLevel.LIMITED.rawSupportedByDefault)
        assertTrue(CameraHardwareLevel.FULL.rawSupportedByDefault)
        assertTrue(CameraHardwareLevel.LEVEL_3.rawSupportedByDefault)
        assertFalse(CameraHardwareLevel.UNKNOWN.rawSupportedByDefault)
    }
}
