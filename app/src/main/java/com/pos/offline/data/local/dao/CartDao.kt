package com.pos.offline.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pos.offline.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

data class CartQuantityChangeResult(
    val previousQty: Double,
    val requestedQty: Double,
    val finalQty: Double,
    val maxStock: Double?,
) {
    val wasClamped: Boolean get() = finalQty != requestedQty
    val exceedsStock: Boolean get() = maxStock != null && finalQty > maxStock
    val crossedIntoExcess: Boolean
        get() = maxStock != null && previousQty <= maxStock && finalQty > maxStock
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY id ASC")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun findByProduct(productId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :qty WHERE productId = :productId")
    suspend fun updateQuantity(
        productId: Long,
        qty: Double,
    )

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun remove(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    @Transaction
    suspend fun applyQuantityDelta(
        productId: Long,
        name: String,
        unitPrice: Long,
        delta: Double,
        maxStock: Double? = null,
    ): CartQuantityChangeResult {
        val existing = findByProduct(productId)
        val previousQty = existing?.quantity ?: 0.0
        val requestedQty = previousQty + delta
        val finalQty = requestedQty.coerceAtLeast(0.0)
        when {
            finalQty <= 0.0 -> if (existing != null) remove(productId)
            existing == null -> upsert(CartItemEntity(productId = productId, name = name, unitPrice = unitPrice, quantity = finalQty))
            else -> updateQuantity(productId, finalQty)
        }
        return CartQuantityChangeResult(previousQty, requestedQty, finalQty, maxStock)
    }
}
