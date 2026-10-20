package com.gcam.compatible.camera.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gcam.compatible.camera.R
import com.gcam.compatible.camera.camera.CameraBackend
import com.gcam.compatible.camera.camera.CameraXBackend
import com.gcam.compatible.camera.data.CameraCapabilities
import com.gcam.compatible.camera.data.CameraHardwareLevel
import com.gcam.compatible.camera.data.ImageProcessingProfile
import com.gcam.compatible.camera.databinding.ActivityMainBinding
import com.gcam.compatible.camera.databinding.DialogSettingsBinding
import com.gcam.compatible.camera.processing.ImageProcessor
import com.gcam.compatible.camera.storage.MediaSaver
import com.gcam.compatible.camera.utils.PermissionHelper
import kotlinx.coroutines.launch

/**
 * Main Camera Activity providing the viewfinder, interactive controls,
 * adaptive computational photography pipeline, and photo review thumbnail.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraBackend: CameraBackend
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var mediaSaver: MediaSaver

    private var activeProfile: ImageProcessingProfile = ImageProcessingProfile.BALANCED_HDR
    private var lastCapturedUri: Uri? = null
    private var currentCapabilities: CameraCapabilities? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            setupCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraBackend = CameraXBackend(this)
        imageProcessor = ImageProcessor()
        mediaSaver = MediaSaver(this)

        setupUI()

        if (PermissionHelper.areAllRequiredPermissionsGranted(this)) {
            setupCamera()
        } else {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
        }
    }

    private fun setupUI() {
        // Shutter Button
        binding.btnShutter.setOnClickListener {
            triggerCapture()
        }

        // Lens Switch Button
        binding.btnSwitchCamera.setOnClickListener {
            binding.btnSwitchCamera.animate().rotationBy(180f).setDuration(300).start()
            cameraBackend.switchCamera(
                lifecycleOwner = this,
                previewView = binding.viewFinder,
                onSwitched = { caps -> updateCapabilityBadge(caps) },
                onError = { error ->
                    Toast.makeText(this, "Failed to switch camera: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Flash Toggle Button
        binding.btnFlash.setOnClickListener {
            val newMode = cameraBackend.toggleFlash()
            updateFlashIcon(newMode)
        }

        // Profile Selection Chip
        updateProfileChipUI()
        binding.tvProfileChip.setOnClickListener {
            activeProfile = activeProfile.next()
            updateProfileChipUI()
        }

        // Settings Button
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Gallery Thumbnail Review
        binding.ivThumbnail.setOnClickListener {
            lastCapturedUri?.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(intent)
            }
        }

        // Tap to focus & metering
        binding.viewFinder.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                showFocusRing(event.x, event.y)
                cameraBackend.focusOnPoint(binding.viewFinder, event)
                true
            } else {
                false
            }
        }
    }

    private fun setupCamera() {
        cameraBackend.startCamera(
            lifecycleOwner = this,
            previewView = binding.viewFinder,
            onInitialized = { caps ->
                updateCapabilityBadge(caps)
            },
            onError = { error ->
                Toast.makeText(this, "Camera initialization error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun updateCapabilityBadge(caps: CameraCapabilities) {
        currentCapabilities = caps
        val levelText = "Camera2: ${caps.hardwareLevel.displayName} | " +
                if (caps.supportsRawSensor) "RAW Capable" else "GCam Lite (JPEG/YUV)"
        binding.tvHardwareBadge.text = levelText

        // Adapt default profile if device is heavily constrained
        if (caps.isLowEndDevice && activeProfile == ImageProcessingProfile.NIGHT_ENHANCED) {
            activeProfile = ImageProcessingProfile.FAST_SINGLE_FRAME
            updateProfileChipUI()
        }
    }

    private fun updateFlashIcon(mode: Int) {
        when (mode) {
            ImageCapture.FLASH_MODE_AUTO -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_auto)
                binding.btnFlash.contentDescription = getString(R.string.flash_auto)
            }
            ImageCapture.FLASH_MODE_ON -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_on)
                binding.btnFlash.contentDescription = getString(R.string.flash_on)
            }
            else -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_off)
                binding.btnFlash.contentDescription = getString(R.string.flash_off)
            }
        }
    }

    private fun updateProfileChipUI() {
        val title = when (activeProfile) {
            ImageProcessingProfile.FAST_SINGLE_FRAME -> getString(R.string.mode_fast)
            ImageProcessingProfile.BALANCED_HDR -> getString(R.string.mode_hdr)
            ImageProcessingProfile.NIGHT_ENHANCED -> getString(R.string.mode_night)
        }
        binding.tvProfileChip.text = title
    }

    private fun triggerCapture() {
        binding.btnShutter.isEnabled = false
        vibrateShutter()

        cameraBackend.takePicture(
            profile = activeProfile,
            onProcessingStarted = {
                binding.layoutProcessing.visibility = View.VISIBLE
                binding.tvProcessingStatus.text = getString(R.string.processing_photo)
            },
            onPictureCaptured = { jpegBytes, rotation ->
                lifecycleScope.launch {
                    try {
                        // Computational photography processing
                        val result = imageProcessor.processCapture(jpegBytes, rotation, activeProfile)

                        // Save to MediaStore
                        val saveResult = mediaSaver.saveImage(result.jpegBytes, activeProfile.title)

                        saveResult.onSuccess { mediaInfo ->
                            lastCapturedUri = mediaInfo.uri
                            updateThumbnail(result.jpegBytes)
                            Toast.makeText(
                                this@MainActivity,
                                "${getString(R.string.photo_saved)} (${result.processingTimeMs}ms)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { error ->
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.error_capture, error.message),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_capture, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        binding.layoutProcessing.visibility = View.GONE
                        binding.btnShutter.isEnabled = true
                    }
                }
            },
            onError = { error ->
                binding.layoutProcessing.visibility = View.GONE
                binding.btnShutter.isEnabled = true
                Toast.makeText(
                    this,
                    getString(R.string.error_capture, error.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun updateThumbnail(jpegBytes: ByteArray) {
        val options = BitmapFactory.Options().apply { inSampleSize = 8 }
        val thumbBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)
        thumbBitmap?.let {
            binding.ivThumbnail.setImageBitmap(it)
        }
    }

    private fun showFocusRing(x: Float, y: Float) {
        binding.focusRing.apply {
            this.x = x - (width / 2)
            this.y = y - (height / 2)
            visibility = View.VISIBLE
            alpha = 1.0f

            val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
                duration = 800
                startOffset = 400
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        visibility = View.GONE
                    }
                })
            }
            startAnimation(fadeOut)
        }
    }

    private fun vibrateShutter() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    private fun showSettingsDialog() {
        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        currentCapabilities?.let { caps ->
            dialogBinding.tvCapabilityDetails.text = caps.toDiagnosticString()
        } ?: run {
            dialogBinding.tvCapabilityDetails.text = getString(R.string.raw_unsupported_notice)
        }

        dialogBinding.rgResolution.setOnCheckedChangeListener { _, checkedId ->
            val tier = when (checkedId) {
                R.id.rbResBalanced -> CameraXBackend.RESOLUTION_TIER_BALANCED
                R.id.rbResLow -> CameraXBackend.RESOLUTION_TIER_LOW
                else -> CameraXBackend.RESOLUTION_TIER_MAX
            }
            cameraBackend.setTargetResolutionTier(tier)
            setupCamera()
        }

        dialogBinding.btnCloseSettings.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraBackend.stopCamera()
    }
}
