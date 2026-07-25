package com.pos.offline.util

import android.content.Context
import com.pos.offline.data.local.entity.PrinterEntity
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.PrinterRepository
import com.pos.offline.data.repository.StoreProfileRepository
import com.pos.offline.ui.receipt.EscPosReceiptFormatter
import com.pos.offline.ui.receipt.ReceiptLine
import com.pos.offline.ui.receipt.ReceiptManager
import kotlinx.coroutines.Dispatchers
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
    // Dedupe cepat berbasis transactionId/printerId (bukan mutual exclusion
    // hardware — itu tanggung jawab PrinterConnectionFactory.printerMutexes).
    // Tujuannya: feedback instan "sedang diproses" tanpa membuat coroutine
    // menunggu, mis. saat user double-tap tombol Bayar atau Cetak Laporan.
    private val activeJobs = ConcurrentHashMap.newKeySet<String>()

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

    suspend fun printCustomLines(printer: PrinterEntity, lines: List<ReceiptLine>): ReceiptPrintOutcome {
        return runGuarded("PRINTER_${printer.id}") {
            val printResult = connectionFactory.printRawLines(printer, lines)
            when (printResult) {
                is PrintResult.Success -> ReceiptPrintOutcome.Success(printer)
                is PrintResult.Failure -> ReceiptPrintOutcome.Failed(listOf(PrintAttemptFailure(printer, printResult.message)), null)
            }
        }
    }

    private suspend fun runGuarded(
        key: String,
        block: suspend () -> ReceiptPrintOutcome,
    ): ReceiptPrintOutcome {
        if (!activeJobs.add(key)) return ReceiptPrintOutcome.AlreadyInProgress
        return try {
            block()
        } finally {
            activeJobs.remove(key)
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
                    if (printer.supportsStatusQuery) {
                        if (printResult.statusQueryFailed) {
                            val streak = printerRepository.incrementStatusQueryFailStreak(printer.id)
                            if (streak >= 3) {
                                printerRepository.disableStatusQuery(printer.id)
                                return ReceiptPrintOutcome.SuccessWithNotice(
                                    printer,
                                    "Deteksi status kertas otomatis dimatikan untuk printer ini (tidak merespons).",
                                )
                            }
                        } else {
                            printerRepository.resetStatusQueryFailStreak(printer.id)
                        }
                    }

                    if (printResult.nearEndWarning) {
                        return ReceiptPrintOutcome.SuccessWithNotice(printer, "Kertas printer hampir habis, siapkan gulungan baru.")
                    }

                    return ReceiptPrintOutcome.Success(printer)
                }
                is PrintResult.Failure -> {
                    var msg = printResult.message
                    if (printer.supportsStatusQuery && printResult.statusQueryFailed) {
                        val streak = printerRepository.incrementStatusQueryFailStreak(printer.id)
                        if (streak >= 3) {
                            printerRepository.disableStatusQuery(printer.id)
                            msg += " (Deteksi status kertas otomatis dimatikan karena tidak merespons)."
                        }
                    }
                    failures += PrintAttemptFailure(printer, msg)
                }
            }
        }

        val fallbackPdf =
            try {
                withContext(Dispatchers.IO) { ReceiptManager.exportToPdf(appContext, result, storeProfile) }
            } catch (e: Exception) {
                null
            }
        return ReceiptPrintOutcome.Failed(failures, fallbackPdf)
    }
}
