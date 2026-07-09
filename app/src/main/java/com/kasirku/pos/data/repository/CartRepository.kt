package com.kasirku.pos.data.repository

import com.kasirku.pos.data.local.entity.ProductEntity
import com.kasirku.pos.domain.model.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository keranjang belanja — SENGAJA disimpan sepenuhnya di memori (StateFlow),
 * BUKAN di Room, dengan alasan performa:
 *
 * 1. Keranjang bersifat transient (hanya relevan selama sesi kasir berjalan), belum
 *    membutuhkan jaminan ACID sampai proses checkout benar-benar terjadi.
 * 2. Setiap interaksi kasir (tap produk, ubah qty) akan sangat sering terjadi berturut-turut;
 *    jika ditulis ke disk setiap saat, ini akan membebani I/O & baterai secara signifikan.
 * 3. Baru dipersist ke database SEKALI saat checkout, sebagai satu operasi atomik
 *    (lihat [TransactionRepository.checkout]).
 *
 * Di-scope sebagai @Singleton (via Hilt) sehingga state keranjang tetap terjaga walau terjadi
 * perubahan konfigurasi (rotasi layar) tanpa perlu SavedStateHandle tambahan.
 */
@Singleton
class CartRepository @Inject constructor() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    /** Tambah produk ke keranjang. Jika sudah ada, qty ditambah (dibatasi maksimum stok tersedia). */
    fun addProduct(product: ProductEntity, quantity: Int = 1) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.productId == product.id }

        if (index >= 0) {
            val existing = current[index]
            val newQty = (existing.quantity + quantity).coerceAtMost(product.stock)
            current[index] = existing.copy(quantity = newQty, stockAvailable = product.stock)
        } else if (product.stock > 0) {
            current.add(
                CartItem(
                    productId = product.id,
                    sku = product.sku,
                    name = product.name,
                    price = product.sellPrice,
                    quantity = quantity.coerceIn(1, product.stock),
                    stockAvailable = product.stock
                )
            )
        }
        _cartItems.value = current
    }

    /** Ubah kuantitas item. Jika hasil akhir 0 (atau kurang), item otomatis dihapus dari keranjang. */
    fun updateQuantity(productId: Long, quantity: Int) {
        _cartItems.value = _cartItems.value.mapNotNull { item ->
            if (item.productId != productId) return@mapNotNull item
            val safeQty = quantity.coerceIn(0, item.stockAvailable)
            if (safeQty <= 0) null else item.copy(quantity = safeQty)
        }
    }

    fun removeItem(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.productId == productId }
    }

    fun clear() {
        _cartItems.value = emptyList()
    }

    /** Snapshot sinkron isi keranjang saat ini (dipakai saat proses checkout). */
    fun currentItems(): List<CartItem> = _cartItems.value
}
