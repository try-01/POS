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
        // Re-verify atomik: bila gagal, lempar exception agar SELURUH transaksi
        // (insert return, insert item, increment stok) di-rollback oleh Room.
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
    override fun fillInStackTrace(): Throwable = this // hindari cost stack-trace untuk control-flow exception
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
        
        // LAPISAN PENGAMAN 1: Kunci histori harga saat transaksi terjadi
        val totalBroken = kotlin.math.round(brokenProduct.price * brokenQty).toLong()
        val totalReplacement = kotlin.math.round(replacementProduct.price * replacementQty).toLong()
        
        // Hitung selisih harga (Delta)
        val delta = totalReplacement - totalBroken
        val idSuffix = (100..999).random()

        // ---------------------------------------------------------
        // SISI 1: LOGIKA RETUR (Mencatat barang rusak yang kembali)
        // ---------------------------------------------------------
        // Jika delta < 0 (Toko mengembalikan uang), ini adalah retur RIIL yang memotong kas laci.
        // Jika tidak, ini retur SINTETIS (EXC-RET) yang akan diabaikan oleh laci kas.
        val isRealRefund = delta < 0
        val returnIdPrefix = if (isRealRefund) "RET-DIR-" else "EXC-RET-"
        val syntheticReturnId = "$returnIdPrefix$now-$idSuffix"
        
        // Hanya catat pengeluaran uang jika memang ada kembalian ke pelanggan
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
            // Baris ini cash-neutral (murni bookkeeping) HANYA bila bukan refund riil —
            // sinkron dengan actualRefundCash yang di atas: 0 bila !isRealRefund.
            isWarrantyExchange = !isRealRefund,
        )

        // ---------------------------------------------------------
        // SISI 2: LOGIKA INVOICE (Mencatat barang pengganti yang keluar)
        // ---------------------------------------------------------
        // Jika delta > 0 (Pelanggan nambah uang), ini adalah invoice RIIL yang menambah kas laci.
        val isRealSale = delta > 0
        val invoiceIdPrefix = if (isRealSale) "INV-EXC-" else "EXC-INV-"
        val syntheticInvoiceId = "$invoiceIdPrefix$now-$idSuffix"
        
        // Uang masuk riil ke laci kasir hanyalah kekurangannya saja (delta)
        val actualSaleCash = if (isRealSale) delta else 0L
        
        // Jadikan nilai barang rusak sebagai "Diskon/Kredit" untuk memotong total invoice
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
            // Jika penjualan riil, isWarrantyExchange = false agar terhitung di laci kasir
            isWarrantyExchange = !isRealSale, 
        )

        val transactionItem = com.pos.offline.data.local.entity.TransactionItemEntity(
            transactionId = syntheticInvoiceId,
            productName = replacementProduct.name,
            unitPrice = replacementProduct.price, // Harga terkunci di sini
            quantity = replacementQty,
            lineTotal = totalReplacement,
            unitCost = replacementProduct.cost,
            productId = replacementProduct.id,
        )

        // LAPISAN PENGAMAN 2: Gunakan Database Transaction agar jika satu proses gagal, semua dibatalkan
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
            
            // Tambah stok ke gudang rusak
            productDao.incrementDamagedStock(brokenProduct.id, brokenQty, now)
            
            // Masukkan data penjualan pengganti
            transactionDao.checkout(transactionHeader, listOf(transactionItem))
            
            // Potong stok barang pengganti, tolak jika stok tidak cukup
            val affected = productDao.decrementStock(replacementProduct.id, replacementQty, now)
            if (affected == 0) {
                throw RuntimeException("Stok ${replacementProduct.name} tidak mencukupi untuk penukaran.")
            }
        }
        
        return ReturnOutcome.Success(0L)
    }
}
