package com.kasirku.pos.data.repository

import com.kasirku.pos.data.local.dao.DailySummaryResult
import com.kasirku.pos.data.local.dao.TransactionDao
import com.kasirku.pos.data.local.entity.CartItemEntity
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository Transaksi - Proses checkout atomik dan laporan
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val productRepository: ProductRepository
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val totalRevenue: Flow<Long> = transactionDao.getTotalRevenue()

    /**
     * Proses checkout dengan kalkulasi lengkap
     */
    suspend fun checkout(
        cartItems: List<CartItemEntity>,
        taxPercent: Double = 0.0,
        paymentAmount: Long
    ): Result<Long> {
        return try {
            require(cartItems.isNotEmpty()) { "Keranjang kosong" }

            val invoiceNumber = generateInvoiceNumber()

            var subtotal = 0L
            var totalDiscount = 0L

            val transactionItems = cartItems.map { item ->
                val grossAmount = item.unitPrice * item.quantity
                val discountAmount = (grossAmount * item.discountPercent / 100.0).toLong()
                val itemSubtotal = grossAmount - discountAmount

                subtotal += itemSubtotal
                totalDiscount += discountAmount

                TransactionItemEntity(
                    transactionId = 0,
                    productId = item.productId,
                    productName = item.productName,
                    sku = "",
                    unitPrice = item.unitPrice,
                    quantity = item.quantity,
                    discountPercent = item.discountPercent,
                    subtotal = itemSubtotal
                )
            }

            val taxAmount = (subtotal * taxPercent / 100.0).toLong()
            val totalAmount = subtotal + taxAmount
            val changeAmount = paymentAmount - totalAmount

            require(changeAmount >= 0) { "Pembayaran kurang! Kurang Rp\${-changeAmount}" }

            val transaction = TransactionEntity(
                invoiceNumber = invoiceNumber,
                subtotal = subtotal,
                taxPercent = taxPercent,
                taxAmount = taxAmount,
                totalDiscount = totalDiscount,
                totalAmount = totalAmount,
                paymentAmount = paymentAmount,
                changeAmount = changeAmount,
                itemCount = cartItems.sumOf { it.quantity }
            )

            val txId = transactionDao.insertFullTransaction(transaction, transactionItems)

            // Potong stok
            cartItems.forEach { item ->
                productRepository.decrementStock(item.productId, item.quantity)
            }

            Result.success(txId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionItems(transactionId: Long) =
        transactionDao.getTransactionItems(transactionId)

    suspend fun getTransactionById(id: Long) =
        transactionDao.getTransactionById(id)

    suspend fun getTodaySummary(): DailySummaryResult {
        val (start, end) = getTodayRange()
        return transactionDao.getDailySummary(start, end)
    }

    fun getTodayTransactions(): Flow<List<TransactionEntity>> {
        val (start, end) = getTodayRange()
        return transactionDao.getTransactionsByDate(start, end)
    }

    fun getTodayTransactionCount(): Flow<Int> {
        val (start, end) = getTodayRange()
        return transactionDao.getTodayTransactionCount(start, end)
    }

    private fun generateInvoiceNumber(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val timestamp = dateFormat.format(System.currentTimeMillis())
        val random = (0xA00..0xFFF).random().toString(16).uppercase()
        return "INV-\$timestamp-\$random"
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }
}
