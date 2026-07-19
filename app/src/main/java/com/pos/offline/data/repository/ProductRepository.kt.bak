package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.ProductDao
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository produk — satu-satunya gerbang akses ke data produk dari ViewModel.
 * Lapisan ini mengabstraksi DAO sehingga UI/VM tidak tahu detail persistensi.
 */
class ProductRepository(private val productDao: ProductDao) {

    val products: Flow<List<ProductEntity>> = productDao.observeAll()

    fun search(query: String): Flow<List<ProductEntity>> =
        if (query.isBlank()) productDao.observeAll() else productDao.search(query.trim())

    suspend fun getById(id: Long): ProductEntity? = productDao.getById(id)

    /** Insert atau update (upsert) tergantung ada/tidak-nya id. */
    suspend fun save(product: ProductEntity): Long = productDao.upsert(product)

    /** Hapus permanen (hard-delete); baris di keranjang aktif ikut terhapus (cascade). */
    suspend fun delete(product: ProductEntity) = productDao.delete(product)

    /** Ubah status aktif (true = tampil di katalog, false = disembunyikan). */
    suspend fun setActive(id: Long, active: Boolean) =
        productDao.setActive(id, active, System.currentTimeMillis())

    /** Arsipkan produk (soft-delete) — hilang dari katalog, data tetap utuh. */
    suspend fun softDelete(id: Long) = setActive(id, false)

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getByBarcode(barcode)
    }
    
    suspend fun getProductByBarcodeAny(barcode: String): ProductEntity? {
        return productDao.getByBarcodeAny(barcode)
    }
}
