package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Akses data produk. Semua fungsi `suspend` berjalan di dispatcher IO via Room.
 * Fungsi observasi mengembalikan [Flow] (cold) — hemat daya karena hanya aktif
 * saat ada subscriber (UI sedang terlihat).
 */
@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProductEntity>>

    /** Pencarian nama/SKU memakai index; LIKE ringan karena di-debounce di ViewModel. */
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

    /**
     * Decrement stok atomik: hanya berkurang jika stok MASIH mencukupi.
     * Kondisi `stock >= :qty` di klausa WHERE menjamin tidak ada race condition
     * antara dua checkout bersamaan. Mengembalikan jumlah baris terpengaruh
     * (0 = stok tidak cukup → pemanggil melempar exception untuk rollback).
     */
    @Query(
        """
        UPDATE products
        SET stock = stock - :qty, updatedAt = :now
        WHERE id = :id AND stock >= :qty
        """
    )
    suspend fun decrementStock(id: Long, qty: Int, now: Long): Int

    /**
     * BATCH D: kebalikan [decrementStock] — dipakai saat Void Transaksi untuk
     * mengembalikan stok item yang dibatalkan. Tidak perlu guard kondisi
     * (menambah stok selalu aman, tidak ada risiko race "kekurangan").
     */
    @Query(
        """
        UPDATE products
        SET stock = stock + :qty, updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun incrementStock(id: Long, qty: Int, now: Long)

    /**
     * Soft-delete (arsipkan): set `active = false` agar produk menghilang dari
     * katalog & layar kasir, namun baris data tetap ada. Aman karena transaksi
     * yang sudah dicatat menyimpan snapshot produk → riwayat tidak rusak.
     */
    @Query("UPDATE products SET active = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, now: Long)
    
    @Query("SELECT * FROM products WHERE barcode=:barcode AND active=1 LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcodeAny(barcode: String): ProductEntity?
}