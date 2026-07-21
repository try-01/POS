package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "returns",
    indices = [
        Index(value = ["transactionId"], unique = true), // 1 transaksi = maks 1 retur (final)
        Index(value = ["returnedAt"]), // dasar filter "Retur Hari Ini"
        Index(value = ["shiftId"]), // dasar hitung cashRefunds per shift
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
    val quantityReturned: Int,
    val restocked: Boolean,
)
