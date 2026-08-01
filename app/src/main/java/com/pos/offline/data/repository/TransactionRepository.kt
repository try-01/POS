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
import com.pos.offline.data.local.entity.TransactionStatus
import com.pos.offline.data.local.entity.isVoid
import com.pos.offline.util.roundToRupiah
import kotlinx.coroutines.flow.Flow

data class CheckoutResult(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>,
)

class InsufficientStockException(
    val productName: String,
) : RuntimeException()

sealed class VoidOutcome {
    data class Success(
        val restoredStockCount: Int,
        val skippedStockCount: Int,
    ) : VoidOutcome()

    data object AlreadyVoided : VoidOutcome()

    data object NotFound : VoidOutcome()

    data object ShiftClosed : VoidOutcome()

    data object HasReturn : VoidOutcome()
}

class TransactionRepository(
    private val database: PosDatabase,
    private val transactionDao: TransactionDao,
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val shiftRepository: ShiftRepository,
) {
    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun dailyTransactions(
        startOfDay: Long,
        endOfDay: Long,
    ): Flow<List<TransactionEntity>> = transactionDao.observeByDateRange(startOfDay, endOfDay)

    fun dailyRevenue(
        startOfDay: Long,
        endOfDay: Long,
    ): Flow<Long> = transactionDao.observeDailyRevenue(startOfDay, endOfDay)

    fun transactionsByShift(shiftId: Long): Flow<List<TransactionEntity>> = transactionDao.observeByShift(shiftId)

    suspend fun checkout(
        cart: List<CartItemEntity>,
        discountType: DiscountType,
        discountValue: Double,
        taxRate: Double,
        paid: Long,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        cashierId: Long? = null,
        cashierName: String = "",
        shiftId: Long? = null,
        changeGivenOverride: Long? = null,
        changeGivenInCash: Boolean = true,
    ): CheckoutResult {
        require(cart.isNotEmpty()) { "Keranjang kosong" }
        val subtotal = cart.sumOf { kotlin.math.round(it.unitPrice * it.quantity).toLong() }
        val rawDiscountAmount =
            (
                when (discountType) {
                    DiscountType.NOMINAL -> discountValue.roundToRupiah()
                    DiscountType.PERCENT -> (subtotal * (discountValue / 100.0)).roundToRupiah()
                }
            ).coerceAtLeast(0L)
        val discountAmount = rawDiscountAmount.coerceAtMost(subtotal)
        val taxableBase = (subtotal - discountAmount).coerceAtLeast(0L)
        val tax = (taxableBase * taxRate).roundToRupiah()
        val total = taxableBase + tax
        val change = paid - total
        val maxChangeGiven = change.coerceAtLeast(0L)
        val changeGiven = (changeGivenOverride ?: maxChangeGiven).coerceIn(0L, maxChangeGiven)
        val effectiveChangeGivenInCash = paymentMethod == PaymentMethod.CASH || changeGivenInCash
        val invoiceId = "INV-${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()
        val transaction =
            TransactionEntity(
                id = invoiceId,
                createdAt = now,
                subtotal = subtotal,
                discount = discountAmount,
                tax = tax,
                total = total,
                paidAmount = paid,
                change = change,
                changeGiven = changeGiven,
                changeGivenInCash = effectiveChangeGivenInCash,
                paymentMethod = paymentMethod.name,
                cashierId = cashierId,
                cashierName = cashierName,
                shiftId = shiftId,
                discountType = discountType.name,
                discountValue = discountValue,
            )
        val items =
            cart.map { cartItem ->
                val unitCost = productDao.getById(cartItem.productId)?.cost ?: 0L
                TransactionItemEntity(
                    transactionId = invoiceId,
                    productId = cartItem.productId,
                    productName = cartItem.name,
                    unitPrice = cartItem.unitPrice,
                    quantity = cartItem.quantity,
                    lineTotal = kotlin.math.round(cartItem.unitPrice * cartItem.quantity).toLong(),
                    unitCost = unitCost,
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

    suspend fun voidTransaction(invoiceId: String): VoidOutcome {
        val tx = transactionDao.getById(invoiceId) ?: return VoidOutcome.NotFound
        if (tx.isVoid) return VoidOutcome.AlreadyVoided
        if (tx.returnId != null) return VoidOutcome.HasReturn
        val shiftId = tx.shiftId
        if (shiftId != null) {
            val shift = shiftRepository.getById(shiftId)
            if (shift?.endedAt != null) return VoidOutcome.ShiftClosed
        }
        val items = transactionDao.getItems(invoiceId)
        val now = System.currentTimeMillis()
        var restored = 0
        var skipped = 0
        database.withTransaction {
            items.forEach { item ->
                val pid = item.productId
                if (pid != null) {
                    productDao.incrementStock(pid, item.quantity, now)
                    restored++
                } else {
                    skipped++
                }
            }
            transactionDao.setStatus(
                id = invoiceId,
                status = TransactionStatus.VOID.name,
                voidedAt = now,
                reason = null,
            )
        }
        return VoidOutcome.Success(restoredStockCount = restored, skippedStockCount = skipped)
    }

    suspend fun searchGlobalTransactions(query: String): List<TransactionEntity> {
        if (query.isBlank()) return emptyList()
        return transactionDao.searchGlobalTransactions(query.trim())
    }
}
