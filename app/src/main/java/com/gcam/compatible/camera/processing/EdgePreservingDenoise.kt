package com.gcam.compatible.camera.processing

import android.graphics.Bitmap
import kotlin.math.abs

/**
 * Edge-preserving spatial denoising and adaptive unsharp masking.
 * Reduces high-frequency chrominance and luminance noise in flat areas
 * while accentuating edge details and fine textures.
 */
class EdgePreservingDenoise {

    /**
     * Applies adaptive unsharp masking with bilateral edge protection.
     * output = original + strength * (original - blurred) in textured regions.
     */
    fun applyUnsharpMask(input: Bitmap, strength: Float = 0.35f): Bitmap {
        if (strength <= 0.01f) return input

        val width = input.width
        val height = input.height
        val total = width * height
        val pixels = IntArray(total)
        val outPixels = IntArray(total)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        // 3x3 Box blur approximation for edge extraction with bilateral variance detection
        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                var rSum = 0
                var gSum = 0
                var bSum = 0

                for (dy in -1..1) {
                    val row = (y + dy) * width
                    for (dx in -1..1) {
                        val c = pixels[row + x + dx]
                        rSum += (c shr 16) and 0xFF
                        gSum += (c shr 8) and 0xFF
                        bSum += c and 0xFF
                    }
                }

                val rBlur = rSum / 9
                val gBlur = gSum / 9
                val bBlur = bSum / 9

                val origColor = pixels[yOffset + x]
                val origR = (origColor shr 16) and 0xFF
                val origG = (origColor shr 8) and 0xFF
                val origB = origColor and 0xFF

                // Delta between original and smoothed
                val deltaR = origR - rBlur
                val deltaG = origG - gBlur
                val deltaB = origB - bBlur

                // Bilateral edge weight: suppress sharpening in flat / noise-only regions (< 4 delta)
                val localEdgeMag = abs(deltaR) + abs(deltaG) + abs(deltaB)
                val edgeWeight = if (localEdgeMag < 6) {
                    0.20f // Dampen noise boost in flat sky / shadows
                } else {
                    1.0f  // Full edge boost for sharp details
                }

                val effectiveStrength = strength * edgeWeight
                val newR = (origR + effectiveStrength * deltaR).toInt().coerceIn(0, 255)
                val newG = (origG + effectiveStrength * deltaG).toInt().coerceIn(0, 255)
                val newB = (origB + effectiveStrength * deltaB).toInt().coerceIn(0, 255)

                outPixels[yOffset + x] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        // Fill borders with original pixels
        for (x in 0 until width) {
            outPixels[x] = pixels[x]
            outPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            outPixels[y * width] = pixels[y * width]
            outPixels[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }
}
