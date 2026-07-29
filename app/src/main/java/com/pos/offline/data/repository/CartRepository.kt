package com.pos.offline.data.repository
import com.pos.offline.data.local.dao.CartDao
import com.pos.offline.data.local.dao.CartQuantityChangeResult
import com.pos.offline.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow
class CartRepository(
    private val cartDao: CartDao,
) {
    val cartItems: Flow<List<CartItemEntity>> = cartDao.observeAll()
    suspend fun changeQuantity(
        productId: Long,
        name: String,
        unitPrice: Long,
        delta: Double,
        maxStock: Double? = null,
    ): CartQuantityChangeResult = cartDao.applyQuantityDelta(productId, name, unitPrice, delta, maxStock)
    suspend fun setQuantity(
        productId: Long,
        qty: Double,
    ) {
        if (qty <= 0.0) {
            cartDao.remove(productId)
        } else {
            cartDao.updateQuantity(productId, qty)
        }
    }
    suspend fun remove(productId: Long) = cartDao.remove(productId)
    suspend fun clear() = cartDao.clear()
}
