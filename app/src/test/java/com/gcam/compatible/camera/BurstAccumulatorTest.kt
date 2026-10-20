package com.gcam.compatible.camera

import android.graphics.Bitmap
import com.gcam.compatible.camera.processing.BurstAccumulator
import org.junit.Assert.assertEquals
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

        // Frame 1: all pixels 100 in R, G, B
        val b1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels1 = IntArray(4) { (0xFF shl 24) or (100 shl 16) or (100 shl 8) or 100 }
        b1.setPixels(pixels1, 0, width, 0, 0, width, height)

        // Frame 2: all pixels 200 in R, G, B
        val b2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels2 = IntArray(4) { (0xFF shl 24) or (200 shl 16) or (200 shl 8) or 200 }
        b2.setPixels(pixels2, 0, width, 0, 0, width, height)

        val merged = accumulator.accumulate(listOf(b1, b2))
        val mergedPixels = IntArray(4)
        merged.getPixels(mergedPixels, 0, width, 0, 0, width, height)

        val r = (mergedPixels[0] shr 16) and 0xFF
        val g = (mergedPixels[0] shr 8) and 0xFF
        val b = mergedPixels[0] and 0xFF

        // Average of 100 and 200 is 150
        assertEquals(150, r)
        assertEquals(150, g)
        assertEquals(150, b)
    }
}
