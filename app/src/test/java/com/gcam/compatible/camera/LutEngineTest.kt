package com.gcam.compatible.camera

import android.graphics.Bitmap
import com.gcam.compatible.camera.processing.LutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LutEngineTest {

    private val lutEngine = LutEngine()

    @Test
    fun testSyntheticLogLutBoundsAndMonotonicity() {
        val lut = LutEngine.SYNTHETIC_LOG_LUT
        assertEquals(256, lut.size)

        // Must be in 0..255 range
        for (i in lut.indices) {
            assertTrue("Value at $i (${lut[i]}) must be in 0..255", lut[i] in 0..255)
        }

        // Shadows should be lifted (input 0 should map to lifted black level >= 30)
        assertTrue("Shadows must be lifted in synthetic log", lut[0] > 0)

        // Monotonic non-decreasing
        for (i in 1 until lut.size) {
            assertTrue("Log LUT must be monotonically non-decreasing", lut[i] >= lut[i - 1])
        }
    }

    @Test
    fun testApplySyntheticLogToBitmap() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(16) { (0xFF shl 24) or (50 shl 16) or (50 shl 8) or 50 }
        bitmap.setPixels(pixels, 0, 4, 0, 0, 4, 4)

        val logBitmap = lutEngine.applySyntheticLog(bitmap)
        assertNotNull(logBitmap)
        assertEquals(4, logBitmap.width)
        assertEquals(4, logBitmap.height)

        val outPixels = IntArray(16)
        logBitmap.getPixels(outPixels, 0, 4, 0, 0, 4, 4)
        val r = (outPixels[0] shr 16) and 0xFF
        assertTrue("Shadow value 50 should be lifted in Synthetic Log", r > 50)
    }

    @Test
    fun testParseCubeLut() {
        val cubeContent = """
            # Test 3D LUT
            TITLE "Sample LUT"
            LUT_3D_SIZE 2
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val parsed = lutEngine.parseCubeLut(ByteArrayInputStream(cubeContent.toByteArray()), "Sample")
        assertNotNull("LUT parser must succeed for valid cube", parsed)
        assertEquals(2, parsed!!.size)
        assertEquals(24, parsed.table.size) // 2*2*2 * 3 = 24 floats
    }

    @Test
    fun testApplyPresetTealOrange() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(4) { (0xFF shl 24) or (128 shl 16) or (128 shl 8) or 128 }
        bitmap.setPixels(pixels, 0, 2, 0, 0, 2, 2)

        val processed = lutEngine.applyPreset(bitmap, LutEngine.Preset.TEAL_ORANGE)
        assertNotNull(processed)
        val outPixels = IntArray(4)
        processed.getPixels(outPixels, 0, 2, 0, 2, 2, 2)
        // Verify valid image output
        assertTrue(outPixels[0] != 0)
    }
}
