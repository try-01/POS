package com.kasirku.pos.ui.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kasirku.pos.data.local.relation.TransactionWithItems
import com.kasirku.pos.util.CurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generator struk PDF native menggunakan android.graphics.pdf.PdfDocument.
 * Hasil PDF disimpan sementara di cache directory dan langsung dimunculkan ke System Share Intent
 * agar kasir bisa membagikan struk ke WhatsApp / Email / File Manager tanpa koneksi internet.
 */
object PdfReceiptExporter {

    fun exportToPdfAndShare(context: Context, transactionWithItems: TransactionWithItems, storeName: String = "KASIRKU STORE") {
        val tx = transactionWithItems.transaction
        val items = transactionWithItems.items

        // Ukuran kertas PDF 58mm thermal (~240 pt width, tinggi dinamis sesuai jumlah item)
        val pageW = 300
        val pageH = 380 + (items.size * 36)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint(paint).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val titlePaint = Paint(boldPaint).apply {
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }

        val centerPaint = Paint(paint).apply {
            textAlign = Paint.Align.CENTER
        }

        val rightPaint = Paint(paint).apply {
            textAlign = Paint.Align.RIGHT
        }

        var y = 30f
        canvas.drawText(storeName, (pageW / 2).toFloat(), y, titlePaint)
        y += 16f
        canvas.drawText("Aplikasi POS 100% Offline & Cepat", (pageW / 2).toFloat(), y, centerPaint)
        y += 18f
        canvas.drawLine(15f, y, (pageW - 15).toFloat(), y, paint)
        y += 16f

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("in", "ID")).format(Date(tx.createdAt))
        canvas.drawText("No   : ${tx.invoiceNumber}", 15f, y, paint)
        y += 14f
        canvas.drawText("Waktu: $dateStr", 15f, y, paint)
        y += 16f
        canvas.drawLine(15f, y, (pageW - 15).toFloat(), y, paint)
        y += 16f

        // Items
        for (item in items) {
            canvas.drawText(item.productName, 15f, y, boldPaint)
            y += 14f
            val qtyPrice = "${item.quantity} x @${CurrencyFormatter.format(item.priceAtSale)}"
            val subTotalStr = CurrencyFormatter.format(item.subtotal)
            canvas.drawText(qtyPrice, 25f, y, paint)
            canvas.drawText(subTotalStr, (pageW - 15).toFloat(), y, rightPaint)
            y += 16f
        }

        canvas.drawLine(15f, y, (pageW - 15).toFloat(), y, paint)
        y += 16f

        // Totals
        canvas.drawText("Subtotal", 15f, y, paint)
        canvas.drawText(CurrencyFormatter.format(tx.subtotal), (pageW - 15).toFloat(), y, rightPaint)
        y += 14f

        if (tx.discountAmount > 0) {
            canvas.drawText("Diskon", 15f, y, paint)
            canvas.drawText("-${CurrencyFormatter.format(tx.discountAmount)}", (pageW - 15).toFloat(), y, rightPaint)
            y += 14f
        }

        if (tx.taxAmount > 0) {
            canvas.drawText("Pajak", 15f, y, paint)
            canvas.drawText(CurrencyFormatter.format(tx.taxAmount), (pageW - 15).toFloat(), y, rightPaint)
            y += 14f
        }

        canvas.drawText("TOTAL", 15f, y, boldPaint)
        canvas.drawText(CurrencyFormatter.format(tx.grandTotal), (pageW - 15).toFloat(), y, Paint(rightPaint).apply { typeface = Typeface.DEFAULT_BOLD })
        y += 18f

        canvas.drawText("Bayar (${tx.paymentMethod})", 15f, y, paint)
        canvas.drawText(CurrencyFormatter.format(tx.paidAmount), (pageW - 15).toFloat(), y, rightPaint)
        y += 14f
        canvas.drawText("Kembalian", 15f, y, boldPaint)
        canvas.drawText(CurrencyFormatter.format(tx.changeAmount), (pageW - 15).toFloat(), y, Paint(rightPaint).apply { typeface = Typeface.DEFAULT_BOLD })
        y += 24f

        canvas.drawText("--- TERIMA KASIH ---", (pageW / 2).toFloat(), y, centerPaint)
        y += 14f
        canvas.drawText("Powered by KasirKu POS Android", (pageW / 2).toFloat(), y, centerPaint)

        pdfDocument.finishPage(page)

        try {
            val cachePath = File(context.cacheDir, "receipts")
            cachePath.mkdirs()
            val file = File(cachePath, "Struk_${tx.invoiceNumber}.pdf")
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            // Share via FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Struk PDF via..."))
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
    }
}
