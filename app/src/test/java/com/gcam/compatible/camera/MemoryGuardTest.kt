package com.gcam.compatible.camera

import android.graphics.BitmapFactory
import com.gcam.compatible.camera.processing.MemoryGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MemoryGuardTest {

    private val memoryGuard = MemoryGuard()

    @Test
    fun testCalculateInSampleSizeForSmallImage() {
        val options = BitmapFactory.Options().apply {
            outWidth = 800
            outHeight = 600
        }
        val sampleSize = memoryGuard.calculateInSampleSize(options, 1920)
        assertTrue(sampleSize >= 1)
    }

    @Test
    fun testCalculateInSampleSizeForLargeImage() {
        val options = BitmapFactory.Options().apply {
            outWidth = 8000
            outHeight = 6000
        }
        val sampleSize = memoryGuard.calculateInSampleSize(options, 2000)
        assertTrue("Sample size should scale down large images", sampleSize >= 2)
    }

    @Test
    fun testGetSafeBurstCount() {
        val requested = 5
        val safe = memoryGuard.getSafeBurstCount(requested)
        assertTrue("Safe burst must be at least 1", safe >= 1)
        assertTrue("Safe burst must not exceed requested frames", safe <= requested)
    }
}
