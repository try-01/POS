package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.ShiftDao
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

class ShiftRepository(private val shiftDao: ShiftDao) {

    val openShift: Flow<ShiftEntity?> = shiftDao.observeOpenShift()
    val allShifts: Flow<List<ShiftEntity>> = shiftDao.observeAll()

    suspend fun getOpenShift(): ShiftEntity? = shiftDao.getOpenShift()

    suspend fun startShift(cashierId: Long, cashierName: String, startingCash: Long): Long {
        val shift = ShiftEntity(
            cashierId = cashierId,
            cashierName = cashierName,
            startingCash = startingCash,
            startedAt = System.currentTimeMillis()
        )
        return shiftDao.insert(shift)
    }

    /**
     * Tutup shift: hitung kas seharusnya (modal awal + total penjualan TUNAI
     * selama shift), lalu bandingkan dgn hasil hitung fisik [actualCash] yang
     * diinput manual kasir. Selisih (+/-) tersimpan otomatis lewat
     * [ShiftEntity.cashDifference].
     */
    suspend fun endShift(shiftId: Long, actualCash: Long, note: String = ""): ShiftEntity {
        val shift = shiftDao.getById(shiftId) ?: error("Shift #$shiftId tidak ditemukan")
        val cashRevenue = shiftDao.cashRevenueForShift(shiftId)
        val expected = shift.startingCash + cashRevenue
        val updated = shift.copy(
            endingCashExpected = expected,
            endingCashActual = actualCash,
            endedAt = System.currentTimeMillis(),
            note = note
        )
        shiftDao.update(updated)
        return updated
    }
}