package com.kasirku.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Transaksi dengan query laporan harian
 */
@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Transaction
    suspend fun insertFullTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        val txId = insertTransaction(transaction)
        val itemsWithTxId = items.map { it.copy(transactionId = txId) }
        insertTransactionItems(itemsWithTxId)
        return txId
    }

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE transaction_date >= :startOfDay 
        AND transaction_date < :endOfDay
        ORDER BY transaction_date DESC
    """)
    fun getTransactionsByDate(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    suspend fun getTransactionItems(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("""
        SELECT 
            COUNT(*) as transactionCount,
            COALESCE(SUM(total_amount), 0) as totalRevenue,
            COALESCE(AVG(total_amount), 0) as averageTransaction
        FROM transactions
        WHERE transaction_date >= :startOfDay 
        AND transaction_date < :endOfDay
    """)
    suspend fun getDailySummary(startOfDay: Long, endOfDay: Long): DailySummaryResult

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM transactions")
    fun getTotalRevenue(): Flow<Long>

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE transaction_date >= :startOfDay 
        AND transaction_date < :endOfDay
    """)
    fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int>
}

data class DailySummaryResult(
    val transactionCount: Int,
    val totalRevenue: Long,
    val averageTransaction: Long
)
