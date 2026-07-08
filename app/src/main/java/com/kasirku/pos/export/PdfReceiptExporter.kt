package com.kasirku.pos.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfReceiptExporter(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 226
        private const val MARGIN = 16f
        private const val LINE_HEIGHT = 14f
    }

    private val titlePaint = Paint().apply {
        color = Color.BLACK; textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true
    }
    private val normalPaint = Paint().apply {
        color = Color.BLACK; textSize = 10f; textAlign = Paint.Align.LEFT; isAntiAlias = true
    }
    private val boldPaint = Paint().apply {
        color = Color.BLACK; textSize = 11f; textAlign = Paint.Align.LEFT; isFakeBoldText = true; isAntiAlias = true
    }
    private val rightPaint = Paint().apply {
        color = Color.BLACK; textSize = 10f; textAlign = Paint.Align.RIGHT; isAntiAlias = true
    }

    suspend fun exportToPdf(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        storeName: String = "KASIRKU POS"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pageHeight = calculatePageHeight(items.size)
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            var y = MARGIN + LINE_HEIGHT
            val centerX = PAGE_WIDTH / 2f

            canvas.drawText("★ \$storeName ★", centerX, y, titlePaint)
            y += LINE_HEIGHT + 2f
            normalPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Toko Serba Ada", centerX, y, normalPaint)
            normalPaint.textAlign = Paint.Align.LEFT
            y += LINE_HEIGHT * 1.5f

            canvas.drawText(transaction.invoiceNumber, MARGIN, y, normalPaint)
            y += LINE_HEIGHT
            canvas.drawText(formatDate(transaction.transactionDate), MARGIN, y, normalPaint)
            y += LINE_HEIGHT
            canvas.drawDashLine(y)
            y += LINE_HEIGHT

            items.forEach { item ->
                canvas.drawText(item.productName, MARGIN, y, normalPaint)
                y += LINE_HEIGHT
                canvas.drawText("  \${item.quantity}x @\${formatPrice(item.unitPrice)}", MARGIN, y, normalPaint)
                canvas.drawText(formatPrice(item.subtotal), PAGE_WIDTH - MARGIN, y, rightPaint)
                y += LINE_HEIGHT
            }

            canvas.drawDashLine(y)
            y += LINE_HEIGHT

            drawSummaryLine(canvas, "Subtotal", formatPrice(transaction.subtotal), y)
            y += LINE_HEIGHT
            if (transaction.taxPercent > 0) {
                drawSummaryLine(canvas, "Pajak (\${transaction.taxPercent.toInt()}%)", formatPrice(transaction.taxAmount), y)
                y += LINE_HEIGHT
            }
            canvas.drawDashLine(y)
            y += LINE_HEIGHT

            canvas.drawText("TOTAL", MARGIN, y, boldPaint)
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatPrice(transaction.totalAmount), PAGE_WIDTH - MARGIN, y, boldPaint)
            boldPaint.textAlign = Paint.Align.LEFT
            y += LINE_HEIGHT

            drawSummaryLine(canvas, "Bayar", formatPrice(transaction.paymentAmount), y)
            y += LINE_HEIGHT
            drawSummaryLine(canvas, "Kembali", formatPrice(transaction.changeAmount), y)
            y += LINE_HEIGHT * 2

            normalPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Terima kasih!", centerX, y, normalPaint)
            normalPaint.textAlign = Paint.Align.LEFT

            document.finishPage(page)

            val fileName = "Struk_\${transaction.invoiceNumber}.pdf"
            val filePath = saveToDownloads(document, fileName)
            document.close()

            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToDownloads(document: PdfDocument, fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KasirKu")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Gagal membuat file")
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            "Downloads/KasirKu/\$fileName"
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KasirKu")
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { document.writeTo(it) }
            file.absolutePath
        }
    }

    private fun Canvas.drawDashLine(y: Float) {
        val dashPaint = Paint().apply { color = Color.GRAY; textSize = 10f; textAlign = Paint.Align.LEFT }
        drawText("-".repeat(30), MARGIN, y, dashPaint)
    }

    private fun drawSummaryLine(canvas: Canvas, label: String, value: String, y: Float) {
        canvas.drawText(label, MARGIN, y, normalPaint)
        canvas.drawText(value, PAGE_WIDTH - MARGIN, y, rightPaint)
    }

    private fun calculatePageHeight(itemCount: Int): Int {
        val lines = 5 + (itemCount * 2.5) + 8 + 3
        return ((lines * LINE_HEIGHT) + (MARGIN * 2)).toInt()
    }

    private fun formatPrice(amount: Long): String = "Rp\${String.format("%,d", amount).replace(',', '.')}"
    private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}
