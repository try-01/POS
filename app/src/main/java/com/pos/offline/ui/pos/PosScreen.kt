package com.pos.offline.ui.pos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.ui.components.rememberBarcodeScanner
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToSettings: () -> Unit,
    onSharePdfFile: (File) -> Unit,
    onExportPdf: (CheckoutResult) -> Unit,
    forceWideLayout: Boolean = false,
    isCartExpanded: Boolean = false,
    onCartExpandedChange: (Boolean) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val localState = rememberPosLocalState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(isCartExpanded) {
        if (localState.isCartExpanded != isCartExpanded) {
            localState.setCartExpanded(isCartExpanded)
        }
    }

    LaunchedEffect(localState.isCartExpanded) {
        if (localState.isCartExpanded != isCartExpanded) {
            onCartExpandedChange(localState.isCartExpanded)
        }
    }

    // ── Snackbar event handler ────────────────────────────────────────────────
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    is PosUiEvent.ShowMessage -> snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    val launchScanner = rememberBarcodeScanner(onScanned = viewModel::onBarcodeScanned)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PosTopBar(
                uiState = uiState,
                onAction = viewModel::onAction,
                launchScanner = launchScanner,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { inner ->
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            val isWide = forceWideLayout || maxWidth >= 840.dp
            val maxH = maxHeight
            val density = LocalDensity.current
            val imeVisible = WindowInsets.ime.getBottom(density) > 0

            if (isWide) {
                // Wide layout: product + cart side by side
                Row(Modifier.fillMaxSize()) {
                    ProductPane(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        products = uiState.catalog.products,
                        cartQtyByProductId = uiState.catalog.cartQtyByProductId,
                        onAdd = { viewModel.onAction(PosAction.AddToCart(it)) },
                    )
                    Spacer(Modifier.width(12.dp))
                    CartPaneContent(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 320.dp, max = 420.dp),
                        cart = uiState.cart,
                        payment = uiState.payment,
                        catalog = uiState.catalog,
                        checkout = uiState.checkout,
                        localState = localState,
                        onAction = viewModel::onAction,
                        collapsible = false,
                    )
                }
            } else {
                // Narrow layout: product behind, cart collapsible at bottom
                Box(Modifier.fillMaxSize()) {
                    ProductPane(
                        modifier = Modifier.fillMaxSize(),
                        products = uiState.catalog.products,
                        cartQtyByProductId = uiState.catalog.cartQtyByProductId,
                        onAdd = { viewModel.onAction(PosAction.AddToCart(it)) },
                    )
                    CartPaneContent(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .let { base ->
                                when {
                                    !localState.isCartExpanded -> base
                                    imeVisible -> base
                                    else -> base.heightIn(max = maxH * 0.65f)
                                }
                            },
                        cart = uiState.cart,
                        payment = uiState.payment,
                        catalog = uiState.catalog,
                        checkout = uiState.checkout,
                        localState = localState,
                        onAction = viewModel::onAction,
                        collapsible = true,
                    )
                }
            }
        }
    }

    // ── All dialogs managed here ──────────────────────────────────────────────
    PosDialogManager(
        uiState = uiState,
        localState = localState,
        onAction = viewModel::onAction,
        onSharePdfFile = onSharePdfFile,
        onExportPdf = onExportPdf,
        onNavigateToSettings = onNavigateToSettings,
    )
}

// ─── Top bar (extracted untuk keterbacaan) ────────────────────────────────────

@Composable
private fun PosTopBar(
    uiState: PosUiState,
    onAction: (PosAction) -> Unit,
    launchScanner: () -> Unit,
) {
    val shift = uiState.shift
    val catalog = uiState.catalog

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 6.dp),
    ) {
        ShiftIndicatorBar(
            openShift = shift.activeShift,
            isOpeningDrawer = shift.isOpeningDrawer,
            onClick = {
                val currentActiveShift = shift.activeShift
                when {
                    currentActiveShift != null ->
                        onAction(PosAction.OpenEndShiftDialog(currentActiveShift))
                    shift.openShifts.isEmpty() ->
                        onAction(PosAction.OpenStartShiftDialog)
                    else ->
                        onAction(PosAction.OpenShiftListDialog)
                }
            },
            onManageClick = { onAction(PosAction.OpenShiftListDialog) },
            onOpenDrawerClick = { onAction(PosAction.OpenCashDrawer) },
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactSearchBar(
                query = catalog.searchQuery,
                onQueryChange = { onAction(PosAction.Search(it)) },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = launchScanner,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = "Scan Barcode",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Scan")
            }
        }
        if (catalog.categories.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            CategoryChipsRow(
                categories = catalog.categories,
                selected = catalog.selectedCategory,
                onSelect = { onAction(PosAction.SelectCategory(it)) },
            )
        }
    }
}