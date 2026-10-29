package com.gcam.compatible.camera.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles saving captured JPEG images and recorded MP4 videos to public MediaStore
 * across all Android API versions (21 through 34).
 */
class MediaSaver(private val context: Context) {

    companion object {
        private const val TAG = "MediaSaver"
        private const val ALBUM_NAME = "GCamLite"
    }

    suspend fun saveImage(
        jpegBytes: ByteArray,
        profileName: String
    ): Result<SavedMediaInfo> = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "IMG_${timestamp}_GCamLite.jpg"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped Storage API (Android 10+)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$ALBUM_NAME")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = context.contentResolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to insert into MediaStore"))

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jpegBytes)
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)

                Log.i(TAG, "Successfully saved image via MediaStore to $uri")
                Result.success(SavedMediaInfo(uri, null, filename))
            } else {
                // Legacy External Storage (Android 5.0 - 9.0)
                val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val appDir = File(dcimDir, ALBUM_NAME)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }

                val outFile = File(appDir, filename)
                FileOutputStream(outFile).use { fos ->
                    fos.write(jpegBytes)
                }

                // Inject EXIF software tag
                try {
                    val exif = ExifInterface(outFile.absolutePath)
                    exif.setAttribute(ExifInterface.TAG_SOFTWARE, "GCam Lite ($profileName)")
                    exif.saveAttributes()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to write EXIF tags", e)
                }

                var scannedUri: Uri? = null
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outFile.absolutePath),
                    arrayOf("image/jpeg")
                ) { _, uri ->
                    scannedUri = uri
                }

                Log.i(TAG, "Successfully saved image via direct file to ${outFile.absolutePath}")
                Result.success(SavedMediaInfo(scannedUri, outFile.absolutePath, filename))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image", e)
            Result.failure(e)
        }
    }

    suspend fun saveVideo(
        videoFile: File
    ): Result<SavedMediaInfo> = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "VID_${timestamp}_GCamLite.mp4"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$ALBUM_NAME")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = context.contentResolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to insert video into MediaStore"))

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(videoFile).use { input ->
                        input.copyTo(output)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)

                Result.success(SavedMediaInfo(uri, null, filename))
            } else {
                val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val appDir = File(dcimDir, ALBUM_NAME)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }

                val outFile = File(appDir, filename)
                FileInputStream(videoFile).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }

                var scannedUri: Uri? = null
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outFile.absolutePath),
                    arrayOf("video/mp4")
                ) { _, uri ->
                    scannedUri = uri
                }

                Result.success(SavedMediaInfo(scannedUri, outFile.absolutePath, filename))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun createTempVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File.createTempFile("VID_${timestamp}_", ".mp4", context.cacheDir)
    }
}

data class SavedMediaInfo(
    val uri: Uri?,
    val absolutePath: String?,
    val filename: String
)
