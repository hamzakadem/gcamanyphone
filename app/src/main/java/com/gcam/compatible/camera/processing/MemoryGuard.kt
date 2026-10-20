package com.gcam.compatible.camera.processing

import android.graphics.BitmapFactory
import android.util.Log

/**
 * Memory safety coordinator designed to prevent OutOfMemoryErrors (OOM) on low-end Android devices.
 * Evaluates runtime heap availability and calculates safe decode sampling and burst limits.
 */
class MemoryGuard {

    companion object {
        private const val TAG = "MemoryGuard"
        private const val MIN_SAFE_HEAP_BYTES = 48L * 1024 * 1024 // 48 MB minimum free heap headroom
    }

    /**
     * Checks if there is sufficient heap to process a full-resolution bitmap burst.
     */
    fun hasSufficientHeap(requiredBytes: Long = 32L * 1024 * 1024): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val availableHeap = maxMemory - (totalMemory - freeMemory)

        val sufficient = (availableHeap - requiredBytes) >= MIN_SAFE_HEAP_BYTES
        if (!sufficient) {
            Log.w(
                TAG,
                "Heap constrained: available=${availableHeap / (1024 * 1024)}MB, required=${requiredBytes / (1024 * 1024)}MB"
            )
        }
        return sufficient
    }

    /**
     * Computes the optimal inSampleSize for decoding bitmaps based on original dimensions
     * and memory availability.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > maxDimension || width > maxDimension) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }

        // If heap is critically low, increase sample size further
        if (!hasSufficientHeap()) {
            inSampleSize *= 2
        }

        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Recommends safe burst frame count based on available memory.
     */
    fun getSafeBurstCount(requestedFrames: Int): Int {
        val runtime = Runtime.getRuntime()
        val availableMb = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / (1024 * 1024)
        return when {
            availableMb < 64 -> 1 // Constrained: single frame only
            availableMb < 128 -> requestedFrames.coerceAtMost(2)
            else -> requestedFrames
        }
    }
}
