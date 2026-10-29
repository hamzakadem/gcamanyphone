package com.gcam.compatible.camera

import android.graphics.Bitmap
import com.gcam.compatible.camera.processing.BurstAccumulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BurstAccumulatorTest {

    private val accumulator = BurstAccumulator()

    @Test
    fun testSingleFrameReturnsSameBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val result = accumulator.accumulate(listOf(bitmap))
        assertEquals(bitmap, result)
    }

    @Test
    fun testTwoFramesAverageCorrectly() {
        val width = 2
        val height = 2

        val b1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels1 = IntArray(4) { (0xFF shl 24) or (100 shl 16) or (100 shl 8) or 100 }
        b1.setPixels(pixels1, 0, width, 0, 0, width, height)

        val b2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels2 = IntArray(4) { (0xFF shl 24) or (200 shl 16) or (200 shl 8) or 200 }
        b2.setPixels(pixels2, 0, width, 0, 0, width, height)

        val merged = accumulator.accumulate(listOf(b1, b2))
        val mergedPixels = IntArray(4)
        merged.getPixels(mergedPixels, 0, width, 0, 0, width, height)

        val r = (mergedPixels[0] shr 16) and 0xFF
        val g = (mergedPixels[0] shr 8) and 0xFF
        val b = mergedPixels[0] and 0xFF

        assertEquals(150, r)
        assertEquals(150, g)
        assertEquals(150, b)
    }

    @Test
    fun testIncrementalBurstAccumulator15Frames() {
        val width = 2
        val height = 2
        val incAccumulator = BurstAccumulator.IncrementalBurstAccumulator(width, height)

        // Add 15 frames with pixel value 150 each
        for (i in 1..15) {
            val frame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(4) { (0xFF shl 24) or (150 shl 16) or (150 shl 8) or 150 }
            frame.setPixels(pixels, 0, width, 0, 0, width, height)
            incAccumulator.addFrame(frame, autoRecycle = true)
        }

        assertEquals(15, incAccumulator.frameCount)

        val merged = incAccumulator.buildResult()
        val mergedPixels = IntArray(4)
        merged.getPixels(mergedPixels, 0, width, 0, 0, width, height)

        val r = (mergedPixels[0] shr 16) and 0xFF
        assertEquals("Averaging 15 frames of 150 must equal 150", 150, r)
    }

    @Test
    fun testIncrementalBurstAccumulator20FramesNoOOM() {
        val width = 4
        val height = 4
        val incAccumulator = BurstAccumulator.IncrementalBurstAccumulator(width, height, isFakeRaw = true)

        // Add 20 frames - must succeed without memory overflow or arithmetic overflow
        for (i in 1..20) {
            val frame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height) { (0xFF shl 24) or (120 shl 16) or (120 shl 8) or 120 }
            frame.setPixels(pixels, 0, width, 0, 0, width, height)
            incAccumulator.addFrame(frame, autoRecycle = true)
        }

        assertEquals(20, incAccumulator.frameCount)
        val result = incAccumulator.buildResult()
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        val r = (pixels[0] shr 16) and 0xFF
        assertTrue("Resulting color channel must be valid 0..255", r in 100..140)
    }
}
