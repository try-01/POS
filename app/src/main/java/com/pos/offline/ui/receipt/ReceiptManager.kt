package com.pos.offline.ui.receipt
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.StoreProfileEntity
import com.pos.offline.data.local.entity.isVoid
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.SalesReportData
import com.pos.offline.ui.components.paymentMethodLabel
import com.pos.offline.util.toRupiah
import com.pos.offline.util.formatQuantity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Color as AndroidColor
enum class ReceiptAlign { LEFT, CENTER, RIGHT }
data class ReceiptLine(
    val left: String = "",
    val right: String = "",
    val align: ReceiptAlign = ReceiptAlign.LEFT,
    val bold: Boolean = false,
    val large: Boolean = false,
)
object ReceiptManager {
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))
    fun buildLines(
        result: CheckoutResult,
        storeProfile: StoreProfileEntity? = null,
    ): List<ReceiptLine> {
        val tx = result.transaction
        val lines = mutableListOf<ReceiptLine>()
        val storeName = storeProfile?.storeName?.trim()
        if (!storeName.isNullOrEmpty()) {
            lines += ReceiptLine(left = storeName, align = ReceiptAlign.CENTER, bold = true, large = true)
        }
        val address = storeProfile?.address?.trim()
        if (!address.isNullOrEmpty()) {
            address.split("\n").forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty()) {
                    lines += ReceiptLine(left = line, align = ReceiptAlign.CENTER)
                }
            }
        }
        lines += divider()
        val dateStr = dateFmt.format(Date(tx.createdAt))
        val invStr = tx.id
        lines += ReceiptLine(left = dateStr, right = invStr)
        val cashier = tx.cashierName.trim()
        val shiftId = tx.shiftId?.toString() ?: ""
        if (cashier.isNotEmpty() || shiftId.isNotEmpty()) {
            val left = cashier
            val right = if (shiftId.isNotEmpty()) "Shift ID: $shiftId" else ""
            if (left.isNotEmpty() && right.isNotEmpty()) {
                lines += ReceiptLine(left = left, right = right)
            } else if (left.isNotEmpty()) {
                lines += ReceiptLine(left = left)
            } else if (right.isNotEmpty()) {
                lines += ReceiptLine(left = "", right = right)
            }
        }
        if (tx.isVoid) {
            lines += ReceiptLine(left = "*** TRANSAKSI DIBATALKAN ***", align = ReceiptAlign.CENTER, bold = true)
        }
        lines += divider()
        for (item in result.items) {
            val name = item.productName.trim().ifEmpty { "(Tanpa nama)" }
            if (item.quantity > 1.0) {
                lines += ReceiptLine(left = name, bold = true)
                lines += ReceiptLine(left = "  ${item.quantity.formatQuantity()} x ${item.unitPrice.toRupiah()}", right = item.lineTotal.toRupiah())
            } else {
                lines += ReceiptLine(left = name, right = item.lineTotal.toRupiah(), bold = true)
            }
        }
        lines += ReceiptLine(left = "")
        lines += ReceiptLine(left = "TOTAL: ${tx.total.toRupiah()}", align = ReceiptAlign.CENTER, bold = true, large = true)
        lines += divider()
        val gridItems = mutableListOf<Pair<String, String>>()
        val payLabel = paymentMethodLabel(tx.paymentMethod)
        gridItems.add(Pair(payLabel, tx.paidAmount.toRupiah()))
        if (tx.change > 0) {
            gridItems.add(Pair("Kembali", tx.change.toRupiah()))
        } else if (tx.change < 0) {
            gridItems.add(Pair("Kurang Bayar", kotlin.math.abs(tx.change).toRupiah()))
        }
        if (tx.discount > 0) gridItems.add(Pair("Diskon", tx.discount.toRupiah()))
        if (tx.tax > 0) gridItems.add(Pair("Pajak", tx.tax.toRupiah()))
        gridItems.chunked(2).forEach { chunk ->
            if (chunk.size == 2) {
                lines += ReceiptLine(left = "${chunk[0].first}: ${chunk[0].second}", right = "${chunk[1].first}: ${chunk[1].second}")
            } else {
                lines += ReceiptLine(left = "${chunk[0].first}: ${chunk[0].second}")
            }
        }
        lines += divider()
        val footerNote = storeProfile?.footerNote?.trim()
        if (!footerNote.isNullOrEmpty()) {
            footerNote.split("\n").forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty()) {
                    lines += ReceiptLine(left = line, align = ReceiptAlign.CENTER)
                }
            }
        }
        lines += ReceiptLine(left = "")
        return lines
    }
    private fun divider(): ReceiptLine = ReceiptLine(left = "--------------------------------", align = ReceiptAlign.CENTER)
    suspend fun exportToPdf(
        context: Context,
        result: CheckoutResult,
        storeProfile: StoreProfileEntity? = null,
    ): File =
        withContext(Dispatchers.IO) {
            val lines = buildLines(result, storeProfile)
            exportPdfFromLines(context, lines, result.transaction.id)
        }
    suspend fun exportPdfFromLines(
        context: Context,
        lines: List<ReceiptLine>,
        fileName: String,
    ): File =
        withContext(Dispatchers.IO) {
            val pageWidth = 240
            val margin = 14f
            val lineHeight = 20f
            val pageHeight = (lines.size * lineHeight + 2 * margin).toInt().coerceAtLeast(320)
            val document = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                var y = margin + 14f
                for (line in lines) {
                    drawLine(canvas, line, pageWidth.toFloat(), margin, y)
                    y += lineHeight
                }
                document.finishPage(page)
                val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "reports").apply { mkdirs() }
                val file = File(dir, "$fileName.pdf")
                FileOutputStream(file).use { document.writeTo(it) }
                file
            } finally {
                document.close()
            }
        }
    fun renderToBitmap(
        result: CheckoutResult,
        scale: Int = 3,
        storeProfile: StoreProfileEntity? = null,
    ): Bitmap {
        val lines = buildLines(result, storeProfile)
        val w = 240 * scale
        val lineHeight = 22f * scale
        val margin = 16f * scale
        val h = (lines.size * lineHeight + 2 * margin).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp).apply { drawColor(AndroidColor.WHITE) }
        var y = margin + lineHeight
        for (line in lines) {
            drawLine(canvas, line, w.toFloat(), margin, y, scale.toFloat())
            y += lineHeight
        }
        return bmp
    }
    private fun drawLine(
        canvas: Canvas,
        line: ReceiptLine,
        pageWidth: Float,
        margin: Float,
        y: Float,
        scale: Float = 1f,
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.BLACK
                textSize = (if (line.large) 16f else 11f) * scale
                typeface = if (line.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        val rightPaint =
            if (line.bold && line.right.isNotEmpty()) {
                Paint(paint).apply { typeface = Typeface.DEFAULT }
            } else {
                paint
            }
        if (line.right.isNotEmpty()) {
            canvas.drawText(line.left, margin, y, paint)
            val rightW = rightPaint.measureText(line.right)
            canvas.drawText(line.right, pageWidth - margin - rightW, y, rightPaint)
        } else {
            when (line.align) {
                ReceiptAlign.LEFT -> {
                    canvas.drawText(line.left, margin, y, paint)
                }
                ReceiptAlign.CENTER -> {
                    val tw = paint.measureText(line.left)
                    canvas.drawText(line.left, (pageWidth - tw) / 2f, y, paint)
                }
                ReceiptAlign.RIGHT -> {
                    val tw = paint.measureText(line.left)
                    canvas.drawText(line.left, pageWidth - margin - tw, y, paint)
                }
            }
        }
    }
    fun buildShareIntent(
        context: Context,
        result: CheckoutResult,
        storeProfile: StoreProfileEntity? = null,
    ): Intent {
        val bitmap = renderToBitmap(result, storeProfile = storeProfile)
        val dir = File(context.cacheDir, "shared_receipts").apply { 
            mkdirs()
            listFiles()?.forEach { oldFile -> runCatching { oldFile.delete() } }
        }
        val file = File(dir, "${result.transaction.id}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return Intent.createChooser(sendIntent, "Bagikan Struk")
    }
    fun buildPdfShareIntent(
        context: Context,
        file: File,
    ): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return Intent.createChooser(sendIntent, "Bagikan Struk PDF")
    }
    fun linesToEscPosMarkup(lines: List<ReceiptLine>): String {
        val sb = StringBuilder()
        for (line in lines) {
            val alignTag = when (line.align) { ReceiptAlign.LEFT -> "[L]"; ReceiptAlign.CENTER -> "[C]"; ReceiptAlign.RIGHT -> "[R]" }
            val boldTag = if (line.bold) "<b>" else ""
            val boldEnd = if (line.bold) "</b>" else ""
            val sizeTag = if (line.large) "<font size='big'>" else ""
            val sizeEnd = if (line.large) "</font>" else ""
            if (line.right.isNotEmpty()) {
                sb.append("[L]$boldTag$sizeTag${line.left}$sizeEnd$boldEnd[R]$boldTag$sizeTag${line.right}$sizeEnd$boldEnd\n")
            } else {
                sb.append("$alignTag$boldTag$sizeTag${line.left}$sizeEnd$boldEnd\n")
            }
        }
        return sb.toString()
    }
    fun buildSalesReportLines(
        data: SalesReportData,
        storeProfile: StoreProfileEntity?,
        periodLabel: String,
        printedBy: String?,
        shiftId: String?,
        includeSalesSummary: Boolean = true,
        includeProductsSold: Boolean = true,
        includeDeadStock: Boolean = true,
    ): List<ReceiptLine> {
        val lines = mutableListOf<ReceiptLine>()
        val storeName = storeProfile?.storeName?.trim()?.ifBlank { "Kasir Offline" } ?: "Kasir Offline"
        lines += ReceiptLine(left = storeName, align = ReceiptAlign.CENTER, bold = true, large = true)
        lines += ReceiptLine(left = "Laporan Penjualan", align = ReceiptAlign.CENTER)
        lines += ReceiptLine(left = periodLabel, align = ReceiptAlign.CENTER)
        lines += ReceiptLine(left = "Dicetak: ${dateFmt.format(Date())}", align = ReceiptAlign.CENTER)
        if (printedBy != null) lines += ReceiptLine(left = "Kasir: $printedBy", right = shiftId?.let { "Shift: $it" } ?: "")
        if (includeSalesSummary) {
            lines += divider()
            lines += ReceiptLine(left = "Total Transaksi", right = "${data.summary.transactionCount} struk")
            lines += ReceiptLine(left = "Penjualan (sblm diskon)", right = data.summary.subtotalSum.toRupiah())
            if (data.diskon > 0) lines += ReceiptLine(left = "Diskon", right = "- ${data.diskon.toRupiah()}")
            if (data.summary.taxSum > 0) lines += ReceiptLine(left = "Pajak", right = data.summary.taxSum.toRupiah())
            lines += ReceiptLine(left = "PENDAPATAN BERSIH", right = data.pendapatanBersih.toRupiah(), bold = true)
            if (data.returnsTotal > 0) lines += ReceiptLine(left = "  (Termasuk Retur)", right = "- ${data.returnsTotal.toRupiah()}")
            lines += ReceiptLine(left = "Laba Bersih", right = data.labaBersih.toRupiah(), bold = true)
            lines += divider()
            lines += ReceiptLine(left = "Metode Pembayaran", align = ReceiptAlign.CENTER, bold = true)
            data.payments.forEach {
                val label = PaymentMethod.fromStorage(it.paymentMethod).label
                lines += ReceiptLine(left = label, right = "${it.count}x  ${it.actualReceived.toRupiah()}")
            }
            lines += divider()
        }
        if (includeProductsSold) {
            val soldProducts = data.products.filter { it.qtySold > 0.0 }
            lines += ReceiptLine(left = "--- Produk Terjual ---", align = ReceiptAlign.CENTER, bold = true)
            lines += divider()
            if (soldProducts.isEmpty()) {
                lines += ReceiptLine(left = "Tidak ada produk yang terjual pada periode ini.", align = ReceiptAlign.CENTER)
            } else {
                soldProducts.forEach { p ->
                    lines += ReceiptLine(left = p.productName, right = "${p.qtySold.formatQuantity()}x  ${p.revenue.toRupiah()}")
                }
            }
            lines += divider()
        }
        if (includeDeadStock) {
            val deadStock = data.products.filter { it.qtySold == 0.0 }
            lines += ReceiptLine(left = "--- Produk Tidak Laku ---", align = ReceiptAlign.CENTER, bold = true)
            lines += divider()
            if (deadStock.isEmpty()) {
                lines += ReceiptLine(left = "Tidak ada produk yang tidak laku pada periode ini.", align = ReceiptAlign.CENTER)
            } else {
                deadStock.forEach {
                    lines += ReceiptLine(left = it.productName, right = "0x")
                }
            }
            lines += divider()
        }
        lines += ReceiptLine(left = "")
        return lines
    }
}
