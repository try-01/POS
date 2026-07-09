package com.example.kasirpos.ui.receipt

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kasirpos.data.local.entity.TransactionEntity
import com.example.kasirpos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(viewModel: ReceiptViewModel) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id")) }

    Scaffold(containerColor = PrimaryDark) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("🧾 Cetak Struk", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // ── Daftar Transaksi (pilih untuk cetak) ──────────
            Text("Pilih Transaksi", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            if (state.transactions.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada transaksi", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.transactions, key = { it.id }) { tx ->
                        val isSelected = state.selectedTransaction?.id == tx.id
                        TransactionSelectableRow(
                            tx = tx,
                            dateFormat = dateFormat,
                            isSelected = isSelected,
                            onClick = { viewModel.selectTransaction(tx) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Preview Struk & Tombol Cetak ──────────────────
            if (state.selectedTransaction != null) {
                ReceiptPreviewCard(
                    transaction = state.selectedTransaction!!,
                    items = state.selectedItems,
                    dateFormat = dateFormat
                )

                Spacer(Modifier.height(12.dp))

                // Input alamat printer Bluetooth
                var printerAddress by remember { mutableStateOf("") }
                var storeName by remember { mutableStateOf("Toko Saya") }

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Nama Toko") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = printerAddress,
                    onValueChange = { printerAddress = it },
                    label = { Text("MAC Address Printer (00:11:22:AA:BB:CC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.printReceipt(printerAddress, storeName) },
                    enabled = printerAddress.isNotBlank() && !state.isPrinting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mencetak...")
                    } else {
                        Icon(Icons.Filled.Print, "Cetak", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("🖨️ Cetak via Bluetooth", fontWeight = FontWeight.Bold)
                    }
                }

                // Status cetak
                state.printResult?.let { result ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result,
                        color = if (result.contains("berhasil")) AccentGreen else AccentRed,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionSelectableRow(
    tx: TransactionEntity,
    dateFormat: SimpleDateFormat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentBlue else GlassBorder
    val bg = if (isSelected) AccentBlue.copy(alpha = 0.1f) else GlassSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("#${tx.id} • ${tx.paymentMethod.uppercase()}", color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(dateFormat.format(Date(tx.createdAt)), color = TextMuted, fontSize = 11.sp)
        }
        Text(
            "Rp ${"%,d".format(tx.grandTotal)}",
            color = AccentGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReceiptPreviewCard(
    transaction: TransactionEntity,
    items: List<com.example.kasirpos.data.local.entity.TransactionItemEntity>,
    dateFormat: SimpleDateFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("Preview Struk", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "═".repeat(36),
            color = TextMuted,
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth()
        )
        items.forEach { item ->
            Text(
                "%-18s %2d× %-8s".format(
                    if (item.productName.length > 16) item.productName.take(14) + ".." else item.productName,
                    item.quantity,
                    "%,d".format(item.unitPrice)
                ),
                color = TextPrimary,
                fontSize = 11.sp
            )
        }
        Text("═".repeat(36), color = TextMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
        Text(
            "TOTAL: Rp ${"%,d".format(transaction.grandTotal)}",
            color = AccentGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            dateFormat.format(Date(transaction.createdAt)),
            color = TextMuted,
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
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

private fun String.format(vararg args: Any?): String =
    java.lang.String.format(java.util.Locale("id"), this, *args)
