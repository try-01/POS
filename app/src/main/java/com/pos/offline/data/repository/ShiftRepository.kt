package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.ShiftDao
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

data class ShiftSummary(
    val startingCash: Long,
    val cashRevenue: Long,
    val qrisRevenue: Long,
    val totalCost: Long,
    val cashRefunds: Long,
) {
    val totalRevenue: Long get() = cashRevenue + qrisRevenue
    val grossProfit: Long get() = totalRevenue - totalCost

    val expectedCashInDrawer: Long get() = startingCash + cashRevenue - cashRefunds
}

sealed class ShiftStartOutcome {
    data class Success(
        val shiftId: Long,
    ) : ShiftStartOutcome()

    /** Kasir yang dipilih sudah punya shift terbuka lain — dicegah di level DB. */
    data object AlreadyOpenForCashier : ShiftStartOutcome()
}

sealed class ShiftEndOutcome {
    data class Success(
        val shift: ShiftEntity,
    ) : ShiftEndOutcome()

    data object AlreadyClosed : ShiftEndOutcome()

    data object NotFound : ShiftEndOutcome()
}

class ShiftRepository(
    private val shiftDao: ShiftDao,
) {
    /**
     * PERINGATAN ARSITEKTUR: Flow ini hanya "shift TERAKHIR yang dibuka",
     * BUKAN "shift yang sedang aktif di terminal/sesi tertentu". Pada mode
     * multi-kasir dengan >1 shift terbuka bersamaan, JANGAN pakai ini untuk
     * atribusi transaksi — gunakan mekanisme active-shift-selection di
     * PosViewModel (activeShift). Dipertahankan untuk kompatibilitas
     * konsumen lain (mis. badge status ringkas) yang tidak butuh presisi ini.
     */
    val openShift: Flow<ShiftEntity?> = shiftDao.observeOpenShift()
    val allShifts: Flow<List<ShiftEntity>> = shiftDao.observeAll()
    val openShifts: Flow<List<ShiftEntity>> = shiftDao.observeOpenShifts()

    suspend fun getOpenShift(): ShiftEntity? = shiftDao.getOpenShift()

    fun closedShiftsBetween(
        start: Long,
        end: Long,
    ): Flow<List<ShiftEntity>> = shiftDao.observeClosedShiftsBetween(start, end)

    suspend fun getById(shiftId: Long): ShiftEntity? = shiftDao.getById(shiftId)

    suspend fun hasOpenShift(cashierId: Long): Boolean = shiftDao.hasOpenShiftForCashier(cashierId)

    suspend fun startShift(
        cashierId: Long,
        cashierName: String,
        startingCash: Long,
    ): ShiftStartOutcome {
        val shift =
            ShiftEntity(
                cashierId = cashierId,
                cashierName = cashierName,
                startingCash = startingCash,
                startedAt = System.currentTimeMillis(),
            )
        val id = shiftDao.insertIfNoOpenShift(shift)
        return if (id == -1L) ShiftStartOutcome.AlreadyOpenForCashier else ShiftStartOutcome.Success(id)
    }

    suspend fun getShiftSummary(shiftId: Long): ShiftSummary {
        val shift = shiftDao.getById(shiftId) ?: error("Shift #$shiftId tidak ditemukan")
        return ShiftSummary(
            startingCash = shift.startingCash,
            cashRevenue = shiftDao.cashRevenueForShift(shiftId),
            qrisRevenue = shiftDao.qrisRevenueForShift(shiftId),
            totalCost = shiftDao.totalCostForShift(shiftId),
            cashRefunds = shiftDao.cashRefundsForShift(shiftId),
        )
    }

    suspend fun endShift(
        shiftId: Long,
        actualCash: Long,
        note: String = "",
    ): ShiftEndOutcome {
        val shift = shiftDao.getById(shiftId) ?: return ShiftEndOutcome.NotFound
        if (shift.endedAt != null) return ShiftEndOutcome.AlreadyClosed

        val summary =
            ShiftSummary(
                startingCash = shift.startingCash,
                cashRevenue = shiftDao.cashRevenueForShift(shiftId),
                qrisRevenue = shiftDao.qrisRevenueForShift(shiftId),
                totalCost = shiftDao.totalCostForShift(shiftId),
                cashRefunds = shiftDao.cashRefundsForShift(shiftId),
            )

        val updated =
            shiftDao.endIfOpen(
                id = shiftId,
                endingCashExpected = summary.expectedCashInDrawer,
                endingCashActual = actualCash,
                endedAt = System.currentTimeMillis(),
                note = note,
            ) ?: return ShiftEndOutcome.AlreadyClosed // ditutup oleh proses lain di antara check & update

        return ShiftEndOutcome.Success(updated)
    }
}
