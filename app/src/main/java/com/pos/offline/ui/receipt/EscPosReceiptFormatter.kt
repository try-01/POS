package com.pos.offline.ui.receipt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.pos.offline.data.local.entity.StoreProfileEntity
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.TransactionItemEntity
import com.pos.offline.data.local.entity.isVoid
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.ui.components.paymentMethodLabel
import com.pos.offline.util.toRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatter MURNI: hanya menghasilkan [String] markup ESC/POS (syntax DantSu)
 * dari data transaksi + profil toko.
 */
object EscPosReceiptFormatter {

    private const val MAX_IMAGE_HEIGHT_PX = 256
    private val RESERVED_CHARS_REGEX = Regex("[\\[\\]<>]")

    private fun sanitize(text: String): String = text.replace(RESERVED_CHARS_REGEX, "")

    private val dateFormatterPattern = "dd/MM/yyyy HH:mm"

    fun build(
        printer: EscPosPrinter,
        checkoutResult: CheckoutResult,
        storeProfile: StoreProfileEntity
    ): List<String> = build(printer, checkoutResult.transaction, checkoutResult.items, storeProfile)

    fun build(
        printer: EscPosPrinter,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        storeProfile: StoreProfileEntity
    ): List<String> {
        val logoHex = buildLogoHex(printer, storeProfile.logoBytes)
        val logoMarkup = logoHex?.let { "[C]<img>$it</img>\n" }

        val sb = StringBuilder()
        val charsPerLine = printer.printerNbrCharactersPerLine

        // ---- 1. Header Toko ----
        // Poin 1: Jika nama toko atau alamat kosong, otomatis skip (tidak ada baris kosong)
        val storeName = sanitize(storeProfile.storeName).trim()
        if (storeName.isNotEmpty()) {
            sb.append("[C]<b><font size='big'>").append(storeName).append("</font></b>\n")
        }

        if (storeProfile.address.isNotBlank()) {
            appendCenteredMultiline(sb, storeProfile.address)
        }

        // Pembatas
        sb.append(divider(charsPerLine))

        // ---- 2. Info Transaksi (Tanggal, No Inv, Kasir, Shift) ----
        // Poin 2: Info dicetak sejajar kiri & kanan
        val dateFormatter = SimpleDateFormat(dateFormatterPattern, Locale.forLanguageTag("id-ID"))
        val dateStr = dateFormatter.format(Date(transaction.createdAt))
        val invStr = sanitize(transaction.id)
        
        sb.append("[L]").append(dateStr).append("[R]").append(invStr).append("\n")

        val cashier = sanitize(transaction.cashierName).trim()
        val shiftId = transaction.shiftId?.toString() ?: ""

        if (cashier.isNotEmpty() || shiftId.isNotEmpty()) {
            val left = cashier
            val right = if (shiftId.isNotEmpty()) "Shift ID: $shiftId" else ""

            if (left.isNotEmpty() && right.isNotEmpty()) {
                sb.append("[L]").append(left).append("[R]").append(right).append("\n")
            } else if (left.isNotEmpty()) {
                sb.append("[L]").append(left).append("\n")
            } else if (right.isNotEmpty()) {
                sb.append("[R]").append(right).append("\n")
            }
        }

        if (transaction.isVoid) {
            sb.append("[C]<b>*** TRANSAKSI DIBATALKAN ***</b>\n")
        }

        sb.append(divider(charsPerLine))

        // ---- 3. Daftar Item ----
        items.forEach { item ->
            val name = sanitize(item.productName).trim().ifEmpty { "(Tanpa nama)" }
            // Jika QTY > 1, pecah ke baris kedua agar tulisan Qty & Harga tidak nabrak Nama Produk
            if (item.quantity > 1) {
                sb.append("[L]<b>").append(name).append("</b>\n")
                sb.append("[L]  ").append(item.quantity).append(" x ").append(item.unitPrice.toRupiah())
                  .append("[R]").append(item.lineTotal.toRupiah()).append("\n")
            } else {
                // Jika QTY = 1, buat sebaris untuk menghemat kertas
                sb.append("[L]<b>").append(name).append("</b>[R]").append(item.lineTotal.toRupiah()).append("\n")
            }
        }

        // ---- 4. Total ----
        sb.append("\n") // Satu baris jarak eksklusif untuk Total sesuai desain
        sb.append("[C]<b><font size='wide'>TOTAL: ").append(transaction.total.toRupiah()).append("</font></b>\n")
        sb.append(divider(charsPerLine))

        // ---- 5. Grid Pembayaran Dinamis (Poin 3, 4, 5) ----
        // List dikumpulkan, lalu dicetak secara berpasangan [Kiri, Kanan]
        val gridItems = mutableListOf<Pair<String, String>>()
        
        // Label pembayaran disesuaikan dengan transaksinya (Misal: Tunai / QRIS)
        val payLabel = paymentMethodLabel(transaction.paymentMethod)
        gridItems.add(Pair(payLabel, transaction.paidAmount.toRupiah()))

        // Jika 0, item ini tidak akan di-push ke list (Otomatis tidak dicetak)
        if (transaction.change > 0) {
            gridItems.add(Pair("Kembali", transaction.change.toRupiah()))
        }
        if (transaction.discount > 0) {
            gridItems.add(Pair("Diskon", transaction.discount.toRupiah()))
        }
        if (transaction.tax > 0) {
            gridItems.add(Pair("Pajak", transaction.tax.toRupiah()))
        }

        // Pecah list menjadi per-2 data (kiri dan kanan)
        val chunks = gridItems.chunked(2)
        for (chunk in chunks) {
            if (chunk.size == 2) {
                sb.append("[L]").append(chunk[0].first).append(": ").append(chunk[0].second)
                sb.append("[R]").append(chunk[1].first).append(": ").append(chunk[1].second).append("\n")
            } else {
                // Skenario ganjil (sisa 1): cetak di kiri saja lalu langsung turun ke garis putus-putus
                sb.append("[L]").append(chunk[0].first).append(": ").append(chunk[0].second).append("\n")
            }
        }

        sb.append(divider(charsPerLine))

        // ---- 6. Footer (Poin 1) ----
        if (storeProfile.footerNote.isNotBlank()) {
            appendCenteredMultiline(sb, storeProfile.footerNote)
        }

        sb.append("[L]\n")

        return listOfNotNull(logoMarkup, sb.toString())
    }

