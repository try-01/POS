package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Item keranjang AKTIF (sesi kasir yang sedang berjalan).
 *
 * Dipersist ke DB (bukan di-memori) supaya tahan terhadap proses di-kill OS
 * (anti memory-leak / kehilangan data saat low-memory). Saat checkout selesai,
 * seluruh baris tabel ini dikosongkan.
 *
 * Denormalisasi: [name] & [unitPrice] di-snapshot saat ditambahkan agar render
 * keranjang tidak perlu JOIN ke tabel produk (render lebih cepat).
 */
@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE // produk dihapus → item keranjang ikut terhapus
        )
    ],
    indices = [Index(value = ["productId"], unique = true)] // 1 produk = 1 baris di keranjang
)
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val name: String,       // snapshot nama produk
    val unitPrice: Long,    // snapshot harga saat ditambahkan (Rupiah)
    val quantity: Int = 1
) {
    /** Total baris = harga satuan × jumlah. Long untuk presisi. */
    val lineTotal: Long get() = unitPrice * quantity.toLong()
}
