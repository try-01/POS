package com.pos.offline.data.local.entity

import androidx.room.ColumnInfo
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
    val discount: Long,      // nominal diskon FINAL (Rupiah) — sumber kebenaran kalkulasi
    val tax: Long,           // nominal pajak (Rupiah), dihitung setelah diskon
    val total: Long,         // subtotal - diskon + pajak (yang harus dibayar)
    val paidAmount: Long,    // uang diterima dari pelanggan
    val change: Long,        // kembalian = max(0, paidAmount - total)

    // ============ KOLOM v3 — lihat Migrations.MIGRATION_2_3 ============
    @ColumnInfo(defaultValue = "'CASH'")
    val paymentMethod: String = PaymentMethod.CASH.name,

    val cashierId: Long? = null,

    @ColumnInfo(defaultValue = "''")
    val cashierName: String = "",

    val shiftId: Long? = null,

    // ============ KOLOM v5 — lihat Migrations.MIGRATION_4_5 ============
    // Snapshot AUDIT: apa yang benar-benar diketik kasir saat memberi
    // diskon. TIDAK dipakai untuk kalkulasi ulang apa pun — `discount`
    // di atas tetap satu-satunya sumber kebenaran nominal final.
    @ColumnInfo(defaultValue = "'NOMINAL'")
    val discountType: String = DiscountType.NOMINAL.name,

    @ColumnInfo(defaultValue = "0.0")
    val discountValue: Double = 0.0
)

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
    val lineTotal: Long,

    @ColumnInfo(defaultValue = "0")
    val unitCost: Long = 0L
)