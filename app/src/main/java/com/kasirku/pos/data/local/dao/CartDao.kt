package com.kasirku.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kasirku.pos.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Keranjang dengan kalkulasi subtotal di SQL
 */
@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    fun getAllCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items")
    fun getCartItemCount(): Flow<Int>

    @Query("""
        SELECT COALESCE(
            SUM(
                CAST(unit_price AS REAL) 
                * quantity 
                * (1.0 - discount_percent / 100.0)
            ), 0
        ) FROM cart_items
    """)
    fun getCartSubtotal(): Flow<Long>

    @Query("SELECT * FROM cart_items WHERE product_id = :productId LIMIT 1")
    suspend fun getCartItemByProductId(productId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity): Long

    @Update
    suspend fun updateCartItem(item: CartItemEntity)

    @Query("""
        UPDATE cart_items 
        SET quantity = quantity + :amount 
        WHERE product_id = :productId
    """)
    suspend fun incrementQuantity(productId: Long, amount: Int = 1)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun removeCartItem(id: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}
