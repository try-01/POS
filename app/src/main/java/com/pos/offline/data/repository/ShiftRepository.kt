package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.ShiftDao
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

/**
 * Ringkasan finansial sebuah shift — dihitung ON-DEMAND dari `transactions`/
 * `transaction_items` (bukan disimpan sebagai kolom di [ShiftEntity]), jadi
 * bisa dipanggil kapan pun, termasuk untuk shift yang sudah lama ditutup.
 *
 * Laba Kotor dihitung dari AGREGAT (totalRevenue − totalCost), BUKAN dengan
 * menjumlahkan (unitPrice − unitCost) per baris — supaya diskon level-struk
 * ikut terpotong dengan benar dari sisi pendapatan, mencegah laba overstated.
 */
data class ShiftSummary(
    val startingCash: Long,
    val cashRevenue: Long,
    val qrisRevenue: Long,
    val totalCost: Long
) {
    val totalRevenue: Long get() = cashRevenue + qrisRevenue
    val grossProfit: Long get() = totalRevenue - totalCost

    /** Kas yang SEHARUSNYA ada di laci fisik — QRIS TIDAK dihitung. */
    val expectedCashInDrawer: Long get() = startingCash + cashRevenue
}

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

    /** Dipanggil saat dialog Tutup Shift dibuka — sebelum kasir input kas aktual. */
    suspend fun getShiftSummary(shiftId: Long): ShiftSummary {
        val shift = shiftDao.getById(shiftId) ?: error("Shift #$shiftId tidak ditemukan")
        return ShiftSummary(
            startingCash = shift.startingCash,
            cashRevenue = shiftDao.cashRevenueForShift(shiftId),
            qrisRevenue = shiftDao.qrisRevenueForShift(shiftId),
            totalCost = shiftDao.totalCostForShift(shiftId)
        )
    }

    /**
     * Tutup shift: hitung kas seharusnya (via [getShiftSummary]), bandingkan
     * dgn hasil hitung fisik [actualCash] yang diinput manual kasir.
     */
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