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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.ui.components.GlassCard
import com.pos.offline.util.toRupiah

/**
 * Layar Kasir utama. Layout responsif: tablet/landscape → katalog + keranjang
 * berdampingan; ponsel → tumpuk vertikal dengan keranjang yang bisa
 * di-collapse/expand agar katalog dapat ruang maksimal saat tidak dibutuhkan.
 *
 * Performa:
 *  - Semua state dikumpulkan dengan [collectAsStateWithLifecycle] (berhenti saat background).
 *  - [derivedStateOf] untuk nilai turunan → recompose terbatas.
 *  - Lazy list/grid memakai `key` + `contentType` → daur ulang slot optimal.
 *
 * Guard stok dua lapis:
 *  - UI: tombol tambah otomatis nonaktif saat stok sisa habis (feedback instan).
 *  - ViewModel: validasi ulang ke DB sebelum menulis (jaga konsistensi akhir),
 *    mengirim [PosUiEvent] via SharedFlow bila ditolak → ditampilkan sebagai Snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onPrintBluetooth: (CheckoutResult) -> Unit,
    onExportPdf: (CheckoutResult) -> Unit
) {
    // ---- State reaktif (sadarkan-siklus) ----
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val discount by viewModel.discount.collectAsStateWithLifecycle()
    val taxRate by viewModel.taxRate.collectAsStateWithLifecycle()
    val paid by viewModel.paid.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()

    // Nilai turunan: hanya recompute saat dependency berubah.
    val isCartEmpty by remember { derivedStateOf { cart.isEmpty() } }
    val isProcessing by remember { derivedStateOf { checkoutState is CheckoutState.Processing } }
    val change by remember(paid, totals) {
        derivedStateOf { (paid - totals.total).coerceAtLeast(0L) }
    }

    // Peta bantu untuk guard stok di sisi UI (lihat catatan keterbatasan di kelas doc).
    val cartQtyByProductId by remember(cart) {
        derivedStateOf { cart.associate { it.productId to it.quantity } }
    }
    val stockByProductId by remember(products) {
        derivedStateOf { products.associate { it.id to it.stock } }
    }

    // ---- Snackbar untuk event sekali-jalan dari ViewModel (mis. stok kurang) ----
    val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(viewModel) {
    viewModel.uiEvents.collect { event ->
        when (event) {
            is PosUiEvent.ShowMessage -> snackbarHostState.showSnackbar(
                message = event.message,
                duration = SnackbarDuration.Short // eksplisit: pesan stok harus singkat, tidak mengganggu alur kasir
            )
        }
    }
}

    // ---- State collapse/expand keranjang (khusus layout ponsel) ----
    var cartExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(isCartEmpty) {
        // Auto-expand begitu ada isi; auto-collapse begitu keranjang kosong.
        cartExpanded = !isCartEmpty
    }

    Scaffold(
        modifier = Modifier.imePadding(), // Cegah keyboard menutupi field Bayar/Diskon.
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 6.dp)
            ) {
                CompactSearchBar(
                    query = query,
                    onQueryChange = viewModel::search,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            }
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { inner ->
        BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(inner)
    ) {
        val isWide = maxWidth >= 840.dp
        val maxH = maxHeight

        // Deteksi apakah keyboard sedang tampil, untuk melonggarkan batas tinggi CartPane saat mengetik.
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
                            .widthIn(min = 320.dp, max = 420.dp) // <-- GANTI dari .width(380.dp)
                            .fillMaxWidth(0.32f),                 // proporsional ~32% lebar layar, dibatasi min/max di atas
                        cart = cart,
                        totals = totals,
                        discount = discount,
                        taxRate = taxRate,
                        paid = paid,
                        change = change,
                        stockByProductId = stockByProductId,
                        onDiscountChange = viewModel::setDiscount,
                        onTaxRateChange = viewModel::setTaxRate,
                        onPaidChange = viewModel::setPaid,
                        onSetQuantity = viewModel::setQuantityDirect, // <-- TAMBAHKAN ini
                        onIncrease = viewModel::increaseQty,
                        onDecrease = viewModel::decreaseQty,
                        onRemove = viewModel::removeFromCart,
                        onClear = viewModel::clearCart,
                        onCheckout = viewModel::checkout,
                        canCheckout = !isCartEmpty && !isProcessing,
                        isProcessing = isProcessing
                        // Mode tablet/landscape: keranjang selalu terbuka penuh (tidak collapsible).
                    )
                }
            } else {
            Column(Modifier.fillMaxSize()) {
                ProductPane(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    products = products,
                    cartQtyByProductId = cartQtyByProductId,
                    onAdd = viewModel::addToCart
                )
                Spacer(Modifier.height(8.dp))
                CartPane(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .let { base ->
                            when {
                                !cartExpanded -> base
                                // Keyboard aktif: JANGAN batasi 65% — biar TotalsSummary
                                // & tombol Bayar selalu punya ruang untuk muncul penuh
                                // di atas keyboard, bukan tersembunyi di baliknya.
                                imeVisible -> base
                                else -> base.heightIn(max = maxH * 0.65f)
                                }
                        },
                        cart = cart,
                        totals = totals,
                        discount = discount,
                        taxRate = taxRate,
                        paid = paid,
                        change = change,
                        stockByProductId = stockByProductId,
                        onDiscountChange = viewModel::setDiscount,
                        onTaxRateChange = viewModel::setTaxRate,
                        onPaidChange = viewModel::setPaid,
                        onSetQuantity = viewModel::setQuantityDirect, // <-- TAMBAHKAN ini
                        onIncrease = viewModel::increaseQty,
                        onDecrease = viewModel::decreaseQty,
                        onRemove = viewModel::removeFromCart,
                        onClear = viewModel::clearCart,
                        onCheckout = viewModel::checkout,
                        canCheckout = !isCartEmpty && !isProcessing,
                        isProcessing = isProcessing,
                        collapsible = true,
                        expanded = cartExpanded,
                        onToggleExpand = { cartExpanded = !cartExpanded }
                    )
                }
            }
        }
    }

    // ---- Dialog hasil checkout ----
    when (val state = checkoutState) {
        is CheckoutState.Success -> SuccessDialog(
            result = state.result,
            onPrint = { onPrintBluetooth(state.result) },
            onExport = { onExportPdf(state.result) },
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
}

// ============================ PANEL KATALOG ============================

@Composable
private fun ProductPane(
    modifier: Modifier,
    products: List<ProductEntity>,
    cartQtyByProductId: Map<Long, Int>,
    onAdd: (ProductEntity) -> Unit
) {
    LazyVerticalGrid(
        // 104.dp dipilih agar 3 kolom muat di layar ~360dp (mis. device 1080x2460px @3x density).
        // Kalau di device lain kolom terasa kurang, coba turunkan sedikit lagi (mis. 96.dp).
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
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
                maxLines = 1, // ganti ke 2 kalau nama sering terpotong mengganggu
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.sku,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
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

// ============================ PANEL KERANJANG ============================

@Composable
@Suppress("LongParameterList")
private fun CartPane(
    modifier: Modifier,
    cart: List<CartItemEntity>,
    totals: Totals,
    discount: Long,
    taxRate: Double,
    paid: Long,
    change: Long,
    stockByProductId: Map<Long, Int>,
    onDiscountChange: (Long) -> Unit,
    onTaxRateChange: (Double) -> Unit,
    onPaidChange: (Long) -> Unit,
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

    Box(
        modifier = modifier
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // WAJIB: mode non-collapsible (landscape/tablet) harus punya tinggi
                // PASTI, jika tidak, weight() di dalam Column ini akan otomatis
                // dipaksa Compose menjadi 0px (bukan error, tapi "diam-diam gagal").
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

                if (cart.isEmpty()) {
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
                } else {
                    val cartListState = rememberLazyListState()
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = !collapsible)
                            // Cadangan tambahan: pastikan sekali lagi tinggi Box ini
                            // pasti (bukan wrap-content) saat mode non-collapsible.
                            .then(if (!collapsible) Modifier.fillMaxHeight() else Modifier)
                    ) {
                        LazyColumn(
                            state = cartListState,
                            modifier = Modifier.fillMaxSize(), // fillMaxSize, bukan fillMaxWidth saja
                            contentPadding = PaddingValues(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(cart, key = { it.id }, contentType = { "cart" }) { item ->
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

                        val showTopFade by remember { derivedStateOf { cartListState.canScrollBackward } }
                        val showBottomFade by remember { derivedStateOf { cartListState.canScrollForward } }
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
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                TotalsSummary(
                    totals = totals,
                    change = change,
                    discount = discount,
                    taxRate = taxRate,
                    paid = paid,
                    onDiscountChange = onDiscountChange,
                    onTaxRateChange = onTaxRateChange,
                    onPaidChange = onPaidChange
                )

                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onCheckout,
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
            } else {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        onClick = onCheckout,
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
}


@Composable
private fun CartRow(
    item: CartItemEntity,
    canIncrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onQuantityClick: () -> Unit, // <-- TAMBAHKAN parameter ini
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
            onQuantityClick = onQuantityClick // <-- teruskan
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
    onQuantityClick: () -> Unit // <-- TAMBAHKAN parameter ini
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompactActionBox(icon = Icons.Rounded.Remove, contentDescription = "Kurangi", onClick = onDecrease)

        // Angka qty sekarang bisa di-tap untuk input manual/pintasan angka besar.
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
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline // isyarat visual "bisa di-tap"
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
    dimmed: Boolean = false, // sebelumnya bernama "enabled" — sekarang murni untuk styling, bukan gating klik
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
            .clickable(onClick = onClick), // SELALU bisa ditekan — validasi ada di ViewModel
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
    discount: Long,
    taxRate: Double,
    paid: Long,
    onDiscountChange: (Long) -> Unit,
    onTaxRateChange: (Double) -> Unit,
    onPaidChange: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SummaryLine("Subtotal", totals.subtotal.toRupiah())

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MoneyField(
                label = "Diskon",
                value = discount,
                onValueChange = onDiscountChange,
                modifier = Modifier.weight(1f).height(40.dp)
            )
            DecimalField(
                label = "Pajak (%)",
                value = taxRate * 100.0,
                onValueChange = { pct -> onTaxRateChange((pct / 100.0).coerceIn(0.0, 100.0)) },
                modifier = Modifier.weight(1f).height(40.dp)
            )
        }

        if (totals.discount > 0) SummaryLine("Diskon", "- ${totals.discount.toRupiah()}")
        if (totals.tax > 0) SummaryLine("Pajak", totals.tax.toRupiah())

        HorizontalDivider(Modifier.padding(vertical = 2.dp))
        SummaryLine("Total", totals.total.toRupiah(), emphasize = true)

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

/**
 * VisualTransformation yang menampilkan separator ribuan ("100000" -> "100.000")
 * tanpa mengubah nilai asli yang disimpan di state.
 *
 * Pemetaan offset dibangun dari array (bukan rumus aritmatika manual) supaya
 * tidak ada celah off-by-one yang bisa menghasilkan indeks negatif/di luar
 * jangkauan — penyebab crash sebelumnya saat jumlah digit kelipatan 3.
 */
