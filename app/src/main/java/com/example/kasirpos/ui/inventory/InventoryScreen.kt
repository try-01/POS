package com.example.kasirpos.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kasirpos.data.local.entity.ProductEntity
import com.example.kasirpos.ui.theme.*

/**
 * Layar Manajemen Inventaris — CRUD produk dengan UI glassmorphism ringan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = PrimaryDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddDialog,
                containerColor = AccentBlue
            ) {
                Icon(Icons.Filled.Add, "Tambah Produk", tint = TextPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "📋 Inventaris",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (state.lowStockProducts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ ${state.lowStockProducts.size} produk stok rendah (< 10)",
                    color = AccentOrange,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(12.dp))

            if (state.products.isEmpty() && !state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada produk", color = TextMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onEdit = { viewModel.showEditDialog(product) },
                            onDelete = { viewModel.deleteProduct(product) }
                        )
                    }
                }
            }
        }
    }

    // ── Dialog Tambah/Edit ─────────────────────────────────────
    if (state.showAddEditDialog) {
        ProductFormDialog(
            existing = state.editingProduct,
            onSave = { name, sku, price, stock, uri ->
                viewModel.saveProduct(name, sku, price, stock, uri)
            },
            onDismiss = viewModel::dismissDialog
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text("SKU: ${product.sku}", color = TextMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Stok: ${product.stock}",
                    color = if (product.stock <= 5) AccentRed else TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    "Rp ${"%,d".format(product.price)}",
                    color = AccentGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, "Edit", tint = AccentBlue)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, "Hapus", tint = AccentRed)
        }
    }
}

@Composable
private fun ProductFormDialog(
    existing: ProductEntity?,
    onSave: (String, String, Long, Int, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var sku by remember { mutableStateOf(existing?.sku ?: "") }
    var price by remember { mutableStateOf(existing?.price?.toString() ?: "") }
    var stock by remember { mutableStateOf(existing?.stock?.toString() ?: "") }

    val isEdit = existing != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                if (isEdit) "✏️ Edit Produk" else "➕ Tambah Produk",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = sku, onValueChange = { sku = it },
                    label = { Text("SKU / Barcode") },
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Harga Jual (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = stock, onValueChange = { stock = it },
                    label = { Text("Stok") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, sku, price.toLongOrNull() ?: 0, stock.toIntOrNull() ?: 0, null)
                },
                enabled = name.isNotBlank() && sku.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEdit) "Simpan" else "Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextMuted) }
        }
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = GlassBorder,
    cursorColor = AccentBlue,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextMuted,
    focusedContainerColor = CardDark,
    unfocusedContainerColor = CardDark
)
