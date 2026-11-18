package com.matrix.launcher

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal data class DevicePicture(val uri: String, val name: String, val albumId: String, val album: String)

/** Read-only, bounded MediaStore access. Never enumerates arbitrary filesystem paths. */
internal object DevicePictures {
    fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun allowed(context: Context): Boolean = permissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun read(context: Context): List<DevicePicture> = withContext(Dispatchers.IO) {
        if (!allowed(context)) return@withContext emptyList()
        val source = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val pictures = mutableListOf<DevicePicture>()
        context.contentResolver.query(source, columns,
            "${MediaStore.Images.Media.SIZE} > 0 AND ${MediaStore.Images.Media.SIZE} <= ?",
            arrayOf((10 * 1024 * 1024).toString()), "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC")?.use { cursor ->
            while (pictures.size < 500 && cursor.moveToNext()) {
                coroutineContext.ensureActive()
                pictures += DevicePicture(ContentUris.withAppendedId(source, cursor.getLong(0)).toString(),
                    cursor.getString(1) ?: "Picture", cursor.getString(2) ?: "unknown", cursor.getString(3) ?: "Pictures")
            }
        }
        pictures
    }
}
