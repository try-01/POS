package com.kasirku.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kasirku.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Produk dengan Flow untuk reactive updates
 */
@Dao
interface ProductDao {

    @Query("""
        SELECT * FROM products 
        WHERE is_active = 1 
        ORDER BY name ASC
    """)
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE is_active = 1 
        AND (name LIKE '%' || :query || '%' 
             OR sku LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT COUNT(*) FROM products WHERE sku = :sku AND id != :excludeId")
    suspend fun isSkuExists(sku: String, excludeId: Long = 0): Int

    @Query("SELECT COUNT(*) FROM products WHERE is_active = 1")
    fun getActiveProductCount(): Flow<Int>

    @Query("""
        SELECT * FROM products 
        WHERE is_active = 1 AND stock <= :threshold 
        ORDER BY stock ASC
    """)
    fun getLowStockProducts(threshold: Int = 5): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET is_active = 0, updated_at = :now WHERE id = :id")
    suspend fun softDeleteProduct(id: Long, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("""
        UPDATE products 
        SET stock = stock - :quantity, updated_at = :now 
        WHERE id = :productId AND stock >= :quantity
    """)
    suspend fun decrementStock(
        productId: Long,
        quantity: Int,
        now: Long = System.currentTimeMillis()
    ): Int
}
