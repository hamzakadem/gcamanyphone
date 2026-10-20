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
        assertEquals(ImageProcessingProfile.FAST_SINGLE_FRAME, profile)
    }

    @Test
    fun testProfileParameters() {
        // FAST_SINGLE_FRAME
        assertEquals(1, ImageProcessingProfile.FAST_SINGLE_FRAME.burstFrameCount)
        assertTrue(ImageProcessingProfile.FAST_SINGLE_FRAME.shadowBoostFactor > 1.0f)

        // BALANCED_HDR
        assertEquals(3, ImageProcessingProfile.BALANCED_HDR.burstFrameCount)
        assertTrue(ImageProcessingProfile.BALANCED_HDR.shadowBoostFactor > ImageProcessingProfile.FAST_SINGLE_FRAME.shadowBoostFactor)

        // NIGHT_ENHANCED
        assertEquals(5, ImageProcessingProfile.NIGHT_ENHANCED.burstFrameCount)
        assertTrue(ImageProcessingProfile.NIGHT_ENHANCED.shadowBoostFactor > ImageProcessingProfile.BALANCED_HDR.shadowBoostFactor)
    }
}
