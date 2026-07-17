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

/** Satu percobaan cetak yang gagal ke printer tertentu, dipakai untuk pesan error rinci di UI (H7). */
data class PrintAttemptFailure(val printer: PrinterEntity, val message: String)

/**
 * Hasil akhir dari satu job cetak struk (auto maupun manual/reprint).
 * Sengaja terpisah dari [TestPrintResult] & [PrintResult] milik [PrinterConnectionFactory] —
 * ini level lebih tinggi yang sudah mencakup kemungkinan cascade & fallback PDF.
 */
sealed class ReceiptPrintOutcome {
    /** Berhasil dicetak ke [printer] (bisa jadi bukan printer pertama yang dicoba, kalau cascade). */
    data class Success(val printer: PrinterEntity) : ReceiptPrintOutcome()

    /**
     * Semua printer yang dicoba gagal. [attempts] berisi rincian tiap kegagalan (untuk pesan error
     * spesifik di UI). [fallbackPdf] berisi file PDF hasil [ReceiptManager.exportToPdf] kalau berhasil
     * dibuat, null kalau bahkan pembuatan PDF pun gagal (mis. storage penuh).
     */
    data class Failed(
        val attempts: List<PrintAttemptFailure>,
        val fallbackPdf: File?
    ) : ReceiptPrintOutcome()

    /** Belum ada printer yang dikonfigurasi sama sekali — tidak ada yang dicoba, TIDAK ada PDF otomatis. */
    object NoPrinterConfigured : ReceiptPrintOutcome()

    /** Job untuk transaksi ini sedang berjalan (guard anti-spam-tap) — permintaan baru ditolak. */
    object AlreadyInProgress : ReceiptPrintOutcome()
}

/**
 * Koordinator pencetakan struk fisik (H6). Murni domain/data layer — TIDAK punya akses Activity
 * Context untuk memicu Intent share; kalau semua printer gagal, cukup mengembalikan File PDF,
 * urusan menampilkan tombol "Bagikan" (Intent.createChooser) adalah tanggung jawab UI layer (H7).
 *
 * Guard anti-dobel-job di-key oleh `transaction.id` (String, format "INV-..."), BUKAN per-printer
 * seperti guard Test Print (H3d) — supaya transaksi yang sama tidak dikirim dua kali secara
 * konkuren (mis. user tap cepat berkali-kali), tapi transaksi BERBEDA tetap bisa diproses paralel.
 */
class PrintCoordinator(
    private val appContext: Context,
    private val printerRepository: PrinterRepository,
    private val storeProfileRepository: StoreProfileRepository,
    private val connectionFactory: PrinterConnectionFactory
) {
    private val activeJobs = ConcurrentHashMap<String, Mutex>()

    /**
     * Auto-print pasca-checkout (H7): coba printer default dulu, kalau gagal cascade ke sisa
     * printer diurutkan `priority` ascending (default dikecualikan dari daftar susulan itu).
     *
     * CATATAN: fungsi ini TIDAK mengecek `StoreProfileEntity.autoPrintEnabled` — pemanggil (H7)
     * yang bertanggung jawab memutuskan kapan fungsi ini dipanggil berdasarkan toggle tersebut.
     */
    suspend fun printReceiptAuto(result: CheckoutResult): ReceiptPrintOutcome =
        runGuarded(result.transaction.id) {
            val candidates = resolveCascadeOrder()
            if (candidates.isEmpty()) {
                ReceiptPrintOutcome.NoPrinterConfigured
            } else {
                executeSequential(candidates, result)
            }
        }

    /**
     * Reprint manual (H7) ke printer yang SENGAJA dipilih user (mis. dari `ReportScreen`).
     * TIDAK ADA cascade — kalau printer pilihan user gagal, langsung kembalikan kegagalan +
     * PDF fallback, jangan diam-diam mencetak ke printer lain tanpa sepengetahuan user.
     */
    suspend fun printReceiptToSpecific(
        printer: PrinterEntity,
        result: CheckoutResult
    ): ReceiptPrintOutcome =
        runGuarded(result.transaction.id) {
            executeSequential(listOf(printer), result)
        }

    private suspend fun runGuarded(
        transactionId: String,
        block: suspend () -> ReceiptPrintOutcome
    ): ReceiptPrintOutcome {
        val mutex = activeJobs.computeIfAbsent(transactionId) { Mutex() }
        if (!mutex.tryLock()) return ReceiptPrintOutcome.AlreadyInProgress
        return try {
            block()
        } finally {
            mutex.unlock()
            // Best-effort cleanup — kalau ada race minor saat penghapusan (job baru datang
            // tepat di celah ini), dampaknya cuma sesekali membuat Mutex baru untuk id yang
            // sama, tidak merusak korektnas guard (mutex lama toh sudah unlocked/selesai).
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
        result: CheckoutResult
    ): ReceiptPrintOutcome {
        val storeProfile = storeProfileRepository.get()
        val failures = mutableListOf<PrintAttemptFailure>()

        for (printer in candidates) {
            val printResult = connectionFactory.printReceipt(printer) { escPosPrinter ->
                EscPosReceiptFormatter.build(escPosPrinter, result, storeProfile)
            }
            when (printResult) {
                is PrintResult.Success -> return ReceiptPrintOutcome.Success(printer)
                is PrintResult.Failure -> failures += PrintAttemptFailure(printer, printResult.message)
            }
        }

        val fallbackPdf = try {
            withContext(Dispatchers.IO) { ReceiptManager.exportToPdf(appContext, result) }
        } catch (e: Exception) {
            null
        }
        return ReceiptPrintOutcome.Failed(failures, fallbackPdf)
    }
}