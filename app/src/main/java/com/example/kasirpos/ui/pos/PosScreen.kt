package com.example.kasirpos.ui.pos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kasirpos.data.local.dao.CartWithProduct
import com.example.kasirpos.data.local.entity.ProductEntity
import com.example.kasirpos.ui.theme.*

/**
 * Layar Kasir Utama — Point of Sale.
 *
 * Layout: 2 panel (desktop-style)
 *   - Kiri (65%): daftar produk + search bar
 *   - Kanan (35%): keranjang + ringkasan + tombol checkout
 *
 * Glassmorphism: diterapkan pada card dan panel — efek blur ringan
 * tanpa backend blur (karena itu mahal di Android). Cukup semi-transparan
 * background + border halus untuk ilusi glass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(viewModel: PosViewModel) {

    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Kumpulkan one-shot events (SharedFlow — cukup 1 collector)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PosEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is PosEvent.NavigateToReceipt -> { /* TODO: navigasi ke layar struk detail */ }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PrimaryDark
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Panel Kiri: Produk ────────────────────────────
            ProductPanel(
                state = state,
                onSearchChanged = viewModel::onSearchQueryChanged,
                onClearSearch = viewModel::clearSearch,
                onAddToCart = viewModel::addToCart,
                modifier = Modifier.weight(0.62f)
            )

            // ── Panel Kanan: Keranjang ───────────────────────
            CartPanel(
                state = state,
                onRemoveItem = viewModel::removeFromCart,
                onUpdateDiscount = viewModel::updateDiscount,
                onClearCart = viewModel::clearCart,
                onShowPayment = viewModel::showPaymentDialog,
                modifier = Modifier.weight(0.38f)
            )
        }
    }

    // ── Dialog Pembayaran ─────────────────────────────────────
    if (state.showPaymentDialog) {
        PaymentDialog(
            state = state,
            onTaxChanged = viewModel::setTaxPercentage,
            onCashChanged = viewModel::setCashReceived,
            onMethodChanged = viewModel::setPaymentMethod,
            onCheckout = viewModel::checkout,
            onDismiss = viewModel::dismissPaymentDialog
        )
    }

    // ── Dialog Struk ──────────────────────────────────────────
    if (state.showReceiptDialog && state.lastTransaction != null) {
        ReceiptDialog(
            transaction = state.lastTransaction!!,
            state = state,
            onDismiss = viewModel::dismissReceiptDialog
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PANEL PRODUK (KIRI)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProductPanel(
    state: PosUiState,
    onSearchChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .padding(12.dp)
    ) {
        // ── Header + Search ──────────────────────────────────
        Text(
            text = "📦 Katalog Produk",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))

        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nama / scan barcode...", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Filled.Search, "Cari", tint = AccentBlue)
            },
            trailingIcon = {
                if (state.searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Filled.Close, "Hapus", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = GlassBorder,
                cursorColor = AccentBlue,
                focusedContainerColor = CardDark,
                unfocusedContainerColor = CardDark
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(10.dp))

        // ── Hasil pencarian / Grid produk ────────────────────
        if (state.isSearching) {
            // Tampilkan hasil pencarian sebagai list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.searchResults, key = { it.id }) { product ->
                    SearchResultItem(product = product, onClick = { onAddToCart(product) })
                }
                if (state.searchResults.isEmpty()) {
                    item { EmptySearchResult() }
                }
            }
        } else {
            // Placeholder untuk daftar produk penuh — bisa diganti grid produk
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "🔍 Cari produk untuk memulai",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ketik nama produk atau scan barcode",
                        color = TextMuted.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    product: ProductEntity,
    onClick: () -> Unit
) {
    // Glass card untuk setiap hasil pencarian
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                product.name,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "SKU: ${product.sku}  •  Stok: ${product.stock}",
                color = if (product.stock <= 5) AccentRed else TextMuted,
                fontSize = 12.sp
            )
        }
        // Harga + tombol tambah
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "Rp ${"%,d".format(product.price)}",
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(4.dp))
            // Chip indikator (dekoratif — klik ditangani oleh Row parent)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.2f))
                    .border(1.dp, AccentBlue.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("+ Tambah", color = AccentBlue, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptySearchResult() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("😕 Produk tidak ditemukan", color = TextMuted, fontSize = 14.sp)
    }
}

