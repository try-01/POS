package com.example.posoffline.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A product in the catalog.
 *
 * @property price sell price in *integer rupiah* (smallest currency unit).
 *              Storing integer rupiah avoids floating-point drift in cart math.
 * @property stock active stock count. Decremented atomically during checkout.
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sku") val sku: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price") val price: Long,
    @ColumnInfo(name = "stock") val stock: Int,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
