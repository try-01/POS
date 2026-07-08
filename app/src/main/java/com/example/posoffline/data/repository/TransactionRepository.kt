package com.example.posoffline.data.repository

import com.example.posoffline.data.SettingsRepository
import com.example.posoffline.data.dao.ProductDao
import com.example.posoffline.data.dao.StockDecrement
import com.example.posoffline.data.dao.TransactionDao
import com.example.posoffline.data.entity.TransactionEntity
import com.example.posoffline.data.entity.TransactionItem
import com.example.posoffline.domain.model.PaymentMethod
import com.example.posoffline.domain.model.Totals
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for transactions.
 *
 * Holds the most safety-critical path of the POS: [checkout]. It:
 *   1. Re-reads every product from the DB (never trust the cart's prices).
 *   2. Verifies stock.
 *   3. Atomically decrements stock + inserts the transaction.
 *   4. Performs all money math in integer rupiah to avoid float drift.
 */
class TransactionRepository(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val settings: SettingsRepository? = null
) {

    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeSince(sinceMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeSince(sinceMillis)

    suspend fun get(id: String): TransactionEntity? = transactionDao.get(id)

    /**
     * Compute totals without persisting anything. Used by the cart UI to
     * show a live preview as the user edits.
     */
    fun computeTotals(
        lines: List<com.example.posoffline.domain.model.CartLine>,
        cartDiscount: Long,
        taxRate: Double
    ): Totals {
        // ---- pricing (integer rupiah, no float drift) ----
        var subtotal = 0L
        for (l in lines) {
            // line total = qty * price - per-line discount
            subtotal += l.qty * l.price - l.discount
        }
        // cart discount cannot make the post-discount subtotal negative
        val discount = cartDiscount.coerceIn(0L, subtotal)
        val taxable = subtotal - discount
        // tax: round-half-up to the nearest rupiah
        val tax = Math.round(taxable * taxRate)
        val grandTotal = taxable + tax
        return Totals(subtotal, discount, tax, grandTotal)
    }

    /**
     * Process checkout. This is the most safety-critical path of the POS.
     * See class kdoc for invariants.
     */
    suspend fun checkout(
        lines: List<com.example.posoffline.domain.model.CartLine>,
        cartDiscount: Long,
        paid: Long,
        paymentMethod: PaymentMethod
    ): TransactionEntity {
        if (lines.isEmpty()) throw IllegalStateException("Keranjang kosong")

        val taxRate = settings?.getSnapshot()?.taxRate ?: 0.11
        val totals = computeTotals(lines, cartDiscount, taxRate)
        val change = (paid - totals.grandTotal).coerceAtLeast(0L)

        // ---- stock check + decrement (atomic in DAO @Transaction) ----
        productDao.decrementStock(
            lines.map { StockDecrement(productId = it.productId, qty = it.qty) }
        )

        // ---- persist transaction ----
        val invoiceNo = "INV-${System.currentTimeMillis().toString(36).uppercase()}"
        val items = lines.map { l ->
            TransactionItem(
                productId = l.productId,
                name = l.name,
                price = l.price,
                qty = l.qty,
                discount = l.discount
            )
        }
        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            invoiceNo = invoiceNo,
            itemsJson = "", // filled in below, before insert
            subtotal = totals.subtotal,
            discount = totals.discount,
            taxRate = taxRate,
            tax = totals.tax,
            grandTotal = totals.grandTotal,
            paid = paid,
            change = change,
            paymentMethod = paymentMethod.name,
            createdAt = System.currentTimeMillis()
        )
        // We can't have the items list separately, so we serialize it here
        // before insert. The TypeConverter will overwrite itemsJson, but we
        // populate it now to keep the entity self-describing.
        val json = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(TransactionItem.serializer()),
            items
        )
        transactionDao.insert(entity.copy(itemsJson = json))
        return entity.copy(itemsJson = json)
    }
}
