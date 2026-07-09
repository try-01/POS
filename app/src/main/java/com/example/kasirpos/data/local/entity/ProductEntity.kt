package com.example.kasirpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity produk/inventaris.
 * Index pada [sku] untuk pencarian cepat saat transaksi (barcode / kode).
 */
@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Nama produk — ditampilkan di list kasir */
    val name: String,

    /** Stock Keeping Unit — kode unik (barcode / kode internal) */
    val sku: String,

    /** Harga jual satuan (dalam Rupiah) */
    val price: Long,

    /** Stok aktif saat ini. Dipotong otomatis saat checkout berhasil. */
    val stock: Int,

    /** URL/URI gambar produk (opsional, bisa null) */
    val imageUri: String? = null,

    /** Timestamp Unix saat produk dibuat */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp Unix saat produk terakhir diupdate */
    val updatedAt: Long = System.currentTimeMillis()
)
