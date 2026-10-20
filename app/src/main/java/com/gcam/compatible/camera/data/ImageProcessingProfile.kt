package com.gcam.compatible.camera.data

/**
 * Image processing profiles tailored for varying device capabilities and lighting situations.
 */
enum class ImageProcessingProfile(
    val title: String,
    val burstFrameCount: Int,
    val shadowBoostFactor: Float,
    val highlightCompressFactor: Float,
    val denoiseRadius: Int,
    val sharpenStrength: Float
) {
    /**
     * Optimized for low-RAM devices or fast-action scenes.
     * Single frame capture, instantaneous tone curve and light sharpening. Zero risk of OOM.
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
     * Merges 3 frames using temporal accumulation, applies adaptive tone mapping to lift shadows,
     * reduces photon noise, and sharpens fine details.
     */
    BALANCED_HDR(
        title = "HDR+ Lite",
        burstFrameCount = 3,
        shadowBoostFactor = 1.45f,
        highlightCompressFactor = 0.85f,
        denoiseRadius = 2,
        sharpenStrength = 0.40f
    ),

    /**
     * Night Lite pipeline.
     * Merges up to 5 frames for deep shadow noise suppression, extended tone mapping curve,
     * and spatial noise reduction.
     */
    NIGHT_ENHANCED(
        title = "Night Lite",
        burstFrameCount = 5,
        shadowBoostFactor = 1.85f,
        highlightCompressFactor = 0.75f,
        denoiseRadius = 3,
        sharpenStrength = 0.35f
    );

    fun next(): ImageProcessingProfile {
        val values = values()
        return values[(ordinal + 1) % values.size]
    }
}
