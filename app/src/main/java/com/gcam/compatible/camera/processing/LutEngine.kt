package com.gcam.compatible.camera.processing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.ln
import kotlin.math.pow

/**
 * High-performance Universal Color LUT & Synthetic Log Engine.
 *
 * Provides:
 * 1. Synthetic Log Profile: A universal logarithmic transfer curve that works on any phone,
 *    lifting shadows into midtones and gracefully compressing highlights for wide dynamic range color grading.
 * 2. 3D LUT (.cube) Parser: Parses standard DaVinci Resolve / Adobe .cube LUT files from storage or any location.
 * 3. Cinematic Presets: Built-in professional film and color grading profiles.
 */
class LutEngine(private val context: Context? = null) {

    companion object {
        private const val TAG = "LutEngine"

        // 256-entry lookup table for fast Synthetic Log conversion
        val SYNTHETIC_LOG_LUT = IntArray(256) { i ->
            val x = i / 255.0f
            // Standardized Cineon/S-Log inspired mathematical transfer:
            // Lifts shadows to ~0.20-0.30 and smoothly rolls off highlights to retain maximum dynamic range.
            val y = if (x <= 0.01f) {
                0.15f + x * 5.0f
            } else {
                0.20f + 0.75f * (ln(1.0f + 12.0f * x) / ln(13.0f))
            }
            (y * 255.0f).toInt().coerceIn(0, 255)
        }

        // Fast inverse Synthetic Log LUT to restore normal gamma when needed
        val INVERSE_SYNTHETIC_LOG_LUT = IntArray(256) { i ->
            val y = i / 255.0f
            val x = if (y < 0.20f) {
                ((y - 0.15f) / 5.0f).coerceAtLeast(0.0f)
            } else {
                val normY = (y - 0.20f) / 0.75f
                ((13.0f.pow(normY) - 1.0f) / 12.0f).coerceIn(0.0f, 1.0f)
            }
            (x * 255.0f).toInt().coerceIn(0, 255)
        }
    }

    enum class Preset(val displayName: String) {
        NATURAL("None (Standard)"),
        SYNTHETIC_LOG("Synthetic Log (Universal Flat)"),
        TEAL_ORANGE("Cinematic Teal & Orange"),
        WARM_FILM("Vintage Kodachrome 64"),
        MOODY_BW("High-Dynamic B&W Pro"),
        CYBER_COLD("Cool Cyberpunk")
    }

