package com.example.kasirpos.data.repository

import com.example.kasirpos.data.local.dao.ProductDao
import com.example.kasirpos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository produk — abstraksi di atas DAO.
 * Semua operasi suspend untuk dipanggil dari coroutine ViewModel.
 */
class ProductRepository(private val dao: ProductDao) {

    /** Observasi reaktif semua produk */
    val allProducts: Flow<List<ProductEntity>> = dao.observeAll()

    fun searchByName(query: String): Flow<List<ProductEntity>> = dao.searchByName(query)

    fun observeLowStock(threshold: Int = 10): Flow<List<ProductEntity>> =
        dao.observeLowStock(threshold)

    suspend fun findBySku(sku: String): ProductEntity? = dao.findBySku(sku)

    suspend fun upsert(product: ProductEntity): Long = dao.upsert(product)

    suspend fun update(product: ProductEntity) = dao.update(product)

    suspend fun delete(product: ProductEntity) = dao.delete(product)

    /**
     * Kurangi stok produk secara atomik.
     * @return jumlah baris terpengaruh (1 = sukses, 0 = stok tidak cukup)
     */
    suspend fun decrementStock(productId: Long, qty: Int): Int =
        dao.decrementStock(productId, qty)

    suspend fun count(): Int = dao.count()
}
