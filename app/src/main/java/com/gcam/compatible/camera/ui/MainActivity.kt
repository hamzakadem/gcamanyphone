package com.gcam.compatible.camera.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gcam.compatible.camera.R
import com.gcam.compatible.camera.camera.CameraBackend
import com.gcam.compatible.camera.camera.CameraMode
import com.gcam.compatible.camera.camera.CameraXBackend
import com.gcam.compatible.camera.data.CameraCapabilities
import com.gcam.compatible.camera.data.ImageProcessingProfile
import com.gcam.compatible.camera.databinding.ActivityMainBinding
import com.gcam.compatible.camera.databinding.DialogSettingsBinding
import com.gcam.compatible.camera.processing.ImageProcessor
import com.gcam.compatible.camera.processing.LutEngine
import com.gcam.compatible.camera.storage.MediaSaver
import com.gcam.compatible.camera.utils.PermissionHelper
import kotlinx.coroutines.launch
import java.io.File

/**
 * Modern Flagship Camera Activity.
 *
 * Provides:
 * - 4K/1080p Video recording with full manual controls (ISO, Shutter Speed, White Balance)
 * - Intelligent Fake RAW computational photography (customizable 2-25 frames without OOM crashes)
 * - Universal Synthetic Log profile available on every phone
 * - DaVinci/Adobe 3D .cube LUT file importer and cinematic presets
 * - Contemporary glassmorphism camera UI with bottom mode carousel
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraBackend: CameraBackend
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var mediaSaver: MediaSaver
    private lateinit var lutEngine: LutEngine

    private var activeProfile: ImageProcessingProfile = ImageProcessingProfile.BALANCED_HDR
    private var lastCapturedUri: Uri? = null
    private var currentCapabilities: CameraCapabilities? = null

    // Pro Controls & Feature States
    private var isProPanelVisible = false
    private var useSyntheticLog = false
    private var activeLutPreset: LutEngine.Preset = LutEngine.Preset.NATURAL
    private var activeCustomLut: LutEngine.Lut3D? = null
    private var fakeRawFrameCount = 20

    // Video Recording State
    private var activeTempVideoFile: File? = null
    private var recordingStartTime = 0L
    private val recordingHandler = Handler(Looper.getMainLooper())
    private val recordingTimerRunnable = object : Runnable {
        override fun run() {
            if (cameraBackend.isRecording()) {
                val elapsedSeconds = (System.currentTimeMillis() - recordingStartTime) / 1000
                val mins = elapsedSeconds / 60
                val secs = elapsedSeconds % 60
                binding.tvRecordingTimer.text = String.format("%02d:%02d", mins, secs)
                recordingHandler.postDelayed(this, 1000)
            }
        }
    }

    // LUT Importer File Picker
    private val lutFilePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val parsedLut = lutEngine.importLutFromUri(it)
            if (parsedLut != null) {
                activeCustomLut = parsedLut
                activeLutPreset = LutEngine.Preset.NATURAL
                useSyntheticLog = false
                updateLutUI()
                Toast.makeText(this, "Custom LUT loaded: ${parsedLut.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to parse .cube LUT file", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        lutEngine = LutEngine(this)

        setupModernUI()

        if (PermissionHelper.areAllRequiredPermissionsGranted(this)) {
            setupCamera()
        } else {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
        }
    }

    private fun setupModernUI() {
        // Shutter Button
        binding.btnShutter.setOnClickListener {
            if (cameraBackend.getCameraMode() == CameraMode.VIDEO) {
                toggleVideoRecording()
            } else {
                triggerCapture()
            }
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

        // Flash Toggle
        binding.btnFlash.setOnClickListener {
            val newMode = cameraBackend.toggleFlash()
            updateFlashIcon(newMode)
        }

        // Pro Controls Toggle Pill
        binding.btnProToggle.setOnClickListener {
            isProPanelVisible = !isProPanelVisible
            binding.layoutProControls.visibility = if (isProPanelVisible) View.VISIBLE else View.GONE
            binding.btnProToggle.setBackgroundResource(
                if (isProPanelVisible) R.drawable.bg_pill_active else R.drawable.bg_pill
            )
            binding.btnProToggle.setTextColor(
                ContextCompat.getColor(this, if (isProPanelVisible) R.color.black else R.color.white)
            )
        }

        // Synthetic Log Toggle Pill
        binding.btnLogToggle.setOnClickListener {
            useSyntheticLog = !useSyntheticLog
            if (useSyntheticLog) {
                activeCustomLut = null
                activeLutPreset = LutEngine.Preset.SYNTHETIC_LOG
            } else {
                activeLutPreset = LutEngine.Preset.NATURAL
            }
            updateLutUI()
            val msg = if (useSyntheticLog) "Synthetic Flat LOG: ON" else "Synthetic LOG: OFF"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // LUTs Selector Pill
        binding.btnLutSelector.setOnClickListener {
            showLutSelectionDialog()
        }

        // Settings Button
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Gallery Thumbnail Review
        binding.ivThumbnail.setOnClickListener {
            lastCapturedUri?.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (cameraBackend.getCameraMode() == CameraMode.VIDEO) "video/*" else "image/*")
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

        setupProSliders()
        setupModeCarousel()
    }

    private fun setupProSliders() {
        val isoValues = intArrayOf(0, 100, 200, 400, 800, 1600, 3200, 6400)
        binding.sbIso.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val iso = isoValues[progress.coerceIn(0, isoValues.size - 1)]
                binding.tvIsoLabel.text = if (iso == 0) "ISO: Auto" else "ISO: $iso"
                cameraBackend.setManualIso(if (iso == 0) null else iso)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Shutter Speeds in Nanoseconds
        val shutterNanos = longArrayOf(
            0L,
            500_000L,        // 1/2000s
            1_000_000L,      // 1/1000s
            2_000_000L,      // 1/500s
            4_000_000L,      // 1/250s
            8_000_000L,      // 1/125s
            16_666_666L,     // 1/60s
            33_333_333L,     // 1/30s
            66_666_666L      // 1/15s
        )
        val shutterLabels = arrayOf("Auto", "1/2000", "1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1/15")
        binding.sbShutter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val idx = progress.coerceIn(0, shutterNanos.size - 1)
                binding.tvShutterLabel.text = "S: ${shutterLabels[idx]}"
                cameraBackend.setManualShutterSpeed(if (shutterNanos[idx] == 0L) null else shutterNanos[idx])
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // White Balance (Kelvin)
        val wbKelvins = intArrayOf(0, 2800, 3500, 4500, 5500, 6500, 7500)
        val wbLabels = arrayOf("Auto", "2800K", "3500K", "4500K", "5500K", "6500K", "7500K")
        binding.sbWb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val idx = progress.coerceIn(0, wbKelvins.size - 1)
                binding.tvWbLabel.text = "WB: ${wbLabels[idx]}"
                cameraBackend.setManualWhiteBalance(if (wbKelvins[idx] == 0) null else wbKelvins[idx])
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Intelligent Fake RAW customizable frame count (2 to 25 frames)
        binding.sbFakeRawFrames.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val count = progress.coerceIn(2, 25)
                fakeRawFrameCount = count
                binding.tvFrameCountLabel.text = "RAW: ${count}x"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupModeCarousel() {
        val allTabs = listOf(
            binding.tabNight,
            binding.tabPhoto,
            binding.tabVideo,
            binding.tabFakeRaw,
            binding.tabAstro
        )

        fun selectTab(selectedView: View, mode: CameraMode, profile: ImageProcessingProfile) {
            allTabs.forEach { tab ->
                val isSelected = (tab == selectedView)
                tab.setTextColor(
                    ContextCompat.getColor(
                        this,
                        if (isSelected) R.color.camera_tab_selected else R.color.camera_tab_unselected
                    )
                )
            }

            activeProfile = profile
            cameraBackend.setCameraMode(mode, this, binding.viewFinder) { caps ->
                updateCapabilityBadge(caps)
            }
            cameraBackend.applyProfileHardwareSettings(profile)

            if (mode == CameraMode.VIDEO) {
                binding.btnShutter.setBackgroundResource(R.drawable.btn_shutter_video)
                binding.rowFakeRawFrames.visibility = View.GONE
            } else {
                binding.btnShutter.setBackgroundResource(R.drawable.btn_shutter)
                binding.layoutRecordingIndicator.visibility = View.GONE
                binding.rowFakeRawFrames.visibility = if (profile.isFakeRaw) View.VISIBLE else View.GONE
            }
        }

        binding.tabNight.setOnClickListener {
            selectTab(it, CameraMode.PHOTO, ImageProcessingProfile.NIGHT_ENHANCED)
        }
        binding.tabPhoto.setOnClickListener {
            selectTab(it, CameraMode.PHOTO, ImageProcessingProfile.BALANCED_HDR)
        }
        binding.tabVideo.setOnClickListener {
            selectTab(it, CameraMode.VIDEO, ImageProcessingProfile.FAST_SINGLE_FRAME)
        }
        binding.tabFakeRaw.setOnClickListener {
            selectTab(it, CameraMode.PHOTO, ImageProcessingProfile.INTELLIGENT_FAKE_RAW)
        }
        binding.tabAstro.setOnClickListener {
            selectTab(it, CameraMode.PHOTO, ImageProcessingProfile.ASTROPHOTOGRAPHY)
            Toast.makeText(this, R.string.astro_info_toast, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLutUI() {
        if (useSyntheticLog) {
            binding.btnLogToggle.setBackgroundResource(R.drawable.bg_pill_active)
            binding.btnLogToggle.setTextColor(ContextCompat.getColor(this, R.color.black))
            binding.btnLutSelector.text = "LUT: Log"
        } else {
            binding.btnLogToggle.setBackgroundResource(R.drawable.bg_pill)
            binding.btnLogToggle.setTextColor(ContextCompat.getColor(this, R.color.white))

            binding.btnLutSelector.text = when {
                activeCustomLut != null -> "LUT: ${activeCustomLut!!.name}"
                activeLutPreset != LutEngine.Preset.NATURAL -> "LUT: ${activeLutPreset.displayName.take(8)}"
                else -> getString(R.string.luts_filter)
            }
        }
    }

    private fun showLutSelectionDialog() {
        val presets = LutEngine.Preset.values()
        val items = presets.map { it.displayName }.toMutableList()
        items.add(getString(R.string.import_custom_lut))

        AlertDialog.Builder(this)
            .setTitle(R.string.select_lut)
            .setItems(items.toTypedArray()) { _, which ->
                if (which == items.size - 1) {
                    // Open file picker for custom .cube file
                    lutFilePicker.launch("*/*")
                } else {
                    activeCustomLut = null
                    activeLutPreset = presets[which]
                    useSyntheticLog = (activeLutPreset == LutEngine.Preset.SYNTHETIC_LOG)
                    updateLutUI()
                    Toast.makeText(this, "Active LUT: ${activeLutPreset.displayName}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun setupCamera() {
        cameraBackend.startCamera(
            lifecycleOwner = this,
            previewView = binding.viewFinder,
            onInitialized = { caps ->
                updateCapabilityBadge(caps)
                cameraBackend.applyProfileHardwareSettings(activeProfile)
            },
            onError = { error ->
                Toast.makeText(this, "Camera initialization error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun updateCapabilityBadge(caps: CameraCapabilities) {
        currentCapabilities = caps
        val levelText = "Camera2: ${caps.hardwareLevel.displayName} | " +
                if (caps.supportsRawSensor) "RAW Capable" else "GCam Pro (Universal)"
        binding.tvHardwareBadge.text = levelText
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

    private fun triggerCapture() {
        binding.btnShutter.isEnabled = false
        vibrateShutter()

        binding.layoutProcessing.visibility = View.VISIBLE
        val burstCount = if (activeProfile.isFakeRaw) fakeRawFrameCount else activeProfile.burstFrameCount
        binding.tvProcessingStatus.text = getString(R.string.capturing_frame_progress, 1, burstCount)

        cameraBackend.takePictureBurst(
            profile = activeProfile,
            customFrameCount = burstCount,
            onCaptureProgress = { current, total ->
                binding.layoutProcessing.visibility = View.VISIBLE
                binding.tvProcessingStatus.text = getString(R.string.capturing_frame_progress, current.coerceAtLeast(1), total)
                vibrateShutter()
            },
            onBurstComplete = { frames, rotation ->
                lifecycleScope.launch {
                    try {
                        binding.tvProcessingStatus.text = getString(R.string.processing_photo)

                        val result = imageProcessor.processCaptureBurst(
                            frames = frames,
                            rotationDegrees = rotation,
                            profile = activeProfile,
                            useSyntheticLog = useSyntheticLog,
                            lutPreset = activeLutPreset,
                            customLut = activeCustomLut,
                            customBurstCount = burstCount,
                            onProgress = { stage ->
                                binding.tvProcessingStatus.text = stage
                            }
                        )

                        val saveResult = mediaSaver.saveImage(result.jpegBytes, activeProfile.title)

                        saveResult.onSuccess { mediaInfo ->
                            lastCapturedUri = mediaInfo.uri
                            updateThumbnail(result.jpegBytes)
                            Toast.makeText(
                                this@MainActivity,
                                "${getString(R.string.photo_saved)} (${result.framesMerged} frames, ${result.processingTimeMs}ms)",
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

    private fun toggleVideoRecording() {
        if (cameraBackend.isRecording()) {
            // Stop recording
            cameraBackend.stopRecording()
            binding.btnShutter.setBackgroundResource(R.drawable.btn_shutter_video)
            binding.layoutRecordingIndicator.visibility = View.GONE
            recordingHandler.removeCallbacks(recordingTimerRunnable)
            vibrateShutter()
        } else {
            // Start recording
            val tempFile = mediaSaver.createTempVideoFile()
            activeTempVideoFile = tempFile
            recordingStartTime = System.currentTimeMillis()

            binding.btnShutter.setBackgroundResource(R.drawable.btn_shutter_recording)
            binding.layoutRecordingIndicator.visibility = View.VISIBLE
            binding.tvRecordingTimer.text = "00:00"
            recordingHandler.post(recordingTimerRunnable)
            vibrateShutter()

            cameraBackend.startRecording(tempFile) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    binding.layoutRecordingIndicator.visibility = View.GONE
                    binding.btnShutter.setBackgroundResource(R.drawable.btn_shutter_video)
                    recordingHandler.removeCallbacks(recordingTimerRunnable)

                    if (!event.hasError()) {
                        lifecycleScope.launch {
                            val result = mediaSaver.saveVideo(tempFile)
                            result.onSuccess { info ->
                                lastCapturedUri = info.uri
                                Toast.makeText(this@MainActivity, R.string.video_saved, Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(this@MainActivity, "Failed to save video: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                            tempFile.delete()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Video recording error: ${event.cause?.message}", Toast.LENGTH_LONG).show()
                        tempFile.delete()
                    }
                }
            }
        }
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
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
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
        recordingHandler.removeCallbacks(recordingTimerRunnable)
        cameraBackend.stopCamera()
    }
}
