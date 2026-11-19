package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageUtils {
    private val bitmapCache = LruCache<String, ImageBitmap>(60)

    fun decodeDataUri(dataUri: String): ImageBitmap? {
        if (dataUri.isBlank()) return null
        val cacheKey = dataUri.hashCode().toString() + "_" + dataUri.length
        bitmapCache.get(cacheKey)?.let { return it }

        return runCatching {
            val base64Part = dataUri.substringAfter("base64,", dataUri)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val imageBitmap = bmp.asImageBitmap()
            bitmapCache.put(cacheKey, imageBitmap)
            imageBitmap
        }.getOrNull()
    }

    fun compressUriToDataUri(context: Context, uri: Uri, maxDim: Int = 220, quality: Int = 45): String? {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sampleSize = 1
            val largest = max(bounds.outWidth, bounds.outHeight)
            while (largest / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null

            val scale = min(1f, maxDim.toFloat() / max(original.width, original.height).toFloat())
            val targetW = max(1, (original.width * scale).roundToInt())
            val targetH = max(1, (original.height * scale).roundToInt())
            val resized = if (targetW != original.width || targetH != original.height) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (resized !== original) resized.recycle()
            original.recycle()

            val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        }.getOrNull()
    }
}
