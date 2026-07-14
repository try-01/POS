package com.pos.offline.ui.components

import com.pos.offline.data.local.entity.DiscountType
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.util.toRupiah

/**
 * Label metode bayar yang ramah pengguna (Tunai/QRIS/dst). Sumber TUNGGAL —
 * dipakai di PosScreen (SuccessDialog), ReportScreen (Detail Transaksi), dan
 * ReceiptManager (struk cetak) — supaya penamaan selalu konsisten di semua
 * tempat, tidak ada beberapa salinan berbeda yang berpotensi menyimpang.
 */
fun paymentMethodLabel(raw: String): String = when (raw) {
    PaymentMethod.CASH.name -> "Tunai"
    PaymentMethod.QRIS.name -> "QRIS"
    PaymentMethod.TRANSFER.name -> "Transfer"
    PaymentMethod.OTHER.name -> "Lainnya"
    else -> raw
}

/**
 * Format angka persen tanpa desimal berlebih (10.0 -> "10", 7.5 -> "7.5").
 * Dipakai untuk nilai persen diskon/pajak yang diketik manual (Double) di
 * berbagai field input, maupun untuk menampilkan ulang [TransactionEntity.discountValue].
 */
fun formatPercentTrim(value: Double): String {
    val i = value.toLong()
    return if (value == i.toDouble()) i.toString() else value.toString()
}

/**
 * Label deskriptif diskon UNTUK BARIS DUA-KOLOM (label di kiri, nominal di
 * kanan) — mis. dipasangkan dengan `SummaryLine("Diskon (10%)", "- Rp 5.000")`
 * atau dua kolom struk cetak. Mengembalikan null kalau tidak ada diskon sama
 * sekali (`discount <= 0`).
 *
 * Membaca [TransactionEntity.discountType]/[discountValue] — snapshot audit
 * "apa yang diketik kasir" — TIDAK mempengaruhi [TransactionEntity.discount]
 * (nominal final) yang tetap satu-satunya sumber kebenaran kalkulasi.
 */
fun TransactionEntity.discountRowLabel(): String? {
    if (discount <= 0L) return null
    return if (discountType == DiscountType.PERCENT.name) {
        "Diskon (${formatPercentTrim(discountValue)}%)"
    } else {
        "Diskon"
    }
}

/**
 * Label diskon LENGKAP SATU-BARIS (label + nominal tergabung) — dipakai di
 * daftar teks vertikal seperti SuccessDialog, BUKAN tabel dua-kolom.
 * Contoh: "Diskon: 10% (Rp 5.000)" atau "Diskon: Rp 5.000".
 * Mengembalikan null kalau tidak ada diskon sama sekali.
 */
fun TransactionEntity.discountInlineLabel(): String? {
    if (discount <= 0L) return null
    return if (discountType == DiscountType.PERCENT.name) {
        "Diskon: ${formatPercentTrim(discountValue)}% (${discount.toRupiah()})"
    } else {
        "Diskon: ${discount.toRupiah()}"
    }
}