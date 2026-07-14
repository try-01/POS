package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.ShiftDao
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

data class ShiftSummary(
    val startingCash: Long,
    val cashRevenue: Long,
    val qrisRevenue: Long,
    val totalCost: Long,
    /** Total refund TUNAI yang terjadi selama shift ini berlangsung (Batch E). */
    val cashRefunds: Long
) {
    val totalRevenue: Long get() = cashRevenue + qrisRevenue
    val grossProfit: Long get() = totalRevenue - totalCost

    /**
     * Kas yang SEHARUSNYA ada di laci fisik — QRIS TIDAK dihitung (bukan uang
     * fisik), refund TUNAI MENGURANGI (uang keluar dari laci). Penyesuaian
     * laba kotor akibat retur SENGAJA TIDAK dilakukan di sini (keterbatasan
     * yang disepakati) — grossProfit di atas murni dari penjualan asli.
     */
    val expectedCashInDrawer: Long get() = startingCash + cashRevenue - cashRefunds
}

class ShiftRepository(private val shiftDao: ShiftDao) {

    val openShift: Flow<ShiftEntity?> = shiftDao.observeOpenShift()
    val allShifts: Flow<List<ShiftEntity>> = shiftDao.observeAll()
    val openShifts: Flow<List<ShiftEntity>> = shiftDao.observeOpenShifts()

    suspend fun getOpenShift(): ShiftEntity? = shiftDao.getOpenShift()
    fun closedShiftsBetween(start: Long, end: Long): Flow<List<ShiftEntity>> =
        shiftDao.observeClosedShiftsBetween(start, end)
    suspend fun getById(shiftId: Long): ShiftEntity? = shiftDao.getById(shiftId)
    suspend fun hasOpenShift(cashierId: Long): Boolean =
        shiftDao.hasOpenShiftForCashier(cashierId)

    suspend fun startShift(cashierId: Long, cashierName: String, startingCash: Long): Long {
        val shift = ShiftEntity(
            cashierId = cashierId,
            cashierName = cashierName,
            startingCash = startingCash,
            startedAt = System.currentTimeMillis()
        )
        return shiftDao.insert(shift)
    }
    suspend fun getShiftSummary(shiftId: Long): ShiftSummary {
        val shift = shiftDao.getById(shiftId) ?: error("Shift #$shiftId tidak ditemukan")
        return ShiftSummary(
            startingCash = shift.startingCash,
            cashRevenue = shiftDao.cashRevenueForShift(shiftId),
            qrisRevenue = shiftDao.qrisRevenueForShift(shiftId),
            totalCost = shiftDao.totalCostForShift(shiftId),
            cashRefunds = shiftDao.cashRefundsForShift(shiftId)
        )
    }
    suspend fun endShift(shiftId: Long, actualCash: Long, note: String = ""): ShiftEntity {
        val shift = shiftDao.getById(shiftId) ?: error("Shift #$shiftId tidak ditemukan")
        val summary = getShiftSummary(shiftId)
        val updated = shift.copy(
            endingCashExpected = summary.expectedCashInDrawer,
            endingCashActual = actualCash,
            endedAt = System.currentTimeMillis(),
            note = note
        )
        shiftDao.update(updated)
        return updated
    }
}