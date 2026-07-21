package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.CartDao
import com.pos.offline.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

class CartRepository(
    private val cartDao: CartDao,
) {
    val cartItems: Flow<List<CartItemEntity>> = cartDao.observeAll()

    suspend fun add(
        productId: Long,
        name: String,
        unitPrice: Long,
    ) = cartDao.incrementQuantity(productId, name, unitPrice)

    suspend fun setQuantity(
        productId: Long,
        qty: Int,
    ) {
        if (qty <= 0) {
            cartDao.remove(productId)
        } else {
            cartDao.updateQuantity(productId, qty)
        }
    }

    suspend fun remove(productId: Long) = cartDao.remove(productId)

    suspend fun clear() = cartDao.clear()
}
