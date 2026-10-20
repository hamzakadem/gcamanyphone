package com.gcam.compatible.camera.camera

import android.view.MotionEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.gcam.compatible.camera.data.CameraCapabilities
import com.gcam.compatible.camera.data.ImageProcessingProfile
import java.io.File

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

    fun toggleFlash(): Int // returns the new flash mode int

    fun getFlashMode(): Int

    fun setTargetResolutionTier(tier: Int) // 0: Max, 1: 12MP/1080p, 2: 720p/Low RAM

    fun takePicture(
        profile: ImageProcessingProfile,
        onProcessingStarted: () -> Unit,
        onPictureCaptured: (ByteArray, Int) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun focusOnPoint(previewView: PreviewView, event: MotionEvent): Boolean
}
