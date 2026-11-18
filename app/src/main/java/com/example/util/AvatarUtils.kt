package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AvatarUtils {
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    suspend fun encodeUriToAvatarData(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.applicationContext.contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (maxDim / sampleSize > 512) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext Result.failure(IllegalStateException("Could not read selected image."))

            val size = minOf(decoded.width, decoded.height)
            val x = (decoded.width - size) / 2
            val y = (decoded.height - size) / 2
            val cropped = Bitmap.createBitmap(decoded, x, y, size, size)
            val scaled = Bitmap.createScaledBitmap(cropped, 160, 160, true)

            val output = ByteArrayOutputStream()
            var quality = 82
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            while (output.size() > 60_000 && quality > 35) {
                output.reset()
                quality -= 12
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }

            if (cropped !== decoded) cropped.recycle()
            if (scaled !== cropped) scaled.recycle()
            decoded.recycle()

            val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            Result.success("$DATA_URI_PREFIX$base64")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeAvatarBitmap(avatarData: String): ImageBitmap? {
        if (!avatarData.startsWith(DATA_URI_PREFIX)) return null
        return try {
            val base64 = avatarData.removePrefix(DATA_URI_PREFIX)
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
