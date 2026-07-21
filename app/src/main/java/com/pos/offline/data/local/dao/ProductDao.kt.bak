package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products
        WHERE active = 1
          AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%')
        ORDER BY name ASC
        """
    )
    fun search(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity): Long

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query(
        """
        UPDATE products
        SET stock = stock - :qty, updatedAt = :now
        WHERE id = :id AND stock >= :qty
        """
    )
    suspend fun decrementStock(id: Long, qty: Int, now: Long): Int

    @Query(
        """
        UPDATE products
        SET stock = stock + :qty, updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun incrementStock(id: Long, qty: Int, now: Long)

    @Query("UPDATE products SET active = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, now: Long)
    
    @Query("SELECT * FROM products WHERE barcode=:barcode AND active=1 LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcodeAny(barcode: String): ProductEntity?

    @Query(
        """
        SELECT DISTINCT category FROM products
        WHERE active = 1 AND category != ''
        ORDER BY category ASC
        """
    )
    fun observeDistinctCategories(): Flow<List<String>>
}