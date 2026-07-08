package com.example.posoffline.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.posoffline.data.entity.TransactionEntity
import com.example.posoffline.ui.components.glass
import com.example.posoffline.ui.components.glassStrong
import com.example.posoffline.ui.theme.AppColors

/**
 * History + today's summary. Read-only screen.
 */
@Composable
fun HistoryScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: TransactionViewModel = viewModel(
        factory = TransactionViewModel.Factory(container.transactionRepository)
    )
    val list by vm.list.collectAsStateWithLifecycle()
    val (todayList, today) by vm.today.collectAsStateWithLifecycle()

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
            Text("Riwayat", color = AppColors.Slate50, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today summary
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxSize()
                    .glassStrong(corner = 20.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Hari Ini", color = AppColors.Slate50, fontWeight = FontWeight.SemiBold)
                StatCard("Pendapatan", vm.format(today.revenue, "Rp"), big = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Transaksi", today.count.toString(), Modifier.weight(1f))
                    StatCard("Rata-rata", vm.format(today.average, "Rp"), Modifier.weight(1f))
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Semua data tersimpan 100% di perangkat ini (offline).",
                    color = AppColors.Slate400, fontSize = 11.sp
                )
            }

            // History list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .glass(corner = 20.dp)
                    .padding(8.dp)
            ) {
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada transaksi.", color = AppColors.Slate400)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items = list, key = { it.id }) { tx ->
                            TxRow(tx = tx, format = { v, c -> vm.format(v, c) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, big: Boolean = false) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(label, color = AppColors.Slate400, fontSize = 10.sp)
        Text(
            value,
            color = if (big) AppColors.Slate50 else AppColors.Slate200,
            fontSize = if (big) 20.sp else 14.sp,
            fontWeight = if (big) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun TxRow(tx: TransactionEntity, format: (Long, String) -> String) {
    val itemCount = remember(tx.itemsJson) {
        runCatching {
            val items = kotlinx.serialization.json.Json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(
                    com.example.posoffline.data.entity.TransactionItem.serializer()
                ),
                tx.itemsJson
            )
            items.sumOf { it.qty }
        }.getOrDefault(0)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(tx.invoiceNo, color = AppColors.Indigo300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID"))
                    .format(java.util.Date(tx.createdAt)),
                color = AppColors.Slate400, fontSize = 11.sp
            )
        }
        Text(
            "$itemCount item",
            color = AppColors.Slate300, fontSize = 11.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(tx.paymentMethod, color = AppColors.Slate300, fontSize = 11.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            format(tx.grandTotal, "Rp"),
            color = AppColors.Slate100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
    }
}
