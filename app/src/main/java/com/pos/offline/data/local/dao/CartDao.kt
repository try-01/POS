package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pos.offline.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Hasil dari operasi perubahan kuantitas atomic.
 *
 * KEBIJAKAN STOK: SOFT-BLOCK. [maxStock] TIDAK LAGI membatasi [finalQty]
 * secara paksa — kasir tetap bisa menambah kuantitas melebihi stok tercatat
 * (mengakomodasi kasus stok fisik ada tapi database belum sempat
 * diperbarui). Gunakan [exceedsStock] untuk menampilkan PERINGATAN
 * non-blocking di UI (mis. "Stok tinggal 0, pastikan produk fisik tersedia
 * sebelum melanjutkan"), BUKAN untuk mencegah penambahan/checkout.
 *
 * [wasClamped] tetap dipertahankan untuk kasus floor di 0 (delta negatif
 * yang membuat permintaan qty menjadi negatif, tetap di-floor ke 0).
 */
data class CartQuantityChangeResult(
    val previousQty: Int,
    val requestedQty: Int,
    val finalQty: Int,
    val maxStock: Int?,
) {
    val wasClamped: Boolean get() = finalQty != requestedQty
    val exceedsStock: Boolean get() = maxStock != null && finalQty > maxStock

    /**
     * True HANYA pada tap/aksi yang membuat qty MELEWATI batas stok untuk
     * pertama kali (previousQty masih <= maxStock, finalQty sudah > maxStock).
     * Dipakai untuk menampilkan dialog peringatan SEKALI SAJA saat momen
     * transisi — bukan pada setiap tap berikutnya selama qty sudah kelebihan
     * (mencegah dialog blocking muncul berulang-ulang & mengganggu alur kasir
     * yang sering dikejar waktu pembeli).
     */
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
        qty: Int,
    )

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun remove(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    /**
     * Mengubah kuantitas item di cart secara atomic sebesar [delta] (bisa +/-),
     * dalam satu transaksi Room (aman dari race condition tap cepat berulang).
     *
     * [maxStock] TIDAK memblokir kenaikan qty (lihat dokumentasi
     * [CartQuantityChangeResult]) — hanya dipakai untuk menghitung
     * [CartQuantityChangeResult.exceedsStock] sebagai sinyal peringatan UI.
     */
    @Transaction
    suspend fun applyQuantityDelta(
        productId: Long,
        name: String,
        unitPrice: Long,
        delta: Int,
        maxStock: Int? = null,
    ): CartQuantityChangeResult {
        val existing = findByProduct(productId)
        val previousQty = existing?.quantity ?: 0
        val requestedQty = previousQty + delta
        val finalQty = requestedQty.coerceAtLeast(0)

        when {
            finalQty <= 0 -> if (existing != null) remove(productId)
            existing == null -> upsert(CartItemEntity(productId = productId, name = name, unitPrice = unitPrice, quantity = finalQty))
            else -> updateQuantity(productId, finalQty)
        }

        return CartQuantityChangeResult(previousQty, requestedQty, finalQty, maxStock)
    }
}
