package com.pos.offline.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["createdAt"])]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String, // nomor invoice, mis. "INV-1700000000000"
    val createdAt: Long, // epoch millis — dipakai filter harian & urutan
    val subtotal: Long, // Σ (harga × qty) sebelum diskon/pajak
    val discount: Long, // nominal diskon FINAL (Rupiah) — sumber kebenaran kalkulasi
    val tax: Long, // nominal pajak (Rupiah), dihitung setelah diskon
    val total: Long, // subtotal - diskon + pajak (yang harus dibayar)
    val paidAmount: Long, // uang diterima dari pelanggan
    val change: Long, // paidAmount - total (boleh negatif = kurang bayar/nego)
    // Kembalian yang BENAR-BENAR diserahkan secara fisik ke pembeli.
    // - Saat change <= 0 (kurang bayar/pas): selalu 0, tidak relevan.
    // - Saat change > 0: 0 <= changeGiven <= change. Selisihnya = tip yang
    //   sengaja tidak diambil pembeli sebagai ucapan terima kasih.
    // Sumber kebenaran untuk uang FISIK yang masuk laci: paidAmount - changeGiven.
    @ColumnInfo(defaultValue = "0")
    val changeGiven: Long = 0L,
    @ColumnInfo(defaultValue = "'CASH'")
    val paymentMethod: String = PaymentMethod.CASH.name,
    val cashierId: Long? = null,
    @ColumnInfo(defaultValue = "''")
    val cashierName: String = "",
    val shiftId: Long? = null,
    @ColumnInfo(defaultValue = "'NOMINAL'")
    val discountType: String = DiscountType.NOMINAL.name,
    @ColumnInfo(defaultValue = "0.0")
    val discountValue: Double = 0.0,
    @ColumnInfo(defaultValue = "'COMPLETED'")
    val status: String = TransactionStatus.COMPLETED.name,
    val voidedAt: Long? = null,
    val voidReason: String? = null,
    val returnId: Long? = null,
)

@Entity(
    tableName = "transaction_items",
    indices = [
        Index(value = ["transactionId"]), // lookup item per struk cepat
        Index(value = ["productId"]) // lookup produk terlaris cepat
    ], 
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
    val unitCost: Long = 0L,
    val productId: Long? = null,
)

val TransactionEntity.hasReturn: Boolean
    get() = returnId != null
