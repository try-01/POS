package com.example.kasirpos.data.local.dao

import androidx.room.*
import com.example.kasirpos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    /** Observasi semua produk — reaktif via Flow, auto-update UI */
    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProductEntity>>

    /** Pencarian cepat by SKU / nama untuk barcode scanner */
    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun findBySku(sku: String): ProductEntity?

    /** Pencarian fuzzy by nama */
    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<ProductEntity>>

    /** Produk dengan stok rendah (< 10) untuk alert */
    @Query("SELECT * FROM products WHERE stock < :threshold ORDER BY stock ASC")
    fun observeLowStock(threshold: Int = 10): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    /** Kurangi stok setelah checkout — query atomik Room */
    @Query("UPDATE products SET stock = stock - :qty, updatedAt = :now WHERE id = :productId AND stock >= :qty")
    suspend fun decrementStock(productId: Long, qty: Int, now: Long = System.currentTimeMillis()): Int

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}
