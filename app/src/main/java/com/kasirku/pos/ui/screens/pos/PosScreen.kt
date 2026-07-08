package com.kasirku.pos.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasirku.pos.ui.components.CartItemRow
import com.kasirku.pos.ui.components.ProductCard
import com.kasirku.pos.ui.components.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel = hiltViewModel(),
    onNavigateToReceipt: (Long) -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val taxPercent by viewModel.taxPercent.collectAsState()
    val isCheckoutLoading by viewModel.isCheckoutLoading.collectAsState()

    val taxAmount = remember(cartSubtotal, taxPercent) {
        (cartSubtotal * taxPercent / 100.0).toLong()
    }
    val grandTotal = remember(cartSubtotal, taxAmount) { cartSubtotal + taxAmount }

    val snackbarHostState = remember { SnackbarHostState() }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var paymentInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is PosUiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
                is PosUiEvent.ShowError -> snackbarHostState.showSnackbar("❌ \${event.message}")
                is PosUiEvent.CheckoutSuccess -> {
                    snackbarHostState.showSnackbar("✅ Transaksi berhasil!")
                    onNavigateToReceipt(event.transactionId)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🏪 KasirKu POS", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            // Product Grid
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Cari produk atau SKU...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", style = MaterialTheme.typography.displayLarge)
                            Text("Belum ada produk", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = products, key = { it.id }) { product ->
                            ProductCard(product = product, onAddToCart = viewModel::addToCart)
                        }
                    }
                }
            }

            // Cart Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgedBox(badge = { if (cartItemCount > 0) Badge { Text("\$cartItemCount") } }) {
                            Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Keranjang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (cartItems.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearCart) {
                            Icon(Icons.Default.DeleteSweep, "Kosongkan", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (cartItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Keranjang kosong", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(items = cartItems, key = { it.id }) { item ->
                            CartItemRow(
                                item = item,
                                onIncrement = { viewModel.updateCartItemQuantity(item, item.quantity + 1) },
                                onDecrement = { viewModel.updateCartItemQuantity(item, item.quantity - 1) },
                                onRemove = { viewModel.removeCartItem(item.id) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupiah(cartSubtotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }

                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pajak (\${taxPercent.toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupiah(taxAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text(formatRupiah(grandTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showCheckoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = cartItems.isNotEmpty() && !isCheckoutLoading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Receipt, null, modifier = Modifier.padding(end = 8.dp))
                    Text(if (isCheckoutLoading) "Memproses..." else "CHECKOUT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Checkout Dialog
    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false; paymentInput = "" },
            title = { Text("💳 Pembayaran", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Total: \${formatRupiah(grandTotal)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = paymentInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) paymentInput = it },
                        label = { Text("Jumlah Bayar (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val payment = paymentInput.toLongOrNull() ?: 0L
                    val change = payment - grandTotal
                    if (payment > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (change >= 0) "Kembalian: \${formatRupiah(change)}" else "Kurang: \${formatRupiah(-change)}",
                            color = if (change >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                val payment = paymentInput.toLongOrNull() ?: 0L
                Button(
                    onClick = {
                        viewModel.checkout(payment)
                        showCheckoutDialog = false
                        paymentInput = ""
                    },
                    enabled = payment >= grandTotal
                ) { Text("Bayar") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false; paymentInput = "" }) { Text("Batal") }
            }
        )
    }
}
