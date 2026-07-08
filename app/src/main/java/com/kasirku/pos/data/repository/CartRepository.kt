package com.kasirku.pos.data.repository

import com.kasirku.pos.data.local.dao.CartDao
import com.kasirku.pos.data.local.entity.CartItemEntity
import com.kasirku.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository Keranjang - Persisten di database untuk survive process death
 */
@Singleton
class CartRepository @Inject constructor(
    private val cartDao: CartDao
) {
    val cartItems: Flow<List<CartItemEntity>> = cartDao.getAllCartItems()
    val cartItemCount: Flow<Int> = cartDao.getCartItemCount()
    val cartSubtotal: Flow<Long> = cartDao.getCartSubtotal()

    suspend fun addToCart(product: ProductEntity, quantity: Int = 1) {
        val existing = cartDao.getCartItemByProductId(product.id)
        if (existing != null) {
            cartDao.incrementQuantity(product.id, quantity)
        } else {
            cartDao.insertCartItem(
                CartItemEntity(
                    productId = product.id,
                    productName = product.name,
                    unitPrice = product.sellPrice,
                    quantity = quantity
                )
            )
        }
    }

    suspend fun updateQuantity(cartItem: CartItemEntity, newQuantity: Int) {
        if (newQuantity <= 0) {
            cartDao.removeCartItem(cartItem.id)
        } else {
            cartDao.updateCartItem(cartItem.copy(quantity = newQuantity))
        }
    }

    suspend fun setItemDiscount(cartItem: CartItemEntity, discountPercent: Double) {
        cartDao.updateCartItem(cartItem.copy(discountPercent = discountPercent.coerceIn(0.0, 100.0)))
    }

    suspend fun removeFromCart(cartItemId: Long) {
        cartDao.removeCartItem(cartItemId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }

    suspend fun getCartItemByProductId(productId: Long): CartItemEntity? {
        return cartDao.getCartItemByProductId(productId)
    }
}
