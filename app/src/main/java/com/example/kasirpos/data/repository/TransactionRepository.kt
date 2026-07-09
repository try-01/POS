package com.example.kasirpos.data.repository

import com.example.kasirpos.data.local.dao.CartDao
import com.example.kasirpos.data.local.dao.CartWithProduct
import com.example.kasirpos.data.local.dao.DailySummary
import com.example.kasirpos.data.local.dao.ProductDao
import com.example.kasirpos.data.local.dao.TransactionDao
import com.example.kasirpos.data.local.entity.TransactionEntity
import com.example.kasirpos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val db: androidx.room.RoomDatabase  // Untuk runInTransaction atomik
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun dailySummary(startOfDay: Long, endOfDay: Long): Flow<DailySummary> =
        transactionDao.observeDailySummary(startOfDay, endOfDay)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getById(id)

    suspend fun getTransactionItems(transactionId: Long) =
        transactionDao.getItemsByTransactionId(transactionId)

    /**
     * Proses checkout lengkap — SEMUA operasi dalam satu transaksi DB atomik.
     * `db.runInTransaction { runBlocking { ... } }` menjamin all-or-nothing.
     *
     * @throws IllegalStateException jika keranjang kosong atau stok tidak mencukupi.
     */
    suspend fun checkout(
        taxPercentage: Int,
        paymentMethod: String,
        cashReceived: Long
    ): TransactionEntity {
        // ── Semua operasi DB dibungkus dalam satu transaksi atomik ──
        //    runInTransaction → jika exception, Room otomatis rollback.
        //    Satu runBlocking di dalam Runnable mencakup semua operasi suspend.
        var result: TransactionEntity? = null
        var error: Exception? = null

        db.runInTransaction {
            kotlinx.coroutines.runBlocking {
                try {
                    // 1. Ambil snapshot keranjang
                    val cartItems = cartDao.observeCartWithProducts().first()
                    if (cartItems.isEmpty()) {
                        throw IllegalStateException("Keranjang kosong — tidak bisa checkout")
                    }

                    // 2. Pre-validasi stok semua item
                    for (item in cartItems) {
                        if (item.quantity > item.stock) {
                            throw IllegalStateException(
                                "Stok \"${item.name}\" tidak cukup! Tersedia: ${item.stock}"
                            )
                        }
                    }

                    // 3. Kalkulasi total
                    var subtotal = 0L; var totalDiscount = 0L
                    for (item in cartItems) {
                        subtotal += item.unitPrice * item.quantity
                        totalDiscount += item.discount
                    }
                    val afterDiscount = (subtotal - totalDiscount).coerceAtLeast(0)
                    val taxAmount = (afterDiscount * taxPercentage) / 100
                    val grandTotal = afterDiscount + taxAmount
                    val change = (cashReceived - grandTotal).coerceAtLeast(0)

                    // 4. Kurangi stok (cek return value)
                    for (item in cartItems) {
                        val rowsAffected = productDao.decrementStock(item.productId, item.quantity)
                        if (rowsAffected == 0) {
                            throw IllegalStateException(
                                "Gagal mengurangi stok \"${item.name}\" — stok mungkin berubah"
                            )
                        }
                    }

                    // 5. Simpan header transaksi
                    val transaction = TransactionEntity(
                        subtotal = subtotal, totalDiscount = totalDiscount,
                        taxPercentage = taxPercentage, taxAmount = taxAmount,
                        grandTotal = grandTotal, paymentMethod = paymentMethod,
                        cashReceived = cashReceived, change = change
                    )
                    val transactionId = transactionDao.insert(transaction)

                    // 6. Simpan snapshot detail item
                    val transactionItems = cartItems.map { item ->
                        TransactionItemEntity(
                            transactionId = transactionId,
                            productName = item.name, productSku = item.sku,
                            quantity = item.quantity, unitPrice = item.unitPrice,
                            discount = item.discount, lineTotal = item.lineTotal
                        )
                    }
                    transactionDao.insertItems(transactionItems)

                    // 7. Bersihkan keranjang
                    cartDao.clearAll()

                    result = transaction.copy(id = transactionId)
                } catch (e: Exception) {
                    error = e
                    throw e // Re-throw agar Room rollback
                }
            }
        }

        return result ?: throw (error ?: IllegalStateException("Checkout gagal"))
    }

    suspend fun getDailyRevenue(startOfDay: Long, endOfDay: Long): Long =
        transactionDao.getDailyRevenue(startOfDay, endOfDay)

    suspend fun getDailyTransactionCount(startOfDay: Long, endOfDay: Long): Int =
        transactionDao.getDailyTransactionCount(startOfDay, endOfDay)
}
