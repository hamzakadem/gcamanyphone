package com.gcam.compatible.camera

import com.gcam.compatible.camera.processing.AdaptiveToneMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveToneMapperTest {

    private val toneMapper = AdaptiveToneMapper()

    @Test
    fun testBuildToneCurveLutBounds() {
        val lut = toneMapper.buildToneCurveLut(
            shadowBoost = 1.35f,
            highlightCompress = 0.85f,
            averageLuminance = 0.5f
        )
        assertEquals(256, lut.size)
        assertEquals("Black input should remain black", 0, lut[0])
        for (i in lut.indices) {
            assertTrue("LUT values must be within 0..255, index $i was ${lut[i]}", lut[i] in 0..255)
        }
    }

    @Test
    fun testToneCurveMonotonicity() {
        val lut = toneMapper.buildToneCurveLut(
            shadowBoost = 1.5f,
            highlightCompress = 0.8f,
            averageLuminance = 0.4f
        )
        for (i in 1 until lut.size) {
            assertTrue(
                "Tone curve must be non-decreasing: index $i (${lut[i]}) < index ${i - 1} (${lut[i - 1]})",
                lut[i] >= lut[i - 1]
            )
        }
    }

    @Test
    fun testAstroToneCurveLut() {
        val astroLut = toneMapper.buildAstroToneCurveLut()
        assertEquals(256, astroLut.size)
        for (i in astroLut.indices) {
            assertTrue("Astro LUT values must be within 0..255, index $i was ${astroLut[i]}", astroLut[i] in 0..255)
        }
        // Black stays black
        assertEquals(0, astroLut[0])
    }

    @Test
    fun testEstimateAverageLuminance() {
        val blackPixels = IntArray(100) { 0xFF000000.toInt() }
        val blackLum = toneMapper.estimateAverageLuminance(blackPixels)
        assertEquals(0.0f, blackLum, 0.01f)

        val whitePixels = IntArray(100) { 0xFFFFFFFF.toInt() }
        val whiteLum = toneMapper.estimateAverageLuminance(whitePixels)
        assertEquals(1.0f, whiteLum, 0.01f)

        val emptyLum = toneMapper.estimateAverageLuminance(IntArray(0))
        assertEquals(0.5f, emptyLum, 0.01f)
    }
}
