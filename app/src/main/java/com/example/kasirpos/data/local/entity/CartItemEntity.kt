package com.example.kasirpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity keranjang — disimpan di DB agar tahan terhadap process death.
 * Foreign key ke [ProductEntity] memastikan integritas referensial.
 */
@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE // Hapus item keranjang jika produk dihapus
        )
    ],
    indices = [Index(value = ["productId"])]
)
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Foreign key ke products.id */
    val productId: Long,

    /** Kuantitas dalam keranjang (≥1) */
    val quantity: Int = 1,

    /** Harga satuan saat produk dimasukkan ke keranjang (snapshot) */
    val unitPrice: Long,

    /** Diskon per-item dalam Rupiah (default 0) */
    val discount: Long = 0
)
