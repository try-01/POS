package com.kasirku.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity
import com.kasirku.pos.data.local.relation.TransactionWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertItems(items: List<TransactionItemEntity>) // batch insert, lebih efisien daripada loop satu-satu

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Query("SELECT * FROM transactions WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    fun observeTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    /**
     * Ringkasan pendapatan (jumlah transaksi & total omzet) dihitung langsung oleh SQLite
     * dalam satu query agregat (COUNT + SUM), jauh lebih cepat & hemat memori dibanding
     * menarik seluruh baris transaksi ke Kotlin lalu menjumlahkannya secara manual.
     */
    @Query(
        """
        SELECT COUNT(*) as transactionCount, IFNULL(SUM(grandTotal), 0) as totalRevenue
        FROM transactions
        WHERE createdAt BETWEEN :start AND :end
        """
    )
    fun observeDailySummary(start: Long, end: Long): Flow<DailySummary>
}

data class DailySummary(
    val transactionCount: Int,
    val totalRevenue: Double
)
