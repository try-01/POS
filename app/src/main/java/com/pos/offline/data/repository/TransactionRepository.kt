package com.pos.offline.data.repository

import androidx.room.withTransaction
import com.pos.offline.data.local.PosDatabase
import com.pos.offline.data.local.dao.CartDao
import com.pos.offline.data.local.dao.ProductDao
import com.pos.offline.data.local.dao.TransactionDao
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

data class CheckoutResult(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>
)

class InsufficientStockException(val productName: String) : RuntimeException()

class TransactionRepository(
    private val database: PosDatabase,
    private val transactionDao: TransactionDao,
    private val cartDao: CartDao,
    private val productDao: ProductDao
) {
    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun dailyTransactions(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeByDateRange(startOfDay, endOfDay)

    fun dailyRevenue(startOfDay: Long, endOfDay: Long): Flow<Long> =
        transactionDao.observeDailyRevenue(startOfDay, endOfDay)

    fun transactionsByShift(shiftId: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeByShift(shiftId)

    /**
     * Proses checkout.
     *
     * URUTAN KALKULASI (penting):
     *  1) subtotal  = Σ (hargaSatuan × qty) seluruh item.
     *  2) diskon dibatasi maksimal sebesar subtotal (tidak boleh negatif).
     *  3) pajak dihitung dari (subtotal − diskon), BUKAN subtotal → adil & benar.
     *  4) total     = (subtotal − diskon) + pajak.
     *  5) kembalian = max(0, paid − total); jika kurang bayar, change = 0.
     *
     * BATCH 3C: setiap item struk kini menyimpan snapshot `unitCost` (harga
     * modal produk SAAT transaksi ini terjadi) — dasar kalkulasi Laba Kotor
     * per shift di [ShiftRepository.getShiftSummary]. Diambil via query
     * terpisah per item (bukan batch) karena jumlah item per struk kasir
     * kecil (biasanya < 20), N+1 query di sini tidak signifikan.
     */
    suspend fun checkout(
        cart: List<CartItemEntity>,
        discount: Long,
        taxRate: Double,
        paid: Long,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        cashierId: Long? = null,
        cashierName: String = "",
        shiftId: Long? = null
    ): CheckoutResult {
        require(cart.isNotEmpty()) { "Keranjang kosong" }

        val subtotal = cart.sumOf { it.unitPrice * it.quantity.toLong() }
        val discountAmount = discount.coerceIn(0L, subtotal)
        val taxableBase = (subtotal - discountAmount).coerceAtLeast(0L)
        val tax = (taxableBase * taxRate).toLong()
        val total = taxableBase + tax
        val change = (paid - total).coerceAtLeast(0L)

        val invoiceId = "INV-${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()

        val transaction = TransactionEntity(
            id = invoiceId,
            createdAt = now,
            subtotal = subtotal,
            discount = discountAmount,
            tax = tax,
            total = total,
            paidAmount = paid,
            change = change,
            paymentMethod = paymentMethod.name,
            cashierId = cashierId,
            cashierName = cashierName,
            shiftId = shiftId
        )

        // NOTE: `productDao.getById(id)` diasumsikan ada mengikuti pola thin
        // wrapper yang sudah terbukti di CashierRepository/ShiftRepository.
        // Kalau nama fungsi asli di ProductDao berbeda, build akan gagal
        // persis di baris ini — sesuaikan nama sesuai DAO Anda.
        val items = cart.map { cartItem ->
            val unitCost = productDao.getById(cartItem.productId)?.cost ?: 0L
            TransactionItemEntity(
                transactionId = invoiceId,
                productName = cartItem.name,
                unitPrice = cartItem.unitPrice,
                quantity = cartItem.quantity,
                lineTotal = cartItem.unitPrice * cartItem.quantity.toLong(),
                unitCost = unitCost
            )
        }

        database.withTransaction {
            cart.forEach { item ->
                val affected = productDao.decrementStock(item.productId, item.quantity, now)
                if (affected == 0) {
                    throw InsufficientStockException(item.name)
                }
            }
            transactionDao.checkout(transaction, items)
            cartDao.clear()
        }

        return CheckoutResult(transaction, items)
    }

    suspend fun loadReceipt(invoiceId: String): CheckoutResult? {
        val tx = transactionDao.getById(invoiceId) ?: return null
        val items = transactionDao.getItems(invoiceId)
        return CheckoutResult(tx, items)
    }
}