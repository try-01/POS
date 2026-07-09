package com.kasirku.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Header transaksi (satu baris = satu struk/nota penjualan).
 * Nilai finansial (subtotal/diskon/pajak/total) disimpan sebagai snapshot pada saat
 * transaksi terjadi, TIDAK dihitung ulang dari harga produk saat ini — supaya laporan
 * historis tidak berubah walau harga produk kemudian di-update.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val invoiceNumber: String,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val paidAmount: Double,
    val changeAmount: Double,
    val paymentMethod: String = "CASH",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Detail item per transaksi (relasi one-to-many terhadap [TransactionEntity]).
 * `productName` & `priceAtSale` disimpan terpisah (denormalisasi ringan) sebagai snapshot,
 * agar detail struk lama tetap akurat walau nama/harga produk asli sudah berubah/dihapus.
 */
@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE // hapus header -> detail ikut terhapus otomatis
        )
    ],
    indices = [Index("transactionId"), Index("productId")]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val priceAtSale: Double,
    val subtotal: Double
)
