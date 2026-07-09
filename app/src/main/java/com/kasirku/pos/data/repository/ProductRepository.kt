package com.kasirku.pos.data.repository

import com.kasirku.pos.data.local.dao.ProductDao
import com.kasirku.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single Source of Truth untuk data produk.
 * Semua akses I/O dijalankan di [Dispatchers.IO] agar tidak pernah memblokir main/UI thread —
 * kunci utama menjaga UI tetap responsif (tidak jank) sekaligus hemat daya karena thread pool
 * IO dikelola secara efisien oleh Coroutines (bukan membuat Thread baru setiap kali).
 */
@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {

    /** Observasi daftar produk aktif, dengan filter pencarian opsional (nama/SKU). */
    fun observeProducts(query: String = ""): Flow<List<ProductEntity>> {
        return if (query.isBlank()) {
            productDao.observeActiveProducts()
        } else {
            productDao.searchProducts(query.trim())
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getProduct(id: Long): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getById(id)
    }

    suspend fun addProduct(
        sku: String,
        name: String,
        sellPrice: Double,
        costPrice: Double,
        stock: Int,
        category: String = "Umum"
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            require(sku.isNotBlank()) { "SKU tidak boleh kosong" }
            require(name.isNotBlank()) { "Nama produk tidak boleh kosong" }
            require(sellPrice >= 0) { "Harga jual tidak boleh negatif" }
            require(stock >= 0) { "Stok tidak boleh negatif" }

            productDao.insert(
                ProductEntity(
                    sku = sku.trim(),
                    name = name.trim(),
                    sellPrice = sellPrice,
                    costPrice = costPrice,
                    stock = stock,
                    category = category
                )
            )
        }
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.update(product.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProduct(id: Long) = withContext(Dispatchers.IO) {
        productDao.softDelete(id) // soft delete -> jaga integritas histori transaksi
    }
}
