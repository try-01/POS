package com.pos.offline.ui.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.pos.offline.ui.components.rememberBarcodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ReturnEntity
import com.pos.offline.data.local.entity.ReturnItemEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.hasReturn
import com.pos.offline.data.local.entity.isVoid
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.ReturnDetail
import com.pos.offline.data.repository.ReturnItemInput
import com.pos.offline.ui.components.GlassCard
import com.pos.offline.ui.components.ThousandsSeparatorTransformation
import com.pos.offline.ui.components.discountRowLabel
import com.pos.offline.ui.components.paymentMethodLabel
import com.pos.offline.ui.receipt.PrintUiState
import com.pos.offline.ui.receipt.ReceiptManager
import com.pos.offline.ui.receipt.forTransaction
import com.pos.offline.util.ReceiptPrintOutcome
import com.pos.offline.util.formatQuantity
import com.pos.offline.util.toRupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onNavigateToSettings: () -> Unit,
    onSharePdfFile: (File) -> Unit,
    onExportPdf: (CheckoutResult) -> Unit,
    onShare: (CheckoutResult) -> Unit,
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val isToday by viewModel.isToday.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedTransaction by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val closedShifts by viewModel.closedShifts.collectAsStateWithLifecycle()
    val selectedShiftDetail by viewModel.selectedShiftDetail.collectAsStateWithLifecycle()
    val showReturnDialog by viewModel.showReturnDialog.collectAsStateWithLifecycle()
    val printUiState by viewModel.printUiState.collectAsStateWithLifecycle()
    val pendingPrintTarget by viewModel.pendingPrintTarget.collectAsStateWithLifecycle()
    val returnMessage by viewModel.returnMessage.collectAsStateWithLifecycle()
    val returnSubmitting by viewModel.returnSubmitting.collectAsStateWithLifecycle()
    val returnSummary by viewModel.returnSummary.collectAsStateWithLifecycle()
    val selectedReturnDetail by viewModel.selectedReturnDetail.collectAsStateWithLifecycle()
    var pendingVoidConfirm by remember { mutableStateOf(false) }
    var voidBanner by remember { mutableStateOf<ReportMessage?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val productHistoryQuery by viewModel.productHistoryQuery.collectAsStateWithLifecycle()
    val productHistoryHierarchy by viewModel.productHistoryHierarchy.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDirectWarrantyDialog by remember { mutableStateOf(false) }
// PERBAIKAN: Panggil kedua fungsi pencarian saat barcode discan
val openScanner = rememberBarcodeScanner { scannedCode ->
    viewModel.searchProductHistory(scannedCode)
    viewModel.searchInvoice(scannedCode)
    true // Mengembalikan true agar scanner memicu umpan balik (beep & getar) sukses
}

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            if (selectedTransaction != null) {
                voidBanner = msg
            } else {
                snackbarHostState.showSnackbar(msg.text)
            }
        }
    }
    LaunchedEffect(voidBanner) {
        if (voidBanner != null) {
            delay(3000)
            voidBanner = null
        }
    }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.selectExactDate(localDate)
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // === 2. TAMBAHKAN DIALOG GARANSI DIRECT DI SINI ===
    if (showDirectWarrantyDialog) {
        AlertDialog(
            onDismissRequest = { showDirectWarrantyDialog = false },
            title = { 
                Text("Klaim Garansi Direct", fontWeight = FontWeight.Bold, fontSize = 15.sp) 
            },
            text = {
                Text(
                    "Fitur ini digunakan jika pembeli kehilangan struk. " +
                    "Sistem akan membuat ID Transaksi Garansi Sintetis " +
                    "agar stok pengganti terpotong, barang rusak tercatat, dan laci kasir tetap balance.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDirectWarrantyDialog = false
                        // Panggil logika/fitur proses garansi kamu di sini
                    }
                ) {
                    Text("Proses Garansi Direct", fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDirectWarrantyDialog = false }) {
                    Text("Batal", fontSize = 13.sp)
                }
            }
        )
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Laporan Harian",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars),
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).imePadding(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
// === ITEM 1: BAR PENCARIAN UNIFIKASI & ACTION BUTTONS ===
item(key = "unified_search_actions") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Bar dengan tinggi 34.dp (Sama seperti di Inventory)
        CompactReportSearchBar(
            query = productHistoryQuery,
            onQueryChange = { query ->
                viewModel.searchProductHistory(query)
                viewModel.searchInvoice(query)
            },
            modifier = Modifier.weight(1f).height(34.dp)
        )
        Spacer(Modifier.width(6.dp))
        
        // Tombol Scan Barcode
        CompactSquareIconButton(
            icon = Icons.Rounded.QrCodeScanner,
            contentDescription = "Scan Barcode",
            onClick = { openScanner() }
        )
        Spacer(Modifier.width(6.dp))
        
        // Tombol Klaim Garansi Direct (Menggunakan Icon Build/Kunci Inggris)
        CompactSquareIconButton(
            icon = Icons.Rounded.Build,
            contentDescription = "Klaim Garansi Direct",
            onClick = { showDirectWarrantyDialog = true },
            isError = true // Membuat warnanya menjadi merah agar mencolok
        )
    }
}

