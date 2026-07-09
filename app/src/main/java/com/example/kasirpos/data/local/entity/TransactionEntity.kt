package com.example.kasirpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Header transaksi — satu record per checkout.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Total harga sebelum diskon & pajak */
    val subtotal: Long,

    /** Total diskon keseluruhan (Rupiah) */
    val totalDiscount: Long,

    /** Persentase pajak (misal: 11 untuk PPN 11%) */
    val taxPercentage: Int = 0,

    /** Nominal pajak dalam Rupiah */
    val taxAmount: Long = 0,

    /** Grand total = subtotal - totalDiscount + taxAmount */
    val grandTotal: Long,

    /** Metode pembayaran: "cash", "qris", "debit", dll */
    val paymentMethod: String = "cash",

    /** Nominal uang tunai yang dibayarkan pelanggan (untuk hitung kembalian) */
    val cashReceived: Long = 0,

    /** Kembalian = cashReceived - grandTotal */
    val change: Long = 0,

    /** Timestamp Unix transaksi */
    val createdAt: Long = System.currentTimeMillis()
)
