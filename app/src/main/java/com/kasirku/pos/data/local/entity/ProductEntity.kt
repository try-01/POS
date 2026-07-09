package com.kasirku.pos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representasi tabel produk di database lokal (Room/SQLite).
 *
 * Index ditambahkan pada kolom [sku] (unik) dan [name] karena kedua kolom ini
 * paling sering dipakai untuk pencarian (LIKE query) pada layar Kasir — index
 * membuat pencarian tetap cepat walau jumlah produk mencapai ribuan baris.
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["name"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "sku")
    val sku: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sell_price")
    val sellPrice: Double,

    @ColumnInfo(name = "cost_price", defaultValue = "0")
    val costPrice: Double = 0.0,

    @ColumnInfo(name = "stock")
    val stock: Int,

    @ColumnInfo(name = "category", defaultValue = "'Umum'")
    val category: String = "Umum",

    // Soft delete: produk "dihapus" cukup dinonaktifkan agar histori transaksi lama
    // yang mereferensikan produk ini tetap valid & bisa ditampilkan di laporan.
    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
