package com.example.posoffline.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted sale record. Items are serialized into [itemsJson] via
 * kotlinx.serialization inside [com.example.posoffline.data.repository.TransactionRepository]
 * before insert. For low-volume POS (hundreds of tx/day) this is faster
 * and simpler than a separate transaction_items table + JOIN. Switch to
 * a relational layout when item lists grow large or you need per-item
 * queries.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["invoice_no"], unique = true),
        Index(value = ["created_at"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "invoice_no") val invoiceNo: String,
    @ColumnInfo(name = "items_json") val itemsJson: String,
    @ColumnInfo(name = "subtotal") val subtotal: Long,
    @ColumnInfo(name = "discount") val discount: Long,
    @ColumnInfo(name = "tax_rate") val taxRate: Double,
    @ColumnInfo(name = "tax") val tax: Long,
    @ColumnInfo(name = "grand_total") val grandTotal: Long,
    @ColumnInfo(name = "paid") val paid: Long,
    @ColumnInfo(name = "change") val change: Long,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

/** One line inside a transaction. Snapshot of price/name at sale time. */
@kotlinx.serialization.Serializable
data class TransactionItem(
    val productId: String,
    val name: String,
    val price: Long,
    val qty: Int,
    val discount: Long
)
