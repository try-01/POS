package com.pos.offline.data.local.entity

/**
 * Tipe diskon level-struk yang dipilih kasir.
 *
 * NOMINAL — potongan harga tetap dalam Rupiah (mis. Rp 5.000).
 * PERCENT — potongan harga dalam persentase dari subtotal (mis. 10%).
 *
 * Disimpan sebagai TEXT (nama enum) di kolom `discountType` pada
 * [TransactionEntity] — pola sama seperti [PaymentMethod]. Nilai MENTAH
 * yang diketik kasir disimpan terpisah di `discountValue` (Double),
 * SEDANGKAN kolom `discount` (Long) tetap menyimpan NOMINAL FINAL hasil
 * konversi (setelah dibatasi maksimal sebesar subtotal) — satu-satunya
 * sumber kebenaran untuk semua kalkulasi total, laporan, dan ringkasan
 * shift. Pemisahan ini sengaja: `discountType`/`discountValue` MURNI
 * untuk audit/tampilan ("kasir tadi input diskon 10%, bukan Rp berapa"),
 * tidak pernah dibaca ulang untuk kalkulasi apa pun.
 */
enum class DiscountType {
    NOMINAL,
    PERCENT
}