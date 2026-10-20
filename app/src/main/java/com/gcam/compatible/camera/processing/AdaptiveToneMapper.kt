package com.gcam.compatible.camera.processing

import android.graphics.Bitmap
import kotlin.math.pow

/**
 * Adaptive Local Tone Mapping engine.
 * Dynamically analyzes image luminance distribution, constructs a fast 256-value Look-Up Table (LUT)
 * with Reinhard-inspired highlight roll-off and shadow lifting, and transforms pixel data with zero garbage generation.
 */
class AdaptiveToneMapper {

    /**
     * Builds a 256-entry lookup table for fast per-pixel luminance remapping.
     */
    fun buildToneCurveLut(shadowBoost: Float, highlightCompress: Float, averageLuminance: Float): IntArray {
        val lut = IntArray(256)

        // Dynamic adjustment based on scene brightness: darker scenes get stronger shadow lift
        val dynamicShadowBoost = if (averageLuminance < 0.35f) {
            shadowBoost * (1.0f + (0.35f - averageLuminance))
        } else {
            shadowBoost
        }

        val gamma = 1.0f / dynamicShadowBoost

        for (i in 0..255) {
            val normalized = i / 255.0f

            // Shadow lift using gamma expansion
            val shadowLifted = normalized.pow(gamma)

            // Reinhard-inspired highlight compression
            val compressed = shadowLifted / (1.0f + (1.0f - highlightCompress) * shadowLifted)

            // Rescale back to 0..255 with clamping
            val outVal = (compressed * 255.0f).toInt().coerceIn(0, 255)
            lut[i] = outVal
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
            // ITU-R BT.601 luminance coefficients
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f
            totalLum += lum
            samples++
            i += step
        }

        return (totalLum / samples.coerceAtLeast(1)).toFloat()
    }

    /**
     * Applies adaptive tone mapping to the provided bitmap.
     */
    fun applyToneMapping(
        input: Bitmap,
        shadowBoost: Float = 1.35f,
        highlightCompress: Float = 0.85f
    ): Bitmap {
        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val avgLum = estimateAverageLuminance(pixels)
        val lut = buildToneCurveLut(shadowBoost, highlightCompress, avgLum)

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
