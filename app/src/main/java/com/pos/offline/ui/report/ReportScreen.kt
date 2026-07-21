package com.pos.offline.ui.report

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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.pos.offline.ui.receipt.forTransaction
import com.pos.offline.util.ReceiptPrintOutcome
import com.pos.offline.util.toRupiah
import kotlinx.coroutines.delay
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .imePadding(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "date_navigator") {
                DateNavigator(
                    label = selectedDate.format(ReportViewModel.dateFmt),
                    isToday = isToday,
                    onPrevious = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                    onToday = viewModel::goToday,
                )
            }

            item(key = "summary") { SummarySection(report = report) }

            if (report.transactionCount > 0) {
                item(key = "chart") {
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

    if (selectedTransaction != null && !pendingVoidConfirm && !showReturnDialog) {
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

@Composable
private fun DateNavigator(
    label: String,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
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
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(5.dp))
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
                            "Hari ini",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
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
                val maxRevenue = totalRevenue.coerceAtLeast(1L)

                fun xFor(time: Long): Float {
                    val ratio = (time - dayStartMillis).toFloat() / (dayEndMillis - dayStartMillis).toFloat()
                    return plotLeft + ratio.coerceIn(0f, 1f) * plotWidth
                }

                fun yFor(value: Long): Float {
                    val ratio = value.toFloat() / maxRevenue.toFloat()
                    return plotBottom - ratio.coerceIn(0f, 1f) * plotHeight
                }

                val ySteps = 4
                for (i in 0..ySteps) {
                    val ratio = i / ySteps.toFloat()
                    val y = plotBottom - ratio * plotHeight
                    drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1.dp.toPx())
                    val measured = textMeasurer.measure((maxRevenue * ratio).toLong().toCompactRupiah(), labelStyle, density = this)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(0f, (y - measured.size.height / 2f).coerceIn(0f, plotBottom - measured.size.height)),
                    )
                }

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
                            linePath.lineTo(x, prevY) // datar dari nilai sebelumnya
                            linePath.lineTo(x, y) // lonjakan tegak saat transaksi terjadi
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
                Text("Item Diretur ($totalQty)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                    "${item.quantityReturned} x ${item.unitPrice.toRupiah()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Text(
                (item.unitPrice * item.quantityReturned.toLong()).toRupiah(),
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
                    ) {
                        if (!hasReturn) {
                            TextButton(onClick = onReturnClick, modifier = Modifier.weight(1f)) {
                                Text("Retur Item", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        TextButton(onClick = onVoidClick, modifier = Modifier.weight(1f)) {
                            Text("Batalkan Transaksi", color = MaterialTheme.colorScheme.error)
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
                                "${item.quantity} x ${item.unitPrice.toRupiah()}",
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
                SummaryLine("Kembali", tx.change.toRupiah(), color = MaterialTheme.colorScheme.primary)

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
            is ReceiptPrintOutcome.Failed -> "Gagal mencetak ke semua printer." to true
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
    val maxQuantity: Int,
    val included: Boolean = false,
    val quantity: Int = maxQuantity,
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

    val suggestedRefund = rows.filter { it.included }.sumOf { it.unitPrice * it.quantity.toLong() }

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
                                    it[index] = row.copy(quantity = qty.coerceIn(1, row.maxQuantity))
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
                )
                if (includedCount > 0) {
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
                enabled = !submitting && includedCount > 0,
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
    onQuantityChange: (Int) -> Unit,
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
                    "${row.unitPrice.toRupiah()} · maks ${row.maxQuantity}",
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
                MiniStepper(
                    qty = row.quantity,
                    canDecrease = row.quantity > 1,
                    canIncrease = row.quantity < row.maxQuantity,
                    onDecrease = { onQuantityChange(row.quantity - 1) },
                    onIncrease = { onQuantityChange(row.quantity + 1) },
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
    qty: Int,
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
            "$qty",
            modifier = Modifier.width(28.dp),
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
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
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
