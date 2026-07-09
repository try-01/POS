package com.kasirku.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kasirku.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    /**
     * Flow otomatis memancarkan data baru setiap kali tabel `products` berubah
     * (insert/update/delete) — tidak perlu polling manual, sehingga hemat CPU & baterai.
     */
    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products 
        WHERE is_active = 1 AND (name LIKE '%' || :keyword || '%' OR sku LIKE '%' || :keyword || '%')
        ORDER BY name ASC
        """
    )
    fun searchProducts(keyword: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT) // SKU unik -> gagal insert jika duplikat, dicegah di layer Repository/UI
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    // Soft delete: histori transaksi lama yang mereferensikan produk ini tetap valid
    @Query("UPDATE products SET is_active = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /**
     * Potong stok secara atomik pada level SQL (bukan read-then-write dari Kotlin),
     * sehingga aman terhadap race condition jika suatu saat ada multi-proses/multi-device.
     * Klausa `stock >= :qty` memastikan stok tidak pernah minus; jika baris tidak ter-update
     * (return 0), berarti stok tidak mencukupi.
     */
    @Query("UPDATE products SET stock = stock - :qty WHERE id = :productId AND stock >= :qty")
    suspend fun decrementStock(productId: Long, qty: Int): Int

    @Query("UPDATE products SET stock = stock + :qty WHERE id = :productId")
    suspend fun incrementStock(productId: Long, qty: Int)
}
