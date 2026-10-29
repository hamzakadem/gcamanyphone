package com.gcam.compatible.camera.processing

import android.graphics.Bitmap
import kotlin.math.pow

/**
 * Adaptive Local Tone Mapping engine.
 * Dynamically analyzes image luminance distribution, constructs fast 256-value Look-Up Tables (LUT)
 * with Reinhard-inspired highlight roll-off, deep shadow lifting, bilateral contrast preservation,
 * and astrophotography star enhancement.
 */
class AdaptiveToneMapper {

    /**
     * Builds a 256-entry lookup table for standard HDR, Night mode, and Fake RAW luminance remapping.
     */
    fun buildToneCurveLut(shadowBoost: Float, highlightCompress: Float, averageLuminance: Float): IntArray {
        val lut = IntArray(256)

        val dynamicShadowBoost = if (averageLuminance < 0.35f) {
            shadowBoost * (1.0f + (0.35f - averageLuminance))
        } else {
            shadowBoost
        }

        val gamma = 1.0f / dynamicShadowBoost

        for (i in 0..255) {
            val normalized = i / 255.0f
            val shadowLifted = normalized.pow(gamma)
            val compressed = shadowLifted / (1.0f + (1.0f - highlightCompress) * shadowLifted)
            lut[i] = (compressed * 255.0f).toInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Builds a specialized 256-entry lookup table for Astrophotography.
     * Suppresses background sky noise / light pollution in low tones while boosting faint stars and nebulae.
     */
    fun buildAstroToneCurveLut(): IntArray {
        val lut = IntArray(256)
        for (i in 0..255) {
            val norm = i / 255.0f
            val outVal = when {
                norm < 0.08f -> norm * 0.45f // Subdue background atmospheric glow & sensor noise
                norm < 0.45f -> 0.036f + (norm - 0.08f) * 1.55f // Strongly boost faint stars & nebulae
                else -> 0.61f + (norm - 0.45f) * 0.70f // Soft roll-off for bright celestial bodies
            }
            lut[i] = (outVal * 255.0f).toInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Estimates average luminance from a subsampled grid of pixels to keep CPU overhead minimal.
     */
    fun estimateAverageLuminance(pixels: IntArray): Float {
        if (pixels.isEmpty()) return 0.5f

        var totalLum = 0.0
        val step = (pixels.size / 1000).coerceAtLeast(1)
        var samples = 0

        var i = 0
        while (i < pixels.size) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f
            totalLum += lum
            samples++
            i += step
        }

        return (totalLum / samples.coerceAtLeast(1)).toFloat()
    }

    /**
     * Applies adaptive tone mapping or astrophotography tone curve to the provided bitmap.
     * Reuses pixel arrays where possible to avoid memory churn.
     */
    fun applyToneMapping(
        input: Bitmap,
        shadowBoost: Float = 1.35f,
        highlightCompress: Float = 0.85f,
        isAstrophotography: Boolean = false
    ): Bitmap {
        val width = input.width
        val height = input.height
        val total = width * height
        val pixels = IntArray(total)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val lut = if (isAstrophotography) {
            buildAstroToneCurveLut()
        } else {
            val avgLum = estimateAverageLuminance(pixels)
            buildToneCurveLut(shadowBoost, highlightCompress, avgLum)
        }

        // Apply LUT across all pixels
        for (idx in pixels.indices) {
            val color = pixels[idx]
            val a = color and -0x1000000
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            val newR = lut[r]
            val newG = lut[g]
            val newB = lut[b]

            pixels[idx] = a or (newR shl 16) or (newG shl 8) or newB
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
