package com.pos.offline.ui.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.local.entity.DiscountType
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.ShiftSummary
import com.pos.offline.ui.components.BarcodeScannerCamera
import com.pos.offline.ui.components.GlassCard
import com.pos.offline.ui.components.discountInlineLabel
import com.pos.offline.ui.components.formatPercentTrim
import com.pos.offline.ui.components.paymentMethodLabel
import com.pos.offline.util.toRupiah
import com.pos.offline.ui.components.ThousandsSeparatorTransformation
import com.pos.offline.ui.receipt.PrintUiState
import com.pos.offline.ui.receipt.forTransaction
import com.pos.offline.util.ReceiptPrintOutcome
import com.pos.offline.ui.components.rememberBarcodeScanner
import java.text.SimpleDateFormat
import java.io.File
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToSettings: () -> Unit,
    onSharePdfFile: (File) -> Unit,
    onExportPdf: (CheckoutResult) -> Unit,
    forceWideLayout: Boolean = false,
    isCartExpanded: Boolean = false,
    onCartExpandedChange: (Boolean) -> Unit = {}
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val discountType by viewModel.discountType.collectAsStateWithLifecycle()
    val discountValue by viewModel.discountValue.collectAsStateWithLifecycle()
    val taxRate by viewModel.taxRate.collectAsStateWithLifecycle()
    val paid by viewModel.paid.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()

    val printUiState by viewModel.printUiState.collectAsStateWithLifecycle()
    val isOpeningDrawer by viewModel.isOpeningDrawer.collectAsStateWithLifecycle()
    val openDrawerOnPrint by viewModel.openDrawerOnPrint.collectAsStateWithLifecycle()

    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()

    val activeCashiers by viewModel.activeCashiers.collectAsStateWithLifecycle()
    val openShift by viewModel.openShift.collectAsStateWithLifecycle()
    val openShifts by viewModel.openShifts.collectAsStateWithLifecycle()
    val showStartShiftDialog by viewModel.showStartShiftDialog.collectAsStateWithLifecycle()
    val showEndShiftDialog by viewModel.showEndShiftDialog.collectAsStateWithLifecycle()
    val showShiftListDialog by viewModel.showShiftListDialog.collectAsStateWithLifecycle()
    val shiftSummary by viewModel.shiftSummary.collectAsStateWithLifecycle()

    val isCartEmpty by remember { derivedStateOf { cart.isEmpty() } }
    val isProcessing by remember { derivedStateOf { checkoutState is CheckoutState.Processing } }
    val change by remember(paid, totals) {
        derivedStateOf { (paid - totals.total).coerceAtLeast(0L) }
    }

    val cartQtyByProductId by remember(cart) {
        derivedStateOf { cart.associate { it.productId to it.quantity } }
    }
    val stockByProductId by remember(products) {
        derivedStateOf { products.associate { it.id to it.stock } }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is PosUiEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val launchScanner = rememberBarcodeScanner(onScanned = viewModel::onBarcodeScanned)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 6.dp)
            ) {
                ShiftIndicatorBar(
                    openShift = openShift,
                    isOpeningDrawer = isOpeningDrawer,
                    onClick = {
                        val shift = openShift
                        if (shift == null) viewModel.openStartShiftDialog()
                        else viewModel.openEndShiftDialog(shift)
                    },
                    onManageClick = viewModel::openShiftListDialog,
                    onOpenDrawerClick = viewModel::openCashDrawerManually
                )
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactSearchBar(
                        query = query,
                        onQueryChange = viewModel::search,
                        modifier = Modifier.weight(1f).height(36.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = launchScanner,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Scan")
                    }
                }

                if (categories.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    CategoryChipsRow(
                        categories = categories,
                        selected = selectedCategory,
                        onSelect = viewModel::selectCategory
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars)
    ) { inner ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
        ) {
            val isWide = forceWideLayout || maxWidth >= 840.dp
            val maxH = maxHeight

            val density = LocalDensity.current
            val imeVisible = WindowInsets.ime.getBottom(density) > 0

            if (isWide) {
                Row(Modifier.fillMaxSize()) {
                    ProductPane(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        products = products,
                        cartQtyByProductId = cartQtyByProductId,
                        onAdd = viewModel::addToCart
                    )
                    Spacer(Modifier.width(12.dp))
                    CartPane(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 320.dp, max = 420.dp),
                        cart = cart,
                        totals = totals,
                        discountType = discountType,
                        discountValue = discountValue,
                        taxRate = taxRate,
                        paid = paid,
                        change = change,
                        paymentMethod = paymentMethod,
                        stockByProductId = stockByProductId,
                        onDiscountTypeToggle = viewModel::toggleDiscountType,
                        onDiscountValueChange = viewModel::setDiscountValue,
                        onTaxRateChange = viewModel::setTaxRate,
                        onPaidChange = viewModel::setPaid,
                        onPaymentMethodChange = viewModel::setPaymentMethod,
                        onSetQuantity = viewModel::setQuantityDirect,
                        onIncrease = viewModel::increaseQty,
                        onDecrease = viewModel::decreaseQty,
                        onRemove = viewModel::removeFromCart,
                        onClear = viewModel::clearCart,
                        onCheckout = viewModel::checkout,
                        canCheckout = !isCartEmpty && !isProcessing,
                        isProcessing = isProcessing
                    )
                }
            } else {
                Box(Modifier.fillMaxSize()) { // <--- FIX: Ubah Column menjadi Box agar bertumpuk
                    ProductPane(
                        modifier = Modifier.fillMaxSize(), // <--- FIX: Katalog produk memenuhi layar
                        products = products,
                        cartQtyByProductId = cartQtyByProductId,
                        onAdd = viewModel::addToCart
                    )
                    CartPane(
                        modifier = Modifier
                            .align(Alignment.BottomCenter) // <--- FIX: Keranjang diposisikan melayang di bawah
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .let { base ->
                                when {
                                    !isCartExpanded -> base
                                    imeVisible -> base
                                    else -> base.heightIn(max = maxH * 0.65f)
                                }
                            },
                            cart = cart,
                            totals = totals,
                            discountType = discountType,
                            discountValue = discountValue,
                            taxRate = taxRate,
                            paid = paid,
                            change = change,
                            paymentMethod = paymentMethod,
                            stockByProductId = stockByProductId,
                            onDiscountTypeToggle = viewModel::toggleDiscountType,
                            onDiscountValueChange = viewModel::setDiscountValue,
                            onTaxRateChange = viewModel::setTaxRate,
                            onPaidChange = viewModel::setPaid,
                            onPaymentMethodChange = viewModel::setPaymentMethod,
                            onSetQuantity = viewModel::setQuantityDirect,
                            onIncrease = viewModel::increaseQty,
                            onDecrease = viewModel::decreaseQty,
                            onRemove = viewModel::removeFromCart,
                            onClear = viewModel::clearCart,
                            onCheckout = viewModel::checkout,
                            canCheckout = !isCartEmpty && !isProcessing,
                            isProcessing = isProcessing,
                            collapsible = true,
                            expanded = isCartExpanded,
                            onToggleExpand = { onCartExpandedChange(!isCartExpanded) }
                        )
                    }
                }
            }
        }

    when (val state = checkoutState) {
        is CheckoutState.Success -> SuccessDialog(
            result = state.result,
            printUiState = printUiState.forTransaction(state.result.transaction.id),
            openDrawerOnPrint = openDrawerOnPrint,
            onToggleOpenDrawer = viewModel::toggleOpenDrawerOnPrint,
            onPrint = { viewModel.printReceipt(state.result) },
            onExport = { onExportPdf(state.result) },
            onSharePdfFile = onSharePdfFile,
            onNavigateToSettings = onNavigateToSettings,
            onDismiss = viewModel::resetCheckoutState
        )
        is CheckoutState.Error -> AlertDialog(
            onDismissRequest = viewModel::resetCheckoutState,
            confirmButton = { TextButton(onClick = viewModel::resetCheckoutState) { Text("Tutup") } },
            title = { Text("Transaksi Gagal") },
            text = { Text(state.message) }
        )
        else -> Unit
    }

    if (showStartShiftDialog) {
        StartShiftDialog(
            cashiers = activeCashiers,
            onDismiss = viewModel::dismissStartShiftDialog,
            onConfirm = { cashierId, startingCash -> viewModel.startShift(cashierId, startingCash) }
        )
    }

    shiftSummary?.let { summary ->
        if (showEndShiftDialog) {
            EndShiftDialog(
                summary = summary,
                onDismiss = viewModel::dismissEndShiftDialog,
                onConfirm = { actualCash -> viewModel.endShift(actualCash) }
            )
        }
    }
    if (showShiftListDialog) {
        ManageShiftsDialog(
            shifts = openShifts,
            activeShiftId = openShift?.id,
            onSelectShift = { shift ->
                viewModel.dismissShiftListDialog()
                viewModel.openEndShiftDialog(shift)
            },
            onStartNewShift = {
                viewModel.dismissShiftListDialog()
                viewModel.openStartShiftDialog()
            },
            onDismiss = viewModel::dismissShiftListDialog
        )
    }
}


