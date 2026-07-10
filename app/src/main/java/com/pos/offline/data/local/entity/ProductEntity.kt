package com.pos.offline.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Produk yang dijual. Disimpan sebagai table Room.
 *
 * CATATAN KEUANGAN: harga [price] & [cost] disimpan sebagai [Long] dalam satuan
 * Rupiah penuh (bukan Double). Rupiah tidak punya pecahan di bawah 1, jadi Long
 * sepenuhnya akurat dan menghindari error floating-point pada akumulasi total.
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true), // SKU unik, pencarian cepat
        Index(value = ["name"])                // index nama untuk search LIKE
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String,
    val price: Long,            // Harga jual (Rupiah). Long = presisi uang.
    // Harga modal/beli (Rupiah). Ditambahkan di v2 (lihat MIGRATION_1_2).
    // defaultValue "0" wajib agar default konsisten antara fresh-install & migrasi.
    @ColumnInfo(name = "cost", defaultValue = "0")
    val cost: Long = 0L,
    val stock: Int,             // Stok aktif yang tersedia untuk dijual
    val active: Boolean = true, // soft-delete: false = disembunyikan dari katalog
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
