package com.pos.offline.data.repository

import androidx.room.withTransaction
import com.pos.offline.data.local.PosDatabase
import com.pos.offline.data.local.dao.CartDao
import com.pos.offline.data.local.dao.ProductDao
import com.pos.offline.data.local.dao.TransactionDao
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.DiscountType
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.TransactionItemEntity
import com.pos.offline.util.roundToRupiah
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
     * URUTAN KALKULASI (penting, sesuai aturan PPN Indonesia):
     *  1) subtotal (bruto) = Σ (hargaSatuan × qty) seluruh item.
     *  2) diskon dikonversi dari [discountType]+[discountValue] mentah ke
     *     nominal Rupiah (dibulatkan round-half-up via [roundToRupiah]),
     *     lalu dibatasi maksimal sebesar subtotal (tidak boleh negatif).
     *  3) DPP (Dasar Pengenaan Pajak) = subtotal − diskon.
     *  4) pajak = DPP × taxRate, dibulatkan round-half-up.
     *  5) total = DPP + pajak.
     *  6) kembalian = max(0, paid − total); jika kurang bayar, change = 0.
     *
     * [discountType] & [discountValue] MURNI disimpan sebagai snapshot audit
     * ("apa yang diketik kasir") pada [TransactionEntity] — TIDAK dipakai
     * ulang untuk kalkulasi apa pun setelah ini; `discount` (nominal final)
     * tetap satu-satunya sumber kebenaran untuk laporan/shift.
     *
     * BATCH 3C: setiap item struk kini menyimpan snapshot `unitCost` (harga
     * modal produk SAAT transaksi ini terjadi) — dasar kalkulasi Laba Kotor
     * per shift di [ShiftRepository.getShiftSummary]. Diambil via query
     * terpisah per item (bukan batch) karena jumlah item per struk kasir
     * kecil (biasanya < 20), N+1 query di sini tidak signifikan.
     */
    suspend fun checkout(
        cart: List<CartItemEntity>,
        discountType: DiscountType,
        discountValue: Double,
        taxRate: Double,
        paid: Long,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        cashierId: Long? = null,
        cashierName: String = "",
        shiftId: Long? = null
    ): CheckoutResult {
        require(cart.isNotEmpty()) { "Keranjang kosong" }

        val subtotal = cart.sumOf { it.unitPrice * it.quantity.toLong() }

        val rawDiscountAmount = (when (discountType) {
            DiscountType.NOMINAL -> discountValue.roundToRupiah()
            DiscountType.PERCENT -> (subtotal * (discountValue / 100.0)).roundToRupiah()
        }).coerceAtLeast(0L)
        val discountAmount = rawDiscountAmount.coerceAtMost(subtotal)

        val taxableBase = (subtotal - discountAmount).coerceAtLeast(0L) // DPP
        val tax = (taxableBase * taxRate).roundToRupiah()
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
            shiftId = shiftId,
            discountType = discountType.name,
            discountValue = discountValue
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