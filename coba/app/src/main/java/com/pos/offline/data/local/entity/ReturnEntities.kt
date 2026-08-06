package com.pos.offline.data.local.entity
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
@Entity(
    tableName = "returns",
    indices = [
        Index(value = ["transactionId"], unique = true),
        Index(value = ["returnedAt"]),
        Index(value = ["shiftId"]),
    ],
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String,
    val returnedAt: Long,
    val shiftId: Long? = null,
    val cashierId: Long? = null,
    val cashierName: String = "",
    val refundAmount: Long,
    val refundMethod: String,
    val note: String = "",
    @ColumnInfo(name = "isWarrantyExchange", defaultValue = "0")
    val isWarrantyExchange: Boolean = false,
)
@Entity(
    tableName = "return_items",
    indices = [Index(value = ["returnId"])],
)
data class ReturnItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnId: Long,
    val transactionItemId: Long,
    val productId: Long? = null,
    val productName: String,
    val unitPrice: Long,
    val quantityReturned: Double,
    val restocked: Boolean,
)
