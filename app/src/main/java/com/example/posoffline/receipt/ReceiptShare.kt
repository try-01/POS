package com.example.posoffline.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Helpers for sharing receipt files from the app's private cache.
 *
 * We always write to the app's own cache dir (no permissions needed) and
 * expose the result through a [FileProvider] URI, so the share works on
 * Android 7+ (FileUriExposedException protection).
 */
object ReceiptShare {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun shareText(ctx: Context, text: String, invoiceNo: String) {
        val file = File(ctx.cacheDir, "$invoiceNo.txt")
        file.writeText(text)
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + AUTHORITY_SUFFIX, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Bagikan struk (ESC/POS)"))
    }

    fun sharePng(ctx: Context, bitmap: Bitmap, invoiceNo: String) {
        val file = File(ctx.cacheDir, "$invoiceNo.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + AUTHORITY_SUFFIX, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Bagikan struk"))
    }
}
