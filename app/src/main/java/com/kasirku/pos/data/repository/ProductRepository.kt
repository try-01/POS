package com.kasirku.pos.data.repository

import com.kasirku.pos.data.local.dao.ProductDao
import com.kasirku.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository Produk - Single source of truth dengan validasi bisnis
 */
@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllActiveProducts()
    val productCount: Flow<Int> = productDao.getActiveProductCount()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    fun searchProducts(query: String): Flow<List<ProductEntity>> =
        productDao.searchProducts(query)

    suspend fun getProductById(id: Long): ProductEntity? =
        productDao.getProductById(id)

    suspend fun addProduct(product: ProductEntity): Result<Long> {
        return try {
            require(product.name.isNotBlank()) { "Nama produk tidak boleh kosong" }
            require(product.sku.isNotBlank()) { "SKU tidak boleh kosong" }
            require(product.sellPrice >= 0) { "Harga jual tidak boleh negatif" }
            require(product.stock >= 0) { "Stok tidak boleh negatif" }

            val skuCount = productDao.isSkuExists(product.sku)
            require(skuCount == 0) { "SKU '\${product.sku}' sudah digunakan" }

            val id = productDao.insertProduct(product)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: ProductEntity): Result<Unit> {
        return try {
            require(product.name.isNotBlank()) { "Nama produk tidak boleh kosong" }

            val skuCount = productDao.isSkuExists(product.sku, excludeId = product.id)
            require(skuCount == 0) { "SKU '\${product.sku}' sudah digunakan" }

            productDao.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(id: Long) {
        productDao.softDeleteProduct(id)
    }

    suspend fun decrementStock(productId: Long, quantity: Int): Boolean {
        return productDao.decrementStock(productId, quantity) > 0
    }
}
