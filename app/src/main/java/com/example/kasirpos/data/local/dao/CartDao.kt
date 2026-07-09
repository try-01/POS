package com.example.kasirpos.data.local.dao

import androidx.room.*
import com.example.kasirpos.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    /** Observasi isi keranjang beserta data produk terkait (join) */
    @Query("""
        SELECT c.id, c.productId, c.quantity, c.unitPrice, c.discount,
               p.name, p.sku, p.stock, p.imageUri
        FROM cart_items c
        INNER JOIN products p ON c.productId = p.id
        ORDER BY c.id ASC
    """)
    fun observeCartWithProducts(): Flow<List<CartWithProduct>>

    /** Hitung jumlah item di keranjang (untuk badge) */
    @Query("SELECT SUM(quantity) FROM cart_items")
    fun observeTotalItems(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CartItemEntity): Long

    @Update
    suspend fun update(item: CartItemEntity)

    @Delete
    suspend fun delete(item: CartItemEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clearAll()

    /** Cari item keranjang by cart_items.id (primary key) */
    @Query("SELECT * FROM cart_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CartItemEntity?

    /** Cek apakah produk sudah ada di keranjang (by productId) */
    @Query("SELECT * FROM cart_items WHERE productId = :productId LIMIT 1")
    suspend fun getByProductId(productId: Long): CartItemEntity?

    /** Update kuantitas absolut — langsung set, bukan increment */
    @Query("UPDATE cart_items SET quantity = :newQty WHERE productId = :productId")
    suspend fun setQuantity(productId: Long, newQty: Int)

    /** Update qty item yang sudah ada di keranjang (delta) */
    @Query("UPDATE cart_items SET quantity = quantity + :delta WHERE productId = :productId")
    suspend fun incrementQuantity(productId: Long, delta: Int = 1)
}

/**
 * Proyeksi hasil JOIN cart_items + products untuk ditampilkan di UI.
 * Tidak perlu seluruh kolom — cukup yang dibutuhkan layar kasir.
 */
data class CartWithProduct(
    val id: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Long,
    val discount: Long,
    val name: String,
    val sku: String,
    val stock: Int,
    val imageUri: String? = null
) {
    /** Harga satuan setelah diskon per-item */
    val effectivePrice: Long get() = (unitPrice - discount).coerceAtLeast(0)

    /** Subtotal baris = effectivePrice * quantity */
    val lineTotal: Long get() = effectivePrice * quantity
}
