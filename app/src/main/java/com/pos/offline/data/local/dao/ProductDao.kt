package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
        """,
    )
    fun search(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity): Long

    @Delete
    suspend fun delete(product: ProductEntity)

    /**
     * Kebijakan SOFT-BLOCK (disengaja): TIDAK ADA lagi syarat `stock >= qty`.
     * Stok boleh menjadi negatif — mengakomodasi kasus stok fisik tersedia
     * tapi database belum sempat diperbarui kasir. `affected == 0` sekarang
     * HANYA berarti id produk tidak ditemukan (kasus data-integrity, mis.
     * produk dihapus permanen secara konkuren), BUKAN kehabisan stok.
     */
    @Query(
        """
        UPDATE products
        SET stock = stock - :qty, updatedAt = :now
        WHERE id = :id
        """,
    )
    suspend fun decrementStock(
        id: Long,
        qty: Int,
        now: Long,
    ): Int

    @Query(
        """
        UPDATE products
        SET stock = stock + :qty, updatedAt = :now
        WHERE id = :id
        """,
    )
    suspend fun incrementStock(
        id: Long,
        qty: Int,
        now: Long,
    )

    @Query("UPDATE products SET active = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(
        id: Long,
        active: Boolean,
        now: Long,
    )

    @Query("SELECT * FROM products WHERE barcode=:barcode AND active=1 LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcodeAny(barcode: String): ProductEntity?

    @Query(
        """
        SELECT DISTINCT category FROM products
        WHERE active = 1 AND category != ''
        ORDER BY category ASC
        """,
    )
    fun observeDistinctCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): ProductEntity?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(products: List<ProductEntity>)
}
