package com.gcam.compatible.camera.processing

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.pow

/**
 * Enhanced multi-frame burst accumulator for noise reduction, dynamic range expansion,
 * and Intelligent Fake RAW synthesis.
 *
 * Employs memory-safe streaming, reusable pixel buffers, 32-bit integer accumulation
 * (preventing OutOfMemory errors when processing 20+ frames), motion de-ghosting,
 * and linear photometric synthesis for computational RAW dynamic range.
 */
class BurstAccumulator {

    companion object {
        // Fast 16-bit linear inverse gamma LUT for Fake RAW accumulation
        private val INV_GAMMA_16 = IntArray(256) { i ->
            val norm = i / 255.0f
            (norm.pow(2.2f) * 65535.0f).toInt().coerceIn(0, 65535)
        }

        // Fast gamma 2.2 compression LUT back to 8-bit
        private val GAMMA_TO_8 = IntArray(65536) { i ->
            val norm = i / 65535.0f
            (norm.pow(1.0f / 2.2f) * 255.0f).toInt().coerceIn(0, 255)
        }
    }

    /**
     * Accumulates and blends a list of bitmaps into a single denoised image.
     */
    fun accumulate(frames: List<Bitmap>, isFakeRaw: Boolean = false): Bitmap {
        require(frames.isNotEmpty()) { "Frames list must not be empty" }
        if (frames.size == 1 && !isFakeRaw) return frames[0]

        val base = frames[0]
        val width = base.width
        val height = base.height
        val accumulator = IncrementalBurstAccumulator(width, height, isFakeRaw = isFakeRaw)

        for (frame in frames) {
            accumulator.addFrame(frame)
        }

        return accumulator.buildResult()
    }

    /**
     * Memory-safe streaming accumulator with IntArray buffers (cutting memory in half compared to LongArray)
     * and a reusable pixel transfer buffer to eliminate GC pressure when merging 20+ RAW/burst frames.
     */
    class IncrementalBurstAccumulator(
        val width: Int,
        val height: Int,
        private val isFakeRaw: Boolean = false,
        private val enableDeGhosting: Boolean = true
    ) {
        private val totalPixels = width * height

        // 32-bit signed Int can hold up to 2,147,483,647.
        // For standard 8-bit (max 255), up to 8,000,000 frames could be accumulated.
        // For 16-bit linear Fake RAW (max 65535), up to 32,767 frames could be accumulated.
        // IntArray cuts memory footprint by 50% compared to LongArray, preventing OOM crashes.
        private val sumR = IntArray(totalPixels)
        private val sumG = IntArray(totalPixels)
        private val sumB = IntArray(totalPixels)
        private val weightMap = if (enableDeGhosting) IntArray(totalPixels) else null

        // Reusable transfer buffer to avoid allocating a huge array on every frame
        private var reusablePixelBuffer: IntArray? = null

        var frameCount = 0
            private set

        /**
         * Incorporates a new frame into the running sum.
         * Reuses internal transfer buffer and immediately recycles bitmap if requested.
         */
        fun addFrame(bitmap: Bitmap, autoRecycle: Boolean = false) {
            if (bitmap.width != width || bitmap.height != height) {
                if (autoRecycle) bitmap.recycle()
                return
            }

            var buffer = reusablePixelBuffer
            if (buffer == null || buffer.size != totalPixels) {
                buffer = IntArray(totalPixels)
                reusablePixelBuffer = buffer
            }

            bitmap.getPixels(buffer, 0, width, 0, 0, width, height)

            if (isFakeRaw) {
                accumulateLinearFakeRaw(buffer)
            } else {
                accumulateStandard(buffer)
            }

            frameCount++

            if (autoRecycle) {
                bitmap.recycle()
            }
        }

        private fun accumulateStandard(pixels: IntArray) {
            val useDeGhost = enableDeGhosting && frameCount >= 2 && weightMap != null

            for (i in 0 until totalPixels) {
                val color = pixels[i]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                if (useDeGhost) {
                    val count = frameCount
                    val avgR = sumR[i] / count
                    val avgG = sumG[i] / count
                    val avgB = sumB[i] / count

                    // Motion detector: if pixel deviates significantly from running average (e.g. moving object)
                    val diff = abs(r - avgR) + abs(g - avgG) + abs(b - avgB)
                    if (diff > 80) {
                        // Outlier / motion detected: down-weight current pixel contribution
                        sumR[i] += avgR
                        sumG[i] += avgG
                        sumB[i] += avgB
                        weightMap!![i] += 1
                        continue
                    }
                }

                sumR[i] += r
                sumG[i] += g
                sumB[i] += b
                if (weightMap != null) {
                    weightMap[i] += 1
                }
            }
        }

        private fun accumulateLinearFakeRaw(pixels: IntArray) {
            // Intelligent Fake RAW synthesis:
            // Accumulate in 16-bit linear light domain to replicate true sensor RAW integration.
            // High photon counts are accumulated linearly without early 8-bit clipping or crushing.
            for (i in 0 until totalPixels) {
                val color = pixels[i]
                val r8 = (color shr 16) and 0xFF
                val g8 = (color shr 8) and 0xFF
                val b8 = color and 0xFF

                val linR = INV_GAMMA_16[r8]
                val linG = INV_GAMMA_16[g8]
                val linB = INV_GAMMA_16[b8]

                sumR[i] += linR
                sumG[i] += linG
                sumB[i] += linB
            }
        }

        /**
         * Normalizes the accumulated pixel buffer and returns the resulting denoised / HDR Bitmap.
         */
        fun buildResult(): Bitmap {
            val count = frameCount.coerceAtLeast(1)
            val outPixels = IntArray(totalPixels)

            if (isFakeRaw) {
                // Synthesize computational RAW with expanded dynamic range
                for (i in 0 until totalPixels) {
                    val avgLinR = (sumR[i] / count).coerceIn(0, 65535)
                    val avgLinG = (sumG[i] / count).coerceIn(0, 65535)
                    val avgLinB = (sumB[i] / count).coerceIn(0, 65535)

                    // Convert linear 16-bit to perceptual 8-bit using gamma compression
                    val r = GAMMA_TO_8[avgLinR]
                    val g = GAMMA_TO_8[avgLinG]
                    val b = GAMMA_TO_8[avgLinB]

                    outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            } else {
                for (i in 0 until totalPixels) {
                    val div = if (weightMap != null && weightMap[i] > 0) weightMap[i] else count
                    val r = (sumR[i] / div).coerceIn(0, 255)
                    val g = (sumG[i] / div).coerceIn(0, 255)
                    val b = (sumB[i] / div).coerceIn(0, 255)

                    outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            // Release buffer to free up memory immediately
            reusablePixelBuffer = null

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            result.setPixels(outPixels, 0, width, 0, 0, width, height)
            return result
        }
    }
}
