package com.example.posoffline.ui.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.posoffline.AppContainer
import com.example.posoffline.data.entity.ProductEntity
import com.example.posoffline.ui.components.glass
import com.example.posoffline.ui.components.glassStrong
import com.example.posoffline.ui.theme.AppColors
import com.example.posoffline.util.Money
import com.example.posoffline.ui.screen.pos.ProductViewModel

/**
 * Inventory CRUD screen.
 *
 * Two-pane layout: a list of products on the left, a form on the right.
 * The form doubles as the "create" and "edit" surface — the ViewModel
 * decides which path to take based on the `editing` reference.
 */
@Composable
fun InventoryScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: ProductViewModel = viewModel(
        factory = ProductViewModel.Factory(container.productRepository)
    )
    val products by vm.products.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var draft by remember {
        mutableStateOf(
            Draft(sku = "", name = "", price = "", stock = "", category = "")
        )
    }

    fun resetDraft() {
        editing = null
        draft = Draft("", "", "", "", "")
    }

    fun loadFor(p: ProductEntity) {
        editing = p
        draft = Draft(
            sku = p.sku,
            name = p.name,
            price = p.price.toString(),
            stock = p.stock.toString(),
            category = p.category.orEmpty()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glass(corner = 16.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = AppColors.Slate200)
            }
            Spacer(Modifier.width(4.dp))
            Text("Inventaris", color = AppColors.Slate50, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text("${products.size} produk", color = AppColors.Slate400, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .glass(corner = 20.dp)
                    .padding(8.dp)
            ) {
                if (products.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada produk.", color = AppColors.Slate400)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items = products, key = { it.id }) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                    .clickable { loadFor(p) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, color = AppColors.Slate100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${p.sku} • ${p.category ?: "—"} • ${Money.format(p.price)} • Stok ${p.stock}",
                                        color = AppColors.Slate400, fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = { loadFor(p) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = AppColors.Indigo300)
                                }
                                IconButton(onClick = { vm.remove(p.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = AppColors.Rose300)
                                }
                            }
                        }
                    }
                }
            }

            // Form
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxSize()
                    .glassStrong(corner = 20.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (editing == null) "Tambah Produk" else "Edit Produk",
                    color = AppColors.Slate50, fontWeight = FontWeight.SemiBold
                )
                Field("SKU", draft.sku) { draft = draft.copy(sku = it.uppercase()) }
                Field("Nama", draft.name) { draft = draft.copy(name = it) }
                Field("Kategori", draft.category) { draft = draft.copy(category = it) }
                Field("Harga (Rp)", draft.price, numeric = true) { draft = draft.copy(price = it) }
                Field("Stok", draft.stock, numeric = true) { draft = draft.copy(stock = it) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(AppColors.Indigo500, AppColors.Indigo600)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable {
                                val price = Money.parseInput(draft.price)
                                val stock = Money.parseInput(draft.stock).toInt()
                                if (draft.sku.isBlank() || draft.name.isBlank()) return@clickable
                                if (editing == null) {
                                    vm.create(draft.sku, draft.name, price, stock, draft.category.ifBlank { null }) {
                                        resetDraft()
                                    }
                                } else {
                                    vm.update(editing!!.id, draft.sku, draft.name, price, stock, draft.category.ifBlank { null }) {
                                        resetDraft()
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (editing == null) "Tambah" else "Simpan",
                            color = Color.White, fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (editing != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                .clickable { resetDraft() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Batal", color = AppColors.Slate200) }
                    }
                }
            }
        }
    }
}

private data class Draft(
    val sku: String,
    val name: String,
    val price: String,
    val stock: String,
    val category: String
)

@Composable
private fun Field(
    label: String,
    value: String,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    Column {
        Text(label, color = AppColors.Slate400, fontSize = 11.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = AppColors.Indigo400,
                unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                focusedTextColor = AppColors.Slate100,
                unfocusedTextColor = AppColors.Slate100,
                cursorColor = AppColors.Indigo300
            )
        )
    }
}
