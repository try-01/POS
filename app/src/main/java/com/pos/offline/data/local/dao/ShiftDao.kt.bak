package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    /** Shift yang sedang berjalan (endedAt IS NULL). Normalnya hanya 0 atau 1 baris. */
    @Query("SELECT * FROM shifts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpenShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenShift(): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Insert
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): ShiftEntity?

    /**
     * Total penjualan TUNAI (bukan QRIS) selama rentang shift tsb — dasar
     * kalkulasi [ShiftEntity.endingCashExpected]. Query lintas-tabel (baca
     * `transactions` dari dalam ShiftDao) adalah hal wajar di Room; DAO hanya
     * pengelompokan logis, bukan batasan akses antar tabel.
     */
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'CASH'
        """
    )
    suspend fun cashRevenueForShift(shiftId: Long): Long
}