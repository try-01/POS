package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.ProductDao
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProductRepository(
    private val productDao: ProductDao,
) {
    val products: Flow<List<ProductEntity>> = productDao.observeAll()

    fun search(query: String): Flow<List<ProductEntity>> = if (query.isBlank()) productDao.observeAll() else productDao.search(query.trim())

    suspend fun getById(id: Long): ProductEntity? = productDao.getById(id)

    suspend fun save(product: ProductEntity): Long = productDao.upsert(product)

    suspend fun delete(product: ProductEntity) = productDao.delete(product)

    suspend fun setActive(
        id: Long,
        active: Boolean,
    ) = productDao.setActive(id, active, System.currentTimeMillis())

    suspend fun softDelete(id: Long) = setActive(id, false)

    suspend fun getProductByBarcode(barcode: String): ProductEntity? = productDao.getByBarcode(barcode)

    suspend fun getProductByBarcodeAny(barcode: String): ProductEntity? = productDao.getByBarcodeAny(barcode)

    fun observeCategories(): Flow<List<String>> = productDao.observeDistinctCategories()

    suspend fun getProductBySku(sku: String): ProductEntity? = productDao.getBySku(sku)

    suspend fun bulkInsert(products: List<ProductEntity>) = productDao.insertAll(products)

    suspend fun getAllProductsOnce(): List<ProductEntity> = productDao.observeAll().first()
}
