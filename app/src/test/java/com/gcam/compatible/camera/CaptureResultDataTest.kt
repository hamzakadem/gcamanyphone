package com.gcam.compatible.camera

import android.net.Uri
import com.gcam.compatible.camera.data.CaptureResultData
import com.gcam.compatible.camera.data.ImageProcessingProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CaptureResultDataTest {

    @Test
    fun testSuccessState() {
        val result = CaptureResultData(
            uri = Uri.parse("content://media/external/images/media/1"),
            filePath = "/sdcard/DCIM/GCamLite/IMG_1.jpg",
            width = 1920,
            height = 1080,
            processingTimeMs = 120,
            profileUsed = ImageProcessingProfile.BALANCED_HDR,
            framesMerged = 3,
            errorMessage = null
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFailureState() {
        val result = CaptureResultData(
            uri = null,
            filePath = null,
            width = 0,
            height = 0,
            processingTimeMs = 10,
            profileUsed = ImageProcessingProfile.FAST_SINGLE_FRAME,
            framesMerged = 0,
            errorMessage = "Camera device closed"
        )
        assertFalse(result.isSuccess)
    }
}
