package com.gcam.compatible.camera.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import com.gcam.compatible.camera.data.ImageProcessingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Computational photography coordinator managing the complete high-dynamic-range pipeline.
 *
 * Supports:
 * - Incremental multi-frame burst merging (up to 20+ frames without OOM crashes)
 * - Intelligent Fake RAW computational synthesis in 16-bit linear light domain
 * - Universal Synthetic Log transfer curve
 * - 3D / 1D LUT filtering
 * - Adaptive tone mapping and bilateral edge preservation
 */
class ImageProcessor(
    private val memoryGuard: MemoryGuard = MemoryGuard(),
    private val toneMapper: AdaptiveToneMapper = AdaptiveToneMapper(),
    private val burstAccumulator: BurstAccumulator = BurstAccumulator(),
    private val edgeDenoise: EdgePreservingDenoise = EdgePreservingDenoise(),
    private val lutEngine: LutEngine = LutEngine()
) {

    companion object {
        private const val TAG = "ImageProcessor"
        private const val MAX_PROCESSING_DIMENSION = 3840 // 4K max bounding box
    }

    /**
     * Processes a burst of captured frames (e.g. 15 frames for Night Mode, 20 frames for Intelligent Fake RAW).
     * Decodes and accumulates frames incrementally with IntArray buffers and auto-recycling to prevent OutOfMemoryError.
     */
    suspend fun processCaptureBurst(
        frames: List<ByteArray>,
        rotationDegrees: Int,
        profile: ImageProcessingProfile,
        useSyntheticLog: Boolean = false,
        lutPreset: LutEngine.Preset = LutEngine.Preset.NATURAL,
        customLut: LutEngine.Lut3D? = null,
        customBurstCount: Int? = null,
        onProgress: ((String) -> Unit)? = null
    ): ProcessedImageResult = withContext(Dispatchers.Default) {
        require(frames.isNotEmpty()) { "Captured frames list must not be empty" }
        val startTime = System.currentTimeMillis()

        // Step 1: Decode bounds from the first frame to determine safe inSampleSize
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(frames[0], 0, frames[0].size, boundsOptions)

        val inSampleSize = memoryGuard.calculateInSampleSize(boundsOptions, MAX_PROCESSING_DIMENSION)

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }

        // Step 2: Decode the base frame and apply rotation
        onProgress?.invoke("Decoding base frame...")
        val baseDecoded = BitmapFactory.decodeByteArray(frames[0], 0, frames[0].size, decodeOptions)
            ?: throw IllegalStateException("Failed to decode base image frame")

        val baseBitmap = rotateBitmapIfNeeded(baseDecoded, rotationDegrees)
        val width = baseBitmap.width
        val height = baseBitmap.height

        val mergedBitmap: Bitmap
        val actualMergedCount: Int

        val totalToMerge = (customBurstCount ?: profile.burstFrameCount).coerceAtMost(frames.size)

        if (totalToMerge <= 1 && !profile.isFakeRaw) {
            mergedBitmap = baseBitmap
            actualMergedCount = 1
        } else {
            // Incremental multi-frame accumulation (15 frames Night / 20 frames Fake RAW)
            val accumulator = BurstAccumulator.IncrementalBurstAccumulator(
                width = width,
                height = height,
                isFakeRaw = profile.isFakeRaw,
                enableDeGhosting = !profile.isAstrophotography // Keep star pinpoints sharp in Astro
            )
            accumulator.addFrame(baseBitmap, autoRecycle = true)

            for (i in 1 until totalToMerge) {
                onProgress?.invoke("Merging frame ${i + 1} of $totalToMerge...")
                try {
                    val frameDecoded = BitmapFactory.decodeByteArray(frames[i], 0, frames[i].size, decodeOptions)
                    if (frameDecoded != null) {
                        val oriented = rotateBitmapIfNeeded(frameDecoded, rotationDegrees)
                        accumulator.addFrame(oriented, autoRecycle = true)
                    }
                } catch (e: OutOfMemoryError) {
                    Log.w(TAG, "OOM during frame $i decode; completing with accumulated frames", e)
                    break
                }
            }

            mergedBitmap = accumulator.buildResult()
            actualMergedCount = accumulator.frameCount
        }

        var currentBitmap = mergedBitmap

        // Step 3: Adaptive Tone Mapping (or specialized Astrophotography curve)
        onProgress?.invoke(
            when {
                profile.isAstrophotography -> "Applying Astrophotography Star Curve..."
                profile.isFakeRaw -> "Reconstructing 16-bit Linear RAW Tone Curves..."
                else -> "Applying Adaptive Tone Mapping..."
            }
        )
        try {
            val toneMapped = toneMapper.applyToneMapping(
                currentBitmap,
                shadowBoost = profile.shadowBoostFactor,
                highlightCompress = profile.highlightCompressFactor,
                isAstrophotography = profile.isAstrophotography
            )
            if (toneMapped != currentBitmap) {
                currentBitmap.recycle()
            }
            currentBitmap = toneMapped
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM during tone mapping stage; skipping", e)
        }

        // Step 4: Synthetic Log Profile & LUT Filtering
        if (useSyntheticLog) {
            onProgress?.invoke("Applying Universal Synthetic Log Profile...")
            try {
                val logApplied = lutEngine.applySyntheticLog(currentBitmap)
                if (logApplied != currentBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = logApplied
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "OOM during synthetic log; skipping", e)
            }
        } else if (customLut != null) {
            onProgress?.invoke("Applying 3D .cube LUT (${customLut.name})...")
            try {
                val lutApplied = lutEngine.apply3dLut(currentBitmap, customLut)
                if (lutApplied != currentBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = lutApplied
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "OOM during custom LUT application; skipping", e)
            }
        } else if (lutPreset != LutEngine.Preset.NATURAL) {
            onProgress?.invoke("Applying Cinematic Filter (${lutPreset.displayName})...")
            try {
                val presetApplied = lutEngine.applyPreset(currentBitmap, lutPreset)
                if (presetApplied != currentBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = presetApplied
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "OOM during LUT preset; skipping", e)
            }
        }

        // Step 5: Spatial Sharpening and Edge Preservation
        if (profile.sharpenStrength > 0.05f) {
            onProgress?.invoke("Sharpening fine details...")
            try {
                val sharpened = edgeDenoise.applyUnsharpMask(
                    currentBitmap,
                    strength = profile.sharpenStrength
                )
                if (sharpened != currentBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = sharpened
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "OOM during sharpening stage; skipping", e)
            }
        }

        // Step 6: High-Quality JPEG Encoding
        onProgress?.invoke("Compressing output photo...")
        val outputStream = ByteArrayOutputStream()
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        val finalBytes = outputStream.toByteArray()

        val finalWidth = currentBitmap.width
        val finalHeight = currentBitmap.height
        currentBitmap.recycle()

        val elapsed = System.currentTimeMillis() - startTime
        Log.i(TAG, "Burst capture completed in ${elapsed}ms: ${finalWidth}x${finalHeight}, merged=$actualMergedCount frames")

        ProcessedImageResult(
            jpegBytes = finalBytes,
            width = finalWidth,
            height = finalHeight,
            processingTimeMs = elapsed,
            profileUsed = profile,
            framesMerged = actualMergedCount
        )
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    data class ProcessedImageResult(
        val jpegBytes: ByteArray,
        val width: Int,
        val height: Int,
        val processingTimeMs: Long,
        val profileUsed: ImageProcessingProfile,
        val framesMerged: Int
    )
}
