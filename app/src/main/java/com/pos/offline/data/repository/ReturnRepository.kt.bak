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
                    productDao.incrementStock(input.productId, input.quantityReturned, now)
                }
            }
            transactionDao.setReturnId(transactionId, newReturnId)
        }
        return ReturnOutcome.Success(newReturnId)
    }
// Di ReturnRepository.kt
suspend fun processManualWarrantyClaim(
    productName: String,
    productId: Long?,
    quantity: Double,
    refundAmount: Long,
    refundMethod: PaymentMethod,
    shiftId: Long?,
    cashierId: Long?,
    cashierName: String,
    note: String
): ReturnOutcome {
    val now = System.currentTimeMillis()
    // Membuat ID Transaksi Garansi Manual Sintetis
    val syntheticTxId = "GARANSI-MANUAL-$now"
    
    val header = ReturnEntity(
        transactionId = syntheticTxId,
        returnedAt = now,
        shiftId = shiftId,
        cashierId = cashierId,
        cashierName = cashierName,
        refundAmount = refundAmount,
        refundMethod = refundMethod.name,
        note = "Garansi Tanpa Struk: $note"
    )
    
    database.withTransaction {
        val newReturnId = returnDao.insertReturn(header)
        val itemEntity = ReturnItemEntity(
            returnId = newReturnId,
            transactionItemId = 0L, // 0 menandakan klaim manual
            productId = productId,
            productName = productName,
            unitPrice = 0L,
            quantityReturned = quantity,
            restocked = false // Barang garansi rusak tidak dikembalikan ke stok
        )
        returnDao.insertItems(listOf(itemEntity))
    }
    return ReturnOutcome.Success(0L)
}
}
