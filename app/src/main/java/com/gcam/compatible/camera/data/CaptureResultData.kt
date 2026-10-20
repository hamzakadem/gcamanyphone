package com.gcam.compatible.camera.data

import android.net.Uri

/**
 * Result of a completed photo capture and image processing pipeline execution.
 */
data class CaptureResultData(
    val uri: Uri?,
    val filePath: String?,
    val width: Int,
    val height: Int,
    val processingTimeMs: Long,
    val profileUsed: ImageProcessingProfile,
    val framesMerged: Int,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = (uri != null || filePath != null) && errorMessage == null
}
