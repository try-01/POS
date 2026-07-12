package com.pos.offline.data.local.entity

/**
 * Metode pembayaran transaksi. Disimpan sebagai String (nama enum) pada kolom
 * `paymentMethod` di [TransactionEntity] — bukan Int/ordinal — supaya aman
 * ditambah/diurutkan ulang di masa depan tanpa merusak data lama.
 */
enum class PaymentMethod(val label: String) {
    CASH("Tunai"),
    QRIS("QRIS"),
    TRANSFER("Transfer Bank"),
    OTHER("Lainnya");

    companion object {
        /** Parsing aman: nilai tak dikenal (mis. data korup) jatuh ke CASH. */
        fun fromStorage(value: String): PaymentMethod =
            entries.find { it.name == value } ?: CASH
    }
}