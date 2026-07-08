package com.kasirku.pos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity Produk - Tabel utama inventaris
 * Index pada SKU dan nama untuk pencarian cepat O(log n)
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["name"]),
        Index(value = ["is_active"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sku")
    val sku: String,

    @ColumnInfo(name = "sell_price")
    val sellPrice: Long,

    @ColumnInfo(name = "stock")
    val stock: Int,

    @ColumnInfo(name = "category")
    val category: String = "",

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
