package com.example.posoffline.ui.screen.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.posoffline.ui.screen.settings.SettingsViewModel
import com.example.posoffline.AppContainer
import com.example.posoffline.data.entity.ProductEntity
import com.example.posoffline.ui.components.glass
import com.example.posoffline.ui.theme.AppColors
import com.example.posoffline.util.Money

/**
 * The main cashier screen.
 *
 * Composes the product grid and the cart panel. State is shared via two
 * ViewModels; we lift the cart ViewModel up to the screen so the
 * ProductGrid can dispatch `addProduct` directly into the cart.
 */
@Composable
fun PosScreen(
    container: AppContainer,
    onNavigateInventory: () -> Unit,
    onNavigateHistory: () -> Unit
) {
    val productVm: ProductViewModel = viewModel(
        factory = ProductViewModel.Factory(container.productRepository)
    )
    val cartVm: CartViewModel = viewModel(
        factory = CartViewModel.Factory(container.transactionRepository)
    )
    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(container.settingsRepository)
    )

    val products by productVm.products.collectAsStateWithLifecycle()
    val cartUi by cartVm.ui.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()

    // Local UI state: search query, active category chip
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }

    val categories = remember(products) {
        listOf("all") + products.mapNotNull { it.category }.distinct().sorted()
    }
    val visible = remember(products, query, category) {
        val q = query.trim().lowercase()
        products.filter { p ->
            (category == "all" || p.category == category) &&
                (q.isEmpty() || p.name.lowercase().contains(q) || p.sku.lowercase().contains(q))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.Bg0,
                        Color(0xFF0A0E1A)
                    )
                )
            )
            .padding(12.dp)
    ) {
        TopBar(
            storeName = settings.storeName,
            onInventory = onNavigateInventory,
            onHistory = onNavigateHistory
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: product search + grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    categories = categories,
                    activeCategory = category,
                    onCategoryChange = { category = it }
                )
                Spacer(Modifier.height(12.dp))
                ProductGrid(
                    products = visible,
                    onPick = { cartVm.addProduct(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Right: cart panel
            CartPanel(
                cartVm = cartVm,
                productRepository = container.productRepository,
                settings = settings,
                onCheckoutSuccess = { /* success handled inside dialog */ },
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun TopBar(
    storeName: String,
    onInventory: () -> Unit,
    onHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(corner = 16.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AppColors.Indigo500, AppColors.Fuchsia500)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("⚡", color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = storeName,
                color = AppColors.Slate50,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = "POS Offline • 100% di perangkat",
                color = AppColors.Slate400,
                fontSize = 11.sp
            )
        }
        TopBarChip("Inventaris", onClick = onInventory)
        Spacer(Modifier.width(6.dp))
        TopBarChip("Riwayat", onClick = onHistory)
    }
}

@Composable
private fun TopBarChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = AppColors.Slate200, fontSize = 12.sp)
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    activeCategory: String,
    onCategoryChange: (String) -> Unit
) {
    Column(modifier = Modifier.glass(corner = 16.dp).padding(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Cari nama / SKU…", color = AppColors.Slate400) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AppColors.Slate400)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = AppColors.Indigo400,
                unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                cursorColor = AppColors.Indigo300,
                focusedTextColor = AppColors.Slate50,
                unfocusedTextColor = AppColors.Slate100
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { c ->
                val active = c == activeCategory
                AssistChip(
                    onClick = { onCategoryChange(c) },
                    label = { Text(c.replaceFirstChar { it.uppercase() }) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (active) AppColors.Indigo500.copy(alpha = 0.80f)
                        else Color.White.copy(alpha = 0.06f),
                        labelColor = if (active) Color.White else AppColors.Slate300
                    )
                )
            }
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<ProductEntity>,
    onPick: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // LazyVerticalGrid is faster than a regular Column+wrap for large
    // catalogs because it only composes visible items.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier,
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = products, key = { it.id }) { p ->
            ProductTile(product = p, onClick = { onPick(p) })
        }
    }
}

@Composable
private fun ProductTile(product: ProductEntity, onClick: () -> Unit) {
    val oos = product.stock <= 0
    val low = !oos && product.stock <= 5
    Column(
        modifier = Modifier
            .glass(corner = 16.dp)
            .heightIn(min = 96.dp)
            .then(if (!oos) Modifier.clickable { onClick() } else Modifier)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = product.sku,
                color = AppColors.Slate300,
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(Modifier.weight(1f))
            val (color, text) = when {
                oos -> AppColors.Rose300 to "Habis"
                low -> AppColors.Amber300 to "Stok: ${product.stock}"
                else -> AppColors.Emerald300 to "Stok: ${product.stock}"
            }
            Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = product.name,
            color = AppColors.Slate100,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = Money.format(product.price),
            color = AppColors.Indigo300,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


