package com.example.posoffline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.posoffline.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun list(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun findBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getMany(ids: List<String>): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    /**
     * Atomic stock decrement.
     *
     * Wrapped in @Transaction so that:
     *  - The stock check + decrement is a single SQLite transaction.
     *  - If any item has insufficient stock, the whole call throws and
     *    *no* stock is decremented. This is the safety guarantee we need:
     *    a crash mid-checkout cannot leave the inventory in an inconsistent
     *    state.
     */
    @Transaction
    suspend fun decrementStock(items: List<StockDecrement>) {
        for (it in items) {
            val cur = get(it.productId)
                ?: throw IllegalStateException("Produk ${it.productId} tidak ditemukan")
            if (cur.stock < it.qty) {
                throw IllegalStateException(
                    "Stok ${cur.name} tidak cukup (sisa ${cur.stock})"
                )
            }
            val updated = cur.copy(
                stock = cur.stock - it.qty,
                updatedAt = System.currentTimeMillis()
            )
            update(updated)
        }
    }
}

/** Helper parameter object so we don't need a Room @Entity for a DTO. */
data class StockDecrement(val productId: String, val qty: Int)
