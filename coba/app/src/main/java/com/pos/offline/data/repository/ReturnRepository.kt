package com.pos.offline.data.repository
import androidx.room.withTransaction
import com.pos.offline.data.local.PosDatabase
import com.pos.offline.data.local.dao.ProductDao
import com.pos.offline.data.local.dao.ReturnDao
import com.pos.offline.data.local.dao.TransactionDao
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ReturnEntity
import com.pos.offline.data.local.entity.ReturnItemEntity
import com.pos.offline.data.local.entity.hasReturn
import com.pos.offline.data.local.entity.isVoid
import kotlinx.coroutines.flow.Flow
data class ReturnItemInput(
    val transactionItemId: Long,
    val productId: Long?,
    val productName: String,
    val unitPrice: Long,
    val quantityReturned: Double,
    val restocked: Boolean,
    val restockToDamaged: Boolean = false,
)
data class ReturnDetail(
    val header: ReturnEntity,
    val items: List<ReturnItemEntity>,
)
sealed class ReturnOutcome {
    data class Success(
        val returnId: Long,
    ) : ReturnOutcome()
    data object TransactionNotFound : ReturnOutcome()
    data object TransactionVoided : ReturnOutcome()
    data object AlreadyReturned : ReturnOutcome()
    data object NoItemsSelected : ReturnOutcome()
    data class InvalidQuantity(
        val productName: String,
    ) : ReturnOutcome()
    data class InvalidRefundAmount(
        val maxAllowed: Long,
    ) : ReturnOutcome()
}
class ReturnRepository(
    private val database: PosDatabase,
    private val returnDao: ReturnDao,
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
) {
    fun returnsBetween(
        start: Long,
        end: Long,
    ): Flow<List<ReturnEntity>> = returnDao.observeReturnsBetween(start, end)
    suspend fun getDetail(returnId: Long): ReturnDetail? {
        val header = returnDao.getById(returnId) ?: return null
        return ReturnDetail(header, returnDao.getItems(returnId))
    }
    suspend fun getDetailByTransactionId(transactionId: String): ReturnDetail? {
        val header = returnDao.getByTransactionId(transactionId) ?: return null
        return ReturnDetail(header, returnDao.getItems(header.id))
    }
    suspend fun processReturn(
        transactionId: String,
        itemInputs: List<ReturnItemInput>,
        refundAmount: Long,
        refundMethod: PaymentMethod,
        shiftId: Long?,
        cashierId: Long?,
        cashierName: String,
        note: String = "",
    ): ReturnOutcome {
        val transaction =
            transactionDao.getById(transactionId)
                ?: return ReturnOutcome.TransactionNotFound
        if (transaction.isVoid) return ReturnOutcome.TransactionVoided
        if (transaction.hasReturn) return ReturnOutcome.AlreadyReturned
        if (itemInputs.isEmpty()) return ReturnOutcome.NoItemsSelected
        if (refundAmount < 0 || refundAmount > transaction.total) {
            return ReturnOutcome.InvalidRefundAmount(maxAllowed = transaction.total)
        }
        val originalItems = transactionDao.getItems(transactionId).associateBy { it.id }
        itemInputs.forEach { input ->
            val original = originalItems[input.transactionItemId]
            if (original == null ||
                input.quantityReturned <= 0.0 ||
                input.quantityReturned > original.quantity
            ) {
                return ReturnOutcome.InvalidQuantity(input.productName)
            }
        }
        val now = System.currentTimeMillis()
        val header =
            ReturnEntity(
                transactionId = transactionId,
                returnedAt = now,
                shiftId = shiftId,
                cashierId = cashierId,
                cashierName = cashierName,
                refundAmount = refundAmount,
                refundMethod = refundMethod.name,
                note = note,
            )
var newReturnId = 0L
    var conflict: ReturnOutcome? = null
    try {
    database.withTransaction {
        newReturnId = returnDao.insertReturn(header)
        val itemEntities =
            itemInputs.map { input ->
                ReturnItemEntity(
                    returnId = newReturnId,
                    transactionItemId = input.transactionItemId,
                    productId = input.productId,
                    productName = input.productName,
                    unitPrice = input.unitPrice,
                    quantityReturned = input.quantityReturned,
                    restocked = input.restocked,
                )
            }
        returnDao.insertItems(itemEntities)
        itemInputs.forEach { input ->
            if (input.restocked && input.productId != null) {
                if (input.restockToDamaged) {
                    productDao.incrementDamagedStock(input.productId, input.quantityReturned, now)
                } else {
                    productDao.incrementStock(input.productId, input.quantityReturned, now)
                }
            }
        }
        val affected = transactionDao.setReturnIdIfAbsent(transactionId, newReturnId)
        if (affected == 0) {
            val latest = transactionDao.getById(transactionId)
            conflict = if (latest?.isVoid == true) ReturnOutcome.TransactionVoided else ReturnOutcome.AlreadyReturned
            throw ReturnConflictRollback
        }
    }
    } catch (e: Throwable) {
        if (e !== ReturnConflictRollback) throw e
    }
    conflict?.let { return it }
    return ReturnOutcome.Success(newReturnId)
}
private object ReturnConflictRollback : RuntimeException() {
    private fun readResolve(): Any = ReturnConflictRollback
    override fun fillInStackTrace(): Throwable = this
}
    suspend fun processDirectExchangeWarranty(
        brokenProduct: com.pos.offline.data.local.entity.ProductEntity,
        brokenQty: Double,
        replacementProduct: com.pos.offline.data.local.entity.ProductEntity,
        replacementQty: Double,
        shiftId: Long?,
        cashierId: Long?,
        cashierName: String,
        note: String,
    ): ReturnOutcome {
        val now = System.currentTimeMillis()
        val totalBroken = kotlin.math.round(brokenProduct.price * brokenQty).toLong()
        val totalReplacement = kotlin.math.round(replacementProduct.price * replacementQty).toLong()
        val delta = totalReplacement - totalBroken
        val idSuffix = (100..999).random()
        val isRealRefund = delta < 0
        val returnIdPrefix = if (isRealRefund) "RET-DIR-" else "EXC-RET-"
        val syntheticReturnId = "$returnIdPrefix$now-$idSuffix"
        val actualRefundCash = if (isRealRefund) kotlin.math.abs(delta) else 0L
        val returnHeader = ReturnEntity(
            transactionId = syntheticReturnId,
            returnedAt = now,
            shiftId = shiftId,
            cashierId = cashierId,
            cashierName = cashierName,
            refundAmount = actualRefundCash,
            refundMethod = PaymentMethod.CASH.name,
            note = "Tukar Guling Garansi: $note",
            isWarrantyExchange = !isRealRefund,
        )
        val isRealSale = delta > 0
        val invoiceIdPrefix = if (isRealSale) "INV-EXC-" else "EXC-INV-"
        val syntheticInvoiceId = "$invoiceIdPrefix$now-$idSuffix"
        val actualSaleCash = if (isRealSale) delta else 0L
        val discountApplied = if (isRealSale) totalBroken else totalReplacement
        val transactionHeader = com.pos.offline.data.local.entity.TransactionEntity(
            id = syntheticInvoiceId,
            createdAt = now,
            subtotal = totalReplacement,
            discount = discountApplied,
            tax = 0L,
            total = actualSaleCash,
            paidAmount = actualSaleCash,
            change = 0L,
            changeGiven = 0L,
            changeGivenInCash = true,
            paymentMethod = PaymentMethod.CASH.name,
            cashierId = cashierId,
            cashierName = cashierName,
            shiftId = shiftId,
            discountType = com.pos.offline.data.local.entity.DiscountType.NOMINAL.name,
            discountValue = discountApplied.toDouble(),
            status = com.pos.offline.data.local.entity.TransactionStatus.COMPLETED.name,
            isWarrantyExchange = !isRealSale,
        )
        val transactionItem = com.pos.offline.data.local.entity.TransactionItemEntity(
            transactionId = syntheticInvoiceId,
            productName = replacementProduct.name,
            unitPrice = replacementProduct.price,
            quantity = replacementQty,
            lineTotal = totalReplacement,
            unitCost = replacementProduct.cost,
            productId = replacementProduct.id,
        )
        database.withTransaction {
            val newReturnId = returnDao.insertReturn(returnHeader)
            val returnItem = ReturnItemEntity(
                returnId = newReturnId,
                transactionItemId = 0L,
                productId = brokenProduct.id,
                productName = brokenProduct.name,
                unitPrice = brokenProduct.price,
                quantityReturned = brokenQty,
                restocked = false,
            )
            returnDao.insertItems(listOf(returnItem))
            productDao.incrementDamagedStock(brokenProduct.id, brokenQty, now)
            transactionDao.checkout(transactionHeader, listOf(transactionItem))
            val affected = productDao.decrementStock(replacementProduct.id, replacementQty, now)
            if (affected == 0) {
                throw RuntimeException("Stok ${replacementProduct.name} tidak mencukupi untuk penukaran.")
            }
        }
        return ReturnOutcome.Success(0L)
    }
}
