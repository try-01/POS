package com.pos.offline.ui.report
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.PrinterEntity
import com.pos.offline.data.local.entity.ReturnEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.isVoid
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.PrinterRepository
import com.pos.offline.data.repository.ReportRepository
import com.pos.offline.data.repository.ReturnDetail
import com.pos.offline.data.repository.ReturnItemInput
import com.pos.offline.data.repository.ReturnOutcome
import com.pos.offline.data.repository.ReturnRepository
import com.pos.offline.data.repository.SalesReportData
import com.pos.offline.data.repository.ShiftRepository
import com.pos.offline.data.repository.ShiftSummary
import com.pos.offline.data.repository.StoreProfileRepository
import com.pos.offline.data.repository.TransactionRepository
import com.pos.offline.data.repository.VoidOutcome
import com.pos.offline.ui.receipt.PrintUiState
import com.pos.offline.ui.receipt.ReceiptLine
import com.pos.offline.ui.receipt.ReceiptManager
import com.pos.offline.util.PrintCoordinator
import com.pos.offline.util.toRupiah
import com.pos.offline.util.formatQuantity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
data class DailyReport(
    val date: LocalDate,
    val transactions: List<TransactionEntity>,
    val totalRevenue: Long,
    val transactionCount: Int,
    val averagePerTransaction: Long,
    val totalDiscount: Long,
    val totalTax: Long,
    val hourlyRevenue: List<Long>,
    val voidedCount: Int,
    val cashRevenue: Long,
    val qrisRevenue: Long,
) {
    companion object {
        fun empty(date: LocalDate) =
            DailyReport(
                date = date,
                transactions = emptyList(),
                totalRevenue = 0L,
                transactionCount = 0,
                averagePerTransaction = 0L,
                totalDiscount = 0L,
                totalTax = 0L,
                hourlyRevenue = List(24) { 0L },
                voidedCount = 0,
                cashRevenue = 0L,
                qrisRevenue = 0L,
            )
    }
}
data class ReportMessage(
    val text: String,
    val isError: Boolean = false,
)
enum class ReportTab { TRANSACTIONS, SHIFTS }
enum class ReportPeriodType { DAILY, MONTHLY }
data class ClosedShiftDetail(
    val shift: ShiftEntity,
    val summary: ShiftSummary,
)
data class ReturnSummary(
    val returns: List<ReturnEntity>,
    val cashRefundTotal: Long,
    val qrisRefundTotal: Long,
) {
    companion object {
        fun empty() = ReturnSummary(emptyList(), 0L, 0L)
    }
}
data class PendingPrintTarget(
    val checkoutResult: CheckoutResult,
    val availablePrinters: List<PrinterEntity>,
)
data class DaySalesGroup(
    val date: LocalDate,
    val transactions: List<TransactionEntity>
)

