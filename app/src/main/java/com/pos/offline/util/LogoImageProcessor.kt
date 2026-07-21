package com.pos.offline.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class LogoImageProcessor(
    private val appContext: Context,
) {
    suspend fun process(uri: Uri): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val original = decodeBitmapSafely(uri) ?: return@withContext null
                val resized = resizeToFit(original, MAX_DIMENSION)
                val grayscale = toGrayscale(resized)
                val output = ByteArrayOutputStream()
                grayscale.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            } catch (e: Exception) {
                null
            }
        }

    private fun decodeBitmapSafely(uri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }

        val boundsStream = appContext.contentResolver.openInputStream(uri) ?: return null
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } // Tidak perlu ?: return null di sini, karena decodeStream memang pasti null

        val sampleSize = calculateInSampleSize(boundsOptions, MAX_DIMENSION * 2, MAX_DIMENSION * 2)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

        val decodeStream = appContext.contentResolver.openInputStream(uri) ?: return null
        return decodeStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun resizeToFit(
        bitmap: Bitmap,
        maxDimension: Int,
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint =
            Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }

    companion object {
        private const val MAX_DIMENSION = 300
    }
}
