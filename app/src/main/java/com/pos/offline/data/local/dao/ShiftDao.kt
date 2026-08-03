package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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
        """,
    )
    fun observeClosedShiftsBetween(
        start: Long,
        end: Long,
    ): Flow<List<ShiftEntity>>

    @Insert
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): ShiftEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM shifts WHERE cashierId = :cashierId AND endedAt IS NULL)")
    suspend fun hasOpenShiftForCashier(cashierId: Long): Boolean

    @Transaction
    suspend fun insertIfNoOpenShift(shift: ShiftEntity): Long {
        if (hasOpenShiftForCashier(shift.cashierId)) return -1L
        return insert(shift)
    }

    @Transaction
    suspend fun endIfOpen(
        id: Long,
        endingCashExpected: Long,
        endingCashActual: Long,
        endedAt: Long,
        note: String,
    ): ShiftEntity? {
        val current = getById(id) ?: return null
        if (current.endedAt != null) return null
        val updated =
            current.copy(
                endingCashExpected = endingCashExpected,
                endingCashActual = endingCashActual,
                endedAt = endedAt,
                note = note,
            )
        update(updated)
        return updated
    }

    @Query(
        """
        SELECT COALESCE(SUM(paidAmount - changeGiven), 0) FROM transactions
        WHERE shiftId = :shiftId 
          AND paymentMethod = 'CASH' 
          AND status = 'COMPLETED'
          AND isWarrantyExchange = 0 
        """
    )
    suspend fun cashRevenueForShift(shiftId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'QRIS' AND status = 'COMPLETED'
        """,
    )
    suspend fun qrisRevenueForShift(shiftId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(changeGiven), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'QRIS' AND status = 'COMPLETED'
          AND changeGivenInCash = 1
        """,
    )
    suspend fun qrisCashChangeOutForShift(shiftId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(CAST(ROUND(ti.unitCost * ti.quantity) AS INTEGER)), 0)
        FROM transaction_items ti
        INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.shiftId = :shiftId AND t.status = 'COMPLETED' AND t.isWarrantyExchange = 0
        """,
    )
    suspend fun totalCostForShift(shiftId: Long): Long

    // Biaya modal barang pengganti pada transaksi tukar-guling garansi dalam shift ini,
    // dipisah dari totalCostForShift agar tampil sebagai baris "Biaya Klaim Garansi" tersendiri.
    @Query(
        """
        SELECT COALESCE(SUM(CAST(ROUND(ti.unitCost * ti.quantity) AS INTEGER)), 0)
        FROM transaction_items ti
        INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.shiftId = :shiftId AND t.status = 'COMPLETED' AND t.isWarrantyExchange = 1
        """,
    )
    suspend fun warrantyExchangeCostForShift(shiftId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(refundAmount), 0) FROM returns
        WHERE shiftId = :shiftId 
          AND refundMethod = 'CASH'
          AND transactionId NOT LIKE 'EXC-%'
        """
    )
    suspend fun cashRefundsForShift(shiftId: Long): Long

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
        WHERE r.shiftId = :shiftId
          AND (ri.restocked = 1 OR ri.transactionItemId = 0)
        """,
    )
    suspend fun restockedReturnsCostForShift(shiftId: Long): Long
}
