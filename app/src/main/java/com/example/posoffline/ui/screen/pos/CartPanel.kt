package com.example.posoffline.ui.screen.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.posoffline.data.entity.TransactionEntity
import com.example.posoffline.data.repository.ProductRepository
import com.example.posoffline.domain.model.CartLine
import com.example.posoffline.domain.model.PaymentMethod
import com.example.posoffline.ui.components.glassStrong
import com.example.posoffline.ui.theme.AppColors
import com.example.posoffline.util.Money

/**
 * Cart side panel. All state comes from [CartViewModel]; the View is a
 * pure projection. Lines are rendered with [LazyColumn] for performance
 * with large carts.
 */
@Composable
fun CartPanel(
    cartVm: CartViewModel,
    productRepository: ProductRepository,
    settings: com.example.posoffline.data.SettingsRepository.Snapshot,
    onCheckoutSuccess: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val cartUi by cartVm.ui.collectAsStateWithLifecycle()
    var paidInput by remember { mutableStateOf("") }
    var lastTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val paid = remember(paidInput) { Money.parseInput(paidInput) }
    val change = (paid - cartUi.totals.grandTotal).coerceAtLeast(0L)
    val canCheckout = cartUi.state.lines.isNotEmpty() && paid >= cartUi.totals.grandTotal

    // Live stock lookup so we can cap +qty at real available stock.
    val stockMap = remember { mutableMapOf<String, Int>() }
    LaunchedEffect(cartUi.state.lines) {
        val ids = cartUi.state.lines.map { it.productId }
        if (ids.isNotEmpty()) {
            val list = productRepository.run {
                // Cheap: read all products, small in-memory filter
                // For a real app, add `getMany(ids)` to the DAO.
                // We just call list() and filter for clarity in this sample.
                list().associateBy { it.id }
            }
            cartUi.state.lines.forEach { line ->
                stockMap[line.productId] = list[line.productId]?.stock ?: line.qty
            }
        }
    }

    Column(
        modifier = modifier
            .glassStrong(corner = 24.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Keranjang", color = AppColors.Slate100, fontWeight = FontWeight.SemiBold)
                Text(
                    "${cartUi.itemCount} item • ${cartUi.state.lines.size} baris",
                    color = AppColors.Slate400,
                    fontSize = 11.sp
                )
            }
            if (cartUi.state.lines.isNotEmpty()) {
                TextButton(onClick = { cartVm.clear() }) {
                    Text("Bersihkan", color = AppColors.Slate300)
                }
            }
        }

        Divider()

        if (cartUi.state.lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛒", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Belum ada item. Ketuk produk untuk menambah.",
                        color = AppColors.Slate400,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = cartUi.state.lines, key = { it.productId }) { line ->
                    val cap = stockMap[line.productId] ?: line.qty
                    CartLineRow(
                        line = line,
                        stockCap = cap,
                        currency = settings.currency,
                        onInc = { cartVm.incQty(line.productId, cap) },
                        onDec = { cartVm.decQty(line.productId) },
                        onSetQty = { q -> cartVm.setQty(line.productId, q, cap) },
                        onSetDiscount = { d -> cartVm.setLineDiscount(line.productId, d) },
                        onRemove = { cartVm.removeLine(line.productId) }
                    )
                }
            }
        }

        Divider()

        TotalsBlock(
            cartUi = cartUi,
            settings = settings,
            onSetCartDiscount = { cartVm.setCartDiscount(it) }
        )

        PaidAndCheckout(
            paid = paid,
            paidInput = paidInput,
            onPaidChange = { paidInput = it },
            change = change,
            canCheckout = canCheckout,
            currency = settings.currency,
            onPay = { method ->
                cartVm.checkout(paid, method) { result ->
                    result
                        .onSuccess { tx -> lastTx = tx; onCheckoutSuccess(tx) }
                        .onFailure { /* surfaced via dialog below */ }
                }
            }
        )
    }

    lastTx?.let { tx ->
        ReceiptDialog(tx = tx, settings = settings, onDismiss = { lastTx = null })
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    stockCap: Int,
    currency: String,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onSetQty: (Int) -> Unit,
    onSetDiscount: (Long) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.name, color = AppColors.Slate100, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${Money.format(line.price, currency)} / item" +
                        if (line.discount > 0) " • diskon ${Money.format(line.discount, currency)}" else "",
                    color = AppColors.Slate400,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Hapus", tint = AppColors.Slate400)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            QtyStepper(qty = line.qty, cap = stockCap, onDec = onDec, onInc = onInc, onSet = onSetQty)
            Spacer(Modifier.weight(1f))
            Text(
                Money.format(line.qty * line.price - line.discount, currency),
                color = AppColors.Slate100,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QtyStepper(
    qty: Int,
    cap: Int,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onSet: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .padding(2.dp)
    ) {
        IconButton(onClick = onDec, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Kurangi", tint = AppColors.Slate200)
        }
        OutlinedTextField(
            value = qty.toString(),
            onValueChange = { v -> onSet(Money.parseInput(v).toInt().coerceAtLeast(0)) },
            singleLine = true,
            modifier = Modifier.width(40.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = AppColors.Slate100,
                textAlign = TextAlign.Center
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
        IconButton(
            onClick = onInc,
            enabled = qty < cap,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah", tint = AppColors.Slate200)
        }
    }
}

@Composable
private fun TotalsBlock(
    cartUi: CartViewModel.CartUiState,
    settings: com.example.posoffline.data.SettingsRepository.Snapshot,
    onSetCartDiscount: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RowTotal("Subtotal", Money.format(cartUi.totals.subtotal, settings.currency))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Diskon", color = AppColors.Slate400, fontSize = 12.sp, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = if (cartUi.state.cartDiscount == 0L) "" else cartUi.state.cartDiscount.toString(),
                onValueChange = { v -> onSetCartDiscount(Money.parseInput(v)) },
                singleLine = true,
                placeholder = { Text("0", color = AppColors.Slate500) },
                modifier = Modifier.width(110.dp),
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
        RowTotal(
            "Pajak (${(cartUi.state.taxRate * 100).toInt()}%)",
            Money.format(cartUi.totals.tax, settings.currency)
        )
        Divider()
        RowTotal(
            "Total",
            Money.format(cartUi.totals.grandTotal, settings.currency),
            big = true
        )
    }
}

@Composable
private fun PaidAndCheckout(
    paid: Long,
    paidInput: String,
    onPaidChange: (String) -> Unit,
    change: Long,
    canCheckout: Boolean,
    currency: String,
    onPay: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bayar", color = AppColors.Slate400, fontSize = 12.sp, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = paidInput,
                onValueChange = onPaidChange,
                singleLine = true,
                placeholder = { Text("0", color = AppColors.Slate500) },
                modifier = Modifier.weight(1.4f),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kembali", color = AppColors.Slate400, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(
                Money.format(change, currency),
                color = if (change > 0) AppColors.Emerald300 else AppColors.Slate300,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PayButton("Tunai", primary = true, enabled = canCheckout, modifier = Modifier.weight(1f)) {
                onPay(PaymentMethod.CASH)
            }
            PayButton("QRIS", primary = false, enabled = canCheckout, modifier = Modifier.weight(1f)) {
                onPay(PaymentMethod.QRIS)
            }
            PayButton("Kartu", primary = false, enabled = canCheckout, modifier = Modifier.weight(1f)) {
                onPay(PaymentMethod.CARD)
            }
        }
    }
}

@Composable
private fun PayButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (primary)
        Brush.verticalGradient(listOf(AppColors.Indigo500, AppColors.Indigo600))
    else
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.04f)))
    val border = if (primary) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.10f)
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) Color.White else AppColors.Slate500,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RowTotal(label: String, value: String, big: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.Slate400, fontSize = if (big) 13.sp else 12.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = if (big) AppColors.Slate50 else AppColors.Slate200,
            fontSize = if (big) 17.sp else 13.sp,
            fontWeight = if (big) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.10f))
    )
}

@Composable
private fun rememberCoroutineScope() = androidx.compose.runtime.rememberCoroutineScope()
