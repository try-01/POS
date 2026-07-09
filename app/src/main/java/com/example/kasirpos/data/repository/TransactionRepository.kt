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
     * Proses checkout lengkap dalam satu transaksi database.
     * Urutan operasi:
     *   1. Ambil snapshot isi keranjang.
     *   2. Kalkulasi subtotal, diskon, pajak, grand total.
     *   3. Kurangi stok setiap produk (atomik per produk).
     *   4. Simpan header transaksi.
     *   5. Simpan detail item transaksi (snapshot).
     *   6. Hapus semua item keranjang.
     */
    /**
     * Proses checkout lengkap — SEMUA operasi dalam satu transaksi DB atomik.
     *
     * Urutan operasi:
     *   1. Ambil snapshot isi keranjang (via [cartDao]).
     *   2. Validasi stok mencukupi untuk SEMUA item sebelum mulai mutasi.
     *   3. Kalkulasi subtotal, diskon, pajak, grand total, kembalian.
     *   4. Kurangi stok setiap produk (atomik per produk, return value dicek).
     *   5. Simpan header transaksi.
     *   6. Simpan detail item transaksi (snapshot nama, harga).
     *   7. Hapus semua item keranjang.
     *
     * @throws IllegalStateException jika keranjang kosong atau stok tidak mencukupi.
     */
    suspend fun checkout(
        taxPercentage: Int,
        paymentMethod: String,
        cashReceived: Long
    ): TransactionEntity {
        // ── SEMUA operasi DB dibungkus dalam satu transaksi atomik ──
        //    room-ktx `withTransaction` → jika exception, semua di-rollback.
        return db.withTransaction {
            // 1. Ambil snapshot keranjang (suspend, one-shot)
            val cartItems: List<CartWithProduct> = cartDao.observeCartWithProducts().first()

            if (cartItems.isEmpty()) {
                throw IllegalStateException("Keranjang kosong — tidak bisa checkout")
            }

            // 2. PRE-VALIDASI: Cek stok semua item SEBELUM mulai mutasi.
            for (item in cartItems) {
                if (item.quantity > item.stock) {
                    throw IllegalStateException(
                        "Stok \"${item.name}\" tidak cukup! Tersedia: ${item.stock}, diminta: ${item.quantity}"
                    )
                }
            }

            // 3. Kalkulasi total
            var subtotal = 0L
            var totalDiscount = 0L
            for (item in cartItems) {
                subtotal += item.unitPrice * item.quantity
                totalDiscount += item.discount
            }

            val afterDiscount = (subtotal - totalDiscount).coerceAtLeast(0)
            val taxAmount = (afterDiscount * taxPercentage) / 100
            val grandTotal = afterDiscount + taxAmount
            val change = (cashReceived - grandTotal).coerceAtLeast(0)

            // 4. Kurangi stok — cek return value
            for (item in cartItems) {
                val rowsAffected = productDao.decrementStock(item.productId, item.quantity)
                if (rowsAffected == 0) {
                    throw IllegalStateException(
                        "Gagal mengurangi stok \"${item.name}\" — stok berubah saat transaksi"
                    )
                }
            }

            // 5. Simpan header
            val transaction = TransactionEntity(
                subtotal = subtotal, totalDiscount = totalDiscount,
                taxPercentage = taxPercentage, taxAmount = taxAmount,
                grandTotal = grandTotal, paymentMethod = paymentMethod,
                cashReceived = cashReceived, change = change
            )
            val transactionId = transactionDao.insert(transaction)

            // 6. Simpan detail item (snapshot)
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

            transaction.copy(id = transactionId)
        }
    }

    suspend fun getDailyRevenue(startOfDay: Long, endOfDay: Long): Long =
        transactionDao.getDailyRevenue(startOfDay, endOfDay)

    suspend fun getDailyTransactionCount(startOfDay: Long, endOfDay: Long): Int =
        transactionDao.getDailyTransactionCount(startOfDay, endOfDay)
}
