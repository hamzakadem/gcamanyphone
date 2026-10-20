package com.gcam.compatible.camera.processing

import android.graphics.Bitmap

/**
 * Edge-preserving spatial denoising and unsharp masking.
 * Reduces high-frequency chrominance and luminance noise while accentuating edge details.
 */
class EdgePreservingDenoise {

    /**
     * Applies unsharp masking to enhance fine textures and edges.
     * output = original + strength * (original - blurred)
     */
    fun applyUnsharpMask(input: Bitmap, strength: Float = 0.35f): Bitmap {
        if (strength <= 0.01f) return input

        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        // 3x3 Box blur approximation for edge extraction
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

                // Enhance high-frequency delta
                val newR = (origR + strength * (origR - rBlur)).toInt().coerceIn(0, 255)
                val newG = (origG + strength * (origG - gBlur)).toInt().coerceIn(0, 255)
                val newB = (origB + strength * (origB - bBlur)).toInt().coerceIn(0, 255)

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
