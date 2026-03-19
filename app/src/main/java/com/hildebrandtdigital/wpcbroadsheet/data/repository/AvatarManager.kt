package com.hildebrandtdigital.wpcbroadsheet.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

private val Context.avatarDataStore: DataStore<Preferences>
        by preferencesDataStore("wpc_avatars")

/**
 * Saves avatar bitmaps to the app's internal filesDir and stores the path
 * in DataStore keyed by userId. Using internal storage means:
 *  - No extra permissions needed
 *  - The file persists across reboots
 *  - The gallery URI granted by the photo picker doesn't expire
 */
object AvatarManager {

    private fun avatarKey(userId: String) = stringPreferencesKey("avatar_$userId")

    /** Observe the avatar file path for a given user. Emits null when not set. */
    fun observeAvatarPath(context: Context, userId: String): Flow<String?> =
        context.avatarDataStore.data.map { prefs -> prefs[avatarKey(userId)] }

    /**
     * Save a bitmap (already cropped to square) as a JPEG in internal storage.
     * Overwrites any existing avatar for this user.
     */
    suspend fun saveAvatar(context: Context, userId: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val dir  = File(context.filesDir, "avatars").also { it.mkdirs() }
            val file = File(dir, "avatar_$userId.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val path = file.absolutePath
            context.avatarDataStore.edit { prefs ->
                prefs[avatarKey(userId)] = path
            }
            path
        }

    /** Delete the saved avatar and clear the DataStore entry. */
    suspend fun deleteAvatar(context: Context, userId: String) =
        withContext(Dispatchers.IO) {
            val dir  = File(context.filesDir, "avatars")
            val file = File(dir, "avatar_$userId.jpg")
            if (file.exists()) file.delete()
            context.avatarDataStore.edit { prefs ->
                prefs.remove(avatarKey(userId))
            }
        }

    /**
     * Decode a content:// URI returned by the photo picker or camera into a
     * Bitmap, scaling it down to at most [maxDim] pixels on either side
     * to keep memory use reasonable.
     */
    suspend fun decodeBitmap(context: Context, uri: Uri, maxDim: Int = 1024): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    // First pass — decode bounds only
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, opts)
                    val sample = maxOf(
                        opts.outWidth  / maxDim,
                        opts.outHeight / maxDim,
                        1,
                    )
                    // Second pass — decode with sub-sampling
                    context.contentResolver.openInputStream(uri)?.use { s2 ->
                        BitmapFactory.decodeStream(s2, null, BitmapFactory.Options().apply {
                            inSampleSize = sample
                        })
                    }
                }
            } catch (e: Exception) { null }
        }

    /**
     * Create a temporary file URI suitable for passing to ACTION_IMAGE_CAPTURE.
     * The camera app writes the full-res photo here; we read it back afterward.
     */
    fun createCameraUri(context: Context): Uri {
        val dir  = File(context.filesDir, "avatars").also { it.mkdirs() }
        val file = File(dir, "camera_temp.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
