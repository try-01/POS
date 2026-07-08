package com.example.posoffline.receipt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.posoffline.data.SettingsRepository
import com.example.posoffline.data.entity.TransactionEntity
import com.example.posoffline.data.entity.TransactionItem
import com.example.posoffline.util.Money

/**
 * Renders a receipt to an off-screen Android [Bitmap].
 *
 * Why offscreen rendering?
 *  - Avoids the cost of a Compose re-composition per row.
 *  - Output is a single image that can be saved, shared, or sent to a
 *    printer driver that accepts PNG (e.g. some Bluetooth thermal printers
 *    on Android).
 *
 * Cost: O(items) per receipt. Cheap enough for typical POS line counts.
 */
object ReceiptRenderer {

    fun render(
        tx: TransactionEntity,
        items: List<TransactionItem>,
        s: SettingsRepository.Snapshot,
        widthPx: Int = 384
    ): Bitmap {
        // 1) First pass: measure height with the chosen text size.
        val padding = 16
        var y = padding + 8
        val tmp = Bitmap.createBitmap(widthPx, 1, Bitmap.Config.ARGB_8888)
        val tmpC = Canvas(tmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        fun lineH(size: Float): Float = size + 4
        // Advance the cursor in *pixels*. We use Int because y itself is Int.
        fun advance(px: Int, extra: Int = 0) { y += px + extra }
        fun center(text: String, size: Float, weight: Int) {
            paint.textSize = size
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, weight)
            val w = paint.measureText(text)
            tmpC.drawText(text, (widthPx - w) / 2f, y.toFloat(), paint)
            advance(size.toInt() + 4)
        }
        fun left(text: String, size: Float, weight: Int) {
            paint.textSize = size
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, weight)
            tmpC.drawText(text, padding.toFloat(), y.toFloat(), paint)
            advance(size.toInt() + 2)
        }
        fun divider() { y += 10 }
        fun row(l: String, r: String, bold: Boolean = false) {
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            tmpC.drawText(l, padding.toFloat(), y.toFloat(), paint)
            val w = paint.measureText(r)
            tmpC.drawText(r, (widthPx - padding - w).toFloat(), y.toFloat(), paint)
            advance(15, 2) // 13px text height + 2px leading
        }
        // measure
        center(s.storeName, 17f, Typeface.BOLD)
        center(s.storeAddress, 12f, Typeface.NORMAL)
        divider()
        left("No   : ${tx.invoiceNo}", 13f, Typeface.NORMAL)
        left("Tgl  : ${formatDate(tx.createdAt)}", 13f, Typeface.NORMAL)
        divider()
        for (it in items) {
            left(it.name, 13f, Typeface.BOLD)
            row(
                "  ${it.qty} x ${Money.format(it.price, s.currency)}",
                Money.format(it.qty * it.price - it.discount, s.currency)
            )
        }
        divider()
        row("Subtotal", Money.format(tx.subtotal, s.currency))
        if (tx.discount > 0) row("Diskon", "-${Money.format(tx.discount, s.currency)}")
        row("Pajak (${(tx.taxRate * 100).toInt()}%)", Money.format(tx.tax, s.currency))
        row("TOTAL", Money.format(tx.grandTotal, s.currency), bold = true)
        row("Bayar", Money.format(tx.paid, s.currency))
        row("Kembali", Money.format(tx.change, s.currency))
        row("Metode", tx.paymentMethod)
        divider()
        y += 4
        center("Terima kasih atas kunjungannya!", 12f, Typeface.NORMAL)
        y += padding

        // 2) Allocate the real bitmap of exact height.
        val final = Bitmap.createBitmap(widthPx, y, Bitmap.Config.ARGB_8888)
        val c = Canvas(final)
        c.drawColor(Color.WHITE)
        var y2 = padding + 8
        paint.color = Color.parseColor("#0f172a")
        fun center2(text: String, size: Float, weight: Int) {
            paint.textSize = size
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, weight)
            val w = paint.measureText(text)
            c.drawText(text, (widthPx - w) / 2f, y2.toFloat(), paint)
            y2 += (size + 4).toInt()
        }
        fun left2(text: String, size: Float, weight: Int) {
            paint.textSize = size
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, weight)
            c.drawText(text, padding.toFloat(), y2.toFloat(), paint)
            y2 += (size + 2).toInt()
        }
        fun divider2() {
            paint.color = Color.parseColor("#cbd5e1")
            c.drawLine(padding.toFloat(), y2 + 4f, (widthPx - padding).toFloat(), y2 + 4f, paint)
            y2 += 10
            paint.color = Color.parseColor("#0f172a")
        }
        fun row2(l: String, r: String, bold: Boolean = false) {
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            paint.color = if (bold) Color.parseColor("#0f172a") else Color.parseColor("#334155")
            c.drawText(l, padding.toFloat(), y2.toFloat(), paint)
            val w = paint.measureText(r)
            c.drawText(r, (widthPx - padding - w).toFloat(), y2.toFloat(), paint)
            y2 += 16
        }
        center2(s.storeName, 17f, Typeface.BOLD)
        center2(s.storeAddress, 12f, Typeface.NORMAL)
        divider2()
        left2("No   : ${tx.invoiceNo}", 13f, Typeface.NORMAL)
        left2("Tgl  : ${formatDate(tx.createdAt)}", 13f, Typeface.NORMAL)
        divider2()
        for (it in items) {
            left2(it.name, 13f, Typeface.BOLD)
            row2(
                "  ${it.qty} x ${Money.format(it.price, s.currency)}",
                Money.format(it.qty * it.price - it.discount, s.currency)
            )
        }
        divider2()
        row2("Subtotal", Money.format(tx.subtotal, s.currency))
        if (tx.discount > 0) row2("Diskon", "-${Money.format(tx.discount, s.currency)}")
        row2("Pajak (${(tx.taxRate * 100).toInt()}%)", Money.format(tx.tax, s.currency))
        row2("TOTAL", Money.format(tx.grandTotal, s.currency), bold = true)
        row2("Bayar", Money.format(tx.paid, s.currency))
        row2("Kembali", Money.format(tx.change, s.currency))
        row2("Metode", tx.paymentMethod)
        divider2()
        y2 += 4
        center2("Terima kasih atas kunjungannya!", 12f, Typeface.NORMAL)
        return final
    }

    private fun formatDate(ts: Long): String =
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("id", "ID"))
            .format(java.util.Date(ts))
}
