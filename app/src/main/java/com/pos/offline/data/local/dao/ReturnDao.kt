package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pos.offline.data.local.entity.ReturnEntity
import com.pos.offline.data.local.entity.ReturnItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnDao {

    @Insert
    suspend fun insertReturn(ret: ReturnEntity): Long

    @Insert
    suspend fun insertItems(items: List<ReturnItemEntity>)

    @Query("SELECT * FROM returns WHERE id = :id")
    suspend fun getById(id: Long): ReturnEntity?

    /** Dasar cek "transaksi ini sudah pernah diretur?" — juga sumber ReturnDetail dari sisi transaksi. */
    @Query("SELECT * FROM returns WHERE transactionId = :transactionId")
    suspend fun getByTransactionId(transactionId: String): ReturnEntity?

    @Query("SELECT * FROM return_items WHERE returnId = :returnId")
    suspend fun getItems(returnId: Long): List<ReturnItemEntity>

    /**
     * Retur yang TERJADI (returnedAt) dalam rentang [start, end) — dasar
     * section "Retur Hari Ini" di tab Shift ReportScreen (Batch E4).
     * Sengaja filter returnedAt, BUKAN tanggal transaksi asal — sesuai
     * keputusan retur boleh lintas hari/shift.
     */
    @Query(
        """
        SELECT * FROM returns
        WHERE returnedAt >= :start AND returnedAt < :end
        ORDER BY returnedAt DESC
        """
    )
    fun observeReturnsBetween(start: Long, end: Long): Flow<List<ReturnEntity>>
}