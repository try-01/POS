package com.pos.offline.data.repository
import com.pos.offline.data.local.dao.ShiftDao
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow
data class ShiftSummary(
    val startingCash: Long,
    val cashRevenue: Long,
    val qrisRevenue: Long,
    val totalCost: Long,
    val restockedReturnsCost: Long,
    val cashRefunds: Long,
    val qrisCashChangeOut: Long = 0L,
    val warrantyExchangeCost: Long = 0L,
    val qrisRefunds: Long = 0L,
) {
    val totalRevenue: Long get() = cashRevenue + qrisRevenue
    val netRevenue: Long get() = totalRevenue - cashRefunds - qrisRefunds
    val netCost: Long get() = (totalCost - restockedReturnsCost + warrantyExchangeCost).coerceAtLeast(0L)
    val grossProfit: Long get() = netRevenue - netCost
    val expectedCashInDrawer: Long get() = startingCash + cashRevenue - cashRefunds - qrisCashChangeOut
}
sealed class ShiftStartOutcome {
    data class Success(
        val shiftId: Long,
    ) : ShiftStartOutcome()
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
            restockedReturnsCost = shiftDao.restockedReturnsCostForShift(shiftId),
            cashRefunds = shiftDao.cashRefundsForShift(shiftId),
            qrisCashChangeOut = shiftDao.qrisCashChangeOutForShift(shiftId),
            warrantyExchangeCost = shiftDao.warrantyExchangeCostForShift(shiftId),
            qrisRefunds = shiftDao.qrisRefundsForShift(shiftId),
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
                restockedReturnsCost = shiftDao.restockedReturnsCostForShift(shiftId),
                cashRefunds = shiftDao.cashRefundsForShift(shiftId),
                qrisCashChangeOut = shiftDao.qrisCashChangeOutForShift(shiftId),
                warrantyExchangeCost = shiftDao.warrantyExchangeCostForShift(shiftId),
                qrisRefunds = shiftDao.qrisRefundsForShift(shiftId),
            )
        val updated =
            shiftDao.endIfOpen(
                id = shiftId,
                endingCashExpected = summary.expectedCashInDrawer,
                endingCashActual = actualCash,
                endedAt = System.currentTimeMillis(),
                note = note,
            ) ?: return ShiftEndOutcome.AlreadyClosed
        return ShiftEndOutcome.Success(updated)
    }
}
