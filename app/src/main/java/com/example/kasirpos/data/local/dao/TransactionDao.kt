package com.example.kasirpos.data.local.dao

import androidx.room.*
import com.example.kasirpos.data.local.entity.TransactionEntity
import com.example.kasirpos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ── Header Transaksi ──────────────────────────────────────

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    // ── Item Transaksi ────────────────────────────────────────

    @Insert
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsByTransactionId(transactionId: Long): List<TransactionItemEntity>

    // ── Laporan ───────────────────────────────────────────────

    /** Ringkasan pendapatan hari ini (00:00 – 23:59 lokal) */
    @Query("""
        SELECT COALESCE(SUM(grandTotal), 0) 
        FROM transactions 
        WHERE createdAt BETWEEN :startOfDay AND :endOfDay
    """)
    suspend fun getDailyRevenue(startOfDay: Long, endOfDay: Long): Long

    /** Jumlah transaksi hari ini */
    @Query("""
        SELECT COUNT(*) 
        FROM transactions 
        WHERE createdAt BETWEEN :startOfDay AND :endOfDay
    """)
    suspend fun getDailyTransactionCount(startOfDay: Long, endOfDay: Long): Int

    /** Observasi ringkasan harian (reaktif) */
    @Query("""
        SELECT COALESCE(SUM(grandTotal), 0) as revenue, COUNT(*) as count
        FROM transactions 
        WHERE createdAt BETWEEN :startOfDay AND :endOfDay
    """)
    fun observeDailySummary(startOfDay: Long, endOfDay: Long): Flow<DailySummary>
}

data class DailySummary(
    val revenue: Long,
    val count: Int
)
