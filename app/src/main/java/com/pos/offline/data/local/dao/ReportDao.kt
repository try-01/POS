package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class SalesSummary(
    val transactionCount: Int,
    val subtotalSum: Long,
    val taxSum: Long,
    val totalSum: Long,
    val actualReceivedSum: Long,
)

data class ProfitAndItemsSummary(
    val itemsSoldSum: Double,
    val revenueSum: Long,
    val costSum: Long,
)

data class PaymentMethodSummary(
    val paymentMethod: String,
    val total: Long,
    val count: Int,
    val actualReceived: Long,
)

data class ProductSalesRow(
    val productId: Long,
    val productName: String,
    val sku: String,
    val price: Long,
    val stock: Double,
    val qtySold: Double,
    val revenue: Long,
)

@Dao
interface ReportDao {
    @Query(
        """
        SELECT COUNT(CASE WHEN isWarrantyExchange = 0 THEN 1 END) as transactionCount,
               COALESCE(SUM(CASE WHEN isWarrantyExchange = 0 THEN subtotal ELSE 0 END), 0) as subtotalSum,
               COALESCE(SUM(CASE WHEN isWarrantyExchange = 0 THEN tax ELSE 0 END), 0) as taxSum,
               COALESCE(SUM(CASE WHEN isWarrantyExchange = 0 THEN total ELSE 0 END), 0) as totalSum,
               COALESCE(SUM(
                   CASE
                       WHEN paymentMethod = 'CASH' THEN paidAmount - changeGiven
                       ELSE total
                   END
               ), 0) as actualReceivedSum
        FROM transactions WHERE createdAt >= :start AND createdAt < :end AND status = 'COMPLETED'
    """,
    )
    suspend fun getSalesSummary(
        start: Long,
        end: Long,
    ): SalesSummary

    @Query(
        """
        SELECT COALESCE(SUM(ti.quantity), 0) as itemsSoldSum,
               COALESCE(SUM(ti.lineTotal), 0) as revenueSum,
               COALESCE(SUM(CAST(ROUND(ti.quantity * ti.unitCost) AS INTEGER)), 0) as costSum
        FROM transaction_items ti INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.createdAt >= :start AND t.createdAt < :end AND t.status = 'COMPLETED'
          AND t.isWarrantyExchange = 0
    """,
    )
    suspend fun getProfitAndItemsSummary(
        start: Long,
        end: Long,
    ): ProfitAndItemsSummary

    // Biaya modal barang pengganti pada transaksi tukar-guling garansi, dipisah
    // dari COGS penjualan murni agar terlihat sebagai baris "Biaya Klaim Garansi" tersendiri.
    @Query(
        """
        SELECT COALESCE(SUM(CAST(ROUND(ti.quantity * ti.unitCost) AS INTEGER)), 0)
        FROM transaction_items ti INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.createdAt >= :start AND t.createdAt < :end AND t.status = 'COMPLETED'
          AND t.isWarrantyExchange = 1
    """,
    )
    suspend fun getWarrantyExchangeCost(
        start: Long,
        end: Long,
    ): Long

    @Query(
        """
        SELECT COALESCE(SUM(
            CAST(ROUND(
                CASE
                    WHEN ri.transactionItemId = 0 THEN COALESCE(p.cost, 0)
                    ELSE ti.unitCost
                END * ri.quantityReturned
            ) AS INTEGER)
        ), 0)
        FROM return_items ri
        LEFT JOIN transaction_items ti ON ri.transactionItemId = ti.id
        LEFT JOIN products p ON ri.productId = p.id
        INNER JOIN returns r ON r.id = ri.returnId
        WHERE r.returnedAt >= :start AND r.returnedAt < :end
          AND (ri.restocked = 1 OR ri.transactionItemId = 0)
    """,
    )
    suspend fun getRestockedReturnsCost(
        start: Long,
        end: Long,
    ): Long

    @Query(
        """
        SELECT paymentMethod,
               COALESCE(SUM(total), 0) as total,
               COUNT(CASE WHEN isWarrantyExchange = 0 THEN 1 END) as count,
               COALESCE(SUM(
                   CASE
                       WHEN paymentMethod = 'CASH' THEN paidAmount - changeGiven
                       ELSE total
                   END
               ), 0) as actualReceived
        FROM transactions WHERE createdAt >= :start AND createdAt < :end AND status = 'COMPLETED'
        GROUP BY paymentMethod
    """,
    )
    suspend fun getPaymentMethodSummary(
        start: Long,
        end: Long,
    ): List<PaymentMethodSummary>

    @Query("SELECT COALESCE(SUM(refundAmount), 0) FROM returns WHERE returnedAt >= :start AND returnedAt < :end")
    suspend fun getReturnsTotal(
        start: Long,
        end: Long,
    ): Long

    @Query(
        """
        SELECT p.id as productId, p.name as productName, p.sku as sku, p.price as price, p.stock as stock,
               COALESCE(SUM(ti.quantity), 0) as qtySold,
               COALESCE(SUM(ti.lineTotal), 0) as revenue
        FROM products p
        LEFT JOIN transaction_items ti ON p.id = ti.productId
        LEFT JOIN transactions t ON t.id = ti.transactionId
              AND t.createdAt >= :start AND t.createdAt < :end
              AND t.status = 'COMPLETED'
        WHERE (:activeOnly = 0 OR p.active = 1)
        GROUP BY p.id
        ORDER BY qtySold DESC, p.name ASC
        LIMIT :limit
    """,
    )
    suspend fun getTopSellingProducts(
        start: Long,
        end: Long,
        limit: Int = Int.MAX_VALUE, // default "tanpa batas" — laporan penjualan butuh data lengkap.
        activeOnly: Boolean = false,
    ): List<ProductSalesRow>

    @Query(
        """
        SELECT p.*
        FROM products p
        LEFT JOIN transaction_items ti ON p.id = ti.productId
        LEFT JOIN transactions t ON t.id = ti.transactionId
              AND t.createdAt >= :start AND t.createdAt < :end
              AND t.status = 'COMPLETED'
        WHERE p.active = 1
        GROUP BY p.id
        ORDER BY COALESCE(SUM(ti.quantity), 0) DESC, p.name ASC
    """,
    )
    fun observeProductsByTopSales(
        start: Long,
        end: Long,
    ): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT DISTINCT t.*
        FROM transactions t
        INNER JOIN transaction_items ti ON t.id = ti.transactionId
        WHERE (ti.productName LIKE '%' || :query || '%'
           OR ti.productId = :productId
           OR t.id LIKE '%' || :query || '%')
          AND t.status = 'COMPLETED'
        ORDER BY t.createdAt DESC
        LIMIT 50
    """,
    )
    suspend fun searchTransactionsByProduct(
        query: String,
        productId: Long? = null,
    ): List<TransactionEntity>

    @Query(
        """
        SELECT DISTINCT t.*
        FROM transactions t
        INNER JOIN transaction_items ti ON t.id = ti.transactionId
        LEFT JOIN products p ON ti.productId = p.id
        WHERE t.createdAt >= :oneYearAgoMillis
          AND t.status = 'COMPLETED'
          AND (
              ti.productName LIKE '%' || :query || '%'
              OR p.name LIKE '%' || :query || '%'
              OR p.sku LIKE '%' || :query || '%'
              OR p.category LIKE '%' || :query || '%'
              OR p.barcode = :query
          )
        ORDER BY t.createdAt DESC
    """,
    )
    suspend fun searchProductSalesHistory1Year(
        query: String,
        oneYearAgoMillis: Long,
    ): List<TransactionEntity>
}
