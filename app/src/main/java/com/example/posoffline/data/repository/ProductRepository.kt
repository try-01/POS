package com.example.posoffline.data.repository

import com.example.posoffline.data.dao.ProductDao
import com.example.posoffline.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for products. The ViewModel layer never touches the DAO
 * directly — this gives us a single seam to add caching, validation,
 * or future remote sync without touching the UI.
 */
class ProductRepository(private val dao: ProductDao) {

    fun observeAll(): Flow<List<ProductEntity>> = dao.observeAll()

    suspend fun list(): List<ProductEntity> = dao.list()

    suspend fun get(id: String): ProductEntity? = dao.get(id)

    suspend fun create(
        sku: String,
        name: String,
        price: Long,
        stock: Int,
        category: String?
    ): ProductEntity {
        val now = System.currentTimeMillis()
        val entity = ProductEntity(
            id = UUID.randomUUID().toString(),
            sku = sku,
            name = name,
            price = price,
            stock = stock,
            category = category,
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun update(
        id: String,
        sku: String,
        name: String,
        price: Long,
        stock: Int,
        category: String?
    ) {
        val cur = dao.get(id) ?: throw IllegalStateException("Produk tidak ditemukan")
        dao.update(
            cur.copy(
                sku = sku,
                name = name,
                price = price,
                stock = stock,
                category = category,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun remove(id: String) = dao.deleteById(id)

    suspend fun count(): Int = dao.count()
}
