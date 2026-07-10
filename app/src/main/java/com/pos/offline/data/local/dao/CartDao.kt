package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pos.offline.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Akses data keranjang aktif. Operasi "tambah produk" dibungkus [Transaction]
 * agar cek-ada + update bersifat atomik (tidak ada duplikat baris).
 */
@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY id ASC")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun findByProduct(productId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :qty WHERE productId = :productId")
    suspend fun updateQuantity(productId: Long, qty: Int)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun remove(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    /**
     * Tambah 1 unit produk ke keranjang. Jika sudah ada, tambah quantity-nya.
     * [Transaction] menjamin "baca lalu tulis" tidak terganggu operasi lain.
     */
    @Transaction
    suspend fun incrementQuantity(productId: Long, name: String, unitPrice: Long) {
        val existing = findByProduct(productId)
        if (existing == null) {
            upsert(CartItemEntity(productId = productId, name = name, unitPrice = unitPrice, quantity = 1))
        } else {
            updateQuantity(productId, existing.quantity + 1)
        }
    }
}
