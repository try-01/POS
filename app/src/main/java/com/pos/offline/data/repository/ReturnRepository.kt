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

/** Input 1 baris item yang diretur — dipilih & diatur kasir di dialog Retur (Batch E3). */
data class ReturnItemInput(
    val transactionItemId: Long,
    val productId: Long?,
    val productName: String,
    val unitPrice: Long,
    val quantityReturned: Int,
    val restocked: Boolean
)

/** Snapshot lengkap 1 proses retur (header + rincian item) — dasar dialog detail read-only. */
data class ReturnDetail(
    val header: ReturnEntity,
    val items: List<ReturnItemEntity>
)

sealed class ReturnOutcome {
    data class Success(val returnId: Long) : ReturnOutcome()
    data object TransactionNotFound : ReturnOutcome()
    data object TransactionVoided : ReturnOutcome()
    data object AlreadyReturned : ReturnOutcome()
    data object NoItemsSelected : ReturnOutcome()

    /** quantityReturned tidak valid (<=0 atau melebihi qty asli baris transaksi terkait). */
    data class InvalidQuantity(val productName: String) : ReturnOutcome()
}

class ReturnRepository(
    private val database: PosDatabase,
    private val returnDao: ReturnDao,
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao
) {
    /** Retur yang TERJADI (returnedAt) dalam rentang tanggal — dasar section "Retur Hari Ini". */
    fun returnsBetween(start: Long, end: Long): Flow<List<ReturnEntity>> =
        returnDao.observeReturnsBetween(start, end)

    suspend fun getDetail(returnId: Long): ReturnDetail? {
        val header = returnDao.getById(returnId) ?: return null
        return ReturnDetail(header, returnDao.getItems(returnId))
    }

    /** Dipakai TransactionDetailDialog untuk menampilkan ringkasan retur transaksi ybs (kalau ada). */
    suspend fun getDetailByTransactionId(transactionId: String): ReturnDetail? {
        val header = returnDao.getByTransactionId(transactionId) ?: return null
        return ReturnDetail(header, returnDao.getItems(header.id))
    }

    /**
     * Proses retur untuk satu transaksi. Data transaksi & item asli di-RE-FETCH
     * dari DB berdasarkan [transactionId] (bukan diterima sebagai parameter dari
     * UI) — konsisten dengan pola TransactionRepository.voidTransaction, supaya
     * tidak ada celah state UI basi antara dialog dibuka & dikonfirmasi.
     *
     * Efek dalam SATU database transaction (atomik):
     * 1. Insert header `returns` + baris `return_items`.
     * 2. Kembalikan stok HANYA untuk item dengan `restocked = true` & `productId`
     *    tersedia (data pra-migrasi tanpa productId dilewati aman, sama seperti
     *    reversal stok di voidTransaction).
     * 3. Set `transactions.returnId` → menandai transaksi FINAL, tidak bisa
     *    diretur lagi (tombol "Retur Item" hilang otomatis di UI).
     */
    suspend fun processReturn(
        transactionId: String,
        itemInputs: List<ReturnItemInput>,
        refundAmount: Long,
        refundMethod: PaymentMethod,
        shiftId: Long?,
        cashierId: Long?,
        cashierName: String,
        note: String = ""
    ): ReturnOutcome {
        val transaction = transactionDao.getById(transactionId)
            ?: return ReturnOutcome.TransactionNotFound
        if (transaction.isVoid) return ReturnOutcome.TransactionVoided
        if (transaction.hasReturn) return ReturnOutcome.AlreadyReturned
        if (itemInputs.isEmpty()) return ReturnOutcome.NoItemsSelected

        val originalItems = transactionDao.getItems(transactionId).associateBy { it.id }
        itemInputs.forEach { input ->
            val original = originalItems[input.transactionItemId]
            if (original == null ||
                input.quantityReturned <= 0 ||
                input.quantityReturned > original.quantity
            ) {
                return ReturnOutcome.InvalidQuantity(input.productName)
            }
        }

        val now = System.currentTimeMillis()
        val header = ReturnEntity(
            transactionId = transactionId,
            returnedAt = now,
            shiftId = shiftId,
            cashierId = cashierId,
            cashierName = cashierName,
            refundAmount = refundAmount,
            refundMethod = refundMethod.name,
            note = note
        )

        var newReturnId = 0L
        database.withTransaction {
            newReturnId = returnDao.insertReturn(header)

            val itemEntities = itemInputs.map { input ->
                ReturnItemEntity(
                    returnId = newReturnId,
                    transactionItemId = input.transactionItemId,
                    productId = input.productId,
                    productName = input.productName,
                    unitPrice = input.unitPrice,
                    quantityReturned = input.quantityReturned,
                    restocked = input.restocked
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
}