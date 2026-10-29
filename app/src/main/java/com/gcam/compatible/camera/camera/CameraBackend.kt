package com.gcam.compatible.camera.camera

import android.view.MotionEvent
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.gcam.compatible.camera.data.CameraCapabilities
import com.gcam.compatible.camera.data.ImageProcessingProfile
import java.io.File

/**
 * Capture operational mode: Still Photo or Video Recording.
 */
enum class CameraMode {
    PHOTO,
    VIDEO
}

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

    fun takePictureBurst(
        profile: ImageProcessingProfile,
        customFrameCount: Int?,
        onCaptureProgress: (current: Int, total: Int) -> Unit,
        onBurstComplete: (List<ByteArray>, Int) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        takePictureBurst(profile, onCaptureProgress, onBurstComplete, onError)
    }

    fun focusOnPoint(previewView: PreviewView, event: MotionEvent): Boolean

    // Video & Pro Manual Controls
    fun setCameraMode(
        mode: CameraMode,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onSwitched: ((CameraCapabilities) -> Unit)? = null
    ) {}

    fun getCameraMode(): CameraMode = CameraMode.PHOTO

    fun startRecording(
        outputFile: File,
        onVideoRecordEvent: (VideoRecordEvent) -> Unit
    ) {}

    fun stopRecording() {}

    fun isRecording(): Boolean = false

    fun setManualIso(iso: Int?) {}

    fun setManualShutterSpeed(nanos: Long?) {}

    fun setManualWhiteBalance(kelvin: Int?) {}

    fun setExposureCompensation(index: Int) {}
}