private object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val n = original.length
        if (n == 0) return TransformedText(text, OffsetMapping.Identity)

        // Bangun string berformat + peta original->transformed sekaligus.
        val sb = StringBuilder()
        val orig2Trans = IntArray(n + 1)
        orig2Trans[0] = 0
        for (i in original.indices) {
            sb.append(original[i])
            val remaining = n - i - 1
            if (remaining > 0 && remaining % 3 == 0) sb.append('.')
            orig2Trans[i + 1] = sb.length
        }
        val formatted = sb.toString()

        // Peta balik transformed->original, dibangun dengan menelusuri karakter hasil.
        val trans2Orig = IntArray(formatted.length + 1)
        var origIdx = 0
        trans2Orig[0] = 0
        for (t in 1..formatted.length) {
            if (formatted[t - 1] != '.') origIdx++
            trans2Orig[t] = origIdx.coerceAtMost(n)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                orig2Trans[offset.coerceIn(0, n)]

            override fun transformedToOriginal(offset: Int): Int =
                trans2Orig[offset.coerceIn(0, formatted.length)]
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/** Field angka uang (Long). Hanya menerima digit → mencegah parse error. */
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
private fun DecimalField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) {
        mutableStateOf(if (value <= 0.0) "" else formatTrim(value))
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

private fun formatTrim(d: Double): String {
    val i = d.toLong()
    return if (d == i.toDouble()) i.toString() else d.toString()
}

// ============================ DIALOG SUKSES ============================

/**
 * Dialog input jumlah manual. Menyediakan tombol pintasan angka umum
 * (10/20/50/100) agar kasir bisa set quantity besar hanya dengan 1 tap,
 * tanpa perlu mengetik atau menekan tombol "+" berulang kali.
 */
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

                // Input manual — teks otomatis ter-select semua saat dialog dibuka,
                // jadi kasir tinggal ketik angka baru untuk langsung menimpa.
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

                // Tombol pintasan: 1 tap langsung terapkan & tutup dialog.
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
    onPrint: () -> Unit,
    onExport: () -> Unit,
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
                    Text("Total: ${result.transaction.total.toRupiah()}", fontWeight = FontWeight.SemiBold)
                    Text("Bayar: ${result.transaction.paidAmount.toRupiah()}")
                    Text("Kembali: ${result.transaction.change.toRupiah()}", color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onPrint, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cetak Bluetooth")
                    }
                    OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                        Text("Ekspor PDF")
                    }
                }
            }
        }
    )
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