data class MonthSalesGroup(
    val yearMonth: java.time.YearMonth,
    val days: List<DaySalesGroup>,
    val totalTransactions: Int
)
sealed class SalesReportUiState {
    object Hidden : SalesReportUiState()
    data class Loading(
        val periodType: ReportPeriodType,
    ) : SalesReportUiState()
    data class Loaded(
        val periodType: ReportPeriodType,
        val data: SalesReportData,
    ) : SalesReportUiState()
}
private data class ReportSelection(
    val periodType: ReportPeriodType,
    val includeSalesSummary: Boolean,
    val includeProductsSold: Boolean,
    val includeDeadStock: Boolean,
)
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class ReportViewModel(
    private val transactionRepository: TransactionRepository,
    private val shiftRepository: ShiftRepository,
    private val returnRepository: ReturnRepository,
    private val printCoordinator: PrintCoordinator,
    private val printerRepository: PrinterRepository,
    private val reportRepository: ReportRepository,
    private val storeProfileRepository: StoreProfileRepository,
) : ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val _selectedDate = MutableStateFlow(LocalDate.now(zone))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    val isToday: StateFlow<Boolean> =
        _selectedDate
            .map { it.isEqual(LocalDate.now(zone)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val report: StateFlow<DailyReport> =
        _selectedDate
            .flatMapLatest { date ->
                val (start, end) = dayBounds(date)
                transactionRepository.dailyTransactions(start, end).map { aggregate(date, it) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyReport.empty(LocalDate.now()))
    private val _selectedTab = MutableStateFlow(ReportTab.TRANSACTIONS)
    val selectedTab: StateFlow<ReportTab> = _selectedTab.asStateFlow()
    fun selectTab(tab: ReportTab) {
        _selectedTab.value = tab
    }
    val closedShifts: StateFlow<List<ShiftEntity>> =
        _selectedDate
            .flatMapLatest { date ->
                val (start, end) = dayBounds(date)
                shiftRepository.closedShiftsBetween(start, end)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _selectedShiftDetail = MutableStateFlow<ClosedShiftDetail?>(null)
    val selectedShiftDetail: StateFlow<ClosedShiftDetail?> = _selectedShiftDetail.asStateFlow()
    fun openShiftDetail(shift: ShiftEntity) {
        viewModelScope.launch {
            _selectedShiftDetail.value =
                ClosedShiftDetail(
                    shift = shift,
                    summary = shiftRepository.getShiftSummary(shift.id),
                )
        }
    }
    fun closeShiftDetail() {
        _selectedShiftDetail.value = null
    }
    val returnSummary: StateFlow<ReturnSummary> =
        _selectedDate
            .flatMapLatest { date ->
                val (start, end) = dayBounds(date)
                returnRepository.returnsBetween(start, end).map { aggregateReturns(it) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReturnSummary.empty())
    private val _selectedReturnDetail = MutableStateFlow<ReturnDetail?>(null)
    val selectedReturnDetail: StateFlow<ReturnDetail?> = _selectedReturnDetail.asStateFlow()
    fun openReturnDetail(returnId: Long) {
        viewModelScope.launch {
            _selectedReturnDetail.value = returnRepository.getDetail(returnId)
        }
    }
    fun closeReturnDetail() {
        _selectedReturnDetail.value = null
    }
    private val _selectedTransaction = MutableStateFlow<CheckoutResult?>(null)
    val selectedTransaction: StateFlow<CheckoutResult?> = _selectedTransaction.asStateFlow()
    private val _messages = MutableSharedFlow<ReportMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<ReportMessage> = _messages.asSharedFlow()
    private val _printUiState = MutableStateFlow<PrintUiState>(PrintUiState.Idle)
    val printUiState: StateFlow<PrintUiState> = _printUiState.asStateFlow()
    private val _pendingPrintTarget = MutableStateFlow<PendingPrintTarget?>(null)
    val pendingPrintTarget: StateFlow<PendingPrintTarget?> = _pendingPrintTarget.asStateFlow()
    fun openTransactionDetail(invoiceId: String) {
        viewModelScope.launch {
            _selectedTransaction.value = transactionRepository.loadReceipt(invoiceId)
        }
    }
    fun closeTransactionDetail() {
        _selectedTransaction.value = null
        _showReturnDialog.value = false
        _returnMessage.value = null
        _printUiState.value = PrintUiState.Idle
        _pendingPrintTarget.value = null
    }
    fun voidSelectedTransaction() {
        val invoiceId = _selectedTransaction.value?.transaction?.id ?: return
        viewModelScope.launch {
            when (val outcome = transactionRepository.voidTransaction(invoiceId)) {
                is VoidOutcome.Success -> {
                    _selectedTransaction.value = transactionRepository.loadReceipt(invoiceId)
                    _messages.emit(
                        ReportMessage(
                            text =
                                if (outcome.skippedStockCount > 0) {
                                    "Transaksi dibatalkan. ${outcome.restoredStockCount} item stok dikembalikan, " +
                                        "${outcome.skippedStockCount} item dilewati (data transaksi lama)."
                                } else {
                                    "Transaksi dibatalkan. Stok ${outcome.restoredStockCount} item dikembalikan."
                                },
                            isError = false,
                        ),
                    )
                }
                VoidOutcome.AlreadyVoided -> {
                    _messages.emit(ReportMessage("Transaksi ini sudah dibatalkan sebelumnya.", isError = true))
                }
                VoidOutcome.ShiftClosed -> {
                    _messages.emit(ReportMessage("Tidak dapat membatalkan — shift transaksi ini sudah ditutup.", isError = true))
                }
                VoidOutcome.NotFound -> {
                    _messages.emit(ReportMessage("Transaksi tidak ditemukan.", isError = true))
                }
                VoidOutcome.HasReturn -> {
                    _messages.emit(ReportMessage("Tidak dapat membatalkan — transaksi ini sudah memiliki riwayat retur.", isError = true))
                }
            }
        }
    }
    private val _showReturnDialog = MutableStateFlow(false)
    val showReturnDialog: StateFlow<Boolean> = _showReturnDialog.asStateFlow()
    private val _returnMessage = MutableStateFlow<ReportMessage?>(null)
    val returnMessage: StateFlow<ReportMessage?> = _returnMessage.asStateFlow()
    private val _returnSubmitting = MutableStateFlow(false)
    val returnSubmitting: StateFlow<Boolean> = _returnSubmitting.asStateFlow()
    fun openReturnDialog() {
        _returnMessage.value = null
        _showReturnDialog.value = true
    }
    fun closeReturnDialog() {
        _showReturnDialog.value = false
        _returnMessage.value = null
    }
    fun submitReturn(
        items: List<ReturnItemInput>,
        refundAmount: Long,
        refundMethod: PaymentMethod,
        note: String,
    ) {
        val invoiceId = _selectedTransaction.value?.transaction?.id
        if (invoiceId == null) {
            _returnMessage.value = ReportMessage("Transaksi tidak ditemukan.", isError = true)
            return
        }
        if (items.isEmpty()) {
            _returnMessage.value = ReportMessage("Pilih minimal satu item untuk diretur.", isError = true)
            return
        }
        viewModelScope.launch {
            _returnSubmitting.value = true
            val activeShift = shiftRepository.getOpenShift()
            val outcome =
                returnRepository.processReturn(
                    transactionId = invoiceId,
                    itemInputs = items,
                    refundAmount = refundAmount,
                    refundMethod = refundMethod,
                    shiftId = activeShift?.id,
                    cashierId = activeShift?.cashierId,
                    cashierName = activeShift?.cashierName ?: "",
                    note = note,
                )
            _returnSubmitting.value = false
            when (outcome) {
                is ReturnOutcome.Success -> {
                    _selectedTransaction.value = transactionRepository.loadReceipt(invoiceId)
                    _showReturnDialog.value = false
                    _returnMessage.value = null
                    val totalQty = items.sumOf { it.quantityReturned }
                    val methodLabel = if (refundMethod == PaymentMethod.QRIS) "QRIS" else "Tunai"
                    _messages.emit(
                        ReportMessage(
                            "Retur berhasil diproses. ${totalQty.formatQuantity()} item · ${refundAmount.toRupiah()} " +
                                "dikembalikan via $methodLabel.",
                            isError = false,
                        ),
                    )
                }
                ReturnOutcome.TransactionNotFound -> {
                    _returnMessage.value = ReportMessage("Transaksi tidak ditemukan.", isError = true)
                }
                ReturnOutcome.TransactionVoided -> {
                    _returnMessage.value = ReportMessage("Transaksi ini sudah dibatalkan, tidak dapat diretur.", isError = true)
                }
                ReturnOutcome.AlreadyReturned -> {
                    _returnMessage.value = ReportMessage("Transaksi ini sudah pernah diretur sebelumnya.", isError = true)
                }
                ReturnOutcome.NoItemsSelected -> {
                    _returnMessage.value = ReportMessage("Pilih minimal satu item untuk diretur.", isError = true)
                }
                is ReturnOutcome.InvalidQuantity -> {
                    _returnMessage.value =
                        ReportMessage(
                            "Jumlah retur untuk \"${outcome.productName}\" tidak valid.",
                            isError = true,
                        )
                }
                is ReturnOutcome.InvalidRefundAmount -> {
                    _returnMessage.value =
                        ReportMessage(
                            "Nominal refund tidak valid. Maksimal ${outcome.maxAllowed.toRupiah()} untuk transaksi ini.",
                            isError = true,
                        )
                }
            }
        }
    }
    fun printReceipt(result: CheckoutResult) {
        if (_printUiState.value is PrintUiState.Printing) return
        viewModelScope.launch {
            val printers = printerRepository.getAllOrderedByPriority()
            when {
                printers.isEmpty() -> {
                    _printUiState.value =
                        PrintUiState.Result(
                            com.pos.offline.util.ReceiptPrintOutcome.NoPrinterConfigured,
                            result,
                        )
                }
                printers.size == 1 -> {
                    executePrint(printers.first(), result)
                }
                else -> {
                    _pendingPrintTarget.value = PendingPrintTarget(result, printers)
                }
            }
        }
    }
    fun onPrinterPicked(printer: PrinterEntity) {
        val target = _pendingPrintTarget.value ?: return
        _pendingPrintTarget.value = null
        viewModelScope.launch { executePrint(printer, target.checkoutResult) }
    }
    fun cancelPrinterPicker() {
        _pendingPrintTarget.value = null
    }
    private suspend fun executePrint(
        printer: PrinterEntity,
        result: CheckoutResult,
    ) {
        _printUiState.value = PrintUiState.Printing(result)
        val outcome = printCoordinator.printReceiptToSpecific(printer, result)
        _printUiState.value = PrintUiState.Result(outcome, result)
    }
// Tambahkan state berikut di ReportViewModel.kt
private val _invoiceSearchQuery = MutableStateFlow("")
val invoiceSearchQuery: StateFlow<String> = _invoiceSearchQuery.asStateFlow()

val searchResults: StateFlow<List<TransactionEntity>> =
    _invoiceSearchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else flow { emit(transactionRepository.searchGlobalTransactions(q)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

fun searchInvoice(q: String) {
    _invoiceSearchQuery.value = q
}

fun selectExactDate(date: LocalDate) {
    _selectedDate.value = date
}
// Tambahkan di ReportViewModel.kt
fun searchTransactionsByScannedProduct(barcodeOrName: String) {
    viewModelScope.launch {
        _invoiceSearchQuery.value = barcodeOrName
        // Cari produk berdasarkan barcode terlebih dahulu
        val product = productRepository.getProductByBarcodeAny(barcodeOrName)
        val results = reportDao.searchTransactionsByProduct(
            query = barcodeOrName, 
            productId = product?.id
        )
        // Set hasil pencarian
        _searchResultsState.value = results
    }
}
private val _productHistoryQuery = MutableStateFlow("")
val productHistoryQuery: StateFlow<String> = _productHistoryQuery.asStateFlow()

val productHistoryHierarchy: StateFlow<List<MonthSalesGroup>> =
    _productHistoryQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                flow {
                    val zone = ZoneId.systemDefault()
                    val oneYearAgoMillis = LocalDate.now(zone)
                        .minusYears(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()

                    val rawTransactions = reportDao.searchProductSalesHistory1Year(query.trim(), oneYearAgoMillis)

                    // 1. Grouping berdasarkan Bulan & Tanggal
                    val grouped = rawTransactions
                        .groupBy { tx ->
                            val instant = Instant.ofEpochMilli(tx.createdAt)
                            val localDate = instant.atZone(zone).toLocalDate()
                            java.time.YearMonth.from(localDate) to localDate
                        }
                        .entries
                        .groupBy({ it.key.first }, { DaySalesGroup(it.key.second, it.value.flatMap { pair -> pair.value }) })
                        .map { (yearMonth, dayGroups) ->
                            MonthSalesGroup(
                                yearMonth = yearMonth,
                                days = dayGroups.sortedByDescending { it.date },
                                totalTransactions = dayGroups.sumOf { it.transactions.size }
                            )
                        }
                        .sortedByDescending { it.yearMonth }

                    emit(grouped)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

fun searchProductHistory(query: String) {
    _productHistoryQuery.value = query
}
    private val _selectedPeriodType = MutableStateFlow<ReportPeriodType?>(null)
    val selectedPeriodType: StateFlow<ReportPeriodType?> = _selectedPeriodType.asStateFlow()
    private val _includeSalesSummary = MutableStateFlow(true)
    val includeSalesSummary: StateFlow<Boolean> = _includeSalesSummary.asStateFlow()
    private val _includeProductsSold = MutableStateFlow(false)
    val includeProductsSold: StateFlow<Boolean> = _includeProductsSold.asStateFlow()
    private val _includeDeadStock = MutableStateFlow(false)
    val includeDeadStock: StateFlow<Boolean> = _includeDeadStock.asStateFlow()
    val canGenerateReport: StateFlow<Boolean> =
        combine(_includeSalesSummary, _includeProductsSold, _includeDeadStock) { sales, sold, dead ->
            sales || sold || dead
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
val salesReportUiState: StateFlow<SalesReportUiState> =
      combine(
          _selectedPeriodType,
          _includeSalesSummary,
          _includeProductsSold,
          _includeDeadStock,
          _selectedDate, // Sertakan selectedDate ke combine Flow
      ) { periodType, sales, sold, dead, date ->
          if (periodType == null || !(sales || sold || dead)) null
          else Triple(ReportSelection(periodType, sales, sold, dead), date, Unit)
      }
      .debounce(REPORT_DEBOUNCE_MS)
      .distinctUntilChanged()
      .flatMapLatest { triple ->
          if (triple == null) {
              flowOf<SalesReportUiState>(SalesReportUiState.Hidden)
          } else {
              val (selection, targetDate) = triple
              flow {
                  emit(SalesReportUiState.Loading(selection.periodType))
                  // Menggunakan targetDate yang dipilih di UI
                  val (start, end) = getReportRange(targetDate, selection.periodType == ReportPeriodType.MONTHLY)
                  val fetchProducts = selection.includeProductsSold || selection.includeDeadStock
                  val data = reportRepository.buildSalesReport(start, end, fetchProducts)
                  emit(SalesReportUiState.Loaded(selection.periodType, data))
              }
          }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesReportUiState.Hidden)
    fun toggleReportPeriod(periodType: ReportPeriodType) {
        _selectedPeriodType.value = if (_selectedPeriodType.value == periodType) null else periodType
    }
    fun toggleIncludeSalesSummary(checked: Boolean) {
        _includeSalesSummary.value = checked
        collapseIfNothingSelected()
    }
    fun toggleIncludeProductsSold(checked: Boolean) {
        _includeProductsSold.value = checked
        collapseIfNothingSelected()
    }
    fun toggleIncludeDeadStock(checked: Boolean) {
        _includeDeadStock.value = checked
        collapseIfNothingSelected()
    }
    private fun collapseIfNothingSelected() {
        if (!(_includeSalesSummary.value || _includeProductsSold.value || _includeDeadStock.value)) {
            _selectedPeriodType.value = null
        }
    }
    private fun periodLabelFor(
        now: LocalDate,
        isMonthly: Boolean,
    ): String = if (isMonthly) "Bulanan: ${now.month.name} ${now.year}" else "Harian: ${now.format(dateFmt)}"
    private suspend fun buildReportLines(
        periodType: ReportPeriodType,
        data: SalesReportData,
    ): List<ReceiptLine> {
        val profile = storeProfileRepository.get()
        val now = LocalDate.now(zone)
        val periodLabel = periodLabelFor(now, periodType == ReportPeriodType.MONTHLY)
        val shift = shiftRepository.getOpenShift()
        return ReceiptManager.buildSalesReportLines(
            data = data,
            storeProfile = profile,
            periodLabel = periodLabel,
            printedBy = shift?.cashierName,
            shiftId = shift?.id?.toString(),
            includeSalesSummary = _includeSalesSummary.value,
            includeProductsSold = _includeProductsSold.value,
            includeDeadStock = _includeDeadStock.value,
        )
    }
    suspend fun buildCurrentReportLinesForExport(): List<ReceiptLine>? {
        val state = salesReportUiState.value as? SalesReportUiState.Loaded ?: return null
        return buildReportLines(state.periodType, state.data)
    }
    private fun buildEmptyStateNote(data: SalesReportData): String? {
        val soldEmpty = _includeProductsSold.value && data.products.none { it.qtySold > 0.0 }
        val deadEmpty = _includeDeadStock.value && data.products.none { it.qtySold == 0.0 }
        return when {
            soldEmpty && deadEmpty -> "tidak ada produk yang terjual maupun tidak laku pada periode ini"
            soldEmpty -> "tidak ada produk yang terjual pada periode ini"
            deadEmpty -> "tidak ada produk yang tidak laku pada periode ini"
            else -> null
        }
    }
    private fun appendEmptyStateNote(
        baseMessage: String,
        data: SalesReportData,
    ): String {
        val note = buildEmptyStateNote(data) ?: return baseMessage
        return "$baseMessage (Catatan: $note)"
    }
    fun printSalesReport() {
        viewModelScope.launch {
            val state = salesReportUiState.value as? SalesReportUiState.Loaded
            if (state == null) {
                _messages.emit(ReportMessage("Pilih Harian/Bulanan dan tunggu laporan selesai dimuat.", isError = true))
                return@launch
            }
            val lines = buildReportLines(state.periodType, state.data)
            val printer = printerRepository.getDefault()
            if (printer == null) {
                _messages.emit(ReportMessage("Printer belum diatur.", isError = true))
                return@launch
            }
            val outcome = printCoordinator.printCustomLines(printer, lines)
            when (outcome) {
                is com.pos.offline.util.ReceiptPrintOutcome.Success ->
                    _messages.emit(ReportMessage(appendEmptyStateNote("Laporan berhasil dicetak.", state.data), isError = false))
                is com.pos.offline.util.ReceiptPrintOutcome.SuccessWithNotice ->
                    _messages.emit(ReportMessage(appendEmptyStateNote("Laporan dicetak: ${outcome.notice}", state.data), isError = false))
                is com.pos.offline.util.ReceiptPrintOutcome.Failed ->
                    _messages.emit(ReportMessage("Gagal mencetak laporan: ${outcome.attempts.firstOrNull()?.message ?: "Unknown"}", isError = true))
                com.pos.offline.util.ReceiptPrintOutcome.AlreadyInProgress ->
                    _messages.emit(ReportMessage("Sedang mencetak, mohon tunggu...", isError = false))
                com.pos.offline.util.ReceiptPrintOutcome.NoPrinterConfigured ->
                    _messages.emit(ReportMessage("Printer belum diatur.", isError = true))
            }
        }
    }
    fun notifyPdfExported() {
        viewModelScope.launch {
            val state = salesReportUiState.value as? SalesReportUiState.Loaded ?: return@launch
            _messages.emit(ReportMessage(appendEmptyStateNote("Laporan PDF berhasil dibuat.", state.data), isError = false))
        }
    }
    private fun getReportRange(now: LocalDate, isMonthly: Boolean): Pair<Long, Long> {
        return if (isMonthly) {
            val s = now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val e = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
            s to e
        } else {
            dayBounds(now)
        }
    }
    private fun dayBounds(date: LocalDate): Pair<Long, Long> {
        val timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli()
        return com.pos.offline.util.getAbsoluteDayRange(timestamp)
    }
    private fun aggregate(
        date: LocalDate,
        txs: List<TransactionEntity>,
    ): DailyReport {
        if (txs.isEmpty()) return DailyReport.empty(date)
        val completed = txs.filterNot { it.isVoid }
        val voidedCount = txs.size - completed.size
        fun actualReceived(tx: TransactionEntity): Long {
            return if (tx.paymentMethod == PaymentMethod.CASH.name) {
                tx.paidAmount - tx.changeGiven
            } else {
                tx.total
            }
        }
        val totalDiscount = completed.sumOf { it.discount }
        val totalTax = completed.sumOf { it.tax }
        val count = completed.size
        val cashRevenue =
            completed
                .filter { it.paymentMethod == PaymentMethod.CASH.name }
                .sumOf(::actualReceived)
        val qrisRevenue =
            completed
                .filter { it.paymentMethod == PaymentMethod.QRIS.name }
                .sumOf(::actualReceived)
        val totalRevenue = completed.sumOf(::actualReceived)
        val hourly = MutableList(24) { 0L }
        for (tx in completed) {
            val hour = Instant.ofEpochMilli(tx.createdAt).atZone(zone).hour
            hourly[hour] += actualReceived(tx)
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
            voidedCount = voidedCount,
            cashRevenue = cashRevenue,
            qrisRevenue = qrisRevenue,
        )
    }
    private fun aggregateReturns(returns: List<ReturnEntity>): ReturnSummary {
        val cashRefundTotal =
            returns
                .filter { it.refundMethod == PaymentMethod.CASH.name }
                .sumOf { it.refundAmount }
        val qrisRefundTotal =
            returns
                .filter { it.refundMethod == PaymentMethod.QRIS.name }
                .sumOf { it.refundAmount }
        return ReturnSummary(returns, cashRefundTotal, qrisRefundTotal)
    }
    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }
    fun nextDay() {
        val today = LocalDate.now(zone)
        val current = _selectedDate.value
        if (current.isBefore(today)) _selectedDate.value = current.plusDays(1)
    }
    fun goToday() {
        _selectedDate.value = LocalDate.now(zone)
    }
    companion object {
        val dateFmt: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.forLanguageTag("id-ID"))
        val timeFmt: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        val dateTimeFmt: DateTimeFormatter =
            DateTimeFormatter
                .ofPattern("EEEE, d MMMM yyyy · HH:mm:ss", Locale.forLanguageTag("id-ID"))
                .withZone(ZoneId.systemDefault())
        private const val REPORT_DEBOUNCE_MS = 300L
    }
}
