package com.example.kasirpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Detail item dalam satu transaksi — snapshot harga & qty saat checkout.
 */
@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Foreign key ke transactions.id */
    val transactionId: Long,

    /** Nama produk (snapshot — tidak terpengaruh rename di masa depan) */
    val productName: String,

    /** SKU produk (snapshot) */
    val productSku: String,

    /** Kuantitas terjual */
    val quantity: Int,

    /** Harga satuan saat transaksi */
    val unitPrice: Long,

    /** Diskon per-item saat transaksi */
    val discount: Long,

    /** Subtotal baris = (unitPrice * quantity) - discount */
    val lineTotal: Long
)
