package com.pos.offline.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.util.toRupiah

/**
 * Layar Inventaris (CRUD produk).
 *
 * Performa:
 *  - Daftar memakai LazyColumn dengan `key` + `contentType` → recycle optimal.
 *  - Baris memakai flat [Surface] (bukan gradient) → murah di GPU untuk list panjang.
 *  - State dikoleksi sadar-siklus ([collectAsStateWithLifecycle]).
 *  - Umpan balik satu-kali via Snackbar (dari Channel di ViewModel).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Konsumsi pesan satu-kali dari Channel → tampilkan Snackbar.
    // Dibatalkan otomatis saat layar meninggalkan komposisi (anti leak).
    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inventaris", fontWeight = FontWeight.SemiBold) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::startAdd,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Tambah Produk") }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Cari nama / SKU…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (products.isEmpty()) {
                EmptyInventory(hasQuery = query.isNotEmpty())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = products,
                        key = { it.id },
                        contentType = { "product" }
                    ) { product ->
                        ProductRow(
                            product = product,
                            onEdit = { viewModel.startEdit(product) },
                            onDelete = { viewModel.requestDelete(product) }
                        )
                    }
                }
            }
        }
    }

    // ---- Dialog tambah / edit ----
    form?.let { state ->
        ProductFormDialog(
            state = state,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissForm
        )
    }

    // ---- Konfirmasi hapus ----
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Batal") }
            },
            title = { Text("Hapus Produk?") },
            text = { Text("\"${target.name}\" akan dihapus dari katalog.") }
        )
    }
}

// ============================ BARIS PRODUK ============================

@Composable
private fun ProductRow(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    product.sku,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.price.toRupiah(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    StockBadge(stock = product.stock)
                    Spacer(Modifier.width(10.dp))
                    // Laba per unit = harga jual − modal (kolom `cost` dari v2).
                    if (product.cost > 0) {
                        Text(
                            "Laba ${(product.price - product.cost).toRupiah()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit ${product.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Hapus ${product.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Lencana stok dengan warna sesuai kondisi (habis / menipis / aman). */
@Composable
private fun StockBadge(stock: Int) {
    val color = when {
        stock <= 0 -> MaterialTheme.colorScheme.error   // merah: habis
        stock <= 5 -> Color(0xFFF5A623)                  // kuning: menipis
        else -> MaterialTheme.colorScheme.primary        // hijau: aman
    }
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text = if (stock <= 0) "Habis" else "Stok $stock",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Tampilan ketika belum ada produk / pencarian kosong. */
@Composable
private fun EmptyInventory(hasQuery: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (hasQuery) "Produk tidak ditemukan" else "Belum ada produk",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (!hasQuery) {
                Text(
                    "Tekan \"+ Tambah Produk\" untuk mulai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ============================ FORM TAMBAH / EDIT ============================

@Composable
private fun ProductFormDialog(
    state: ProductFormState,
    onSave: (ProductFormState) -> Unit,
    onDismiss: () -> Unit
) {
    // remember keyed by id → reset field ketika beralih ke produk berbeda.
    var name by remember(state.id) { mutableStateOf(state.name) }
    var sku by remember(state.id) { mutableStateOf(state.sku) }
    var price by remember(state.id) {
        mutableStateOf(if (state.price > 0) state.price.toString() else "")
    }
    var stock by remember(state.id) {
        mutableStateOf(if (state.stock > 0) state.stock.toString() else "")
    }
    // Modal/harga beli (kolom `cost` v2). Opsional; 0 = belum diisi.
    var cost by remember(state.id) {
        mutableStateOf(if (state.cost > 0) state.cost.toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.isNew) "Tambah Produk" else "Edit Produk") },
        text = {
            // Laba per unit dihitung langsung dari input harga jual & modal.
            val priceLong = price.toLongOrNull() ?: 0L
            val costLong = cost.toLongOrNull() ?: 0L
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU (opsional)") },
                    supportingText = { Text("Kosongkan untuk dibuat otomatis") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                // Baris harga: harga jual & harga modal (cost) berdampingan.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        value = price,
                        onValueChange = { price = it },
                        label = "Harga Jual (Rp)",
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = "Modal (Rp)",
                        modifier = Modifier.weight(1f)
                    )
                }
                // Baris stok & laba per unit (read-only, terhitung otomatis).
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = "Stok",
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = (priceLong - costLong).toRupiah(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Laba/Unit") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    ProductFormState(
                        id = state.id,
                        name = name,
                        sku = sku,
                        price = price.toLongOrNull() ?: 0L,
                        cost = cost.toLongOrNull() ?: 0L,
                        stock = stock.toIntOrNull() ?: 0,
                        createdAt = state.createdAt
                    )
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

/** Field angka murni: hanya menerima digit → mencegah error parse. */
@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}
