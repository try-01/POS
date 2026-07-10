package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Header transaksi (1 struk = 1 baris). Nomor invoice [id] sebagai PK.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,          // nomor invoice, mis. "INV-1700000000000"
    val createdAt: Long,     // epoch millis — dipakai filter harian & urutan
    val subtotal: Long,      // Σ (harga × qty) sebelum diskon/pajak
    val discount: Long,      // nominal diskon (Rupiah)
    val tax: Long,           // nominal pajak (Rupiah), dihitung setelah diskon
    val total: Long,         // subtotal - diskon + pajak (yang harus dibayar)
    val paidAmount: Long,    // uang diterima dari pelanggan
    val change: Long         // kembalian = max(0, paidAmount - total)
)

/**
 * Detail item dari sebuah transaksi (baris produk dalam struk).
 * Di-snapshot (tidak FK ke produk) agar riwayat tidak berubah walau produk
 * master diubah/dihapus kemudian hari.
 */
@Entity(
    tableName = "transaction_items",
    indices = [Index(value = ["transactionId"])] // lookup item per struk cepat
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String,
    val productName: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineTotal: Long      // unitPrice × quantity (disimpan utk laporan cepat)
)
