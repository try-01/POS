package com.pos.offline.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["name"]),
        Index(value = ["barcode"], unique = true),
        Index(value = ["category"]) // BARU: mempercepat query DISTINCT kategori & filter
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String,
    val barcode: String? = null,
    val price: Long,
    @ColumnInfo(name = "cost", defaultValue = "0")
    val cost: Long = 0L,
    val stock: Int,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // BARU: kategori bebas teks. Non-null + default "" (bukan nullable seperti
    // barcode) — kategori bukan identifier unik, jadi tidak perlu null-check
    // berulang di Kotlin; "" dianggap "tanpa kategori" di seluruh UI/filter.
    @ColumnInfo(name = "category", defaultValue = "''")
    val category: String = ""
)