// === RENDER EKSKLUSIF MENGGUNAKAN SEALED STATE ===
when (val state = searchUiState) {
    is SearchUiState.ProductHistoryResults -> {
        item(key = "product_history_header") {
            Text(
                "Hasil Pencarian Produk Lintas 1 Tahun",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(
            items = state.hierarchy,
            key = { "month_${it.yearMonth}" }
        ) { monthGroup ->
            MonthGroupCard(
                monthGroup = monthGroup,
                onTransactionClick = { txId -> viewModel.openTransactionDetail(txId) }
            )
        }
    }

    is SearchUiState.InvoiceResults -> {
        item(key = "invoice_search_header") {
            Text(
                "Hasil Pencarian Struk (${state.transactions.size})",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(
            items = state.transactions,
            key = { "invoice_${it.id}" }
        ) { tx ->
            TransactionRow(
                tx = tx,
                onClick = { viewModel.openTransactionDetail(tx.id) }
            )
        }
    }

    is SearchUiState.Empty -> {
        item(key = "search_empty_state") {
            Text(
                "Data \"${state.query}\" tidak ditemukan dalam 365 hari terakhir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }

    SearchUiState.Loading -> {
        item(key = "search_loading") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }

    SearchUiState.Idle -> {
        // TAMPILAN LAPORAN HARIAN NORMAL (Tampil saat tidak ada pencarian)
        item(key = "date_navigator") {
            DateNavigator(
                label = selectedDate.format(ReportViewModel.dateFmt),
                isToday = isToday,
                onPrevious = viewModel::previousDay,
                onNext = viewModel::nextDay,
                onToday = viewModel::goToday,
                onCalendarClick = { showDatePicker = true }
            )
        }

item(key = "sales_report_generator") {
            val periodType by viewModel.selectedPeriodType.collectAsStateWithLifecycle()
            val includeSalesSummary by viewModel.includeSalesSummary.collectAsStateWithLifecycle()
            val includeProductsSold by viewModel.includeProductsSold.collectAsStateWithLifecycle()
            val includeDeadStock by viewModel.includeDeadStock.collectAsStateWithLifecycle()
            val salesReportState by viewModel.salesReportUiState.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            GlassCard(
                 modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                 .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow // Membuat animasi sedikit lebih lambat dan halus
                    )
                 ),
            contentPadding = PaddingValues(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Generator Laporan Penjualan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    ReportPeriodToggleRow(
                        selected = periodType,
                        enabled = true,
                        onSelect = viewModel::toggleReportPeriod
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = includeSalesSummary,
                                onCheckedChange = viewModel::toggleIncludeSalesSummary
                            )
                            Text("Ringkasan Penjualan & Keuangan", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = includeProductsSold,
                                onCheckedChange = viewModel::toggleIncludeProductsSold
                            )
                            Text("Daftar Produk Terjual", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = includeDeadStock,
                                onCheckedChange = viewModel::toggleIncludeDeadStock
                            )
                            Text("Daftar Produk Tidak Laku (Dead Stock)", fontSize = 12.sp)
                        }
                    }
                    if (periodType != null) {
                        SalesReportResultCard(
                            uiState = salesReportState,
                            onPrint = viewModel::printSalesReport,
                            onExportPdf = {
                                viewModel.buildCurrentReportLinesForExportAsync { lines ->
                                    if (lines != null) {
                                        scope.launch {
                                            val file = ReceiptManager.exportPdfFromLines(
                                                context,
                                                lines,
                                                "Laporan_${periodType?.name}_${selectedDate}"
                                            )
                                            viewModel.notifyPdfExported()
                                            onSharePdfFile(file)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        item(key = "summary") { SummarySection(report = report) }

        if (report.transactionCount > 0) {
            item(key = "revenue_trend_chart") {
                RevenueTrendChart(
                    date = report.date,
                    transactions = report.transactions.filterNot { it.isVoid },
                    totalRevenue = report.totalRevenue,
                    hourly = report.hourlyRevenue,
                )
            }
        }

        item(key = "tab_switcher") {
            ReportTabSwitcher(selected = selectedTab, onSelect = viewModel::selectTab)
        }

        when (selectedTab) {
            ReportTab.TRANSACTIONS -> {
                item(key = "list_header") {
                    Text(
                        "Daftar Transaksi (${report.transactions.size})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (report.transactions.isEmpty()) {
                    item(key = "empty") { EmptyReport() }
                } else {
                    items(
                        items = report.transactions,
                        key = { it.id },
                        contentType = { "transaction" },
                    ) { tx ->
                        TransactionRow(
                            tx = tx,
                            onClick = { viewModel.openTransactionDetail(tx.id) },
                        )
                    }
                }
            }
                    ReportTab.SHIFTS -> {
                        item(key = "payment_breakdown") {
                            PaymentBreakdownSection(
                                cashRevenue = report.cashRevenue,
                                qrisRevenue = report.qrisRevenue,
                            )
                        }
                        item(key = "return_summary") {
                            ReturnSummarySection(
                                cashRefundTotal = returnSummary.cashRefundTotal,
                                qrisRefundTotal = returnSummary.qrisRefundTotal,
                            )
                        }
                        item(key = "returns_header") {
                            Text(
                                "Daftar Retur (${returnSummary.returns.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (returnSummary.returns.isEmpty()) {
                            item(key = "empty_returns") { EmptyReturns() }
                        } else {
                            items(
                                items = returnSummary.returns,
                                key = { "return_${it.id}" },
                                contentType = { "return" },
                            ) { ret ->
                                ReturnRow(
                                    ret = ret,
                                    onClick = { viewModel.openReturnDetail(ret.id) },
                                )
                            }
                        }
                        item(key = "closed_shifts_header") {
                            Text(
                                "Riwayat Tutup Shift (${closedShifts.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (closedShifts.isEmpty()) {
                            item(key = "empty_shifts") { EmptyClosedShifts() }
                        } else {
                            items(
                                items = closedShifts,
                                key = { it.id },
                                contentType = { "closed_shift" },
                            ) { shift ->
                                ClosedShiftRow(
                                    shift = shift,
                                    onClick = { viewModel.openShiftDetail(shift) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (selectedTransaction != null && !pendingVoidConfirm && !showReturnDialog && pendingPrintTarget == null) {
        val current = selectedTransaction!!
        TransactionDetailDialog(
            result = current,
            banner = voidBanner,
            printUiState = printUiState.forTransaction(current.transaction.id),
            onVoidClick = { pendingVoidConfirm = true },
            onReturnClick = { viewModel.openReturnDialog() },
            onPrint = { viewModel.printReceipt(current) },
            onExport = { onExportPdf(current) },
            onShare = { onShare(current) },
            onSharePdfFile = onSharePdfFile,
            onNavigateToSettings = onNavigateToSettings,
            onDismiss = {
                pendingVoidConfirm = false
                voidBanner = null
                viewModel.closeTransactionDetail()
            },
        )
    }
    pendingPrintTarget?.let { target ->
        PrinterPickerDialog(
            printers = target.availablePrinters,
            onSelect = { printer -> viewModel.onPrinterPicked(printer) },
            onDismiss = { viewModel.cancelPrinterPicker() },
        )
    }
    if (pendingVoidConfirm) {
        VoidConfirmDialog(
            invoiceId = selectedTransaction?.transaction?.id.orEmpty(),
            onConfirm = {
                viewModel.voidSelectedTransaction()
                pendingVoidConfirm = false
            },
            onDismiss = { pendingVoidConfirm = false },
        )
    }
    if (showReturnDialog && selectedTransaction != null) {
        ReturnItemDialog(
            result = selectedTransaction!!,
            submitting = returnSubmitting,
            message = returnMessage,
            onDismiss = viewModel::closeReturnDialog,
            onSubmit = { items, refundAmount, refundMethod, note ->
                viewModel.submitReturn(items, refundAmount, refundMethod, note)
            },
        )
    }
    selectedShiftDetail?.let { detail ->
        ClosedShiftDetailDialog(
            detail = detail,
            onDismiss = viewModel::closeShiftDetail,
        )
    }
    selectedReturnDetail?.let { detail ->
        ReturnDetailDialog(
            detail = detail,
            onViewOriginalTransaction = { invoiceId ->
                viewModel.closeReturnDetail()
                viewModel.openTransactionDetail(invoiceId)
            },
            onDismiss = viewModel::closeReturnDetail,
        )
    }
}
}

@Composable
private fun ReportPeriodToggleRow(
    selected: ReportPeriodType?,
    enabled: Boolean,
    onSelect: (ReportPeriodType) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ReportPeriodChip(
            label = "Harian",
            isSelected = selected == ReportPeriodType.DAILY,
            enabled = enabled,
            onClick = { onSelect(ReportPeriodType.DAILY) },
            modifier = Modifier.weight(1f),
        )
        ReportPeriodChip(
            label = "Bulanan",
            isSelected = selected == ReportPeriodType.MONTHLY,
            enabled = enabled,
            onClick = { onSelect(ReportPeriodType.MONTHLY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReportPeriodChip(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color =
                when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun SalesReportResultCard(
    uiState: SalesReportUiState,
    onPrint: () -> Unit,
    onExportPdf: () -> Unit,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            ..animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        contentPadding = PaddingValues(12.dp),
    ) {
        when (uiState) {
            is SalesReportUiState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Memuat laporan...", style = MaterialTheme.typography.bodySmall)
                }
            }
            is SalesReportUiState.Loaded -> {
                val data = uiState.data
                val periodLabel = if (uiState.periodType == ReportPeriodType.MONTHLY) "Bulanan" else "Harian"
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SummaryLine("Total Transaksi", "${data.summary.transactionCount} struk")
                    SummaryLine("Pendapatan Bersih", data.pendapatanBersih.toRupiah(), emphasize = true)
                    SummaryLine("Laba Bersih", data.labaBersih.toRupiah(), color = MaterialTheme.colorScheme.primary)
                    if (data.diskon > 0) SummaryLine("Diskon", "- ${data.diskon.toRupiah()}")
                    if (data.summary.taxSum > 0) SummaryLine("Pajak", data.summary.taxSum.toRupiah())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPrint, modifier = Modifier.weight(1f)) { Text("Cetak $periodLabel") }
                        OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f)) { Text("PDF $periodLabel") }
                    }
                }
            }
            SalesReportUiState.Hidden -> Unit
        }
    }
}

@Composable
private fun DateNavigator(
    label: String,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onCalendarClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompactNavIcon(
                    icon = Icons.Rounded.ChevronLeft,
                    contentDescription = "Hari sebelumnya",
                    onClick = onPrevious,
                )
                Column(
                    modifier = Modifier.weight(1f).clickable(onClick = onCalendarClick),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = "Pilih Tanggal Kalender",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isToday) {
                        Text(
                            "Hari ini (Ketuk untuk pilih kalender)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            "Ketuk untuk pilih kalender",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
                CompactNavIcon(
                    icon = Icons.Rounded.ChevronRight,
                    contentDescription = "Hari berikutnya",
                    enabled = !isToday,
                    onClick = onNext,
                )
            }
            if (!isToday) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                    TodayPillButton(onClick = onToday)
                }
            }
        }
    }
}

@Composable
fun ProductHistorySearchSection(
    query: String,
    monthGroups: List<MonthSalesGroup>,
    onQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onDirectWarrantyClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Cari Nama Produk / SKU / Kategori / Barcode…", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .clickable(onClick = onScanClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (query.isNotBlank()) {
            if (monthGroups.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Tidak ada riwayat penjualan 1 tahun terakhir untuk \"$query\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onDirectWarrantyClick) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Proses Klaim Garansi Direct (Tanpa Struk)")
                        }
                    }
                }
            } else {
                Text(
                    "Riwayat Penjualan 1 Tahun Terakhir:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                monthGroups.forEach { monthGroup ->
                    MonthExpandableCard(
                        monthGroup = monthGroup,
                        onTransactionClick = onTransactionClick
                    )
                }

                TextButton(
                    onClick = onDirectWarrantyClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Nota tidak sesuai? Gunakan Garansi Direct (Tanpa Struk)", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MonthExpandableCard(
    monthGroup: MonthSalesGroup,
    onTransactionClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val monthLabel = monthGroup.yearMonth.format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        contentPadding = PaddingValues(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(monthLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text("${monthGroup.totalTransactions} Transaksi", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                }
                Spacer(Modifier.width(4.dp))
                Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, contentDescription = null)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    monthGroup.days.forEach { dayGroup ->
                        DayExpandableSection(
                            dayGroup = dayGroup,
                            onTransactionClick = onTransactionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayExpandableSection(
    dayGroup: DaySalesGroup,
    onTransactionClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateLabel = dayGroup.date.format(
        DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
    )

    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dateLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${dayGroup.transactions.size} Nota", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                dayGroup.transactions.forEach { tx ->
                    TransactionRow(
                        tx = tx,
                        onClick = { onTransactionClick(tx.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactNavIcon(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                },
        )
    }
}

@Composable
private fun TodayPillButton(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Today,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "Ke Hari Ini",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SummarySection(report: DailyReport) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(12.dp),
        ) {
            Column {
                Text(
                    "Total Pendapatan",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    report.totalRevenue.toRupiah(),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${report.transactionCount} transaksi" +
                        (
                            if (report.totalDiscount > 0 || report.totalTax > 0) {
                                " · diskon ${report.totalDiscount.toRupiah()} · pajak ${report.totalTax.toRupiah()}"
                            } else {
                                ""
                            }
                        ) +
                        (if (report.voidedCount > 0) " · ${report.voidedCount} dibatalkan" else ""),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Jumlah Transaksi",
                value = report.transactionCount.toString(),
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Rata-rata / Transaksi",
                value = report.averagePerTransaction.toRupiah(),
                icon = Icons.AutoMirrored.Rounded.ShowChart,
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 14.dp,
        contentPadding = PaddingValues(10.dp),
    ) {
        Column {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Long.toCompactRupiah(): String {
    fun trim(d: Double): String {
        val rounded = Math.round(d * 10) / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString().replace('.', ',')
        }
    }
    return when {
        this >= 1_000_000_000L -> "Rp${trim(this / 1_000_000_000.0)}M"
        this >= 1_000_000L -> "Rp${trim(this / 1_000_000.0)}jt"
        this >= 1_000L -> "Rp${trim(this / 1_000.0)}rb"
        else -> "Rp$this"
    }
}

@Composable
private fun RevenueTrendChart(
    date: LocalDate,
    transactions: List<TransactionEntity>,
    totalRevenue: Long,
    hourly: List<Long>,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val gridColor = onSurface.copy(alpha = 0.08f)
    val axisTextColor = onSurface.copy(alpha = 0.55f)
    val textMeasurer = rememberTextMeasurer()
    val zone = remember { ZoneId.systemDefault() }
    val dayStartMillis = remember(date) { date.atStartOfDay(zone).toInstant().toEpochMilli() }
    val dayEndMillis =
        remember(date) {
            date
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        }
    val peakHour = remember(hourly) { hourly.indices.maxByOrNull { hourly[it] } ?: 0 }
    val peakValue = remember(hourly) { hourly.getOrElse(peakHour) { 0L } }
    val points =
        remember(transactions, dayStartMillis, dayEndMillis) {
            val sorted = transactions.sortedBy { it.createdAt }
            val list = mutableListOf(dayStartMillis to 0L)
            var running = 0L
            for (tx in sorted) {
                running += tx.total
                list.add(tx.createdAt.coerceIn(dayStartMillis, dayEndMillis) to running)
            }
            list.add(dayEndMillis to running)
            list
        }
    val labelStyle = remember(axisTextColor) { TextStyle(color = axisTextColor, fontSize = 9.sp) }

    // PERBAIKAN: Perhitungan label sumbu Y dilakukan di luar Canvas DrawScope
    val maxRevenue = totalRevenue.coerceAtLeast(1L)
    val ySteps = 4
    val yAxisLabels = remember(maxRevenue, labelStyle) {
        (0..ySteps).map { i ->
            val ratio = i / ySteps.toFloat()
            val labelStr = (maxRevenue * ratio).toLong().toCompactRupiah()
            textMeasurer.measure(labelStr, labelStyle)
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.ShowChart,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Tren Pendapatan",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (totalRevenue > 0L) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "Jam ramai: ${"%02d".format(peakHour)}.00 · ${peakValue.toRupiah()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
            ) {
                val leftAxisWidth = 40.dp.toPx()
                val bottomAxisHeight = 16.dp.toPx()
                val plotLeft = leftAxisWidth
                val plotRight = size.width
                val plotTop = 0f
                val plotBottom = size.height - bottomAxisHeight
                val plotWidth = plotRight - plotLeft
                val plotHeight = plotBottom - plotTop

                fun xFor(time: Long): Float {
                    val ratio = (time - dayStartMillis).toFloat() / (dayEndMillis - dayStartMillis).toFloat()
                    return plotLeft + ratio.coerceIn(0f, 1f) * plotWidth
                }
                fun yFor(value: Long): Float {
                    val ratio = value.toFloat() / maxRevenue.toFloat()
                    return plotBottom - ratio.coerceIn(0f, 1f) * plotHeight
                }

                // PERBAIKAN: Menggambar garis dan teks sumbu Y dengan sintaksis valid
                for (i in 0..ySteps) {
                    val ratio = i / ySteps.toFloat()
                    val y = plotBottom - ratio * plotHeight
                    drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1.dp.toPx())
                    val measured = yAxisLabels.getOrElse(i) { textMeasurer.measure("0", labelStyle) }
                    drawText(textLayoutResult = measured, topLeft = Offset(0f, (y - measured.size.height / 2f).coerceIn(0f, plotBottom - measured.size.height)))
                }

                // Sumbu X
                listOf(0, 6, 12, 18, 24).forEach { hour ->
                    val time = (dayStartMillis + hour.toLong() * 3_600_000L).coerceAtMost(dayEndMillis)
                    val x = xFor(time)
                    drawLine(gridColor, Offset(x, plotTop), Offset(x, plotBottom), strokeWidth = 1.dp.toPx())
                    val label = if (hour == 24) "24.00" else "%02d.00".format(hour)
                    val measured = textMeasurer.measure(label, labelStyle, density = this)
                    val labelX =
                        when (hour) {
                            0 -> x
                            24 -> x - measured.size.width
                            else -> x - measured.size.width / 2f
                        }
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(labelX.coerceIn(0f, size.width - measured.size.width), plotBottom + 4.dp.toPx()),
                    )
                }

                if (points.size >= 2) {
                    val linePath = Path()
                    val areaPath = Path()
                    points.forEachIndexed { index, (time, value) ->
                        val x = xFor(time)
                        val y = yFor(value)
                        if (index == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, plotBottom)
                            areaPath.lineTo(x, y)
                        } else {
                            val prevY = yFor(points[index - 1].second)
                            linePath.lineTo(x, prevY) 
                            linePath.lineTo(x, y) 
                            areaPath.lineTo(x, prevY)
                            areaPath.lineTo(x, y)
                        }
                    }
                    areaPath.lineTo(xFor(points.last().first), plotBottom)
                    areaPath.close()
                    drawPath(
                        path = areaPath,
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(primary.copy(alpha = 0.28f), primary.copy(alpha = 0.02f)),
                                startY = plotTop,
                                endY = plotBottom,
                            ),
                    )
                    drawPath(path = linePath, color = primary, style = Stroke(width = 2.dp.toPx()))
                    for (i in 1 until points.size - 1) {
                        val (time, value) = points[i]
                        drawCircle(color = primary, radius = 2.5.dp.toPx(), center = Offset(xFor(time), yFor(value)))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTabSwitcher(
    selected: ReportTab,
    onSelect: (ReportTab) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ReportTabChip(
            label = "Transaksi",
            selected = selected == ReportTab.TRANSACTIONS,
            onClick = { onSelect(ReportTab.TRANSACTIONS) },
            modifier = Modifier.weight(1f),
        )
        ReportTabChip(
            label = "Shift",
            selected = selected == ReportTab.SHIFTS,
            onClick = { onSelect(ReportTab.SHIFTS) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReportTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun PaymentBreakdownSection(
    cashRevenue: Long,
    qrisRevenue: Long,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Breakdown Metode Bayar",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Tunai",
                value = cashRevenue.toRupiah(),
                icon = Icons.Rounded.AttachMoney,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "QRIS",
                value = qrisRevenue.toRupiah(),
                icon = Icons.Rounded.QrCode,
            )
        }
    }
}

@Composable
private fun ReturnSummarySection(
    cashRefundTotal: Long,
    qrisRefundTotal: Long,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Retur Hari Ini",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Refund Tunai",
                value = cashRefundTotal.toRupiah(),
                icon = Icons.Rounded.AttachMoney,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Refund QRIS",
                value = qrisRefundTotal.toRupiah(),
                icon = Icons.Rounded.QrCode,
            )
        }
    }
}

@Composable
private fun ReturnRow(
    ret: ReturnEntity,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ReportViewModel.timeFmt.format(Instant.ofEpochMilli(ret.returnedAt)),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ret.transactionId,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${ret.cashierName.ifBlank { "Tanpa kasir" }} · ${paymentMethodLabel(ret.refundMethod)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
                if (ret.note.isNotBlank()) {
                    Text(
                        ret.note,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                "- ${ret.refundAmount.toRupiah()}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Lihat detail retur",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun EmptyReturns() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Belum ada retur pada hari ini",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ReturnDetailDialog(
    detail: ReturnDetail,
    onViewOriginalTransaction: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val header = detail.header
    val items = detail.items
    val totalQty = items.sumOf { it.quantityReturned }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Retur") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(header.transactionId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    ReportViewModel.dateTimeFmt.format(Instant.ofEpochMilli(header.returnedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { onViewOriginalTransaction(header.transactionId) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Lihat Transaksi Asal", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Kasir", header.cashierName.ifBlank { "Tanpa kasir" })
                SummaryLine("Shift", header.shiftId?.let { "#$it" } ?: "Tanpa shift")
                Spacer(Modifier.height(10.dp))
                Text("Item Diretur (${totalQty.formatQuantity()})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                items.forEach { item ->
                    ReturnDetailItemRow(item)
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Metode Pengembalian", paymentMethodLabel(header.refundMethod))
                SummaryLine(
                    "Total Refund",
                    header.refundAmount.toRupiah(),
                    emphasize = true,
                    color = MaterialTheme.colorScheme.error,
                )
                if (header.note.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Catatan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        header.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
    )
}

@Composable
private fun ReturnDetailItemRow(item: ReturnItemEntity) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(item.productName, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp))
                Text(
                    "${item.quantityReturned.formatQuantity()} x ${item.unitPrice.toRupiah()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Text(
                kotlin.math.round(item.unitPrice * item.quantityReturned).toLong().toRupiah(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
        RestockBadge(restocked = item.restocked, hasProduct = item.productId != null)
    }
}

@Composable
private fun RestockBadge(
    restocked: Boolean,
    hasProduct: Boolean,
) {
    val (label, color) =
        when {
            !hasProduct -> "Produk sudah dihapus · stok dilewati" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            restocked -> "✓ Dikembalikan ke stok" to MaterialTheme.colorScheme.primary
            else -> "✗ Tidak dikembalikan ke stok" to MaterialTheme.colorScheme.error
        }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = color,
    )
}

@Composable
private fun TransactionRow(
    tx: TransactionEntity,
    onClick: () -> Unit,
) {
    val isVoid = tx.isVoid
    val dimmedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ReportViewModel.timeFmt.format(Instant.ofEpochMilli(tx.createdAt)),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color = if (isVoid) dimmedColor else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tx.id,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isVoid) dimmedColor else Color.Unspecified,
                    )
                    if (isVoid) {
                        Spacer(Modifier.width(6.dp))
                        VoidBadge()
                    }
                }
                Text(
                    "Dibayar ${tx.paidAmount.toRupiah()} · ${paymentMethodLabel(tx.paymentMethod)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            Text(
                tx.total.toRupiah(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold,
                color = if (isVoid) dimmedColor else Color.Unspecified,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Lihat detail transaksi",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun VoidBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
    ) {
        Text(
            "DIBATALKAN",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun ReturnedBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
    ) {
        Text(
            "SUDAH DIRETUR",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun ReceiptActionsRow(
    onPrint: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    printEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ReceiptActionButton(
            icon = Icons.Rounded.Print,
            label = if (printEnabled) "Cetak" else "Mencetak...",
            onClick = onPrint,
            enabled = printEnabled,
            modifier = Modifier.weight(1f),
        )
        ReceiptActionButton(
            icon = Icons.Rounded.PictureAsPdf,
            label = "PDF",
            onClick = onExport,
            modifier = Modifier.weight(1f),
        )
        ReceiptActionButton(
            icon = Icons.Rounded.Share,
            label = "Bagikan",
            onClick = onShare,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReceiptActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.4f else 0.2f),
                ).clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
        )
    }
}

@Composable
private fun TransactionDetailDialog(
    result: CheckoutResult,
    banner: ReportMessage?,
    printUiState: PrintUiState,
    onVoidClick: () -> Unit,
    onReturnClick: () -> Unit,
    onPrint: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onSharePdfFile: (File) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tx = result.transaction
    val isVoid = tx.isVoid
    val hasReturn = tx.hasReturn
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Transaksi") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                banner?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color =
                            if (msg.isError) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                    ) {
                        Text(
                            msg.text,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (msg.isError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tx.id,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isVoid) {
                        Spacer(Modifier.width(8.dp))
                        VoidBadge()
                    }
                    if (hasReturn) {
                        Spacer(Modifier.width(8.dp))
                        ReturnedBadge()
                    }
                }
                Text(
                    ReportViewModel.dateTimeFmt.format(Instant.ofEpochMilli(tx.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                if (isVoid && tx.voidedAt != null) {
                    Text(
                        "Dibatalkan pada ${ReportViewModel.dateTimeFmt.format(Instant.ofEpochMilli(tx.voidedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ReceiptActionsRow(
                    onPrint = onPrint,
                    onExport = onExport,
                    onShare = onShare,
                    printEnabled = printUiState !is PrintUiState.Printing,
                )
                ReprintResultBanner(
                    printUiState = printUiState,
                    onSharePdfFile = onSharePdfFile,
                    onNavigateToSettings = onNavigateToSettings,
                )
                if (!isVoid) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!hasReturn) {
                            TextButton(onClick = onReturnClick, modifier = Modifier.weight(1f)) {
                                Text("Retur Item", color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = onVoidClick, modifier = Modifier.weight(1f)) {
                                Text("Batalkan Transaksi", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text(
                                "Transaksi ini sudah memiliki riwayat retur, sehingga tidak dapat dibatalkan.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Item", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                result.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                        ) {
                            Text(
                                item.productName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            )
                            Text(
                                "${item.quantity.formatQuantity()} x ${item.unitPrice.toRupiah()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        Text(
                            item.lineTotal.toRupiah(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Subtotal", tx.subtotal.toRupiah())
                tx.discountRowLabel()?.let { label ->
                    SummaryLine(label, "- ${tx.discount.toRupiah()}")
                }
                if (tx.tax > 0) SummaryLine("Pajak", tx.tax.toRupiah())
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Total", tx.total.toRupiah(), emphasize = true)
                SummaryLine("Bayar", tx.paidAmount.toRupiah())
                when {
                    tx.change < 0L -> {
                        SummaryLine(
                            "Kurang Bayar",
                            kotlin.math.abs(tx.change).toRupiah(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    tx.change == 0L -> {
                        SummaryLine("Kembali", tx.change.toRupiah(), color = MaterialTheme.colorScheme.primary)
                    }
                    else -> {
                        val isQrisCashOut = tx.paymentMethod == PaymentMethod.QRIS.name && tx.changeGivenInCash
                        SummaryLine(
                            if (isQrisCashOut) "Kembali Diberikan (Tunai dari Laci)" else "Kembali Diberikan",
                            tx.changeGiven.toRupiah(),
                            color = if (isQrisCashOut) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        )
                        val tip = (tx.change - tx.changeGiven).coerceAtLeast(0L)
                        if (tip > 0L) {
                            SummaryLine("Tip", tip.toRupiah(), color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Metode Bayar", paymentMethodLabel(tx.paymentMethod))
                SummaryLine("Kasir", tx.cashierName.ifBlank { "Tanpa kasir" })
                tx.shiftId?.let { id ->
                    SummaryLine("Shift", "#$id")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
    )
}

@Composable
private fun ReprintResultBanner(
    printUiState: PrintUiState,
    onSharePdfFile: (File) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val state = printUiState as? PrintUiState.Result ?: return
    val outcome = state.outcome
    val (message, isError) =
        when (outcome) {
            is ReceiptPrintOutcome.Success -> "Struk terkirim ke \"${outcome.printer.label}\"." to false
            is ReceiptPrintOutcome.SuccessWithNotice -> "Struk terkirim ke \"${outcome.printer.label}\".\n⚠ ${outcome.notice}" to false
            is ReceiptPrintOutcome.Failed -> {
                val printerCount = outcome.attempts.size
                val reason = outcome.attempts.firstOrNull()?.message ?: ""
                if (reason.contains("terhubung", ignoreCase = true)) {
                    if (printerCount > 1) {
                        "Gagal mencetak ke semua printer. Mohon hubungkan ke perangkat" to true
                    } else {
                        "Gagal mencetak ke printer. Mohon hubungkan ke perangkat" to true
                    }
                } else {
                    val title = if (printerCount > 1) "Gagal mencetak ke semua printer." else "Gagal mencetak ke printer."
                    "$title\nAlasan: $reason" to true
                }
            }
            ReceiptPrintOutcome.NoPrinterConfigured -> "Printer belum diatur." to true
            ReceiptPrintOutcome.AlreadyInProgress -> "Sedang mencetak, mohon tunggu..." to false
        }
    Spacer(Modifier.height(6.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color =
            if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
    ) {
        Text(
            message,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
        )
    }
    if (outcome is ReceiptPrintOutcome.Failed && outcome.fallbackPdf != null) {
        TextButton(onClick = { onSharePdfFile(outcome.fallbackPdf) }) {
            Text("Bagikan PDF Cadangan")
        }
    }
    if (outcome is ReceiptPrintOutcome.NoPrinterConfigured) {
        TextButton(onClick = onNavigateToSettings) {
            Text("Buka Pengaturan Printer")
        }
    }
}

@Composable
private fun VoidConfirmDialog(
    invoiceId: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batalkan Transaksi?") },
        text = {
            Text(
                "Transaksi $invoiceId akan dibatalkan dan stok item akan dikembalikan. " +
                    "Tindakan ini tidak dapat diurungkan.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Ya, Batalkan", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tidak") }
        },
    )
}

private data class ReturnRowState(
    val transactionItemId: Long,
    val productId: Long?,
    val productName: String,
    val unitPrice: Long,
    val maxQuantity: Double,
    val included: Boolean = false,
    val quantity: Double = maxQuantity,
    val restocked: Boolean = productId != null,
)

@Composable
private fun ReturnItemDialog(
    result: CheckoutResult,
    submitting: Boolean,
    message: ReportMessage?,
    onDismiss: () -> Unit,
    onSubmit: (items: List<ReturnItemInput>, refundAmount: Long, refundMethod: PaymentMethod, note: String) -> Unit,
) {
    val tx = result.transaction
    var rows by remember(tx.id) {
        mutableStateOf(
            result.items.map { item ->
                ReturnRowState(
                    transactionItemId = item.id,
                    productId = item.productId,
                    productName = item.productName,
                    unitPrice = item.unitPrice,
                    maxQuantity = item.quantity,
                )
            },
        )
    }
    val suggestedRefund = rows.filter { it.included }.sumOf { kotlin.math.round(it.unitPrice * it.quantity).toLong() }
    var refundAmountEdited by remember(tx.id) { mutableStateOf(false) }
    var refundAmountText by remember(tx.id) { mutableStateOf("") }
    LaunchedEffect(suggestedRefund) {
        if (!refundAmountEdited) {
            refundAmountText = if (suggestedRefund <= 0L) "" else suggestedRefund.toString()
        }
    }
    var refundMethod by remember(tx.id) {
        mutableStateOf(
            if (tx.paymentMethod == PaymentMethod.QRIS.name) PaymentMethod.QRIS else PaymentMethod.CASH,
        )
    }
    var note by remember(tx.id) { mutableStateOf("") }
    val includedCount = rows.count { it.included }
    val maxRefundable = tx.total
    val refundAmountValue = refundAmountText.toLongOrNull() ?: 0L
    val isRefundOverLimit = refundAmountValue > maxRefundable
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("Retur Item") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                message?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color =
                            if (msg.isError) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                    ) {
                        Text(
                            msg.text,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (msg.isError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    "Transaksi ${tx.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pilih item yang dikembalikan pelanggan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(4.dp))
                rows.forEachIndexed { index, row ->
                    ReturnItemRow(
                        row = row,
                        onToggleIncluded = { checked ->
                            rows =
                                rows.toMutableList().also {
                                    it[index] = row.copy(included = checked)
                                }
                        },
                        onQuantityChange = { qty ->
                            rows =
                                rows.toMutableList().also {
                                    it[index] = row.copy(quantity = qty.coerceIn(0.1, row.maxQuantity))
                                }
                        },
                        onRestockedChange = { checked ->
                            rows =
                                rows.toMutableList().also {
                                    it[index] = row.copy(restocked = checked)
                                }
                        },
                    )
                    HorizontalDivider(Modifier.padding(vertical = 2.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("Metode Pengembalian", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                RefundMethodToggle(selected = refundMethod, onSelect = { refundMethod = it })
                Spacer(Modifier.height(10.dp))
                Text("Nominal Pengembalian", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                RefundAmountField(
                    value = refundAmountText,
                    onValueChange = { digits ->
                        refundAmountEdited = true
                        refundAmountText = digits
                    },
                    isError = isRefundOverLimit,
                )
                if (isRefundOverLimit) {
                    Text(
                        "Melebihi total transaksi (maks ${maxRefundable.toRupiah()})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (includedCount > 0) {
                    Text(
                        "Sugesti: ${suggestedRefund.toRupiah()} (tanpa prorata diskon/pajak)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Catatan (opsional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Mis. barang cacat produksi") },
                    minLines = 2,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting && includedCount > 0 && !isRefundOverLimit,
                onClick = {
                    val items =
                        rows.filter { it.included }.map { row ->
                            ReturnItemInput(
                                transactionItemId = row.transactionItemId,
                                productId = row.productId,
                                productName = row.productName,
                                unitPrice = row.unitPrice,
                                quantityReturned = row.quantity,
                                restocked = row.restocked && row.productId != null,
                            )
                        }
                    onSubmit(items, refundAmountText.toLongOrNull() ?: 0L, refundMethod, note.trim())
                },
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Proses Retur")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !submitting, onClick = onDismiss) { Text("Batal") }
        },
    )
}

@Composable
private fun ReturnItemRow(
    row: ReturnRowState,
    onToggleIncluded: (Boolean) -> Unit,
    onQuantityChange: (Double) -> Unit,
    onRestockedChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = row.included, onCheckedChange = onToggleIncluded)
            Column(Modifier.weight(1f)) {
                Text(
                    row.productName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${row.unitPrice.toRupiah()} · maks ${row.maxQuantity.formatQuantity()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        if (row.included) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val step = if (row.maxQuantity % 1.0 == 0.0) 1.0 else 0.1
                MiniStepper(
                    qty = row.quantity,
                    canDecrease = row.quantity > step,
                    canIncrease = row.quantity < row.maxQuantity,
                    onDecrease = { onQuantityChange((row.quantity - step).coerceAtLeast(step)) },
                    onIncrease = { onQuantityChange((row.quantity + step).coerceAtMost(row.maxQuantity)) },
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        if (row.productId != null) {
                            Modifier.clickable { onRestockedChange(!row.restocked) }
                        } else {
                            Modifier
                        },
                ) {
                    Checkbox(
                        checked = row.restocked && row.productId != null,
                        onCheckedChange = if (row.productId != null) onRestockedChange else null,
                        enabled = row.productId != null,
                    )
                    Text(
                        "Kembalikan ke stok?",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (row.productId != null) 0.8f else 0.4f,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStepper(
    qty: Double,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (canDecrease) 1f else 0.4f))
                    .then(if (canDecrease) Modifier.clickable(onClick = onDecrease) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Kurangi jumlah", modifier = Modifier.size(14.dp))
        }
        Text(
            qty.formatQuantity(),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (canIncrease) 1f else 0.4f))
                    .then(if (canIncrease) Modifier.clickable(onClick = onIncrease) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Tambah jumlah", modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun RefundMethodToggle(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(PaymentMethod.CASH to "Tunai", PaymentMethod.QRIS to "QRIS").forEach { (method, label) ->
            val isSelected = selected == method
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(method) }
                        .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

@Composable
private fun RefundAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        visualTransformation = ThousandsSeparatorTransformation,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        modifier = Modifier.fillMaxWidth().height(44.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            width = 1.dp,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(10.dp),
                        ).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Rp ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun ClosedShiftRow(
    shift: ShiftEntity,
    onClick: () -> Unit,
) {
    val diff = shift.cashDifference
    val diffColor =
        when {
            diff == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            diff < 0L -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
    val diffLabel =
        when {
            diff == null -> "-"
            diff == 0L -> "Pas"
            diff < 0L -> "-${(-diff).toRupiah()}"
            else -> "+${diff.toRupiah()}"
        }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                shift.endedAt?.let { ReportViewModel.timeFmt.format(Instant.ofEpochMilli(it)) } ?: "-",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    shift.cashierName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Kas awal ${shift.startingCash.toRupiah()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    diffLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    color = diffColor,
                )
                Text(
                    "Selisih",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Lihat detail shift",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun ClosedShiftDetailDialog(
    detail: ClosedShiftDetail,
    onDismiss: () -> Unit,
) {
    val shift = detail.shift
    val summary = detail.summary
    val expected = shift.endingCashExpected ?: summary.expectedCashInDrawer
    val actual = shift.endingCashActual ?: 0L
    val difference = shift.cashDifference ?: (actual - expected)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Tutup Shift") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(shift.cashierName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Mulai: ${ReportViewModel.dateTimeFmt.format(Instant.ofEpochMilli(shift.startedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                shift.endedAt?.let {
                    Text(
                        "Ditutup: ${ReportViewModel.dateTimeFmt.format(Instant.ofEpochMilli(it))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("📋 Ringkasan Shift", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Penjualan Tunai", summary.cashRevenue.toRupiah())
                SummaryLine("Penjualan QRIS", summary.qrisRevenue.toRupiah())
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Total Pendapatan", summary.totalRevenue.toRupiah(), emphasize = true)
                SummaryLine("Laba Kotor", summary.grossProfit.toRupiah(), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
                Text("💵 Rekonsiliasi Laci", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Kas Awal (Modal)", summary.startingCash.toRupiah())
                SummaryLine("Penjualan Tunai", summary.cashRevenue.toRupiah())
                if (summary.cashRefunds > 0L) {
                    SummaryLine(
                        "Refund Tunai",
                        "- ${summary.cashRefunds.toRupiah()}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (summary.qrisCashChangeOut > 0L) {
                    SummaryLine(
                        "Kembalian Tunai (dari QRIS)",
                        "- ${summary.qrisCashChangeOut.toRupiah()}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Estimasi di Laci", expected.toRupiah(), emphasize = true)
                SummaryLine("Kas Fisik (Aktual)", actual.toRupiah())
                Spacer(Modifier.height(6.dp))
                val diffAbs = kotlin.math.abs(difference)
                val diffColor =
                    if (difference < 0L) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                val diffLabel =
                    when {
                        difference == 0L -> "Pas ✓"
                        difference < 0L -> "-${diffAbs.toRupiah()} (Uang Kurang)"
                        else -> "+${diffAbs.toRupiah()} (Uang Lebih)"
                    }
                SummaryLine("Selisih", diffLabel, emphasize = true, color = diffColor)
                if (shift.note.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Catatan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        shift.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
    )
}

@Composable
private fun EmptyClosedShifts() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Belum ada shift yang ditutup pada hari ini",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    emphasize: Boolean = false,
    color: Color = Color.Unspecified,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = color,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
    }
}

@Composable
private fun EmptyReport() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Rounded.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Belum ada transaksi pada hari ini",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun MonthGroupCard(
    monthGroup: MonthSalesGroup,
    onTransactionClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Level 1: Bulan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${monthGroup.yearMonth}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${monthGroup.totalTransactions} Transaksi >",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Level 2 & 3: Tanggal & Transaksi
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                monthGroup.days.forEach { dayGroup ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp)
                    ) {
                        Text(
                            text = "📌 Tanggal ${dayGroup.date}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        dayGroup.transactions.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTransactionClick(tx.id) }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Struk #${tx.id.takeLast(6)}",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = tx.total.toRupiah(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactReportSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Cari struk / produk...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Hapus pencarian",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun CompactSquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    // Jika isError true, icon akan berwarna merah (untuk garansi). Jika tidak, warna primary.
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val borderColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = color,
        )
    }
}