    private fun divider(charsPerLine: Int): String {
        val safeLength = charsPerLine.coerceAtLeast(1)
        // Memakai karakter "-" agar sesuai dengan desain
        return "[C]" + "-".repeat(safeLength) + "\n"
    }

    private fun appendCenteredMultiline(sb: StringBuilder, rawText: String) {
        if (rawText.isBlank()) return
        rawText.split("\n").forEach { rawLine ->
            val line = sanitize(rawLine).trim()
            if (line.isNotEmpty()) {
                sb.append("[C]").append(line).append("\n")
            }
        }
    }

    private fun buildLogoHex(printer: EscPosPrinter, logoBytes: ByteArray?): String? {
        if (logoBytes == null) return null
        val original = try {
            BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)
        } catch (e: Exception) {
            null
        } ?: return null

        val resized = if (original.height > MAX_IMAGE_HEIGHT_PX) {
            val ratio = MAX_IMAGE_HEIGHT_PX.toFloat() / original.height
            val newWidth = (original.width * ratio).toInt().coerceAtLeast(1)
            try {
                Bitmap.createScaledBitmap(original, newWidth, MAX_IMAGE_HEIGHT_PX, true)
            } catch (e: Exception) {
                original
            }
        } else {
            original
        }

        return try {
            PrinterTextParserImg.bitmapToHexadecimalString(printer, resized, true)
        } catch (e: Exception) {
            null
        }
    }
}