package com.gcam.compatible.camera.data

/**
 * Image processing profiles tailored for varying device capabilities and photography scenarios.
 */
enum class ImageProcessingProfile(
    val title: String,
    val burstFrameCount: Int,
    val shadowBoostFactor: Float,
    val highlightCompressFactor: Float,
    val denoiseRadius: Int,
    val sharpenStrength: Float,
    val isAstrophotography: Boolean = false,
    val useInfiniteFocus: Boolean = false,
    val useMaxShutter: Boolean = false,
    val isFakeRaw: Boolean = false
) {
    /**
     * Fast single-frame capture for instant shooting or low-RAM scenarios.
     */
    FAST_SINGLE_FRAME(
        title = "Fast Single",
        burstFrameCount = 1,
        shadowBoostFactor = 1.15f,
        highlightCompressFactor = 0.95f,
        denoiseRadius = 1,
        sharpenStrength = 0.25f
    ),

    /**
     * Balanced HDR+ Lite pipeline.
     * Merges 3 frames for standard dynamic range enhancement.
     */
    BALANCED_HDR(
        title = "HDR+ Lite (3x)",
        burstFrameCount = 3,
        shadowBoostFactor = 1.45f,
        highlightCompressFactor = 0.85f,
        denoiseRadius = 2,
        sharpenStrength = 0.40f
    ),

    /**
     * Enhanced Night Mode pipeline.
     * Merges 15 frames using temporal accumulation to completely eliminate noise
     * in extreme low-light environments without OOM.
     */
    NIGHT_ENHANCED(
        title = "Night Mode (15x)",
        burstFrameCount = 15,
        shadowBoostFactor = 2.10f,
        highlightCompressFactor = 0.70f,
        denoiseRadius = 3,
        sharpenStrength = 0.45f
    ),

    /**
     * Astrophotography mode for capturing night skies and stars.
     * Captures 10 frames with maximum shutter/exposure, locked infinite focus,
     * deep sky background suppression, and star point contrast enhancement.
     */
    ASTROPHOTOGRAPHY(
        title = "Astrophotography (10x ∞ Focus)",
        burstFrameCount = 10,
        shadowBoostFactor = 2.30f,
        highlightCompressFactor = 0.65f,
        denoiseRadius = 3,
        sharpenStrength = 0.55f,
        isAstrophotography = true,
        useInfiniteFocus = true,
        useMaxShutter = true
    ),

    /**
     * Intelligent Fake RAW mode.
     * Accumulates a customizable burst of frames (up to 20+ frames) in 16-bit linear light domain,
     * synthesizing computational RAW with massive dynamic range expansion, deep shadow recovery,
     * and extreme highlight protection.
     */
    INTELLIGENT_FAKE_RAW(
        title = "Intelligent Fake RAW (20x Linear)",
        burstFrameCount = 20,
        shadowBoostFactor = 2.20f,
        highlightCompressFactor = 0.60f,
        denoiseRadius = 2,
        sharpenStrength = 0.35f,
        isFakeRaw = true
    );

    fun next(): ImageProcessingProfile {
        val values = values()
        return values[(ordinal + 1) % values.size]
    }
}
