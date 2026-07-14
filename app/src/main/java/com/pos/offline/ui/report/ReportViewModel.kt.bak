package com.pos.offline.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.isVoid
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.TransactionRepository
import com.pos.offline.data.repository.VoidOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Ringkasan laporan untuk satu hari tertentu. Semua angka uang bertipe [Long]
 * (presisi penuh, bebas floating-point).
 *
 * BATCH D — pemisahan penting:
 *  - [transactions]: SEMUA baris (termasuk VOID) — sumber untuk daftar
 *    riwayat di UI (badge "Dibatalkan" ditampilkan di sana untuk yang VOID).
 *  - [totalRevenue]/[transactionCount]/dst: HANYA dari transaksi COMPLETED —
 *    sumber untuk kartu statistik & grafik tren (transaksi dibatalkan tidak
 *    boleh mendistorsi angka pendapatan/laba).
 *  - [voidedCount]: jumlah transaksi VOID pada hari ini (info tambahan).
 */
data class DailyReport(
    val date: LocalDate,
    val transactions: List<TransactionEntity>, // SEMUA baris, termasuk VOID
    val totalRevenue: Long,        // Σ total transaksi COMPLETED
    val transactionCount: Int,     // jumlah transaksi COMPLETED
    val averagePerTransaction: Long,
    val totalDiscount: Long,       // Σ diskon transaksi COMPLETED
    val totalTax: Long,            // Σ pajak transaksi COMPLETED
    val hourlyRevenue: List<Long>, // panjang 24; hanya dari transaksi COMPLETED
    val voidedCount: Int            // jumlah transaksi VOID hari ini
) {
    companion object {
        fun empty(date: LocalDate) = DailyReport(
            date = date,
            transactions = emptyList(),
            totalRevenue = 0L,
            transactionCount = 0,
            averagePerTransaction = 0L,
            totalDiscount = 0L,
            totalTax = 0L,
            hourlyRevenue = List(24) { 0L },
            voidedCount = 0
        )
    }
}