@Composable
private fun ShiftIndicatorBar(
    openShift: ShiftEntity?,
    isOpeningDrawer: Boolean,
    onClick: () -> Unit,
    onManageClick: () -> Unit,
    onOpenDrawerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (openShift != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (openShift != null) "${openShift.cashierName} · Shift Aktif"
                       else "Tanpa Shift · Ketuk untuk mulai",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (openShift != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(enabled = !isOpeningDrawer, onClick = onOpenDrawerClick),
            contentAlignment = Alignment.Center
        ) {
            if (isOpeningDrawer) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Icon(
                    Icons.Rounded.PointOfSale,
                    contentDescription = "Buka laci kasir",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.width(2.dp))

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onManageClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = "Kelola semua shift",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
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
private fun ManageShiftsDialog(
    shifts: List<ShiftEntity>,
    activeShiftId: Long?,
    onSelectShift: (ShiftEntity) -> Unit,
    onStartNewShift: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kelola Shift") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (shifts.isEmpty()) {
                    Text(
                        "Tidak ada shift yang sedang berjalan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        "Ketuk shift untuk menutupnya. Semua kasir bisa menutup shift siapa pun.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    shifts.forEach { shift ->
                        OpenShiftRow(
                            shift = shift,
                            isDesignatedActive = shift.id == activeShiftId,
                            onClick = { onSelectShift(shift) }
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
        }
    )
}

@Composable
private fun OpenShiftRow(
    shift: ShiftEntity,
    isDesignatedActive: Boolean,
    onClick: () -> Unit
) {
    val elapsed = remember(shift.id, shift.startedAt) { formatElapsedSince(shift.startedAt) }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        contentPadding = PaddingValues(10.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shift.cashierName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isDesignatedActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "SAAT INI",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    "Mulai: ${shiftDateFmt.format(Date(shift.startedAt))} · $elapsed",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "Kas awal: ${shift.startingCash.toRupiah()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Tutup shift ini",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun CashierDropdownField(
    cashiers: List<CashierEntity>,
    selected: CashierEntity?,
    onSelect: (CashierEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected?.name ?: "Pilih kasir",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cashiers.forEach { cashier ->
                DropdownMenuItem(
                    text = { Text(cashier.name) },
                    onClick = {
                        onSelect(cashier)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StartShiftDialog(
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onConfirm: (cashierId: Long, startingCash: Long) -> Unit
) {
    var selectedCashier by remember(cashiers) { mutableStateOf(cashiers.firstOrNull()) }
    var startingCash by remember { mutableStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mulai Shift") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (cashiers.isEmpty()) {
                    Text(
                        "Belum ada kasir terdaftar. Tambahkan kasir dulu di tab Pengaturan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    CashierDropdownField(
                        cashiers = cashiers,
                        selected = selectedCashier,
                        onSelect = { selectedCashier = it }
                    )
                    MoneyField(
                        label = "Kas Awal",
                        value = startingCash,
                        onValueChange = { startingCash = it },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedCashier?.let { onConfirm(it.id, startingCash) } },
                enabled = selectedCashier != null
            ) {
                Text("Mulai Shift")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun EndShiftDialog(
    summary: ShiftSummary,
    onDismiss: () -> Unit,
    onConfirm: (actualCash: Long) -> Unit
) {
    var actualCash by remember { mutableStateOf(0L) }
    val expected = summary.expectedCashInDrawer
    val difference = actualCash - expected
    val hasInput = actualCash > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tutup Shift") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                        color = MaterialTheme.colorScheme.error
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                SummaryLine("Estimasi di Laci", expected.toRupiah(), emphasize = true)

                Spacer(Modifier.height(10.dp))
                MoneyField(
                    label = "Uang Fisik",
                    value = actualCash,
                    onValueChange = { actualCash = it },
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )

                if (hasInput) {
                    Spacer(Modifier.height(10.dp))
                    val diffAbs = kotlin.math.abs(difference)
                    val diffColor = if (difference < 0L) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.primary
                    val diffLabel = when {
                        difference == 0L -> "Pas ✓"
                        difference < 0L -> "-${diffAbs.toRupiah()} (Uang Kurang)"
                        else -> "+${diffAbs.toRupiah()} (Uang Lebih)"
                    }
                    Text("💡 Hasil", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    SummaryLine("Selisih", diffLabel, emphasize = true, color = diffColor)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(actualCash) },
                enabled = hasInput
            ) { Text("Tutup Shift") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}


@Composable
private fun ProductPane(
    modifier: Modifier,
    products: List<ProductEntity>,
    cartQtyByProductId: Map<Long, Int>,
    onAdd: (ProductEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(
            items = products,
            key = { it.id },
            contentType = { "product" }
        ) { product ->
            val qtyInCart = cartQtyByProductId[product.id] ?: 0
            val remainingStock = product.stock - qtyInCart
            ProductCard(
                product = product,
                remainingStock = remainingStock,
                onAdd = { onAdd(product) }
            )
        }
    }
}

@Composable
private fun ProductCard(product: ProductEntity, remainingStock: Int, onAdd: () -> Unit) {
    val outOfStock = remainingStock <= 0
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(6.dp),
        onClick = onAdd
    ) {
        Column {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.sku,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            if (product.category.isNotBlank()) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = product.price.toRupiah(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (outOfStock) "Habis" else "Stok: $remainingStock",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (outOfStock) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (outOfStock) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Tambah ${product.name}",
                        tint = if (outOfStock) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


@Composable
@Suppress("LongParameterList")
private fun CartPane(
    modifier: Modifier,
    cart: List<CartItemEntity>,
    totals: Totals,
    discountType: DiscountType,
    discountValue: Double,
    taxRate: Double,
    paid: Long,
    change: Long,
    paymentMethod: PaymentMethod,
    stockByProductId: Map<Long, Int>,
    onDiscountTypeToggle: () -> Unit,
    onDiscountValueChange: (Double) -> Unit,
    onTaxRateChange: (Double) -> Unit,
    onPaidChange: (Long) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onIncrease: (CartItemEntity) -> Unit,
    onDecrease: (CartItemEntity) -> Unit,
    onSetQuantity: (CartItemEntity, Int) -> Unit,
    onRemove: (CartItemEntity) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    canCheckout: Boolean,
    isProcessing: Boolean,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onToggleExpand: () -> Unit = {}
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var qtyEditItem by remember { mutableStateOf<CartItemEntity?>(null) }
    val showFull = !collapsible || expanded
    var showInsufficientPaymentDialog by remember { mutableStateOf(false) }
    
    fun attemptCheckout() {
        if (paid in 1 until totals.total) {
            showInsufficientPaymentDialog = true
        } else {
            onCheckout()
        }
    }

    val listState = rememberLazyListState()

    var previousCartSize by remember { mutableStateOf(cart.size) }
    LaunchedEffect(cart.size) {
        if (cart.size > previousCartSize && cart.isNotEmpty()) {
            listState.animateScrollToItem(cart.lastIndex)
        }
        previousCartSize = cart.size
    }

    Box(
        modifier = modifier
            .padding(start = 12.dp, end = 12.dp, top = 1.dp, bottom = 1.dp)
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)) // <--- FIX: Background solid agar menutupi produk di belakangnya
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)) // <--- FIX: Memastikan sudut tetap melengkung rapi
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!collapsible) Modifier.fillMaxHeight() else Modifier)
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Keranjang", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

                if (collapsible && !expanded && cart.isNotEmpty()) {
                    Text(
                        text = "${cart.size} item",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                if (cart.isNotEmpty() && showFull) {
                    TextButton(
                        onClick = { showClearConfirm = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kosongkan", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (collapsible) {
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown
                            else Icons.Rounded.KeyboardArrowUp,
                            contentDescription = if (expanded) "Ciutkan keranjang" else "Perluas keranjang"
                        )
                    }
                }
            }

            if (showFull) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                
                // 1. AREA LIST ITEM (Scrollable)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false) // fill=false agar keranjang tidak memakan ruang kosong jika item sedikit
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (cart.isEmpty()) {
                            item(key = "empty_cart") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Rounded.ShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Keranjang masih kosong",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            "Ketuk produk di atas untuk menambahkan",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = cart,
                                key = { it.id }
                            ) { item ->
                                val stock = stockByProductId[item.productId]
                                val canIncrease = stock == null || item.quantity < stock
                                CartRow(
                                    item = item,
                                    canIncrease = canIncrease,
                                    onIncrease = { onIncrease(item) },
                                    onDecrease = { onDecrease(item) },
                                    onRemove = { onRemove(item) },
                                    onQuantityClick = { qtyEditItem = item },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                    
                    val showTopFade by remember { derivedStateOf { listState.canScrollBackward } }
                    val showBottomFade by remember { derivedStateOf { listState.canScrollForward } }
                    if (showTopFade) {
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    if (showBottomFade) {
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                }

                // 2. AREA TOTAL & CHECKOUT (Fixed di bawah, tidak ikut ter-scroll)
                if (cart.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    TotalsSummary(
                        totals = totals,
                        change = change,
                        discountType = discountType,
                        discountValue = discountValue,
                        taxRate = taxRate,
                        paid = paid,
                        paymentMethod = paymentMethod,
                        onDiscountTypeToggle = onDiscountTypeToggle,
                        onDiscountValueChange = onDiscountValueChange,
                        onTaxRateChange = onTaxRateChange,
                        onPaidChange = onPaidChange,
                        onPaymentMethodChange = onPaymentMethodChange
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = ::attemptCheckout,
                        enabled = canCheckout,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bayar · ${totals.total.toRupiah()}")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 84.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Total: ${totals.total.toRupiah()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = ::attemptCheckout,
                        enabled = canCheckout,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Bayar", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Kosongkan Keranjang?") },
            text = { Text("Semua item di keranjang akan dihapus. Tindakan ini tidak bisa dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        showClearConfirm = false
                    }
                ) {
                    Text("Ya, Kosongkan", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Batal") }
            }
        )
    }

    qtyEditItem?.let { item ->
        QuantityEditDialog(
            item = item,
            maxStock = stockByProductId[item.productId],
            onConfirm = { newQty ->
                onSetQuantity(item, newQty)
                qtyEditItem = null
            },
            onDismiss = { qtyEditItem = null }
        )
    }

    if (showInsufficientPaymentDialog) {
        InsufficientPaymentDialog(
            paid = paid,
            total = totals.total,
            onDismiss = { showInsufficientPaymentDialog = false },
            onConfirm = {
                showInsufficientPaymentDialog = false
                onCheckout()
            }
        )
    }
}

@Composable
private fun InsufficientPaymentDialog(
    paid: Long,
    total: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val shortfall = (total - paid).coerceAtLeast(0L)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Pembayaran Kurang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Uang yang diterima (${paid.toRupiah()}) kurang dari total (${total.toRupiah()}).")
                Text(
                    "Kekurangan: ${shortfall.toRupiah()}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pastikan ini disengaja (mis. dicatat sebagai piutang), bukan salah ketik.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Tetap Lanjutkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Periksa Lagi") }
        }
    )
}

@Composable
private fun CartRow(
    item: CartItemEntity,
    canIncrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onQuantityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.unitPrice.toRupiah()} × ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Text(
            item.lineTotal.toRupiah(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(12.dp))

        QuantityStepper(
            qty = item.quantity,
            canIncrease = canIncrease,
            onDecrease = onDecrease,
            onIncrease = onIncrease,
            onQuantityClick = onQuantityClick
        )

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Hapus",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun QuantityStepper(
    qty: Int,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onQuantityClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompactActionBox(icon = Icons.Rounded.Remove, contentDescription = "Kurangi", onClick = onDecrease)

        Box(
            modifier = Modifier
                .width(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onQuantityClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$qty",
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
            )
        }

        CompactActionBox(
            icon = Icons.Rounded.Add,
            contentDescription = "Tambah",
            dimmed = !canIncrease,
            onClick = onIncrease
        )
    }
}

@Composable
private fun CompactActionBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    dimmed: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (dimmed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dimmed) 0.4f else 1f)
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun TotalsSummary(
    totals: Totals,
    change: Long,
    discountType: DiscountType,
    discountValue: Double,
    taxRate: Double,
    paid: Long,
    paymentMethod: PaymentMethod,
    onDiscountTypeToggle: () -> Unit,
    onDiscountValueChange: (Double) -> Unit,
    onTaxRateChange: (Double) -> Unit,
    onPaidChange: (Long) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SummaryLine("Subtotal", totals.subtotal.toRupiah())

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiscountField(
                type = discountType,
                rawValue = discountValue,
                onToggleType = onDiscountTypeToggle,
                onValueChange = onDiscountValueChange,
                modifier = Modifier.weight(1f).height(40.dp)
            )
            DecimalField(
                label = "Pajak (%)",
                value = taxRate * 100.0,
                onValueChange = { pct -> onTaxRateChange((pct / 100.0).coerceIn(0.0, 100.0)) },
                modifier = Modifier.weight(1f).height(40.dp)
            )
        }
        if (totals.discountCapped) {
            Text(
                text = "⚠ Diskon melebihi subtotal, dibatasi menjadi ${totals.discount.toRupiah()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (totals.discount > 0) {
            val label = if (discountType == DiscountType.PERCENT) {
                "Diskon (${formatPercentTrim(discountValue)}%)"
            } else {
                "Diskon"
            }
            SummaryLine(label, "- ${totals.discount.toRupiah()}")
        }
        if (totals.tax > 0) SummaryLine("Pajak", totals.tax.toRupiah())

        HorizontalDivider(Modifier.padding(vertical = 2.dp))
        SummaryLine("Total", totals.total.toRupiah(), emphasize = true)

        Spacer(Modifier.height(2.dp))
        PaymentMethodToggle(
            selected = paymentMethod,
            onSelect = onPaymentMethodChange
        )
        Spacer(Modifier.height(2.dp))

        MoneyField(
            label = "Bayar",
            value = paid,
            onValueChange = onPaidChange,
            modifier = Modifier.fillMaxWidth().height(44.dp)
        )
        if (paid > 0) SummaryLine("Kembalian", change.toRupiah(), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PaymentMethodToggle(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        PaymentMethodChip(
            label = "Tunai",
            selected = selected == PaymentMethod.CASH,
            onClick = { onSelect(PaymentMethod.CASH) },
            modifier = Modifier.weight(1f)
        )
        PaymentMethodChip(
            label = "QRIS",
            selected = selected == PaymentMethod.QRIS,
            onClick = { onSelect(PaymentMethod.QRIS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String, emphasize: Boolean = false, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = color
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

@Composable
private fun MoneyField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(if (value <= 0) "" else value.toString()) }

    BasicTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            text = digits
            onValueChange(digits.toLongOrNull() ?: 0L)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        visualTransformation = ThousandsSeparatorTransformation,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun DiscountField(
    type: DiscountType,
    rawValue: Double,
    onToggleType: () -> Unit,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(type, rawValue) {
        mutableStateOf(
            when {
                rawValue <= 0.0 -> ""
                type == DiscountType.NOMINAL -> rawValue.toLong().toString()
                else -> formatPercentTrim(rawValue)
            }
        )
    }

    val isNominal = type == DiscountType.NOMINAL
    val keyboardType = if (isNominal) KeyboardType.Number else KeyboardType.Decimal
    val visualTransformation = if (isNominal) ThousandsSeparatorTransformation else VisualTransformation.None

    BasicTextField(
        value = text,
        onValueChange = { input ->
            val cleaned = if (isNominal) {
                input.filter { it.isDigit() }
            } else {
                buildString {
                    var dotSeen = false
                    for (c in input) {
                        when {
                            c.isDigit() -> append(c)
                            c == '.' && !dotSeen -> { append(c); dotSeen = true }
                        }
                    }
                }
            }
            text = cleaned
            onValueChange(cleaned.toDoubleOrNull() ?: 0.0)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .clickable(onClick = onToggleType)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNominal) "Rp" else "%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "⟲",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.6f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun DecimalField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) {
        mutableStateOf(if (value <= 0.0) "" else formatPercentTrim(value))
    }

    BasicTextField(
        value = text,
        onValueChange = { input ->
            val cleaned = buildString {
                var dotSeen = false
                for (c in input) {
                    when {
                        c.isDigit() -> append(c)
                        c == '.' && !dotSeen -> { append(c); dotSeen = true }
                    }
                }
            }
            text = cleaned
            onValueChange(cleaned.toDoubleOrNull() ?: 0.0)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}


@Composable
private fun QuantityEditDialog(
    item: CartItemEntity,
    maxStock: Int?,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var fieldValue by remember {
        val initial = item.quantity.toString()
        mutableStateOf(TextFieldValue(text = initial, selection = TextRange(0, initial.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun confirmWith(qty: Int) = onConfirm(qty.coerceAtLeast(0))

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
                    overflow = TextOverflow.Ellipsis
                )
                if (maxStock != null) {
                    Text(
                        "Stok tersedia: $maxStock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.height(12.dp))

                BasicTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }.take(5)
                        fieldValue = newValue.copy(text = digits)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { confirmWith(fieldValue.text.toIntOrNull() ?: item.quantity) }
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(vertical = 14.dp),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            innerTextField()
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Pintasan cepat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(10, 20, 50, 100).forEach { shortcut ->
                        FilledTonalButton(
                            onClick = { confirmWith(shortcut) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("$shortcut", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirmWith(fieldValue.text.toIntOrNull() ?: item.quantity) }) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun SuccessDialog(
    result: CheckoutResult,
    printUiState: PrintUiState,
    openDrawerOnPrint: Boolean,
    onToggleOpenDrawer: (Boolean) -> Unit,
    onPrint: () -> Unit,
    onExport: () -> Unit,
    onSharePdfFile: (File) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Selesai")
            }
        },
        title = { Text("Transaksi Berhasil ✓") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No. Struk: ${result.transaction.id}")
                    Text(
                        "Metode Bayar: ${paymentMethodLabel(result.transaction.paymentMethod)}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    result.transaction.discountInlineLabel()?.let { label ->
                        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Text("Total: ${result.transaction.total.toRupiah()}", fontWeight = FontWeight.SemiBold)
                    Text("Bayar: ${result.transaction.paidAmount.toRupiah()}")
                    Text("Kembali: ${result.transaction.change.toRupiah()}", color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleOpenDrawer(!openDrawerOnPrint) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = openDrawerOnPrint,
                            onCheckedChange = onToggleOpenDrawer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buka laci saat mencetak", style = MaterialTheme.typography.bodyMedium)
                    }

                    FilledTonalButton(
                        onClick = onPrint,
                        enabled = printUiState !is PrintUiState.Printing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (printUiState is PrintUiState.Printing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Mencetak...")
                        } else {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
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
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        }
    )
}

@Composable
private fun PrintResultBanner(
    printUiState: PrintUiState,
    onSharePdfFile: (File) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state = printUiState as? PrintUiState.Result ?: return
    val outcome = state.outcome
    val (message, isError) = when (outcome) {
        is ReceiptPrintOutcome.Success -> "Struk terkirim ke \"${outcome.printer.label}\"." to false
        is ReceiptPrintOutcome.Failed -> "Gagal mencetak ke semua printer." to true
        ReceiptPrintOutcome.NoPrinterConfigured -> "Printer belum diatur." to true
        ReceiptPrintOutcome.AlreadyInProgress -> "Sedang mencetak, mohon tunggu..." to false
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                message,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
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
private fun CompactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Cari produk/SKU…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item(key = "__all__") {
            CategoryChip(label = "Semua", selected = selected == null, onClick = { onSelect(null) })
        }
        items(items = categories, key = { it }) { cat ->
            CategoryChip(label = cat, selected = selected == cat, onClick = { onSelect(cat) })
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}