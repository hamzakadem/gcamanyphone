package com.gcam.compatible.camera

import com.gcam.compatible.camera.data.ImageProcessingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageProcessingProfileTest {

    @Test
    fun testProfileCycle() {
        var profile = ImageProcessingProfile.FAST_SINGLE_FRAME
        profile = profile.next()
        assertEquals(ImageProcessingProfile.BALANCED_HDR, profile)

        profile = profile.next()
        assertEquals(ImageProcessingProfile.NIGHT_ENHANCED, profile)

        profile = profile.next()
        assertEquals(ImageProcessingProfile.ASTROPHOTOGRAPHY, profile)

        profile = profile.next()
        assertEquals(ImageProcessingProfile.FAST_SINGLE_FRAME, profile)
    }

    @Test
    fun testNightMode15Frames() {
        assertEquals("Night mode must merge 15 frames to eliminate noise", 15, ImageProcessingProfile.NIGHT_ENHANCED.burstFrameCount)
        assertTrue(ImageProcessingProfile.NIGHT_ENHANCED.shadowBoostFactor >= 2.0f)
    }

    @Test
    fun testAstrophotographyParameters() {
        val astro = ImageProcessingProfile.ASTROPHOTOGRAPHY
        assertEquals("Astrophotography must capture 10 frames", 10, astro.burstFrameCount)
        assertTrue("Astrophotography must lock focus to infinity", astro.useInfiniteFocus)
        assertTrue("Astrophotography must use maximum shutter / exposure", astro.useMaxShutter)
        assertTrue("Astrophotography flag must be enabled", astro.isAstrophotography)
    }
}
