package com.kasirku.pos.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasirku.pos.data.local.entity.ProductEntity
import com.kasirku.pos.domain.model.CartItem
import com.kasirku.pos.util.CurrencyFormatter

/**
 * Layar Kasir (Point of Sale) — layar utama aplikasi.
 *
 * Catatan performa rendering:
 * - Semua daftar (produk & keranjang) memakai `key = { it.id }` pada LazyVerticalGrid/LazyColumn,
 *   sehingga Compose hanya me-recompose baris yang datanya benar-benar berubah, bukan seluruh list.
 * - `uiState` berasal dari satu StateFlow tunggal (lihat [PosViewModel]) sehingga hanya ada SATU
 *   collector aktif dari UI ke ViewModel, menghindari banyak observer kecil yang tersebar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPaymentSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Gradient statis sebagai latar — jauh lebih murah secara GPU dibanding
                // real-time background blur, tetapi tetap memberi kesan modern & lembut.
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PosSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                // Panel kiri: grid produk (bobot lebih besar karena berisi banyak item)
                ProductGrid(
                    products = uiState.products,
                    onProductClick = viewModel::addToCart,
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Panel kanan: keranjang + ringkasan total + tombol bayar
                CartPanel(
                    cartItems = uiState.cartItems,
                    totals = uiState.totals,
                    onIncrease = viewModel::increaseQuantity,
                    onDecrease = viewModel::decreaseQuantity,
                    onRemove = { viewModel.removeFromCart(it.productId) },
                    onCheckoutClick = { showPaymentSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }

    if (showPaymentSheet) {
        PaymentBottomSheet(
            grandTotal = uiState.totals.grandTotal,
            onDismiss = { showPaymentSheet = false },
            onConfirm = { paidAmount ->
                viewModel.checkout(paidAmount)
                showPaymentSheet = false
            }
        )
    }
}

@Composable
private fun PosSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Cari produk / SKU...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ProductGrid(
    products: List<ProductEntity>,
    onProductClick: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // key = product.id -> mencegah recomposition/measure ulang item yang tidak berubah
        // saat daftar produk lain berubah (mis. hasil pencarian baru).
        items(products, key = { it.id }) { product ->
            ProductCard(product = product, onClick = { onProductClick(product) })
        }
    }
}

@Composable
private fun ProductCard(product: ProductEntity, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium
            )
            Column {
                Text(
                    text = CurrencyFormatter.format(product.sellPrice),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Stok: ${product.stock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CartPanel(
    cartItems: List<CartItem>,
    totals: CartTotals,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keranjang (${cartItems.size})", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Keranjang kosong", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems, key = { it.productId }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrease = { onIncrease(item) },
                            onDecrease = { onDecrease(item) },
                            onRemove = { onRemove(item) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            TotalsSection(totals)

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCheckoutClick,
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Bayar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, maxLines = 1, fontWeight = FontWeight.Medium)
            Text(
                CurrencyFormatter.format(item.subtotal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Kurangi")
        }
        Text(text = "${item.quantity}", modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Tambah")
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Hapus")
        }
    }
}

@Composable
private fun TotalsSection(totals: CartTotals) {
    Column {
        TotalRow("Subtotal", totals.subtotal)
        if (totals.discount > 0) TotalRow("Diskon", -totals.discount)
        if (totals.tax > 0) TotalRow("Pajak", totals.tax)
        Spacer(modifier = Modifier.height(4.dp))
        TotalRow("Total", totals.grandTotal, emphasize = true)
    }
}

@Composable
private fun TotalRow(label: String, value: Double, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
        Text(
            CurrencyFormatter.format(value),
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentBottomSheet(
    grandTotal: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var paidText by remember { mutableStateOf("") }
    val paidAmount = paidText.toDoubleOrNull() ?: 0.0
    // Kembalian dihitung reaktif setiap kali kasir mengetik nominal uang yang diterima.
    val change = (paidAmount - grandTotal).coerceAtLeast(0.0)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            TotalRow("Total Tagihan", grandTotal, emphasize = true)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = paidText,
                onValueChange = { input -> if (input.all { it.isDigit() }) paidText = input },
                label = { Text("Jumlah Bayar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TotalRow("Kembalian", change)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onConfirm(paidAmount) },
                enabled = paidAmount >= grandTotal,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Konfirmasi & Cetak Struk")
            }
        }
    }
}

/**
 * GlassSurface — implementasi "glassmorphism ringan" yang menjadi ciri visual utama aplikasi ini.
 *
 * Alih-alih memakai real-time background blur (`RenderEffect`/`Modifier.blur` pada layer di
 * belakangnya) yang mahal secara GPU dan berisiko menurunkan frame rate saat scrolling cepat,
 * efek kaca di sini dibangun murni dari tiga operasi compositing yang sangat murah:
 *   1. Warna permukaan semi-transparan (`surface.copy(alpha = 0.65f)`)
 *   2. Border tipis semi-transparan untuk kesan "tepi kaca"
 *   3. Sudut membulat (RoundedCornerShape) untuk kesan modern
 * Ketiganya hanya melibatkan alpha-blending sederhana, sehingga tetap menjaga frame rate tinggi
 * (target 60–120fps) bahkan di perangkat kelas bawah.
 */
@Composable
private fun GlassSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    var glass = Modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
        .border(1.dp, Color.White.copy(alpha = 0.25f), shape)

    if (onClick != null) {
        glass = glass.clickable(onClick = onClick)
    }

    Box(modifier = modifier.then(glass)) {
        content()
    }
}
