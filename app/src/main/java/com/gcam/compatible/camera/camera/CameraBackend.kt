package com.gcam.compatible.camera.camera

import android.view.MotionEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.gcam.compatible.camera.data.CameraCapabilities
import com.gcam.compatible.camera.data.ImageProcessingProfile

/**
 * Common contract for camera implementations.
 */
interface CameraBackend {
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onInitialized: (CameraCapabilities) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun stopCamera()

    fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onSwitched: (CameraCapabilities) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun toggleFlash(): Int

    fun getFlashMode(): Int

    fun setTargetResolutionTier(tier: Int)

    fun applyProfileHardwareSettings(profile: ImageProcessingProfile)

    fun takePictureBurst(
        profile: ImageProcessingProfile,
        onCaptureProgress: (current: Int, total: Int) -> Unit,
        onBurstComplete: (List<ByteArray>, Int) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun focusOnPoint(previewView: PreviewView, event: MotionEvent): Boolean
}
