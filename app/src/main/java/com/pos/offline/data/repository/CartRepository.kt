package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.CartDao
import com.pos.offline.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository keranjang aktif. Membungkus logika "tambah/kurang/hapus".
 */
class CartRepository(private val cartDao: CartDao) {

    val cartItems: Flow<List<CartItemEntity>> = cartDao.observeAll()

    /** Tambah produk; jika sudah ada, quantity-nya naik 1. */
    suspend fun add(productId: Long, name: String, unitPrice: Long) =
        cartDao.incrementQuantity(productId, name, unitPrice)

    /** Set quantity eksplisit. qty <= 0 berarti hapus item dari keranjang. */
    suspend fun setQuantity(productId: Long, qty: Int) {
        if (qty <= 0) cartDao.remove(productId)
        else cartDao.updateQuantity(productId, qty)
    }

    suspend fun remove(productId: Long) = cartDao.remove(productId)

    suspend fun clear() = cartDao.clear()
}
