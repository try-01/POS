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
            transactionDao.setReturnId(transactionId, newReturnId)
        }
        return ReturnOutcome.Success(newReturnId)
    }

    suspend fun processManualWarrantyClaim(
        productName: String,
        productId: Long?,
        quantity: Double,
        refundAmount: Long,
        refundMethod: PaymentMethod,
        shiftId: Long?,
        cashierId: Long?,
        cashierName: String,
        note: String,
    ): ReturnOutcome {
        val now = System.currentTimeMillis()
        val syntheticTxId = "GARANSI-MANUAL-$now"

        val header =
            ReturnEntity(
                transactionId = syntheticTxId,
                returnedAt = now,
                shiftId = shiftId,
                cashierId = cashierId,
                cashierName = cashierName,
                refundAmount = refundAmount,
                refundMethod = refundMethod.name,
                note = "Garansi Tanpa Struk: $note",
            )

        database.withTransaction {
            val newReturnId = returnDao.insertReturn(header)
            val itemEntity =
                ReturnItemEntity(
                    returnId = newReturnId,
                    transactionItemId = 0L,
                    productId = productId,
                    productName = productName,
                    unitPrice = 0L,
                    quantityReturned = quantity,
                    restocked = false,
                )
            returnDao.insertItems(listOf(itemEntity))

            if (productId != null) {
                productDao.incrementDamagedStock(productId, quantity, now)
            }
        }
        return ReturnOutcome.Success(0L)
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

        val totalBroken = (brokenProduct.price * brokenQty).toLong()
        val totalReplacement = (replacementProduct.price * replacementQty).toLong()

        val syntheticReturnId = "EXC-RET-$now"
        val syntheticInvoiceId = "EXC-INV-$now"

        val returnHeader =
            ReturnEntity(
                transactionId = syntheticReturnId,
                returnedAt = now,
                shiftId = shiftId,
                cashierId = cashierId,
                cashierName = cashierName,
                refundAmount = totalBroken,
                refundMethod = PaymentMethod.CASH.name,
                note = "Tukar Guling Garansi (Rusak): $note",
            )

        val transactionHeader =
            com.pos.offline.data.local.entity.TransactionEntity(
                id = syntheticInvoiceId,
                createdAt = now,
                subtotal = totalReplacement,
                discount = 0L,
                tax = 0L,
                total = totalReplacement,
                paidAmount = totalReplacement,
                change = 0L,
                changeGiven = 0L,
                changeGivenInCash = true,
                paymentMethod = PaymentMethod.CASH.name,
                cashierId = cashierId,
                cashierName = cashierName,
                shiftId = shiftId,
                discountType = com.pos.offline.data.local.entity.DiscountType.NOMINAL.name,
                discountValue = 0.0,
                status = com.pos.offline.data.local.entity.TransactionStatus.COMPLETED.name,
            )

        val transactionItem =
            com.pos.offline.data.local.entity.TransactionItemEntity(
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
            val returnItem =
                ReturnItemEntity(
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
