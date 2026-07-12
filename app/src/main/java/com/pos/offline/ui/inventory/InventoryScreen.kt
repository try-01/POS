package com.pos.offline.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.util.toRupiah
import com.pos.offline.ui.components.GlassCard
import com.pos.offline.ui.components.ThousandsSeparatorTransformation

/**
 * Layar Inventaris (CRUD produk) — versi kompak, disamakan densitas
 * visualnya dengan PosScreen agar lebih banyak item muat di layar ponsel
 * tanpa scroll berlebihan.
 *
 * Performa:
 *  - LazyColumn dengan `key` + `contentType` → recycle optimal.
 *  - Baris produk memakai Surface tipis (bukan Card bergradien) → murah di GPU.
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
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Inventaris",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (products.isNotEmpty()) {
                        Text(
                            "${products.size} produk",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    CompactInventorySearchBar(
        query = query,
        onQueryChange = viewModel::search,
        modifier = Modifier.weight(1f).height(34.dp)
    )
    Spacer(Modifier.width(6.dp))
    SortMenuButton(current = sortOption, onSelect = viewModel::setSortOption)
}
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = viewModel::startAdd) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah Produk", modifier = Modifier.size(20.dp))
            }
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (products.isEmpty()) {
                EmptyInventory(hasQuery = query.isNotEmpty())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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

// ============================ BARIS PRODUK (SWIPE ACTION) ============================

/**
 * Baris produk dengan gestur swipe:
 *  - Swipe KANAN (StartToEnd) → Edit  (aksi aman, warna primary, ikon kiri)
 *  - Swipe KIRI  (EndToStart) → Hapus (aksi destruktif, warna error, ikon kanan)
 *
 * Kartu TIDAK benar-benar "dismiss" — confirmValueChange selalu true agar animasi
 * swipe selesai penuh (feedback visual jelas), lalu LaunchedEffect memicu aksi
 * dan otomatis reset() kartu kembali ke posisi semula. Penghapusan aktual tetap
 * lewat dialog konfirmasi di ViewModel — jadi tidak ada risiko kehapus tanpa sengaja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductRow(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { true }, // izinkan swipe selesai penuh secara visual
        positionalThreshold = { totalDistance -> totalDistance * 0.32f } // ~1/3 lebar, cukup ringan buat di-trigger
    )

    // Efek samping: begitu swipe "settle" di salah satu arah, jalankan aksi lalu kembalikan posisi.
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onEdit()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeActionBackground(direction = dismissState.targetValue) },
        modifier = Modifier.fillMaxWidth()
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 14.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            onClick = null // kartu murni visual; semua aksi lewat swipe
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        product.sku,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            product.price.toRupiah(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        StockBadge(stock = product.stock)
                        if (product.cost > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Laba ${(product.price - product.cost).toRupiah()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Ikon kecil di kanan sebagai HINT visual bahwa baris ini bisa di-swipe.
                // Tidak fungsional (bukan tombol) — murni affordance untuk discoverability,
                // karena gestur swipe tidak seintuitif tombol bagi sebagian pengguna awam.
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            }
        }
    }
}

/** Latar yang tersingkap saat kartu di-swipe — warna & ikon berubah sesuai arah. */
@Composable
private fun SwipeActionBackground(direction: SwipeToDismissBoxValue) {
    val (color, icon, alignment, label) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Quad(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            Icons.Rounded.Edit,
            Alignment.CenterStart,
            "Edit"
        )
        SwipeToDismissBoxValue.EndToStart -> Quad(
            MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
            Icons.Rounded.Delete,
            Alignment.CenterEnd,
            "Hapus"
        )
        SwipeToDismissBoxValue.Settled -> Quad(Color.Transparent, null, Alignment.Center, "")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 18.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (alignment == Alignment.CenterStart) {
                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Helper kecil pengganti Tuple 4-elemen (Kotlin stdlib hanya sediakan Pair/Triple). */
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/** Tombol aksi bundar kompak (26dp) — pengganti IconButton standar (48dp) agar hemat ruang. */
// @Composable
// private fun CompactIconAction(
//    icon: ImageVector,
//    contentDescription: String,
//    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
//    background: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
//    onClick: () -> Unit
// ) {
//    Box(
//        modifier = Modifier
//            .size(26.dp)
//            .clip(CircleShape)
//            .background(background)
//            .clickable(onClick = onClick),
//        contentAlignment = Alignment.Center
//    ) {
//        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(14.dp))
//    }
// }

/** Lencana stok — ukuran dipangkas agar sebaris dengan harga tanpa memakan tinggi tambahan. */
@Composable
private fun StockBadge(stock: Int) {
    val color = when {
        stock <= 0 -> MaterialTheme.colorScheme.error
        stock <= 5 -> Color(0xFFF5A623)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = if (stock <= 0) "Habis" else "Stok $stock",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyInventory(hasQuery: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (hasQuery) "Produk tidak ditemukan" else "Belum ada produk",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (!hasQuery) {
                Text(
                    "Ketuk tombol + untuk mulai",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/** Search bar kompak — gaya identik dengan CompactSearchBar di PosScreen. */
@Composable
private fun CompactInventorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Cari nama / SKU…",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Hapus pencarian",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

/** Tombol kompak pemicu dropdown pilihan urutan daftar produk. */
@Composable
private fun SortMenuButton(
    current: ProductSortOption,
    onSelect: (ProductSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Sort, contentDescription = "Urutkan", modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                current.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                maxLines = 1
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProductSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option.label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp))
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (option == current) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
}

// ============================ FORM TAMBAH / EDIT ============================
// Dialog dibiarkan memakai OutlinedTextField standar (tidak perlu sepadat list)
// tapi spacing dipangkas sedikit agar tidak terlalu tinggi di layar kecil.

@Composable
private fun ProductFormDialog(
    state: ProductFormState,
    onSave: (ProductFormState) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(state.id) { mutableStateOf(state.name) }
    var sku by remember(state.id) { mutableStateOf(state.sku) }
    var price by remember(state.id) {
        mutableStateOf(if (state.price > 0) state.price.toString() else "")
    }
    var stock by remember(state.id) {
        mutableStateOf(if (state.stock > 0) state.stock.toString() else "")
    }
    var cost by remember(state.id) {
        mutableStateOf(if (state.cost > 0) state.cost.toString() else "")
    }

    // Batas tinggi konten dihitung dinamis dari tinggi layar — krusial untuk
    // mode landscape & saat keyboard terbuka, di mana ruang vertikal sangat
    // terbatas. Tanpa ini, Column akan overflow dan Compose MEMOTONGNYA
    // secara diam-diam (bukan crash, tapi field jadi tak terlihat/terjangkau).
    val configuration = LocalConfiguration.current
    val maxContentHeight = (configuration.screenHeightDp * 0.42f).dp
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.isNew) "Tambah Produk" else "Edit Produk", style = MaterialTheme.typography.titleMedium) },
        text = {
            val priceLong = price.toLongOrNull() ?: 0L
            val costLong = cost.toLongOrNull() ?: 0L
            Column(
                modifier = Modifier
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU (opsional)", style = MaterialTheme.typography.bodySmall) },
                    supportingText = { Text("Kosongkan untuk dibuat otomatis", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyNumberField(price, { price = it }, "Harga Jual", Modifier.weight(1f))
                    MoneyNumberField(cost, { cost = it }, "Modal", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(stock, { stock = it }, "Stok", Modifier.weight(1f))
                    OutlinedTextField(
                        value = (priceLong - costLong).toRupiah(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Laba/Unit", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                // Spacer kecil di akhir agar field terakhir tidak "menempel" persis
                // di tepi bawah area scroll saat digulir penuh.
                Spacer(Modifier.height(2.dp))
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
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    )
}

/**
 * Field angka uang dengan separator ribuan otomatis + prefix "Rp" —
 * dipakai khusus untuk Harga Jual & Modal. Field mentah (tanpa titik)
 * tetap yang disimpan ke state; titik hanya efek visual (VisualTransformation).
 */
@Composable
private fun MoneyNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        prefix = { Text("Rp", style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = ThousandsSeparatorTransformation,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    )
}