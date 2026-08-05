package com.pos.offline.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.iamjosephmj.flinger.behaviours.FlingPresets
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.ShiftSummary
import com.pos.offline.ui.components.GlassCard
import com.pos.offline.ui.components.ThousandsSeparatorTransformation
import com.pos.offline.ui.components.discountInlineLabel
import com.pos.offline.ui.components.paymentMethodLabel
import com.pos.offline.ui.receipt.PrintUiState
import com.pos.offline.ui.receipt.forTransaction
import com.pos.offline.util.ReceiptPrintOutcome
import com.pos.offline.util.formatQuantity
import com.pos.offline.util.toRupiah
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PosDialogManager(
    uiState: PosUiState,
    localState: PosLocalStateHolder,
    onAction: (PosAction) -> Unit,
    onSharePdfFile: (File) -> Unit,
    onExportPdf: (CheckoutResult) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val shift = uiState.shift
    val checkout = uiState.checkout

    val stockWarning = shift.stockWarning
    if (stockWarning != null &&
        checkout.flow !is CheckoutFlow.Success &&
        checkout.flow !is CheckoutFlow.Error
    ) {
        AlertDialog(
            onDismissRequest = { onAction(PosAction.DismissStockWarning) },
            icon = {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Stok Tidak Mencukupi") },
            text = {
                Text(
                    "Stok \"${stockWarning.productName}\" tercatat tinggal " +
                        "${stockWarning.currentStock}, tapi Anda tetap menambahkannya ke " +
                        "keranjang. Transaksi tetap bisa dilanjutkan — pastikan produk fisik " +
                        "memang tersedia, lalu perbarui stok di menu Inventaris setelah " +
                        "transaksi ini selesai.",
                )
            },
            confirmButton = {
                Button(onClick = { onAction(PosAction.DismissStockWarning) }) {
                    Text("Mengerti, Lanjutkan")
                }
            },
        )
    }

    when (val flow = checkout.flow) {
        is CheckoutFlow.Success -> {
            SuccessDialog(
                result = flow.result,
                printUiState = checkout.printUiState.forTransaction(flow.result.transaction.id),
                openDrawerOnPrint = checkout.openDrawerOnPrint,
                onToggleOpenDrawer = { onAction(PosAction.ToggleOpenDrawer(it)) },
                onPrint = { onAction(PosAction.PrintReceipt(flow.result)) },
                onExport = { onExportPdf(flow.result) },
                onSharePdfFile = onSharePdfFile,
                onNavigateToSettings = onNavigateToSettings,
                onDismiss = { onAction(PosAction.ResetCheckout) },
            )
        }

        is CheckoutFlow.Error -> {
            AlertDialog(
                onDismissRequest = { onAction(PosAction.ResetCheckout) },
                confirmButton = {
                    TextButton(onClick = { onAction(PosAction.ResetCheckout) }) { Text("Tutup") }
                },
                title = { Text("Transaksi Gagal") },
                text = { Text(flow.message) },
            )
        }

        else -> {
            Unit
        }
    }

    if (localState.showClearConfirm) {
        AlertDialog(
            onDismissRequest = localState::dismissClearDialog,
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Kosongkan Keranjang?") },
            text = {
                Text("Semua item di keranjang akan dihapus. Tindakan ini tidak bisa dibatalkan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(PosAction.ClearCart)
                        localState.dismissClearDialog()
                    },
                ) {
                    Text("Ya, Kosongkan", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = localState::dismissClearDialog) { Text("Batal") }
            },
        )
    }

    localState.qtyEditItem?.let { item ->
        QuantityEditDialog(
            item = item,
            maxStock = uiState.catalog.stockByProductId[item.productId],
            onConfirm = { newQty ->
                onAction(PosAction.SetQuantity(item, newQty))
                localState.dismissQtyEdit()
            },
            onDismiss = localState::dismissQtyEdit,
        )
    }

    if (localState.showInsufficientPaymentDialog) {
        InsufficientPaymentDialog(
            paid = uiState.payment.paid,
            total = uiState.cart.totals.total,
            onDismiss = localState::dismissInsufficientPayment,
            onConfirm = {
                localState.dismissInsufficientPayment()
                onAction(PosAction.Checkout)
            },
        )
    }

    if (shift.showStartShiftDialog) {
        StartShiftDialog(
            cashiers = shift.activeCashiers,
            isProcessing = shift.isStartingShift,
            onDismiss = { onAction(PosAction.DismissStartShiftDialog) },
            onConfirm = { cashierId, startingCash ->
                onAction(PosAction.StartShift(cashierId, startingCash))
            },
        )
    }

    shift.shiftSummary?.let { summary ->
        if (shift.showEndShiftDialog) {
            EndShiftDialog(
                summary = summary,
                isProcessing = shift.isEndingShift,
                onDismiss = { onAction(PosAction.DismissEndShiftDialog) },
                onConfirm = { actualCash -> onAction(PosAction.EndShift(actualCash)) },
            )
        }
    }

    if (shift.showShiftListDialog) {
        ManageShiftsDialog(
            shifts = shift.openShifts,
            activeShiftId = shift.activeShift?.id,
            onCloseShift = { target ->
                onAction(PosAction.DismissShiftListDialog)
                onAction(PosAction.OpenEndShiftDialog(target))
            },
            onDesignateActive = { target ->
                onAction(PosAction.SelectActiveShift(target.id))
                onAction(PosAction.DismissShiftListDialog)
            },
            onStartNewShift = {
                onAction(PosAction.DismissShiftListDialog)
                onAction(PosAction.OpenStartShiftDialog)
            },
            onDismiss = { onAction(PosAction.DismissShiftListDialog) },
        )
    }
}

private val shiftDateFmt = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("id-ID"))

