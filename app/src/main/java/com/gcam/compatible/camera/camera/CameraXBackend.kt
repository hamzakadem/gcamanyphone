package com.gcam.compatible.camera.camera

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.gcam.compatible.camera.data.CameraCapabilities
import com.gcam.compatible.camera.data.CameraHardwareLevel
import com.gcam.compatible.camera.data.ImageProcessingProfile
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX implementation of CameraBackend. Provides resilient multi-vendor support,
 * vendor quirk workarounds, and seamless operation across Camera2 LEGACY, LIMITED, and FULL devices.
 */
class CameraXBackend(private val context: Context) : CameraBackend {

    companion object {
        private const val TAG = "CameraXBackend"
        const val RESOLUTION_TIER_MAX = 0
        const val RESOLUTION_TIER_BALANCED = 1
        const val RESOLUTION_TIER_LOW = 2
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var flashMode: Int = ImageCapture.FLASH_MODE_AUTO
    private var resolutionTier: Int = RESOLUTION_TIER_MAX

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val detector = CameraCapabilityDetector(context)
    private var currentCapabilities: CameraCapabilities? = null

    override fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onInitialized: (CameraCapabilities) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                val caps = detector.getPrimaryCameraCapabilities()
                currentCapabilities = caps
                onInitialized(caps)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraX provider", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w(TAG, "Error unbinding camera provider", e)
        }
    }

    override fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onSwitched: (CameraCapabilities) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        try {
            bindCameraUseCases(lifecycleOwner, previewView)
            val allCaps = detector.probeAllCameras()
            val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
            val caps = allCaps.values.firstOrNull { it.isFrontFacing == isFront }
                ?: detector.getPrimaryCameraCapabilities()
            currentCapabilities = caps
            onSwitched(caps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch camera lens", e)
            onError(e)
        }
    }

    override fun toggleFlash(): Int {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        imageCapture?.flashMode = flashMode
        return flashMode
    }

    override fun getFlashMode(): Int = flashMode

    override fun setTargetResolutionTier(tier: Int) {
        resolutionTier = tier
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val provider = cameraProvider ?: return

        provider.unbindAll()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val captureBuilder = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

        // Apply resolution constraint if requested for low-end / low RAM tier
        when (resolutionTier) {
            RESOLUTION_TIER_BALANCED -> {
                captureBuilder.setTargetResolution(Size(1920, 1080))
            }
            RESOLUTION_TIER_LOW -> {
                captureBuilder.setTargetResolution(Size(1280, 720))
            }
            else -> {
                // Default: let CameraX select max resolution
            }
        }

        imageCapture = captureBuilder.build()

        camera = provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
    }

    override fun takePicture(
        profile: ImageProcessingProfile,
        onProcessingStarted: () -> Unit,
        onPictureCaptured: (ByteArray, Int) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            onError(IllegalStateException("Camera capture is not initialized"))
            return
        }

        onProcessingStarted()

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val rotation = image.imageInfo.rotationDegrees
                        val bytes = imageProxyToByteArray(image)
                        image.close()
                        ContextCompat.getMainExecutor(context).execute {
                            onPictureCaptured(bytes, rotation)
                        }
                    } catch (e: Exception) {
                        image.close()
                        ContextCompat.getMainExecutor(context).execute {
                            onError(e)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exception)
                    }
                }
            }
        )
    }

    override fun focusOnPoint(previewView: PreviewView, event: MotionEvent): Boolean {
        val cam = camera ?: return false
        val factory = SurfaceOrientedMeteringPointFactory(
            previewView.width.toFloat(),
            previewView.height.toFloat()
        )
        val point = factory.createPoint(event.x, event.y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
        return true
    }

    private fun imageProxyToByteArray(image: ImageProxy): ByteArray {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }
}