    data class Lut3D(
        val name: String,
        val size: Int,
        // Flat array of floats [size * size * size * 3]
        val table: FloatArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Lut3D) return false
            return name == other.name && size == other.size
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + size
            return result
        }
    }

    /**
     * Applies the universal Synthetic Log transfer curve to an image.
     * Works on any Android camera sensor without requiring proprietary vendor Log APIs.
     */
    fun applySyntheticLog(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in 0 until total) {
            val c = pixels[i]
            val a = c and -0x1000000
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            val logR = SYNTHETIC_LOG_LUT[r]
            val logG = SYNTHETIC_LOG_LUT[g]
            val logB = SYNTHETIC_LOG_LUT[b]

            pixels[i] = a or (logR shl 16) or (logG shl 8) or logB
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Applies a built-in cinematic preset to an image.
     */
    fun applyPreset(bitmap: Bitmap, preset: Preset): Bitmap {
        return when (preset) {
            Preset.NATURAL -> bitmap
            Preset.SYNTHETIC_LOG -> applySyntheticLog(bitmap)
            Preset.TEAL_ORANGE -> applyTealOrange(bitmap)
            Preset.WARM_FILM -> applyWarmFilm(bitmap)
            Preset.MOODY_BW -> applyMoodyBW(bitmap)
            Preset.CYBER_COLD -> applyCyberCold(bitmap)
        }
    }

    /**
     * Parses a standard .cube 3D or 1D LUT file from an InputStream.
     */
    fun parseCubeLut(inputStream: InputStream, name: String = "Custom LUT"): Lut3D? {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var size3d = 0
        val entries = mutableListOf<Float>()

        try {
            reader.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) {
                    return@forEachLine
                }

                if (line.startsWith("LUT_3D_SIZE")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        size3d = parts[1].toIntOrNull() ?: 0
                    }
                    return@forEachLine
                }

                if (line.startsWith("LUT_1D_SIZE") || line.startsWith("TITLE") || line.startsWith("DOMAIN_")) {
                    return@forEachLine
                }

                // Color triplet: R G B
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val r = parts[0].toFloatOrNull()
                    val g = parts[1].toFloatOrNull()
                    val b = parts[2].toFloatOrNull()
                    if (r != null && g != null && b != null) {
                        entries.add(r)
                        entries.add(g)
                        entries.add(b)
                    }
                }
            }

            if (size3d <= 0) {
                // Infer size if not explicitly stated in header
                val count = entries.size / 3
                val cubeRoot = Math.cbrt(count.toDouble()).toInt()
                if (cubeRoot * cubeRoot * cubeRoot == count) {
                    size3d = cubeRoot
                } else {
                    Log.e(TAG, "Unsupported or non-cubic LUT dimension: $count entries")
                    return null
                }
            }

            return Lut3D(name, size3d, entries.toFloatArray())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse .cube LUT: ${e.message}", e)
            return null
        }
    }

    /**
     * Applies a 3D LUT to a Bitmap using trilinear interpolation.
     */
    fun apply3dLut(bitmap: Bitmap, lut: Lut3D): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val size = lut.size
        val table = lut.table
        val maxIndex = (size - 1).toFloat()

        for (i in 0 until total) {
            val c = pixels[i]
            val a = c and -0x1000000
            val rNorm = ((c shr 16) and 0xFF) / 255.0f
            val gNorm = ((c shr 8) and 0xFF) / 255.0f
            val bNorm = (c and 0xFF) / 255.0f

            val rIdx = (rNorm * maxIndex).coerceIn(0.0f, maxIndex)
            val gIdx = (gNorm * maxIndex).coerceIn(0.0f, maxIndex)
            val bIdx = (bNorm * maxIndex).coerceIn(0.0f, maxIndex)

            val r0 = rIdx.toInt()
            val g0 = gIdx.toInt()
            val b0 = bIdx.toInt()

            val r1 = (r0 + 1).coerceAtMost(size - 1)
            val g1 = (g0 + 1).coerceAtMost(size - 1)
            val b1 = (b0 + 1).coerceAtMost(size - 1)

            val rFrac = rIdx - r0
            val gFrac = gIdx - g0
            val bFrac = bIdx - b0

            // Trilinear sampling
            fun sampleTable(r: Int, g: Int, b: Int, ch: Int): Float {
                val index = (r + g * size + b * size * size) * 3 + ch
                return if (index in table.indices) table[index] else 0.0f
            }

            var outR = 0.0f
            var outG = 0.0f
            var outB = 0.0f

            for (ch in 0..2) {
                val c000 = sampleTable(r0, g0, b0, ch)
                val c100 = sampleTable(r1, g0, b0, ch)
                val c010 = sampleTable(r0, g1, b0, ch)
                val c110 = sampleTable(r1, g1, b0, ch)
                val c001 = sampleTable(r0, g0, b1, ch)
                val c101 = sampleTable(r1, g0, b1, ch)
                val c011 = sampleTable(r0, g1, b1, ch)
                val c111 = sampleTable(r1, g1, b1, ch)

                val c00 = c000 * (1 - rFrac) + c100 * rFrac
                val c10 = c010 * (1 - rFrac) + c110 * rFrac
                val c01 = c001 * (1 - rFrac) + c101 * rFrac
                val c11 = c011 * (1 - rFrac) + c111 * rFrac

                val c0 = c00 * (1 - gFrac) + c10 * gFrac
                val c1 = c01 * (1 - gFrac) + c11 * gFrac

                val value = c0 * (1 - bFrac) + c1 * bFrac
                when (ch) {
                    0 -> outR = value
                    1 -> outG = value
                    2 -> outB = value
                }
            }

            val finalR = (outR * 255.0f).toInt().coerceIn(0, 255)
            val finalG = (outG * 255.0f).toInt().coerceIn(0, 255)
            val finalB = (outB * 255.0f).toInt().coerceIn(0, 255)

            pixels[i] = a or (finalR shl 16) or (finalG shl 8) or finalB
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Imports a user-selected .cube LUT file from device storage, saves it to private app files,
     * and returns the parsed Lut3D object.
     */
    fun importLutFromUri(uri: Uri): Lut3D? {
        val ctx = context ?: return null
        return try {
            val contentResolver = ctx.contentResolver
            val fileName = "custom_lut_${System.currentTimeMillis()}.cube"
            val targetFile = File(ctx.filesDir, "luts/$fileName")
            targetFile.parentFile?.mkdirs()

            contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            targetFile.inputStream().use { input ->
                parseCubeLut(input, name = "Custom LUT")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing custom LUT from URI: ${e.message}", e)
            null
        }
    }

    // --- High Performance Built-in Cinematic Presets ---

    private fun applyTealOrange(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in 0 until total) {
            val c = pixels[i]
            val a = c and -0x1000000
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            // Warm skin tones & highlights (Orange/Gold), cool shadows (Teal/Cyan)
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val shadowFactor = (1.0f - lum / 255.0f).coerceIn(0.0f, 1.0f)
            val highlightFactor = (lum / 255.0f).coerceIn(0.0f, 1.0f)

            val newR = (r + 35 * highlightFactor - 15 * shadowFactor).toInt().coerceIn(0, 255)
            val newG = (g + 10 * highlightFactor + 10 * shadowFactor).toInt().coerceIn(0, 255)
            val newB = (b - 20 * highlightFactor + 40 * shadowFactor).toInt().coerceIn(0, 255)

            pixels[i] = a or (newR shl 16) or (newG shl 8) or newB
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    private fun applyWarmFilm(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in 0 until total) {
            val c = pixels[i]
            val a = c and -0x1000000
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            // Vintage Kodachrome warmth with slight green shadow undertone
            val newR = (r * 1.08f + 12).toInt().coerceIn(0, 255)
            val newG = (g * 1.02f + 4).toInt().coerceIn(0, 255)
            val newB = (b * 0.92f - 6).toInt().coerceIn(0, 255)

            pixels[i] = a or (newR shl 16) or (newG shl 8) or newB
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    private fun applyMoodyBW(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in 0 until total) {
            val c = pixels[i]
            val a = c and -0x1000000
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            // Leica Monochrome high-contrast spectral sensitivity (0.35 R, 0.45 G, 0.20 B)
            var gray = (0.35f * r + 0.45f * g + 0.20f * b)
            // S-curve contrast boost
            val norm = gray / 255.0f
            val sCurve = if (norm < 0.5f) {
                2.0f * norm.pow(1.6f)
            } else {
                1.0f - 2.0f * (1.0f - norm).pow(1.6f)
            }
            val finalGray = (sCurve * 255.0f).toInt().coerceIn(0, 255)

            pixels[i] = a or (finalGray shl 16) or (finalGray shl 8) or finalGray
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    private fun applyCyberCold(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in 0 until total) {
            val c = pixels[i]
            val a = c and -0x1000000
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            val newR = (r * 0.90f - 8).toInt().coerceIn(0, 255)
            val newG = (g * 0.98f + 5).toInt().coerceIn(0, 255)
            val newB = (b * 1.15f + 20).toInt().coerceIn(0, 255)

            pixels[i] = a or (newR shl 16) or (newG shl 8) or newB
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }
}
