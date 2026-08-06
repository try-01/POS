package com.pos.offline.ui.pos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.DiscountType
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.ui.components.GlassCard
import com.pos.offline.ui.components.ThousandsSeparatorTransformation
import com.pos.offline.ui.components.formatPercentTrim
import com.pos.offline.util.formatQuantity
import com.pos.offline.util.toRupiah
import com.pos.offline.util.bouncyOverscroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.CompositionLocalProvider
import com.pos.offline.util.iosGlideFlingBehavior
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
@Composable
internal fun ShiftIndicatorBar(
    openShift: ShiftEntity?,
    isOpeningDrawer: Boolean,
    onClick: () -> Unit,
    onManageClick: () -> Unit,
    onOpenDrawerClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (openShift != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        ),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text =
                    if (openShift != null) {
                        "${openShift.cashierName} · Shift Aktif"
                    } else {
                        "Tanpa Shift · Ketuk untuk mulai"
                    },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color =
                    if (openShift != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
            )
        }
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isOpeningDrawer, onClick = onOpenDrawerClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isOpeningDrawer) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            } else {
                Icon(
                    Icons.Rounded.PointOfSale,
                    contentDescription = "Buka laci kasir",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
        Spacer(Modifier.width(2.dp))
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onManageClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = "Kelola semua shift",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
@Composable
internal fun CompactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Cari produk/SKU…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryChipsRow(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null
    ) {
        LazyRow(
            modifier = Modifier.bouncyOverscroll(Orientation.Horizontal),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            flingBehavior = iosGlideFlingBehavior()
        ) {
            item(key = "__all__") {
                CategoryChip(label = "Semua", selected = selected == null, onClick = { onSelect(null) })
            }
            items(items = categories, key = { it }) { cat ->
                CategoryChip(label = cat, selected = selected == cat, onClick = { onSelect(cat) })
            }
        }
    }
}
@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                ).clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProductPane(
    modifier: Modifier,
    products: List<ProductEntity>,
    cartQtyByProductId: Map<Long, Double>,
    onAdd: (ProductEntity) -> Unit,
) {
    var selectedProductForDetails by remember { mutableStateOf<ProductEntity?>(null) }
    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            modifier = modifier
                .padding(horizontal = 12.dp)
                .bouncyOverscroll(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            flingBehavior = iosGlideFlingBehavior()
        ) {
            items(items = products, key = { it.id }, contentType = { "product" }) { product ->
            val qtyInCart = cartQtyByProductId[product.id] ?: 0.0
            ProductCard(
                product = product,
                remainingStock = product.stock - qtyInCart,
                onAdd = { onAdd(product) },
                onLongClick = { selectedProductForDetails = product }
            )
            }
        }
    }
    selectedProductForDetails?.let { product ->
        ProductDetailPopup(
            product = product,
            onDismiss = { selectedProductForDetails = null }
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductCard(
    product: ProductEntity,
    remainingStock: Double,
    onAdd: () -> Unit,
    onLongClick: () -> Unit,
) {
    val outOfStock = remainingStock <= 0.0
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        onClick = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onAdd,
                    onLongClick = onLongClick
                )
                .padding(6.dp)
        ) {
            Column {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.sku,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = product.price.toRupiah(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (outOfStock) "Habis" else "Stok: ${remainingStock.formatQuantity()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color =
                                if (outOfStock) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                },
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (outOfStock) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                ),
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Tambah ${product.name}",
                            tint =
                                if (outOfStock) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                },
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
@Composable
internal fun CartPaneContent(
    modifier: Modifier,
    cart: CartState,
    payment: PaymentState,
    catalog: CatalogState,
    checkout: CheckoutState,
    localState: PosLocalStateHolder,
    onAction: (PosAction) -> Unit,
    collapsible: Boolean = false,
) {
    val expanded = localState.isCartExpanded
    val showFull = !collapsible || expanded
    fun attemptCheckout() {
        if (payment.paid in 1 until cart.totals.total) {
            localState.showInsufficientPayment()
        } else {
            onAction(PosAction.Checkout)
        }
    }
    val listState = rememberLazyListState()
    var previousCartSize by remember { mutableStateOf(cart.items.size) }
    LaunchedEffect(cart.items.size) {
        if (cart.items.size > previousCartSize && cart.items.isNotEmpty()) {
            listState.animateScrollToItem(cart.items.lastIndex)
        }
        previousCartSize = cart.items.size
    }
    Box(
        modifier =
            modifier
                .padding(start = 12.dp, end = 12.dp, top = 1.dp, bottom = 1.dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp),
                ).clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (collapsible && !expanded) localState.toggleCart() },
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (!collapsible) Modifier.fillMaxHeight() else Modifier)
                    .padding(10.dp),
        ) {
            if (collapsible) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 6.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (collapsible) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = localState::toggleCart)
                                    .padding(vertical = 2.dp)
                            } else {
                                Modifier
                            },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Keranjang", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (collapsible && !expanded && !cart.isEmpty) {
                    Text(
                        text = "${cart.items.size} item · ${cart.totals.total.toRupiah()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                if (!cart.isEmpty && showFull) {
                    TextButton(
                        onClick = localState::showClearDialog,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp),
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kosongkan", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (collapsible) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                if (expanded) {
                                    Icons.Rounded.KeyboardArrowDown
                                } else {
                                    Icons.Rounded.KeyboardArrowUp
                                },
                            contentDescription = if (expanded) "Ciutkan" else "Perluas",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (showFull) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Box(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .clipToBounds(),
            ) {
                @OptIn(ExperimentalFoundationApi::class)
                CompositionLocalProvider(
                    LocalOverscrollConfiguration provides null
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState, 
                        flingBehavior = iosGlideFlingBehavior()
                    ) {
                        if (cart.isEmpty) {
                            item(key = "empty_cart") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Rounded.ShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Keranjang masih kosong",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        )
                                        Text(
                                            "Ketuk produk di atas untuk menambahkan",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        )
                                    }
                                }
                            }
                        } else {
                            items(items = cart.items, key = { it.id }) { item ->
                                CartRow(
                                    item = item,
                                    onIncrease = { onAction(PosAction.IncreaseQty(item)) },
                                    onDecrease = { onAction(PosAction.DecreaseQty(item)) },
                                    onRemove = { onAction(PosAction.RemoveFromCart(item)) },
                                    onQuantityClick = { localState.startQtyEdit(item) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                    }
                    val showTopFade by remember { derivedStateOf { listState.canScrollBackward } }
                    val showBottomFade by remember { derivedStateOf { listState.canScrollForward } }
                    if (showTopFade) {
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        )
                    }
                    if (showBottomFade) {
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        )
                    }
                }
                if (!cart.isEmpty) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    TotalsSummary(
                        payment = payment,
                        totals = cart.totals,
                        onAction = onAction,
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = ::attemptCheckout,
                        enabled = !cart.isEmpty && !checkout.isProcessing,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (checkout.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bayar · ${cart.totals.total.toRupiah()}")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 84.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Total: ${cart.totals.total.toRupiah()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = ::attemptCheckout,
                        enabled = !cart.isEmpty && !checkout.isProcessing,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        if (checkout.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Bayar", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
@Composable
internal fun CartRow(
    item: CartItemEntity,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onQuantityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.unitPrice.toRupiah()} × ${item.quantity.formatQuantity()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Text(
            item.lineTotal.toRupiah(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(12.dp))
        QuantityStepper(
            qty = item.quantity,
            onDecrease = onDecrease,
            onIncrease = onIncrease,
            onQuantityClick = onQuantityClick,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Hapus",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
@Composable
private fun QuantityStepper(
    qty: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onQuantityClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompactActionBox(icon = Icons.Rounded.Remove, contentDescription = "Kurangi", onClick = onDecrease)
        Box(
            modifier =
                Modifier
                    .width(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onQuantityClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = qty.formatQuantity(),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
            )
        }
        CompactActionBox(icon = Icons.Rounded.Add, contentDescription = "Tambah", onClick = onIncrease)
    }
}
@Composable
internal fun CompactActionBox(
    icon: ImageVector,
    contentDescription: String,
    dimmed: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (dimmed) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ).clickable(enabled = !dimmed, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dimmed) 0.4f else 1f),
        )
    }
}
@Composable
internal fun TotalsSummary(
    payment: PaymentState,
    totals: Totals,
    onAction: (PosAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        SummaryLine("Subtotal", totals.subtotal.toRupiah())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DiscountField(
                type = payment.discountType,
                rawValue = payment.discountValue,
                onToggleType = { onAction(PosAction.ToggleDiscountType) },
                onValueChange = { onAction(PosAction.SetDiscountValue(it)) },
                modifier = Modifier.weight(1f).height(34.dp),
            )
            DecimalField(
                label = "Pajak (%)",
                value = payment.taxRate * 100.0,
                onValueChange = { pct ->
                    onAction(PosAction.SetTaxRate((pct / 100.0).coerceIn(0.0, 100.0)))
                },
                modifier = Modifier.weight(1f).height(34.dp),
            )
        }
        if (totals.discountCapped) {
            Text(
                text = "⚠ Diskon melebihi subtotal, dibatasi menjadi ${totals.discount.toRupiah()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (totals.discount > 0) {
            val label =
                if (payment.discountType == DiscountType.PERCENT) {
                    "Diskon (${formatPercentTrim(payment.discountValue)}%)"
                } else {
                    "Diskon"
                }
            SummaryLine(label, "- ${totals.discount.toRupiah()}")
        }
        if (totals.tax > 0) SummaryLine("Pajak", totals.tax.toRupiah())
        HorizontalDivider(Modifier.padding(vertical = 1.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Total",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            CompactPaymentSwitch(
                selected = payment.method,
                onToggle = {
                    val next =
                        if (payment.method == PaymentMethod.CASH) {
                            PaymentMethod.QRIS
                        } else {
                            PaymentMethod.CASH
                        }
                    onAction(PosAction.SetPaymentMethod(next))
                },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                totals.total.toRupiah(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        PayMoneyField(
            value = payment.paid,
            total = totals.total,
            onValueChange = { onAction(PosAction.SetPaid(it)) },
            modifier = Modifier.fillMaxWidth().height(34.dp),
        )
        if (payment.paid > 0) {
            val change = payment.change
            when {
                change < 0L -> {
                    SummaryLine(
                        "Kurang Bayar",
                        kotlin.math.abs(change).toRupiah(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                change > 0L -> {
                    ChangeGivenField(
                        maxChange = change,
                        value = payment.changeGivenOverride,
                        onValueChange = { onAction(PosAction.SetChangeGivenOverride(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (payment.method == PaymentMethod.QRIS) {
                        val effectiveChangeGiven =
                            payment.changeGivenOverride
                                ?.coerceIn(0L, change) ?: change
                        QrisCashChangeToggle(
                            changeGivenAmount = effectiveChangeGiven,
                            isCash = payment.changeGivenInCash,
                            onToggle = { onAction(PosAction.SetChangeGivenInCash(it)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                else -> {
                    SummaryLine(
                        "Kembalian",
                        0L.toRupiah(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
@Composable
private fun CompactPaymentSwitch(
    selected: PaymentMethod,
    onToggle: () -> Unit,
) {
    val isCash = selected == PaymentMethod.CASH
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isCash) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    },
                ).clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (isCash) "Tunai" else "QRIS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color =
                if (isCash) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
        )
        Icon(
            Icons.Rounded.SwapHoriz,
            contentDescription = "Ganti metode bayar",
            modifier = Modifier.size(14.dp),
            tint =
                if (isCash) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
        )
    }
}
@Composable
private fun PayMoneyField(
    value: Long,
    total: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) {
        mutableStateOf(if (value <= 0) "" else value.toString())
    }
    BasicTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            text = digits
            onValueChange(digits.toLongOrNull() ?: 0L)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        visualTransformation = ThousandsSeparatorTransformation,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(10.dp),
                        ).padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Bayar: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                    }
                    innerTextField()
                }
                InlinePresetChip(
                    label = "50rb",
                    onClick = { onValueChange(50_000L) },
                )
                Spacer(Modifier.width(3.dp))
                InlinePresetChip(
                    label = "100rb",
                    onClick = { onValueChange(100_000L) },
                )
            }
        },
    )
}
@Composable
private fun InlinePresetChip(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable(onClick = onClick)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
@Composable
private fun ChangeGivenField(
    maxChange: Long,
    value: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveValue = value?.coerceIn(0L, maxChange) ?: maxChange
    var text by remember(effectiveValue) { mutableStateOf(effectiveValue.toString()) }
    val tip = (maxChange - effectiveValue).coerceAtLeast(0L)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                text = digits
                val parsed = (digits.toLongOrNull() ?: 0L).coerceIn(0L, maxChange)
                onValueChange(if (parsed == maxChange) null else parsed)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            visualTransformation = ThousandsSeparatorTransformation,
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
            modifier = Modifier.fillMaxWidth().height(34.dp),
            decorationBox = { innerTextField ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp),
                            ).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Kembalian: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(Modifier.weight(1f)) {
                        if (text.isEmpty()) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            )
                        }
                        innerTextField()
                    }
                }
            },
        )
        if (tip > 0L) {
            Text(
                text = "Tip: ${tip.toRupiah()}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
@Composable
private fun QrisCashChangeToggle(
    changeGivenAmount: Long,
    isCash: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor =
        if (isCash) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        }
    val contentColor =
        if (isCash) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .clickable { onToggle(!isCash) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isCash) Icons.Rounded.AttachMoney else Icons.Rounded.QrCode,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Kembalian Tunai dari Laci?",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Text(
                text =
                    if (isCash) {
                        "⚠ ${changeGivenAmount.toRupiah()} dari laci"
                    } else {
                        "Non-tunai — tidak mengurangi kas"
                    },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = isCash,
            onCheckedChange = onToggle,
            modifier = Modifier.height(24.dp),
        )
    }
}
@Composable
internal fun SummaryLine(
    label: String,
    value: String,
    emphasize: Boolean = false,
    color: Color = Color.Unspecified,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style =
                if (emphasize) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            color = color,
        )
        Text(
            value,
            style =
                if (emphasize) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
    }
}
@Composable
internal fun MoneyField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) {
        mutableStateOf(if (value <= 0) "" else value.toString())
    }
    BasicTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            text = digits
            onValueChange(digits.toLongOrNull() ?: 0L)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        visualTransformation = ThousandsSeparatorTransformation,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$label: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}
@Composable
internal fun DiscountField(
    type: DiscountType,
    rawValue: Double,
    onToggleType: () -> Unit,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(type) {
        mutableStateOf(
            when {
                rawValue <= 0.0 -> ""
                type == DiscountType.NOMINAL -> rawValue.toLong().toString()
                else -> formatPercentTrim(rawValue)
            },
        )
    }
    LaunchedEffect(type, rawValue) {
        val parsed = text.toDoubleOrNull() ?: 0.0
        val isDifferent = kotlin.math.abs(parsed - rawValue) > 1e-9
        if (isDifferent && (rawValue == 0.0 || !text.endsWith("."))) {
            text =
                when {
                    rawValue <= 0.0 -> ""
                    type == DiscountType.NOMINAL -> rawValue.toLong().toString()
                    else -> formatPercentTrim(rawValue)
                }
        }
    }
    val isNominal = type == DiscountType.NOMINAL
    val keyboardType = if (isNominal) KeyboardType.Number else KeyboardType.Decimal
    val visualTransformation =
        if (isNominal) ThousandsSeparatorTransformation else VisualTransformation.None
    BasicTextField(
        value = text,
        onValueChange = { input ->
            val cleaned =
                if (isNominal) {
                    input.filter { it.isDigit() }
                } else {
                    buildString {
                        var dotSeen = false
                        for (c in input) {
                            when {
                                c.isDigit() -> {
                                    append(c)
                                }
                                c == '.' && !dotSeen -> {
                                    append(c)
                                    dotSeen = true
                                }
                            }
                        }
                    }
                }
            text = cleaned
            onValueChange(cleaned.toDoubleOrNull() ?: 0.0)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        visualTransformation = visualTransformation,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                            .clickable(onClick = onToggleType)
                            .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isNominal) "Rp" else "%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "⟲",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.6f)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Box(
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}
@Composable
internal fun DecimalField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) {
        mutableStateOf(if (value <= 0.0) "" else formatPercentTrim(value))
    }
    BasicTextField(
        value = text,
        onValueChange = { input ->
            val cleaned =
                buildString {
                    var dotSeen = false
                    for (c in input) {
                        when {
                            c.isDigit() -> {
                                append(c)
                            }
                            c == '.' && !dotSeen -> {
                                append(c)
                                dotSeen = true
                            }
                        }
                    }
                }
            text = cleaned
            onValueChange(cleaned.toDoubleOrNull() ?: 0.0)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$label: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}
@Composable
private fun ProductDetailPopup(
    product: ProductEntity,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.8f, animationSpec = tween(250)),
                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200))
            ) {
                GlassCard(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(16.dp)
                        .clickable( // Mencegah klik tembus ke background
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} 
                        ),
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PopupDetailRow("Kategori", product.category.ifBlank { "-" })
                            PopupDetailRow("SKU", product.sku)
                            if (product.barcode?.isNotBlank() == true) {
                                PopupDetailRow("Barcode", product.barcode)
                            }
                            PopupDetailRow("Harga Jual", product.price.toRupiah())
                            PopupDetailRow("Stok Tersedia", product.stock.formatQuantity())
                            if (product.damagedStock > 0) {
                                PopupDetailRow("Stok Rusak", product.damagedStock.formatQuantity(), isError = true)
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tutup", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupDetailRow(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}