package com.pos.offline.data.local.entity
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["name"]),
        Index(value = ["barcode"], unique = true),
        Index(value = ["category"]),
    ],
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
    val stock: Double,
    @ColumnInfo(name = "damagedStock", defaultValue = "0.0")
    val damagedStock: Double = 0.0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "category", defaultValue = "''")
    val category: String = "",
)
