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

    @Insert
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): ShiftEntity?

    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'CASH'
        """
    )
    suspend fun cashRevenueForShift(shiftId: Long): Long

    /**
     * BATCH 3C: total penjualan QRIS selama shift — uangnya masuk rekening
     * bank, BUKAN ke laci fisik, jadi sengaja dipisah dari cashRevenueForShift.
     */
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'QRIS'
        """
    )
    suspend fun qrisRevenueForShift(shiftId: Long): Long

    /**
     * BATCH 3C: total modal (Σ unitCost × qty) seluruh item yang terjual
     * dalam shift ini — dasar kalkulasi Laba Kotor. Join ke `transactions`
     * untuk memfilter berdasarkan `shiftId` (kolom itu ada di header, bukan
     * di `transaction_items`).
     */
    @Query(
        """
        SELECT COALESCE(SUM(ti.unitCost * ti.quantity), 0)
        FROM transaction_items ti
        INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.shiftId = :shiftId
        """
    )
    suspend fun totalCostForShift(shiftId: Long): Long
}