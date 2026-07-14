package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    @Query("SELECT * FROM shifts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpenShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenShift(): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<ShiftEntity>>
    @Query("SELECT * FROM shifts WHERE endedAt IS NULL ORDER BY startedAt ASC")
    fun observeOpenShifts(): Flow<List<ShiftEntity>>
    @Query(
        """
        SELECT * FROM shifts
        WHERE endedAt >= :start AND endedAt < :end
        ORDER BY endedAt DESC
        """
    )
    fun observeClosedShiftsBetween(start: Long, end: Long): Flow<List<ShiftEntity>>

    @Insert
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): ShiftEntity?
    @Query("SELECT EXISTS(SELECT 1 FROM shifts WHERE cashierId = :cashierId AND endedAt IS NULL)")
    suspend fun hasOpenShiftForCashier(cashierId: Long): Boolean
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'CASH' AND status = 'COMPLETED'
        """
    )
    suspend fun cashRevenueForShift(shiftId: Long): Long
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'QRIS' AND status = 'COMPLETED'
        """
    )
    suspend fun qrisRevenueForShift(shiftId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(ti.unitCost * ti.quantity), 0)
        FROM transaction_items ti
        INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.shiftId = :shiftId AND t.status = 'COMPLETED'
        """
    )
    suspend fun totalCostForShift(shiftId: Long): Long

    /**
     * Total refund TUNAI yang dikaitkan ke shift ini (shiftId di ReturnEntity =
     * shift yang AKTIF SAAT RETUR TERJADI, bukan shift transaksi asal) — dasar
     * pengurang estimasi kas di laci saat Tutup Shift (Batch E).
     * Query lintas tabel `returns` langsung, konsisten dengan pola
     * cashRevenueForShift/totalCostForShift di atas yang juga query tabel lain.
     */
    @Query(
        """
        SELECT COALESCE(SUM(refundAmount), 0) FROM returns
        WHERE shiftId = :shiftId AND refundMethod = 'CASH'
        """
    )
    suspend fun cashRefundsForShift(shiftId: Long): Long
}