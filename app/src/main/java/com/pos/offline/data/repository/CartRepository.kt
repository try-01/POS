package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.CartDao
import com.pos.offline.data.local.dao.CartQuantityChangeResult
import com.pos.offline.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

class CartRepository(
    private val cartDao: CartDao,
) {
    val cartItems: Flow<List<CartItemEntity>> = cartDao.observeAll()

    /**
     * Mengubah kuantitas item secara atomic sebesar [delta] (positif/negatif).
     * Jika [maxStock] disediakan, hasil akhir di-clamp agar tidak melebihi stok.
     * Gunakan [CartQuantityChangeResult.wasClamped] untuk mendeteksi permintaan
     * yang gagal sepenuhnya diterapkan (mis. tampilkan pesan stok tidak cukup).
     */
    suspend fun changeQuantity(
        productId: Long,
        name: String,
        unitPrice: Long,
        delta: Int,
        maxStock: Int? = null,
    ): CartQuantityChangeResult = cartDao.applyQuantityDelta(productId, name, unitPrice, delta, maxStock)

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