// ═══════════════════════════════════════════════════════════════
// PANEL KERANJANG (KANAN)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CartPanel(
    state: PosUiState,
    onRemoveItem: (Long) -> Unit,
    onUpdateDiscount: (Long, Long) -> Unit,
    onClearCart: () -> Unit,
    onShowPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .padding(12.dp)
    ) {
        // ── Header Keranjang ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🛒 Keranjang",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.totalItems > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AccentBlue)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${state.totalItems}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (state.cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Icon(Icons.Filled.DeleteSweep, "Bersihkan", tint = AccentRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Bersihkan", color = AccentRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── List Item Keranjang ──────────────────────────────
        if (state.cartItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Keranjang kosong", color = TextMuted, fontSize = 14.sp)
                    Text("Tambah produk dari panel kiri", color = TextMuted.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.cartItems, key = { it.productId }) { item ->
                    CartItemRow(
                        item = item,
                        onRemove = { onRemoveItem(item.productId) },
                        onDiscountChanged = { onUpdateDiscount(item.productId, it) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
        Spacer(Modifier.height(8.dp))

        // ── Ringkasan ────────────────────────────────────────
        SummaryRow("Subtotal", state.subtotal, TextSecondary)
        if (state.totalDiscount > 0) {
            SummaryRow("Diskon", -state.totalDiscount, AccentOrange)
        }
        if (state.taxPercentage > 0) {
            SummaryRow("Pajak (${state.taxPercentage}%)", state.taxAmount, TextSecondary)
        }
        HorizontalDivider(color = GlassBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
        SummaryRow("TOTAL", state.grandTotal, AccentGreen, isBold = true, fontSize = 20.sp)

        Spacer(Modifier.height(10.dp))

        // ── Tombol Checkout ──────────────────────────────────
        Button(
            onClick = onShowPayment,
            enabled = state.canCheckout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue,
                disabledContainerColor = CardDark
            )
        ) {
            Icon(Icons.Filled.PointOfSale, "Checkout", modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "BAYAR • Rp ${"%,d".format(state.grandTotal)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartWithProduct,
    onRemove: () -> Unit,
    onDiscountChanged: (Long) -> Unit
) {
    // Glass card per item keranjang
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Info produk
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Rp ${"%,d".format(item.unitPrice)} × ${item.quantity}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (item.discount > 0) {
                    Text(
                        "Diskon: Rp ${"%,d".format(item.discount)}",
                        color = AccentOrange,
                        fontSize = 11.sp
                    )
                }
            }

            // Hapus
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.RemoveCircleOutline, "Hapus", tint = AccentRed, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Baris: Diskon + Subtotal ──────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input diskon per-item (compact)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Diskon Rp", color = TextMuted, fontSize = 10.sp)
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = if (item.discount == 0L) "" else item.discount.toString(),
                    onValueChange = { raw ->
                        val parsed = raw.filter { it.isDigit() }.take(9)
                        onDiscountChanged(parsed.toLongOrNull() ?: 0L)
                    },
                    modifier = Modifier.width(100.dp).height(40.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(color = AccentOrange, fontSize = 11.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AccentOrange,
                        unfocusedTextColor = AccentOrange,
                        focusedBorderColor = AccentOrange.copy(alpha = 0.5f),
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = AccentOrange,
                        focusedContainerColor = CardDark,
                        unfocusedContainerColor = CardDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Text(
                "Rp ${"%,d".format(item.lineTotal)}",
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Long,
    color: Color,
    isBold: Boolean = false,
    fontSize: Int = 14
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (isBold) TextPrimary else TextSecondary,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = fontSize.sp
        )
        Text(
            "Rp ${"%,d".format(amount)}",
            color = color,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontSize = fontSize.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// DIALOG PEMBAYARAN
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PaymentDialog(
    state: PosUiState,
    onTaxChanged: (Int) -> Unit,
    onCashChanged: (Long) -> Unit,
    onMethodChanged: (String) -> Unit,
    onCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf("cash" to "💵 Tunai", "qris" to "📱 QRIS", "debit" to "💳 Debit")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("💰 Pembayaran", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Total yang harus dibayar
                GlassSurfaceBox {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Tagihan", color = TextSecondary, fontSize = 14.sp)
                        Text(
                            "Rp ${"%,d".format(state.grandTotal)}",
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Pajak
                GlassSurfaceBox {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pajak (%)", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.taxPercentage.toString(),
                            onValueChange = { onTaxChanged(it.toIntOrNull() ?: 0) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = GlassBorder,
                                cursorColor = AccentBlue,
                                focusedContainerColor = CardDark,
                                unfocusedContainerColor = CardDark
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Metode pembayaran
                GlassSurfaceBox {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Metode Pembayaran", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            methods.forEach { (value, label) ->
                                FilterChip(
                                    selected = state.paymentMethod == value,
                                    onClick = { onMethodChanged(value) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentBlue.copy(alpha = 0.3f),
                                        selectedLabelColor = AccentBlue
                                    )
                                )
                            }
                        }
                    }
                }

                // Uang tunai diterima
                if (state.paymentMethod == "cash") {
                    GlassSurfaceBox {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Uang Tunai Diterima", color = TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = if (state.cashReceived == 0L) "" else state.cashReceived.toString(),
                                onValueChange = { onCashChanged(it.toLongOrNull() ?: 0L) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                prefix = { Text("Rp ", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = GlassBorder,
                                    cursorColor = AccentBlue,
                                    focusedContainerColor = CardDark,
                                    unfocusedContainerColor = CardDark
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            // Kembalian
                            if (state.cashReceived >= state.grandTotal) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "💸 Kembalian: Rp ${"%,d".format(state.change)}",
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            } else if (state.cashReceived > 0) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "⚠️ Kurang: Rp ${"%,d".format(state.grandTotal - state.cashReceived)}",
                                    color = AccentRed,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCheckout,
                enabled = state.cashReceived >= state.grandTotal || state.paymentMethod != "cash",
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("✅ Konfirmasi Bayar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextMuted)
            }
        }
    )
}

// ── Helper: Glass Surface Box ──────────────────────────────────
@Composable
private fun GlassSurfaceBox(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
    ) {
        Column { content() }
    }
}

// ═══════════════════════════════════════════════════════════════
// DIALOG STRUK (POST-CHECKOUT)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ReceiptDialog(
    transaction: com.example.kasirpos.data.local.entity.TransactionEntity,
    state: PosUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🧾 Transaksi Berhasil", color = AccentGreen, fontWeight = FontWeight.Bold)
                Text("#${transaction.id}", color = TextMuted, fontSize = 14.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Ringkasan
                GlassSurfaceBox {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ReceiptRow("Subtotal", transaction.subtotal)
                        if (transaction.totalDiscount > 0)
                            ReceiptRow("Diskon", -transaction.totalDiscount, AccentOrange)
                        if (transaction.taxAmount > 0)
                            ReceiptRow("Pajak (${transaction.taxPercentage}%)", transaction.taxAmount)
                        HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                        ReceiptRow("TOTAL", transaction.grandTotal, AccentGreen, bold = true)
                        HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                        ReceiptRow(transaction.paymentMethod.uppercase(), transaction.cashReceived)
                        if (transaction.change > 0)
                            ReceiptRow("Kembalian", transaction.change, AccentBlue, bold = true)
                    }
                }

                Text(
                    "🕐 ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("id")).format(java.util.Date(transaction.createdAt))}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Tutup dialog dulu, lalu navigasi bisa ditangani di parent
                    onDismiss()
                    // TODO: Trigger navigasi ke tab Struk via callback
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🖨️ Cetak Struk", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TextMuted)
            }
        }
    )
}

@Composable
private fun ReceiptRow(label: String, amount: Long, color: Color = TextSecondary, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (bold) TextPrimary else TextSecondary,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
        Text(
            "Rp ${"%,d".format(amount)}",
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
