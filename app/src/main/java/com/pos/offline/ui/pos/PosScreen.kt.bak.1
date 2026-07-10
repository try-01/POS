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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.ui.components.GlassCard
import com.pos.offline.util.toRupiah

/**
 * Layar Kasir utama. Layout responsif: tablet/landscape → katalog + keranjang
 * berdampingan; ponsel → tumpuk vertikal.
 *
 * Performa:
 *  - Semua state dikumpulkan dengan [collectAsStateWithLifecycle] (berhenti saat background).
 *  - [derivedStateOf] untuk nilai turunan → recompose terbatas.
 *  - Lazy list/grid memakai `key` + `contentType` → daur ulang slot optimal.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir Offline", fontWeight = FontWeight.SemiBold) },
                actions = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::search,
                        modifier = Modifier
                            .width(220.dp)
                            .padding(end = 12.dp),
                        placeholder = { Text("Cari produk/SKU…", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            )
        }
    ) { inner ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            val isWide = maxWidth >= 840.dp
            if (isWide) {
                Row(Modifier.fillMaxSize()) {
                    ProductPane(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        products = products,
                        onAdd = viewModel::addToCart
                    )
                    Spacer(Modifier.width(12.dp))
                    CartPane(
                        modifier = Modifier.width(380.dp).fillMaxHeight(),
                        cart = cart,
                        totals = totals,
                        discount = discount,
                        taxRate = taxRate,
                        paid = paid,
                        change = change,
                        onDiscountChange = viewModel::setDiscount,
                        onTaxRateChange = viewModel::setTaxRate,
                        onPaidChange = viewModel::setPaid,
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
                Column(Modifier.fillMaxSize()) {
                    ProductPane(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        products = products,
                        onAdd = viewModel::addToCart
                    )
                    Spacer(Modifier.height(8.dp))
                    CartPane(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        cart = cart,
                        totals = totals,
                        discount = discount,
                        taxRate = taxRate,
                        paid = paid,
                        change = change,
                        onDiscountChange = viewModel::setDiscount,
                        onTaxRateChange = viewModel::setTaxRate,
                        onPaidChange = viewModel::setPaid,
                        onIncrease = viewModel::increaseQty,
                        onDecrease = viewModel::decreaseQty,
                        onRemove = viewModel::removeFromCart,
                        onClear = viewModel::clearCart,
                        onCheckout = viewModel::checkout,
                        canCheckout = !isCartEmpty && !isProcessing,
                        isProcessing = isProcessing
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
    onAdd: (ProductEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = products,
            key = { it.id },                 // key stabil → recycle benar saat reorder
            contentType = { "product" }      // satu tipe → pool recycle optimal
        ) { product ->
            ProductCard(product = product, onAdd = { onAdd(product) })
        }
    }
}

@Composable
private fun ProductCard(product: ProductEntity, onAdd: () -> Unit) {
    val outOfStock = product.stock <= 0
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !outOfStock, onClick = onAdd),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = product.sku,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = product.price.toRupiah(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Stok: ${product.stock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (outOfStock) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                // Tombol tambah bulat — area sentuh cukup besar (aksesibilitas).
                Surface(
                    shape = CircleShape,
                    color = if (outOfStock) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable(onClick = onAdd)) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Tambah ${product.name}",
                            tint = if (outOfStock) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// ============================ PANEL KERANJANG ============================

@Composable
@Suppress("LongParameterList") // Parameter dipisah eksplisit agar fungsi composable stabil & testable.
private fun CartPane(
    modifier: Modifier,
    cart: List<CartItemEntity>,
    totals: Totals,
    discount: Long,
    taxRate: Double,
    paid: Long,
    change: Long,
    onDiscountChange: (Long) -> Unit,
    onTaxRateChange: (Double) -> Unit,
    onPaidChange: (Long) -> Unit,
    onIncrease: (CartItemEntity) -> Unit,
    onDecrease: (CartItemEntity) -> Unit,
    onRemove: (CartItemEntity) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    canCheckout: Boolean,
    isProcessing: Boolean
) {
    GlassCard(
        modifier = modifier.padding(12.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Keranjang", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (cart.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kosongkan")
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Daftar item; weight(1f) agar total tetap menempel di bawah.
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cart, key = { it.id }, contentType = { "cart" }) { item ->
                    CartRow(
                        item = item,
                        onIncrease = { onIncrease(item) },
                        onDecrease = { onDecrease(item) },
                        onRemove = { onRemove(item) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ---- Input & ringkasan total ----
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

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCheckout,
                enabled = canCheckout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Bayar · ${totals.total.toRupiah()}")
                }
            }
        }
    }
}

@Composable
private fun CartRow(
    item: CartItemEntity,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Spacer(Modifier.width(6.dp))
        QuantityStepper(item.quantity, onDecrease, onIncrease)
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuantityStepper(qty: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrease) {
            Icon(Icons.Rounded.Remove, contentDescription = "Kurangi", modifier = Modifier.size(18.dp))
        }
        Text("$qty", modifier = Modifier.width(28.dp), fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onIncrease) {
            Icon(Icons.Rounded.Add, contentDescription = "Tambah", modifier = Modifier.size(18.dp))
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SummaryLine("Subtotal", totals.subtotal.toRupiah())
        // Input diskon (Rupiah) & pajak (%).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoneyField(
                label = "Diskon (Rp)",
                value = discount,
                onValueChange = onDiscountChange,
                modifier = Modifier.weight(1f)
            )
            DecimalField(
                label = "Pajak (%)",
                value = taxRate * 100.0,
                onValueChange = { pct -> onTaxRateChange((pct / 100.0).coerceIn(0.0, 100.0)) },
                modifier = Modifier.weight(1f)
            )
        }
        if (totals.discount > 0) SummaryLine("Diskon", "- ${totals.discount.toRupiah()}")
        if (totals.tax > 0) SummaryLine("Pajak", totals.tax.toRupiah())

        HorizontalDivider(Modifier.padding(vertical = 2.dp))
        SummaryLine("Total", totals.total.toRupiah(), emphasize = true)

        MoneyField(
            label = "Uang dibayar (Rp)",
            value = paid,
            onValueChange = onPaidChange,
            modifier = Modifier.fillMaxWidth()
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

/** Field angka uang (Long). Hanya menerima digit → mencegah parse error. */
@Composable
private fun MoneyField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(if (value <= 0) "" else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            text = digits
            onValueChange(digits.toLongOrNull() ?: 0L)
        },
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}

/** Field desimal (untuk persentase pajak). Menerima satu titik desimal. */
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
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            // Izinkan hanya digit & maksimal satu titik desimal.
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
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}

private fun formatTrim(d: Double): String {
    val i = d.toLong()
    return if (d == i.toDouble()) i.toString() else d.toString()
}

// ============================ DIALOG SUKSES ============================

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
            Button(onClick = onDismiss) { Text("Selesai") }
        },
        title = { Text("Transaksi Berhasil ✓") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No. Struk: ${result.transaction.id}")
                Text("Total: ${result.transaction.total.toRupiah()}", fontWeight = FontWeight.SemiBold)
                Text("Bayar: ${result.transaction.paidAmount.toRupiah()}")
                Text("Kembali: ${result.transaction.change.toRupiah()}", color = MaterialTheme.colorScheme.primary)
            }
        },
        // Tombol aksi struk (disusun vertikal agar mudah disentuh).
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                FilledTonalButton(onClick = onPrint, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Cetak Bluetooth")
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Text("Ekspor PDF")
                }
            }
        }
    )
}
