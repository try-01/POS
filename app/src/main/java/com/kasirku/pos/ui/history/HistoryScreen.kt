package com.kasirku.pos.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasirku.pos.data.local.relation.TransactionWithItems
import com.kasirku.pos.ui.receipt.PdfReceiptExporter
import com.kasirku.pos.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDetail by remember { mutableStateOf<TransactionWithItems?>(null) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Riwayat & Laporan Penjualan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Range Pilihan Waktu
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeRange.values().forEach { range ->
                    val selected = uiState.selectedRange == range
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onTimeRangeChanged(range) },
                        label = { Text(range.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kartu Ringkasan Kinerja (Glassmorphic Card)
            SummaryGlassCard(
                revenue = uiState.summary.totalRevenue,
                count = uiState.summary.transactionCount
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Daftar Transaksi (${uiState.transactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada transaksi pada rentang waktu ini", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.transactions, key = { it.transaction.id }) { tx ->
                        TransactionCard(
                            tx = tx,
                            onClick = { selectedDetail = tx },
                            onExportPdf = {
                                PdfReceiptExporter.exportToPdfAndShare(context, tx)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Detail Struk
    selectedDetail?.let { tx ->
        ReceiptDetailBottomSheet(
            transactionWithItems = tx,
            onDismiss = { selectedDetail = null },
            onExportPdf = {
                PdfReceiptExporter.exportToPdfAndShare(context, tx)
            }
        )
    }
}

@Composable
private fun SummaryGlassCard(revenue: Double, count: Int) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total Pendapatan", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = CurrencyFormatter.format(revenue),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$count", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Transaksi", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    tx: TransactionWithItems,
    onClick: () -> Unit,
    onExportPdf: () -> Unit
) {
    val dateStr = remember(tx.transaction.createdAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID")).format(Date(tx.transaction.createdAt))
    }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.transaction.invoiceNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${tx.items.sumOf { it.quantity }} item terjual | ${tx.transaction.paymentMethod}", style = MaterialTheme.typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyFormatter.format(tx.transaction.grandTotal),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onExportPdf, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Ekspor PDF", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptDetailBottomSheet(
    transactionWithItems: TransactionWithItems,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit
) {
    val tx = transactionWithItems.transaction
    val dateStr = remember(tx.createdAt) {
        SimpleDateFormat("dd MMMM yyyy - HH:mm:ss", Locale("in", "ID")).format(Date(tx.createdAt))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Detail Struk Penjualan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("No. Invoice: ${tx.invoiceNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text("Waktu: $dateStr", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Daftar Item (${transactionWithItems.items.size}):", fontWeight = FontWeight.Bold)
            transactionWithItems.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.quantity}x ${item.productName}", modifier = Modifier.weight(1f))
                    Text(CurrencyFormatter.format(item.subtotal), fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal")
                Text(CurrencyFormatter.format(tx.subtotal))
            }
            if (tx.discountAmount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Diskon")
                    Text("-${CurrencyFormatter.format(tx.discountAmount)}", color = Color(0xFF2E7D32))
                }
            }
            if (tx.taxAmount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pajak")
                    Text(CurrencyFormatter.format(tx.taxAmount))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Tagihan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(CurrencyFormatter.format(tx.grandTotal), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dibayar (${tx.paymentMethod})")
                Text(CurrencyFormatter.format(tx.paidAmount))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kembalian")
                Text(CurrencyFormatter.format(tx.changeAmount), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onExportPdf,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan / Bagikan PDF Struk", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
