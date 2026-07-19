package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>
    @Query(
        """
        SELECT * FROM transactions
        WHERE createdAt >= :startOfDay AND createdAt < :endOfDay
        ORDER BY createdAt DESC
        """
    )
    fun observeByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE createdAt >= :startOfDay AND createdAt < :endOfDay AND status = 'COMPLETED'
        """
    )
    fun observeDailyRevenue(startOfDay: Long, endOfDay: Long): Flow<Long>

    @Query("SELECT * FROM transactions WHERE id = :invoiceId")
    suspend fun getById(invoiceId: String): TransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :invoiceId")
    suspend fun getItems(invoiceId: String): List<TransactionItemEntity>

    @Query("SELECT * FROM transactions WHERE shiftId = :shiftId ORDER BY createdAt ASC")
    fun observeByShift(shiftId: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        UPDATE transactions
        SET status = :status, voidedAt = :voidedAt, voidReason = :reason
        WHERE id = :id
        """
    )
    suspend fun setStatus(id: String, status: String, voidedAt: Long?, reason: String?)

    @Query("UPDATE transactions SET returnId = :returnId WHERE id = :id")
    suspend fun setReturnId(id: String, returnId: Long)

    @Transaction
    suspend fun checkout(tx: TransactionEntity, items: List<TransactionItemEntity>) {
        insertTransaction(tx)
        insertItems(items)
    }
}