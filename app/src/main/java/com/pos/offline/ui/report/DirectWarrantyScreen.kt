package com.pos.offline.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pos.offline.ui.inventory.InventoryViewModel
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.util.toRupiah

@Composable
fun DirectWarrantyScreen(
    inventoryViewModel: InventoryViewModel,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onNavigateBack: () -> Unit,
    // Menambahkan fungsi callback saat proses garansi disetujui di dialog
    onSubmitWarranty: (product: ProductEntity, qty: Double, note: String) -> Unit
) {
    BackHandler(onBack = onNavigateBack)
    
    val products by inventoryViewModel.products.collectAsStateWithLifecycle()
    
    // State untuk menyimpan produk yang sedang diklik (untuk memunculkan dialog)
    var selectedProductForWarranty by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) {
            emptyList() 
        } else {
            val q = searchQuery.lowercase()
            products.filter {
                it.name.lowercase().contains(q) ||
                it.sku.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                // PERBAIKAN: Tambahkan pencocokan Barcode di sini
                (it.barcode?.lowercase()?.contains(q) == true)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack, 
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.width(4.dp))
                
                WarrantySearchBar(
                    query = searchQuery,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.weight(1f).height(40.dp)
                )
                
                Spacer(Modifier.width(8.dp))
                
                WarrantySquareIconButton(
                    icon = Icons.Rounded.QrCodeScanner,
                    contentDescription = "Scan Barcode Produk",
                    onClick = onScanClick
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    item {
                        Text(
                            "Ketik nama produk, SKU, kategori, atau scan barcode untuk mencari produk yang akan diklaim...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (filteredProducts.isEmpty()) {
                    item {
                        Text(
                            "Produk tidak ditemukan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    items(
                        items = filteredProducts,
                        key = { it.id }
                    ) { product ->
                        WarrantyProductRow(
                            product = product,
                            onClick = {
                                // Saat diklik, simpan produk ke State untuk membuka dialog
                                selectedProductForWarranty = product
                            }
                        )
                    }
                }
            }
        }
    }

    // Tampilkan Dialog jika ada produk yang dipilih
    selectedProductForWarranty?.let { product ->
        WarrantyClaimDialog(
            product = product,
            onDismiss = { selectedProductForWarranty = null }, // Tutup dialog
            onSubmit = { qty, note ->
                // Panggil callback ke ReportScreen
                onSubmitWarranty(product, qty, note)
                // Tutup dialog dan kembali ke layar laporan
                selectedProductForWarranty = null
                onNavigateBack()
            }
        )
    }
}

@Composable
private fun WarrantyClaimDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSubmit: (qty: Double, note: String) -> Unit
) {
    var qty by remember { mutableStateOf(1.0) }
    var note by remember { mutableStateOf("") }
    
    // Menentukan langkah penambahan/pengurangan berdasarkan tipe stok
    val step = if (product.stock % 1.0 == 0.0) 1.0 else 0.1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("Proses Garansi Direct", fontSize = 16.sp, fontWeight = FontWeight.Bold) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Produk: ${product.name}\nStok di inventory: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Stepper Qty
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Jumlah Klaim:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    
                    // Tombol Minus
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { if (qty > step) qty -= step },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Kurangi", modifier = Modifier.size(16.dp))
                    }
                    
                    Text(
                        text = qty.toString(),
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Tombol Plus
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { qty += step },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                    }
                }
                
                // Input Catatan
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Kerusakan (Wajib/Opsional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(qty, note) }) {
                Text("Konfirmasi Garansi", fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", fontSize = 13.sp)
            }
        }
    )
}

// ... [Biarkan fungsi WarrantyProductRow, WarrantySearchBar, dan WarrantySquareIconButton persis sama seperti sebelumnya] ...

@Composable
private fun WarrantyProductRow(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "SKU: ${product.sku}  •  Kategori: ${product.category.ifBlank { "-" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Stok: ${product.stock}  •  Harga: ${product.price.toRupiah()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Build,
                contentDescription = "Pilih untuk Garansi",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun WarrantySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Cari Produk",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Cari produk untuk garansi...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Hapus",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun WarrantySquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
