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
    val id: String, 
    val createdAt: Long, 
    val subtotal: Long, 
    val discount: Long, 
    val tax: Long, 
    val total: Long, 
    val paidAmount: Long, 
    val change: Long, 
    @ColumnInfo(defaultValue = "0")
    val changeGiven: Long = 0L,
    @ColumnInfo(defaultValue = "1")
    val changeGivenInCash: Boolean = true,
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
        Index(value = ["transactionId"]), 
        Index(value = ["productId"]) 
    ], 
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String,
    val productName: String,
    val unitPrice: Long,
    val quantity: Double,
    val lineTotal: Long,
    @ColumnInfo(defaultValue = "0")
    val unitCost: Long = 0L,
    val productId: Long? = null,
)
val TransactionEntity.hasReturn: Boolean
    get() = returnId != null
