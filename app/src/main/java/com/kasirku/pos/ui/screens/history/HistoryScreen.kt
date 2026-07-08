package com.kasirku.pos.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasirku.pos.ui.components.formatRupiah
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onViewReceipt: (Long) -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val totalRevenue by viewModel.totalRevenue.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Riwayat", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total Pendapatan", style = MaterialTheme.typography.labelMedium)
                    Text(formatRupiah(totalRevenue), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("\${transactions.size} transaksi", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (transactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", style = MaterialTheme.typography.displayLarge)
                        Text("Belum ada transaksi", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(transactions, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onViewReceipt(tx.id) }
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(tx.invoiceNumber, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tx.transactionDate)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("\${tx.itemCount} item", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(formatRupiah(tx.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
