package com.example.kasirpos.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utility ekspor struk ke file PDF.
 * Menggunakan Android PdfDocument API bawaan — tanpa library tambahan.
 */
object PdfExportUtil {

    /**
     * Generate PDF struk dan simpan ke Downloads/Struk/.
     *
     * @return File PDF yang sudah disimpan, atau null jika gagal
     */
    suspend fun exportReceiptToPdf(
        context: Context,
        storeName: String,
        receiptLines: List<String>,  // Setiap string = 1 baris di struk
        fileName: String = "Struk_${System.currentTimeMillis()}.pdf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pageWidth = 226  // 80mm ≈ 226px pada 72dpi
            val lineHeight = 14  // px per baris
            val marginH = 12
            val marginV = 16
            val pageHeight = marginV * 2 + receiptLines.size * lineHeight + 60

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // ── Paint Styles ─────────────────────────────────────
            val paintHeader = Paint().apply {
                textSize = 11f; typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER; isAntiAlias = true
            }
            val paintNormal = Paint().apply {
                textSize = 9f; typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.LEFT; isAntiAlias = true
            }
            val paintDivider = Paint().apply {
                textSize = 9f; typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER; isAntiAlias = true
            }

            var y = marginV.toFloat()

            // Header toko
            canvas.drawText(storeName, pageWidth / 2f, y, paintHeader)
            y += lineHeight * 1.5f

            // Garis divider
            canvas.drawText("═".repeat(36), pageWidth / 2f, y, paintDivider)
            y += lineHeight

            // Isi struk
            for (line in receiptLines) {
                canvas.drawText(line, marginH.toFloat(), y, paintNormal)
                y += lineHeight
            }

            // Footer
            y += lineHeight * 0.5f
            canvas.drawText("═".repeat(36), pageWidth / 2f, y, paintDivider)
            y += lineHeight
            canvas.drawText("TERIMA KASIH", pageWidth / 2f, y, paintHeader)

            document.finishPage(page)

            // ── Simpan file ──────────────────────────────────────
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Struk"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            document.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
