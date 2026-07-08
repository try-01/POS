package com.kasirku.pos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity Transaksi - Menyimpan ringkasan setiap checkout
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transaction_date"]),
        Index(value = ["invoice_number"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,

    @ColumnInfo(name = "subtotal")
    val subtotal: Long,

    @ColumnInfo(name = "tax_percent")
    val taxPercent: Double = 0.0,

    @ColumnInfo(name = "tax_amount")
    val taxAmount: Long = 0,

    @ColumnInfo(name = "total_discount")
    val totalDiscount: Long = 0,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Long,

    @ColumnInfo(name = "payment_amount")
    val paymentAmount: Long,

    @ColumnInfo(name = "change_amount")
    val changeAmount: Long,

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "CASH",

    @ColumnInfo(name = "transaction_date")
    val transactionDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "item_count")
    val itemCount: Int
)
