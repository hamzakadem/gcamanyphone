package com.gcam.compatible.camera

import android.Manifest
import com.gcam.compatible.camera.utils.PermissionHelper
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PermissionHelperTest {

    @Test
    fun testRequiredPermissionsContainCameraAndAudio() {
        val permissions = PermissionHelper.getRequiredPermissions()
        assertTrue("Must require CAMERA permission", permissions.contains(Manifest.permission.CAMERA))
        assertTrue("Must require RECORD_AUDIO permission for video", permissions.contains(Manifest.permission.RECORD_AUDIO))
    }
}