private fun formatElapsedSince(startedAt: Long): String {
    val diffMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
    val totalMinutes = diffMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "berjalan ${hours}j ${minutes}m" else "berjalan ${minutes}m"
}

@Composable
internal fun ManageShiftsDialog(
    shifts: List<ShiftEntity>,
    activeShiftId: Long?,
    onCloseShift: (ShiftEntity) -> Unit,
    onDesignateActive: (ShiftEntity) -> Unit,
    onStartNewShift: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kelola Shift") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(state = rememberScrollState(), flingBehavior = FlingPresets.iOSStyle())
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shifts.isEmpty()) {
                    Text(
                        "Tidak ada shift yang sedang berjalan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    Text(
                        "Ketuk shift untuk menutupnya. Semua kasir bisa menutup shift siapa pun.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    if (shifts.size > 1) {
                        Text(
                            "Gunakan \"Jadikan Aktif\" untuk memilih kasir yang bertugas di " +
                                "terminal ini (tanpa menutup shift).",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    shifts.forEach { shift ->
                        OpenShiftRow(
                            shift = shift,
                            isDesignatedActive = shift.id == activeShiftId,
                            showDesignateButton = shifts.size > 1,
                            onCloseClick = { onCloseShift(shift) },
                            onDesignateActiveClick = { onDesignateActive(shift) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStartNewShift) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mulai Shift Baru")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
    )
}

@Composable
private fun OpenShiftRow(
    shift: ShiftEntity,
    isDesignatedActive: Boolean,
    showDesignateButton: Boolean,
    onCloseClick: () -> Unit,
    onDesignateActiveClick: () -> Unit,
) {
    val elapsed = remember(shift.id, shift.startedAt) { formatElapsedSince(shift.startedAt) }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        contentPadding = PaddingValues(10.dp),
        onClick = onCloseClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shift.cashierName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isDesignatedActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                "SAAT INI",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    "Mulai: ${shiftDateFmt.format(Date(shift.startedAt))} · $elapsed",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    "Kas awal: ${shift.startingCash.toRupiah()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                if (showDesignateButton && !isDesignatedActive) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = onDesignateActiveClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Jadikan Aktif",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        )
                    }
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Tutup shift ini",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
internal fun StartShiftDialog(
    cashiers: List<CashierEntity>,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (cashierId: Long, startingCash: Long) -> Unit,
) {
    var selectedCashier by remember { mutableStateOf(cashiers.firstOrNull()) }
    var startingCash by remember { mutableStateOf(0L) }

    LaunchedEffect(cashiers) {
        val current = selectedCashier
        if (current == null || cashiers.none { it.id == current.id }) {
            selectedCashier = cashiers.firstOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("Mulai Shift") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (cashiers.isEmpty()) {
                    Text(
                        "Belum ada kasir terdaftar. Tambahkan kasir dulu di tab Pengaturan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    CashierDropdownField(
                        cashiers = cashiers,
                        selected = selectedCashier,
                        onSelect = { selectedCashier = it },
                    )
                    MoneyField(
                        label = "Kas Awal",
                        value = startingCash,
                        onValueChange = { startingCash = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedCashier?.let { onConfirm(it.id, startingCash) } },
                enabled = selectedCashier != null && !isProcessing,
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Mulai Shift")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("Batal") }
        },
    )
}

@Composable
internal fun EndShiftDialog(
    summary: ShiftSummary,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (actualCash: Long) -> Unit,
) {
    var actualCash by remember { mutableStateOf(0L) }
    var hasBeenEdited by remember { mutableStateOf(false) }

    val expected = summary.expectedCashInDrawer
    val difference = actualCash - expected
    val isCleanZeroAllowed = actualCash == 0L && expected == 0L
    val hasInput = hasBeenEdited || isCleanZeroAllowed

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("Tutup Shift") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(state = rememberScrollState(), flingBehavior = FlingPresets.iOSStyle())
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "📋 Ringkasan Shift",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Penjualan Tunai", summary.cashRevenue.toRupiah())
                SummaryLine("Penjualan QRIS", summary.qrisRevenue.toRupiah())
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Total Pendapatan", summary.totalRevenue.toRupiah(), emphasize = true)
                if (summary.qrisRefunds > 0L) {
                    SummaryLine(
                        "Refund via QRIS",
                        "- ${summary.qrisRefunds.toRupiah()}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                SummaryLine(
                    "Laba Kotor",
                    summary.grossProfit.toRupiah(),
                    color = MaterialTheme.colorScheme.primary,
                )
                if (summary.warrantyExchangeCost > 0L) {
                    SummaryLine(
                        "Biaya Klaim Garansi",
                        "- ${summary.warrantyExchangeCost.toRupiah()}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "💵 Rekonsiliasi Laci",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
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
                Spacer(Modifier.height(10.dp))
                MoneyField(
                    label = "Uang Fisik",
                    value = actualCash,
                    onValueChange = {
                        actualCash = it
                        hasBeenEdited = true
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                )
                if (hasInput) {
                    Spacer(Modifier.height(10.dp))
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
                    Text(
                        "💡 Hasil",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    SummaryLine("Selisih", diffLabel, emphasize = true, color = diffColor)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(actualCash) },
                enabled = hasInput && !isProcessing,
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Tutup Shift")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("Batal") }
        },
    )
}

@Composable
internal fun SuccessDialog(
    result: CheckoutResult,
    printUiState: PrintUiState,
    openDrawerOnPrint: Boolean,
    onToggleOpenDrawer: (Boolean) -> Unit,
    onPrint: () -> Unit,
    onExport: () -> Unit,
    onSharePdfFile: (File) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Selesai") }
        },
        title = { Text("Transaksi Berhasil ✓") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No. Struk: ${result.transaction.id}")
                    Text(
                        "Metode Bayar: ${paymentMethodLabel(result.transaction.paymentMethod)}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    result.transaction.discountInlineLabel()?.let { label ->
                        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Text(
                        "Total: ${result.transaction.total.toRupiah()}",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Bayar: ${result.transaction.paidAmount.toRupiah()}")

                    val txChange = result.transaction.change
                    val txChangeGiven = result.transaction.changeGiven
                    val isQrisCashOut =
                        result.transaction.paymentMethod == PaymentMethod.QRIS.name &&
                            result.transaction.changeGivenInCash

                    when {
                        txChange < 0L -> {
                            Text(
                                "Kurang Bayar: ${kotlin.math.abs(txChange).toRupiah()}",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        txChange == 0L -> {
                            Text(
                                "Kembali: ${txChange.toRupiah()}",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        else -> {
                            Text(
                                if (isQrisCashOut) {
                                    "Kembali Diberikan (Tunai dari Laci): ${txChangeGiven.toRupiah()}"
                                } else {
                                    "Kembali Diberikan: ${txChangeGiven.toRupiah()}"
                                },
                                color =
                                    if (isQrisCashOut) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                fontWeight = if (isQrisCashOut) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            val tip = (txChange - txChangeGiven).coerceAtLeast(0L)
                            if (tip > 0L) {
                                Text(
                                    "Tip: ${tip.toRupiah()}",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleOpenDrawer(!openDrawerOnPrint) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = openDrawerOnPrint,
                            onCheckedChange = onToggleOpenDrawer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buka laci saat mencetak", style = MaterialTheme.typography.bodyMedium)
                    }
                    FilledTonalButton(
                        onClick = onPrint,
                        enabled = printUiState !is PrintUiState.Printing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (printUiState is PrintUiState.Printing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Mencetak...")
                        } else {
                            Icon(
                                Icons.Rounded.Print,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Cetak Struk")
                        }
                    }
                    OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                        Text("Ekspor PDF")
                    }
                    PrintResultBanner(
                        printUiState = printUiState,
                        onSharePdfFile = onSharePdfFile,
                        onNavigateToSettings = onNavigateToSettings,
                    )
                }
            }
        },
    )
}

@Composable
private fun PrintResultBanner(
    printUiState: PrintUiState,
    onSharePdfFile: (File) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val state = printUiState as? PrintUiState.Result ?: return
    val outcome = state.outcome

    val (message, isError) =
        when (outcome) {
            is ReceiptPrintOutcome.Success -> {
                "Struk terkirim ke \"${outcome.printer.label}\"." to false
            }

            is ReceiptPrintOutcome.SuccessWithNotice -> {
                "Struk terkirim ke \"${outcome.printer.label}\".\n⚠ ${outcome.notice}" to false
            }

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
                    val title =
                        if (printerCount > 1) {
                            "Gagal mencetak ke semua printer."
                        } else {
                            "Gagal mencetak ke printer."
                        }
                    "$title\nAlasan: $reason" to true
                }
            }

            ReceiptPrintOutcome.NoPrinterConfigured -> {
                "Printer belum diatur." to true
            }

            ReceiptPrintOutcome.AlreadyInProgress -> {
                "Sedang mencetak, mohon tunggu..." to false
            }
        }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}

@Composable
internal fun InsufficientPaymentDialog(
    paid: Long,
    total: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val shortfall = (total - paid).coerceAtLeast(0L)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Pembayaran Kurang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Uang yang diterima (${paid.toRupiah()}) kurang dari total (${total.toRupiah()}).",
                )
                Text(
                    "Kekurangan: ${shortfall.toRupiah()}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pastikan ini disengaja (mis. dicatat sebagai piutang), bukan salah ketik.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Tetap Lanjutkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Periksa Lagi") }
        },
    )
}

@Composable
internal fun QuantityEditDialog(
    item: CartItemEntity,
    maxStock: Double?,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var fieldValue by remember {
        val initial = item.quantity.formatQuantity()
        mutableStateOf(TextFieldValue(text = initial, selection = TextRange(0, initial.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun confirmWith(qty: Double) = onConfirm(qty.coerceAtLeast(0.0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Jumlah") },
        text = {
            Column {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (maxStock != null) {
                    Text(
                        "Stok tersedia: ${maxStock.formatQuantity()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        val cleaned =
                            buildString {
                                var dotSeen = false
                                for (c in newValue.text) {
                                    when {
                                        c.isDigit() -> {
                                            append(c)
                                        }

                                        c == '.' && !dotSeen -> {
                                            append(c)
                                            dotSeen = true
                                        }
                                    }
                                }
                            }.take(8)
                        fieldValue = newValue.copy(text = cleaned)
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                confirmWith(fieldValue.text.toDoubleOrNull() ?: item.quantity)
                            },
                        ),
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(vertical = 14.dp),
                    decorationBox = { innerTextField ->
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) { innerTextField() }
                    },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Pintasan cepat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(1.0, 5.0, 10.0, 20.0).forEach { shortcut ->
                        FilledTonalButton(
                            onClick = { confirmWith(shortcut) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            Text(
                                shortcut.formatQuantity(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirmWith(fieldValue.text.toDoubleOrNull() ?: item.quantity) },
            ) { Text("Terapkan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

@Composable
internal fun CashierDropdownField(
    cashiers: List<CashierEntity>,
    selected: CashierEntity?,
    onSelect: (CashierEntity) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp),
                    ).clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected?.name ?: "Pilih kasir",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (selected != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            cashiers.forEach { cashier ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(cashier.name) },
                    onClick = {
                        onSelect(cashier)
                        expanded = false
                    },
                )
            }
        }
    }
}
