package com.pos.offline.util

import android.content.Context
import com.pos.offline.data.local.entity.PrinterEntity
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.PrinterRepository
import com.pos.offline.data.repository.StoreProfileRepository
import com.pos.offline.ui.receipt.EscPosReceiptFormatter
import com.pos.offline.ui.receipt.ReceiptManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class PrintAttemptFailure(
    val printer: PrinterEntity,
    val message: String,
)

sealed class ReceiptPrintOutcome {
    data class Success(
        val printer: PrinterEntity,
    ) : ReceiptPrintOutcome()

    data class SuccessWithNotice(
        val printer: PrinterEntity,
        val notice: String,
    ) : ReceiptPrintOutcome()

    data class Failed(
        val attempts: List<PrintAttemptFailure>,
        val fallbackPdf: File?,
    ) : ReceiptPrintOutcome()

    object NoPrinterConfigured : ReceiptPrintOutcome()

    object AlreadyInProgress : ReceiptPrintOutcome()
}

class PrintCoordinator(
    private val appContext: Context,
    private val printerRepository: PrinterRepository,
    private val storeProfileRepository: StoreProfileRepository,
    private val connectionFactory: PrinterConnectionFactory,
) {
    private val activeJobs = ConcurrentHashMap<String, Mutex>()
    private val statusQueryFailureCounts = ConcurrentHashMap<Long, Int>()

    suspend fun printReceiptAuto(
        result: CheckoutResult,
        openCashDrawer: Boolean = false,
    ): ReceiptPrintOutcome =
        runGuarded(result.transaction.id) {
            val candidates = resolveCascadeOrder()
            if (candidates.isEmpty()) {
                ReceiptPrintOutcome.NoPrinterConfigured
            } else {
                executeSequential(candidates, result, openCashDrawer)
            }
        }

    suspend fun printReceiptToSpecific(
        printer: PrinterEntity,
        result: CheckoutResult,
        openCashDrawer: Boolean = false,
    ): ReceiptPrintOutcome =
        runGuarded(result.transaction.id) {
            executeSequential(listOf(printer), result, openCashDrawer)
        }

    private suspend fun runGuarded(
        transactionId: String,
        block: suspend () -> ReceiptPrintOutcome,
    ): ReceiptPrintOutcome {
        val mutex = activeJobs.computeIfAbsent(transactionId) { Mutex() }
        if (!mutex.tryLock()) return ReceiptPrintOutcome.AlreadyInProgress
        return try {
            block()
        } finally {
            mutex.unlock()
            activeJobs.remove(transactionId)
        }
    }

    private suspend fun resolveCascadeOrder(): List<PrinterEntity> {
        val default = printerRepository.getDefault()
        val ordered = printerRepository.getAllOrderedByPriority()
        val rest = ordered.filter { it.id != default?.id }
        return listOfNotNull(default) + rest
    }

    private suspend fun executeSequential(
        candidates: List<PrinterEntity>,
        result: CheckoutResult,
        openCashDrawer: Boolean,
    ): ReceiptPrintOutcome {
        val storeProfile = storeProfileRepository.get()
        val failures = mutableListOf<PrintAttemptFailure>()

        for (printer in candidates) {
            val printResult = connectionFactory.printReceipt(printer, openCashDrawer) { escPosPrinter ->
                EscPosReceiptFormatter.build(escPosPrinter, result, storeProfile)
            }
            when (printResult) {
                is PrintResult.Success -> {
                    if (printResult.statusQueryFailed) {
                        val fails = statusQueryFailureCounts.merge(printer.id, 1, Int::plus) ?: 1
                        if (fails >= 3) {
                            printerRepository.update(printer.copy(supportsStatusQuery = false, statusQueryFailStreak = 0, autoDisabledDueToNoResponse = true))
                            statusQueryFailureCounts.remove(printer.id)
                            return ReceiptPrintOutcome.SuccessWithNotice(printer, "Deteksi status kertas otomatis dimatikan untuk printer ini (tidak merespons).")
                        }
                    } else {
                        statusQueryFailureCounts.remove(printer.id)
                    }
                    return ReceiptPrintOutcome.Success(printer)
                }
                is PrintResult.Failure -> {
                    var msg = printResult.message
                    if (printResult.statusQueryFailed) {
                        val fails = statusQueryFailureCounts.merge(printer.id, 1, Int::plus) ?: 1
                        if (fails >= 3) {
                            printerRepository.update(printer.copy(supportsStatusQuery = false, statusQueryFailStreak = 0, autoDisabledDueToNoResponse = true))
                            statusQueryFailureCounts.remove(printer.id)
                            msg += " (Deteksi status kertas otomatis dimatikan karena tidak merespons)."
                        }
                    }
                    failures += PrintAttemptFailure(printer, msg)
                }
            }
        }

        val fallbackPdf =
            try {
                withContext(Dispatchers.IO) { ReceiptManager.exportToPdf(appContext, result) }
            } catch (e: Exception) {
                null
            }
        return ReceiptPrintOutcome.Failed(failures, fallbackPdf)
    }
}