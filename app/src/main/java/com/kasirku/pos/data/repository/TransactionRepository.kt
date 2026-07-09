package com.kasirku.pos.data.repository

import androidx.room.withTransaction
import com.kasirku.pos.data.local.AppDatabase
import com.kasirku.pos.data.local.dao.DailySummary
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity
import com.kasirku.pos.data.local.relation.TransactionWithItems
import com.kasirku.pos.domain.model.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Hasil proses checkout, dimodelkan sebagai sealed class agar UI wajib menangani semua kemungkinan. */
sealed class CheckoutResult {
    data class Success(val transactionId: Long, val invoiceNumber: String) : CheckoutResult()
    data class InsufficientStock(val productName: String) : CheckoutResult()
    data class Error(val message: String) : CheckoutResult()
}

private class InsufficientStockException(val productName: String) : Exception()

@Singleton
class TransactionRepository @Inject constructor(
    private val db: AppDatabase
) {
    private val transactionDao = db.transactionDao()
    private val productDao = db.productDao()

    fun observeHistory(): Flow<List<TransactionWithItems>> =
        transactionDao.observeTransactionsWithItems().flowOn(Dispatchers.IO)

    fun observeDailySummary(start: Long, end: Long): Flow<DailySummary> =
        transactionDao.observeDailySummary(start, end).flowOn(Dispatchers.IO)

    /**
     * Proses checkout: potong stok + simpan header transaksi + simpan detail item,
     * dijalankan sebagai SATU transaksi database atomik ([db.withTransaction]).
     *
     * Jika di tengah proses ada kegagalan (misal stok produk lain sudah berubah lebih dulu),
     * Room akan me-ROLLBACK seluruh operasi secara otomatis — sehingga tidak pernah terjadi
     * kondisi data setengah jalan (misalnya stok sudah terpotong tapi transaksi gagal tersimpan),
     * meski aplikasi berjalan tanpa server/koneksi internet.
     */
    suspend fun checkout(
        cartItems: List<CartItem>,
        discountAmount: Double,
        taxAmount: Double,
        paidAmount: Double,
        paymentMethod: String = "CASH"
    ): CheckoutResult = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) return@withContext CheckoutResult.Error("Keranjang kosong")

        try {
            db.withTransaction {
                // 1) Potong stok tiap item terlebih dahulu secara atomik di level SQL.
                //    Jika salah satu produk stoknya sudah tidak cukup (rowsAffected == 0),
                //    lempar exception -> seluruh transaksi di-rollback otomatis oleh Room.
                for (item in cartItems) {
                    val rowsAffected = productDao.decrementStock(item.productId, item.quantity)
                    if (rowsAffected == 0) throw InsufficientStockException(item.name)
                }

                // 2) Hitung ulang nominal akhir sebagai validasi terakhir sebelum disimpan.
                //    subtotal -> dikurangi diskon -> ditambah pajak -> grandTotal.
                val subtotal = cartItems.sumOf { it.subtotal }
                val grandTotal = (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)
                val change = (paidAmount - grandTotal).coerceAtLeast(0.0)
                val invoiceNumber = generateInvoiceNumber()

                // 3) Simpan header transaksi (snapshot nominal, tidak akan berubah walau harga produk berubah nanti).
                val transactionId = transactionDao.insertTransaction(
                    TransactionEntity(
                        invoiceNumber = invoiceNumber,
                        subtotal = subtotal,
                        discountAmount = discountAmount,
                        taxAmount = taxAmount,
                        grandTotal = grandTotal,
                        paidAmount = paidAmount,
                        changeAmount = change,
                        paymentMethod = paymentMethod
                    )
                )

                // 4) Simpan seluruh detail item sekaligus (batch insert), lebih efisien dari loop insert satu-satu.
                val itemEntities = cartItems.map { item ->
                    TransactionItemEntity(
                        transactionId = transactionId,
                        productId = item.productId,
                        productName = item.name,
                        quantity = item.quantity,
                        priceAtSale = item.price,
                        subtotal = item.subtotal
                    )
                }
                transactionDao.insertItems(itemEntities)

                CheckoutResult.Success(transactionId, invoiceNumber) as CheckoutResult
            }
        } catch (e: InsufficientStockException) {
            CheckoutResult.InsufficientStock(e.productName)
        } catch (e: Exception) {
            CheckoutResult.Error(e.message ?: "Gagal memproses transaksi")
        }
    }

    /** Nomor invoice sederhana berbasis timestamp, cukup unik untuk skala satu perangkat kasir offline. */
    private fun generateInvoiceNumber(): String = "INV-${System.currentTimeMillis()}"
}
