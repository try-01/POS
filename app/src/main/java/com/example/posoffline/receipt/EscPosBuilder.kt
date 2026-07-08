package com.example.posoffline.receipt

import com.example.posoffline.data.SettingsRepository
import com.example.posoffline.data.entity.TransactionEntity
import com.example.posoffline.data.entity.TransactionItem
import com.example.posoffline.util.Money

/**
 * Builds an ESC/POS-friendly plain text receipt.
 *
 * The output is intentionally simple plain text (32-char columns) so it
 * works on virtually any 58mm/80mm thermal printer that accepts raw text
 * over a Bluetooth serial socket. When you wire up the printer (via a
 * companion app or a custom Android `BluetoothSocket` flow), pipe this
 * string straight to the OutputStream.
 */
object EscPosBuilder {

    fun build(t: TransactionEntity, items: List<TransactionItem>, s: SettingsRepository.Snapshot): String {
        val line = "-".repeat(32)
        val out = StringBuilder()
        out.append(s.storeName.uppercase()).append('\n')
        out.append(s.storeAddress).append('\n')
        out.append(line).append('\n')
        out.append("No   : ${t.invoiceNo}").append('\n')
        out.append("Tgl  : ${formatDate(t.createdAt)}").append('\n')
        out.append(line).append('\n')
        for (it in items) {
            out.append(it.name).append('\n')
            out.append("  ${it.qty} x ${Money.format(it.price, s.currency)}")
            if (it.discount > 0) out.append("  -${Money.format(it.discount, s.currency)}")
            out.append('\n')
        }
        out.append(line).append('\n')
        out.append("Subtotal   : ${Money.format(t.subtotal, s.currency)}").append('\n')
        if (t.discount > 0) out.append("Diskon     : -${Money.format(t.discount, s.currency)}").append('\n')
        out.append("Pajak (${(t.taxRate * 100).toInt()}%): ${Money.format(t.tax, s.currency)}").append('\n')
        out.append("TOTAL      : ${Money.format(t.grandTotal, s.currency)}").append('\n')
        out.append("Bayar      : ${Money.format(t.paid, s.currency)}").append('\n')
        out.append("Kembali    : ${Money.format(t.change, s.currency)}").append('\n')
        out.append("Metode     : ${t.paymentMethod}").append('\n')
        out.append(line).append('\n')
        out.append("Terima kasih atas kunjungannya!").append('\n')
        return out.toString()
    }

    private fun formatDate(ts: Long): String =
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("id", "ID"))
            .format(java.util.Date(ts))
}
