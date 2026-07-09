package com.example.kasirpos.data.repository

import com.example.kasirpos.data.local.dao.CartDao
import com.example.kasirpos.data.local.dao.CartWithProduct
import com.example.kasirpos.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

class CartRepository(private val dao: CartDao) {

    /** Observasi keranjang + data produk (join) */
    val cartWithProducts: Flow<List<CartWithProduct>> = dao.observeCartWithProducts()

    /** Jumlah total item di keranjang (untuk badge) */
    val totalItems: Flow<Int?> = dao.observeTotalItems()

    /**
     * Tambah item ke keranjang. Jika produk sudah ada, increment qty.
     * Jika belum, buat entry baru dengan snapshot harga saat ini.
     */
    suspend fun addToCart(productId: Long, unitPrice: Long) {
        val existing = dao.getByProductId(productId)
        if (existing != null) {
            dao.incrementQuantity(productId, delta = 1)
        } else {
            dao.insert(
                CartItemEntity(
                    productId = productId,
                    quantity = 1,
                    unitPrice = unitPrice
                )
            )
        }
    }

    /**
     * Update kuantitas absolut (dari input manual di keranjang).
     * @param productId ID produk (bukan cart item ID) — sesuai cara UI memanggil.
     */
    suspend fun updateQuantity(productId: Long, newQuantity: Int) {
        val item = dao.getByProductId(productId) ?: return
        if (newQuantity <= 0) {
            dao.delete(item)
        } else {
            dao.setQuantity(productId, newQuantity)
        }
    }

    /**
     * Update diskon per-item di keranjang.
     * @param productId ID produk yang ada di keranjang.
     */
    suspend fun updateDiscount(productId: Long, discount: Long) {
        val item = dao.getByProductId(productId) ?: return
        dao.update(item.copy(discount = discount.coerceAtLeast(0)))
    }

    suspend fun removeFromCart(productId: Long) {
        val item = dao.getByProductId(productId) ?: return
        dao.delete(item)
    }

    suspend fun clearCart() = dao.clearAll()
}
