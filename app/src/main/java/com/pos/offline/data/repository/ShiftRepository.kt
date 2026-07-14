package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.ShiftDao
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

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

    /**
     * BATCH F (Fitur 1): SEMUA shift terbuka (multi-shift-aktif dibolehkan
     * sesuai keputusan arsitektur) — dipakai layar "Kelola Shift" di
     * PosScreen, BEDA dari [openShift] yang hanya shift "ditunjuk aktif"
     * (startedAt terbaru).
     */
    val openShifts: Flow<List<ShiftEntity>> = shiftDao.observeOpenShifts()

    suspend fun getOpenShift(): ShiftEntity? = shiftDao.getOpenShift()

    /**
     * BATCH G (Fitur 2): shift yang sudah ditutup dalam rentang waktu
     * tertentu — dasar "Riwayat Tutup Shift" di ReportScreen.
     */
    fun closedShiftsBetween(start: Long, end: Long): Flow<List<ShiftEntity>> =
        shiftDao.observeClosedShiftsBetween(start, end)

    /**
     * BATCH D: dipakai [TransactionRepository.voidTransaction] untuk
     * memvalidasi apakah shift dari suatu transaksi masih terbuka
     * (`endedAt == null`) sebelum mengizinkan pembatalan — sesuai aturan
     * "Void dibatasi hanya untuk transaksi yang shift-nya masih terbuka".
     */
    suspend fun getById(shiftId: Long): ShiftEntity? = shiftDao.getById(shiftId)

    /**
     * BATCH B: dipakai [SettingsViewModel] untuk memblokir nonaktifkan kasir
     * yang masih punya shift berjalan (belum direkonsiliasi kasnya).
     */
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

    /**
     * Dipanggil saat dialog Tutup Shift dibuka (shift terbuka) — MAUPUN saat
     * membuka detail shift historis yang sudah ditutup (Batch G). Aman
     * dipakai untuk keduanya karena hanya menghitung agregat berdasarkan
     * `shiftId`, tidak peduli status buka/tutup shift itu sendiri.
     */
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