/**
 * ViewModel Laporan Harian.
 *
 * Prinsip performa:
 *  - Satu-satunya query DB adalah `dailyTransactions(start, end)`; sisanya
 *    (pendapatan, jumlah, rata-rata, distribusi per jam) diturunkan di-memori
 *    lewat [map] → hemat round-trip DB.
 *  - `flatMapLatest` membatalkan query tanggal lama saat pengguna pindah hari.
 *  - `WhileSubscribed(5000)` → Flow berhenti saat layar tak terlihat (hemat baterai).
 *
 * BATCH C: [selectedTransaction] menampung detail transaksi (header + item)
 * yang sedang dibuka pengguna dari daftar riwayat — dimuat sekali-jalan via
 * [TransactionRepository.loadReceipt] (BUKAN reaktif/Flow) karena ini murni
 * tampilan read-only snapshot, tidak perlu ikut berubah live.
 *
 * BATCH D: tambah [voidSelectedTransaction] + [messages] (SharedFlow untuk
 * Snackbar) — hasil Void (sukses/gagal validasi) dilaporkan sebagai pesan
 * satu-kali, bukan state permanen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    // Tanggal terpilih (default hari ini).
    private val _selectedDate = MutableStateFlow(LocalDate.now(zone))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** Apakah tanggal terpilih = hari ini (untuk menonaktifkan tombol "besok"). */
    val isToday: StateFlow<Boolean> = _selectedDate
        .map { it.isEqual(LocalDate.now(zone)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** Laporan reaktif untuk tanggal terpilih. */
    val report: StateFlow<DailyReport> = _selectedDate
        .flatMapLatest { date ->
            val (start, end) = dayBounds(date)
            transactionRepository.dailyTransactions(start, end).map { aggregate(date, it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyReport.empty(LocalDate.now()))

    // ---------- BATCH C: Detail Transaksi (read-only) ----------

    private val _selectedTransaction = MutableStateFlow<CheckoutResult?>(null)
    /** Transaksi yang sedang dilihat detailnya (null = dialog tertutup). */
    val selectedTransaction: StateFlow<CheckoutResult?> = _selectedTransaction.asStateFlow()

    // ---------- BATCH D: pesan satu-kali (Snackbar) untuk hasil aksi Void ----------

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /**
     * Buka dialog detail untuk satu transaksi. Memuat ULANG dari DB via
     * [TransactionRepository.loadReceipt] (bukan mengambil dari [report]
     * yang sudah ada di memori) — supaya header & item selalu konsisten
     * satu paket, dan siap dipakai ulang setelah Void (status ter-refresh).
     */
    fun openTransactionDetail(invoiceId: String) {
        viewModelScope.launch {
            _selectedTransaction.value = transactionRepository.loadReceipt(invoiceId)
        }
    }

    fun closeTransactionDetail() {
        _selectedTransaction.value = null
    }

    /**
     * BATCH D: batalkan transaksi yang sedang dibuka di dialog detail.
     * Setelah selesai (berhasil/gagal), [selectedTransaction] di-refresh
     * (supaya dialog langsung menampilkan status VOID terbaru jika sukses)
     * dan hasilnya dilaporkan lewat [messages] untuk ditampilkan Snackbar.
     *
     * [report] TIDAK perlu di-refresh manual — Room Flow otomatis emit ulang
     * begitu tabel `transactions` berubah (invalidation tracking bawaan).
     */
    fun voidSelectedTransaction() {
        val invoiceId = _selectedTransaction.value?.transaction?.id ?: return
        viewModelScope.launch {
            when (val outcome = transactionRepository.voidTransaction(invoiceId)) {
                is VoidOutcome.Success -> {
                    _selectedTransaction.value = transactionRepository.loadReceipt(invoiceId)
                    _messages.emit(
                        if (outcome.skippedStockCount > 0)
                            "Transaksi dibatalkan. ${outcome.restoredStockCount} item stok dikembalikan, " +
                                "${outcome.skippedStockCount} item dilewati (data transaksi lama)."
                        else
                            "Transaksi dibatalkan. Stok ${outcome.restoredStockCount} item dikembalikan."
                    )
                }
                VoidOutcome.AlreadyVoided ->
                    _messages.emit("Transaksi ini sudah dibatalkan sebelumnya.")
                VoidOutcome.ShiftClosed ->
                    _messages.emit("Tidak dapat membatalkan — shift transaksi ini sudah ditutup.")
                VoidOutcome.NotFound ->
                    _messages.emit("Transaksi tidak ditemukan.")
            }
        }
    }

    // ---------- Kalkulasi murni (terpisah → mudah diuji) ----------

    /**
     * Batas hari dalam epoch millis. `end` = awal hari berikutnya (eksklusif),
     * cocok dengan query `createdAt >= start AND createdAt < end`.
     */
    private fun dayBounds(date: LocalDate): Pair<Long, Long> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    /**
     * Agregasi harian + distribusi pendapatan per jam.
     *
     * BATCH D: [txs] berisi SEMUA transaksi (termasuk VOID) untuk daftar
     * riwayat UI, tapi seluruh statistik (pendapatan, diskon, pajak, rata-rata,
     * distribusi per jam) HANYA dihitung dari transaksi COMPLETED — transaksi
     * VOID dikecualikan total dari angka-angka ini.
     */
    private fun aggregate(date: LocalDate, txs: List<TransactionEntity>): DailyReport {
        if (txs.isEmpty()) return DailyReport.empty(date)

        val completed = txs.filterNot { it.isVoid }
        val voidedCount = txs.size - completed.size

        val totalRevenue = completed.sumOf { it.total }
        val totalDiscount = completed.sumOf { it.discount }
        val totalTax = completed.sumOf { it.tax }
        val count = completed.size

        // Distribusi per jam: bucket-kan setiap transaksi COMPLETED ke jam kejadian.
        val hourly = MutableList(24) { 0L }
        for (tx in completed) {
            val hour = Instant.ofEpochMilli(tx.createdAt).atZone(zone).hour
            hourly[hour] += tx.total
        }

        val average = if (count > 0) totalRevenue / count else 0L

        return DailyReport(
            date = date,
            transactions = txs,
            totalRevenue = totalRevenue,
            transactionCount = count,
            averagePerTransaction = average,
            totalDiscount = totalDiscount,
            totalTax = totalTax,
            hourlyRevenue = hourly,
            voidedCount = voidedCount
        )
    }

    // ---------- Aksi UI ----------

    fun previousDay() { _selectedDate.value = _selectedDate.value.minusDays(1) }

    /** Maju satu hari, tapi tidak melewati hari ini. */
    fun nextDay() {
        val today = LocalDate.now(zone)
        val current = _selectedDate.value
        if (current.isBefore(today)) _selectedDate.value = current.plusDays(1)
    }

    fun goToday() { _selectedDate.value = LocalDate.now(zone) }

    companion object {
        /** Format tanggal panjang Indonesia, mis. "Senin, 5 Mei 2025". */
        val dateFmt: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.forLanguageTag("id-ID"))

        /** Format jam singkat, mis. "14.05". */
        val timeFmt: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

        /**
         * BATCH C: format tanggal+jam LENGKAP (dengan detik) untuk Detail
         * Transaksi — mis. "Senin, 5 Mei 2025 · 14:05:32". Detik disengaja
         * disertakan (berbeda dari [timeFmt] yang dipakai daftar ringkas)
         * karena konteks audit/detail butuh presisi lebih tinggi.
         */
        val dateTimeFmt: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · HH:mm:ss", Locale.forLanguageTag("id-ID"))
                .withZone(ZoneId.systemDefault())
    }
}