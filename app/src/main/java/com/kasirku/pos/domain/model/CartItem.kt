package com.kasirku.pos.domain.model

/**
 * Model domain untuk item di keranjang belanja.
 * Sengaja dibuat TERPISAH dari [com.kasirku.pos.data.local.entity.ProductEntity] karena:
 * - Keranjang adalah objek transient (hanya hidup selama sesi kasir), bukan representasi tabel DB.
 * - `price` diambil sebagai snapshot saat produk dimasukkan ke keranjang, sehingga jika harga produk
 *   berubah di tengah transaksi, item yang sudah ada di keranjang tidak ikut berubah harganya.
 */
data class CartItem(
    val productId: Long,
    val sku: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val stockAvailable: Int
) {
    /** Subtotal per baris = harga x kuantitas. */
    val subtotal: Double
        get() = price * quantity
}
