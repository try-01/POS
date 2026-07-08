package com.kasirku.pos.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasirku.pos.ui.components.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = hiltViewModel()) {
    val products by viewModel.products.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val editingProduct by viewModel.editingProduct.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is InventoryEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
                is InventoryEvent.ShowError -> snackbarHostState.showSnackbar("❌ \${event.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("📦 Inventaris", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog) {
                Icon(Icons.Default.Add, "Tambah Produk")
            }
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦", style = MaterialTheme.typography.displayLarge)
                    Text("Belum ada produk", style = MaterialTheme.typography.titleMedium)
                    Text("Tekan + untuk menambah", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products, key = { it.id }) { product ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold)
                                Text(product.sku, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatRupiah(product.sellPrice), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Stok: \${product.stock}", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.showEditDialog(product) }) {
                                Icon(Icons.Default.Edit, "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteProduct(product.id) }) {
                                Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(if (editingProduct != null) "Edit Produk" else "Tambah Produk") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = formState.name, onValueChange = { viewModel.updateFormField(FormField.NAME, it) }, label = { Text("Nama Produk") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = formState.sku, onValueChange = { viewModel.updateFormField(FormField.SKU, it) }, label = { Text("SKU") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = formState.price, onValueChange = { viewModel.updateFormField(FormField.PRICE, it) }, label = { Text("Harga Jual (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = formState.stock, onValueChange = { viewModel.updateFormField(FormField.STOCK, it) }, label = { Text("Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = formState.category, onValueChange = { viewModel.updateFormField(FormField.CATEGORY, it) }, label = { Text("Kategori") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = viewModel::saveProduct) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Batal") } }
        )
    }
}
