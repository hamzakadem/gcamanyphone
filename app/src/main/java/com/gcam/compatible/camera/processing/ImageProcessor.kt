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
 * Computational photography coordinator that manages the complete image post-processing pipeline.
 */
class ImageProcessor(
    private val memoryGuard: MemoryGuard = MemoryGuard(),
    private val toneMapper: AdaptiveToneMapper = AdaptiveToneMapper(),
    private val burstAccumulator: BurstAccumulator = BurstAccumulator(),
    private val edgeDenoise: EdgePreservingDenoise = EdgePreservingDenoise()
) {

    companion object {
        private const val TAG = "ImageProcessor"
        private const val MAX_PROCESSING_DIMENSION = 3840 // 4K max bounding box
    }

    /**
     * Processes captured JPEG bytes with the specified profile on a background coroutine.
     */
    suspend fun processCapture(
        jpegBytes: ByteArray,
        rotationDegrees: Int,
        profile: ImageProcessingProfile
    ): ProcessedImageResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // Decode bounds to calculate safe inSampleSize
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, boundsOptions)

        val inSampleSize = memoryGuard.calculateInSampleSize(boundsOptions, MAX_PROCESSING_DIMENSION)

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, decodeOptions)
            ?: throw IllegalStateException("Failed to decode captured image bytes into Bitmap")

        // Handle sensor orientation rotation
        val orientedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                decodedBitmap,
                0,
                0,
                decodedBitmap.width,
                decodedBitmap.height,
                matrix,
                true
            )
            if (rotated != decodedBitmap) {
                decodedBitmap.recycle()
            }
            rotated
        } else {
            decodedBitmap
        }

        // Apply computational photography stages
        var currentBitmap = orientedBitmap

        // Stage 1: Tone Mapping
        try {
            val toneMapped = toneMapper.applyToneMapping(
                currentBitmap,
                shadowBoost = profile.shadowBoostFactor,
                highlightCompress = profile.highlightCompressFactor
            )
            if (toneMapped != currentBitmap) {
                currentBitmap.recycle()
            }
            currentBitmap = toneMapped
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM during tone mapping stage; skipping to preserve image", e)
        }

        // Stage 2: Sharpening and Edge Preservation
        if (profile.sharpenStrength > 0.05f) {
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

        // Encode to final high quality JPEG
        val outputStream = ByteArrayOutputStream()
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        val finalBytes = outputStream.toByteArray()

        val width = currentBitmap.width
        val height = currentBitmap.height
        currentBitmap.recycle()

        val elapsed = System.currentTimeMillis() - startTime
        Log.i(TAG, "Image processed in ${elapsed}ms: ${width}x${height}, size=${finalBytes.size / 1024}KB")

        ProcessedImageResult(
            jpegBytes = finalBytes,
            width = width,
            height = height,
            processingTimeMs = elapsed,
            profileUsed = profile,
            framesMerged = 1
        )
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
