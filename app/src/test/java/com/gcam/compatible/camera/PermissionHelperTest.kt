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
    fun testRequiredPermissionsContainCamera() {
        val permissions = PermissionHelper.getRequiredPermissions()
        assertTrue(permissions.contains(Manifest.permission.CAMERA))
    }
}
