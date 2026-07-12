package com.pos.offline.ui.report

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.ui.components.GlassCard
import com.pos.offline.util.toRupiah
import java.time.Instant

/**
 * Layar Laporan Harian.
 *
 * Performa:
 *  - Grafik memakai [Canvas] langsung (1 pass draw, tanpa library chart) → ringan.
 *  - Daftar transaksi `LazyColumn` dengan `key` + `contentType`.
 *  - State dikoleksi sadar-siklus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val isToday by viewModel.isToday.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Laporan Harian", fontWeight = FontWeight.SemiBold) })
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- Navigator tanggal ----------
            item(key = "date_navigator") {
                DateNavigator(
                    label = selectedDate.format(ReportViewModel.dateFmt),
                    isToday = isToday,
                    onPrevious = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                    onToday = viewModel::goToday
                )
            }

            // ---------- Ringkasan ----------
            item(key = "summary") { SummarySection(report = report) }

            // ---------- Grafik per jam (hanya bila ada transaksi) ----------
            if (report.transactionCount > 0) {
                item(key = "chart") { HourlyRevenueChart(hourly = report.hourlyRevenue) }
            }

            // ---------- Daftar transaksi ----------
            item(key = "list_header") {
                Text(
                    "Daftar Transaksi (${report.transactionCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (report.transactions.isEmpty()) {
                item(key = "empty") { EmptyReport() }
            } else {
                items(
                    items = report.transactions,
                    key = { it.id },
                    contentType = { "transaction" }
                ) { tx ->
                    TransactionRow(tx)
                }
            }
        }
    }
}

// ============================ NAVIGATOR TANGGAL ============================

@Composable
private fun DateNavigator(
    label: String,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Hari sebelumnya")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isToday) {
                        Text(
                            "Hari ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = !isToday) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Hari berikutnya",
                        tint = if (isToday) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Tombol lompat ke hari ini muncul hanya saat tidak sedang di hari ini.
            if (!isToday) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onToday) {
                        Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ke Hari Ini")
                    }
                }
            }
        }
    }
}

// ============================ RINGKASAN ============================

@Composable
private fun SummarySection(report: DailyReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Kartu utama: total pendapatan (glassmorphism sebagai titik fokus).
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column {
                Text("Total Pendapatan", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    report.totalRevenue.toRupiah(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${report.transactionCount} transaksi" +
                        if (report.totalDiscount > 0 || report.totalTax > 0)
                            " · diskon ${report.totalDiscount.toRupiah()} · pajak ${report.totalTax.toRupiah()}"
                        else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Dua kartu statistik pendukung (Surface datar = murah di GPU).
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Jumlah Transaksi",
                value = report.transactionCount.toString(),
                icon = Icons.Rounded.ReceiptLong
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Rata-rata / Transaksi",
                value = report.averagePerTransaction.toRupiah(),
                icon = Icons.Rounded.ShowChart
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

// ============================ GRAFIK BATANG PER JAM ============================

/**
 * Grafik batang pendapatan per jam (0–23). Digambar manual dengan [Canvas]:
 * tidak ada dependency chart library, hanya satu pass draw → sangat ringan.
 * Garis bantu horizontal membantu membaca skala.
 */
@Composable
private fun HourlyRevenueChart(hourly: List<Long>) {
    val max = hourly.maxOrNull() ?: 0L
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    // Cari jam puncak untuk keterangan.
    val peakHour = hourly.indices.maxByOrNull { hourly[it] } ?: 0
    val peakValue = hourly.getOrElse(peakHour) { 0L }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pendapatan per Jam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (max > 0L) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Jam ramai: ${"%02d".format(peakHour)}.00 · ${peakValue.toRupiah()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val w = size.width
                val h = size.height
                // Garis bantu horizontal (4 tingkat).
                val gridCount = 4
                for (i in 1..gridCount) {
                    val y = h * i / gridCount
                    drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                }
                if (max == 0L) return@Canvas // tak ada data → hanya garis bantu

                // 24 batang dengan jalan kecil antar batang.
                val n = 24
                val gap = 3.dp.toPx()
                val barWidth = (w - gap * (n - 1)) / n
                hourly.forEachIndexed { hour, value ->
                    if (value <= 0) return@forEachIndexed
                    val ratio = value.toFloat() / max.toFloat()
                    val barHeight = h * ratio
                    val x = hour * (barWidth + gap)
                    val y = h - barHeight
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f) // ujung membulat
                    )
                }
            }

            // Penanda sumbu jam (00, 06, 12, 18) — spasi berbobot agar selaras batang.
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "06", "12", "18").forEach {
                    Text(
                        "$it.00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

// ============================ BARIS TRANSAKSI ============================

@Composable
private fun TransactionRow(tx: TransactionEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Waktu transaksi (HH:mm).
            Text(
                ReportViewModel.timeFmt.format(Instant.ofEpochMilli(tx.createdAt)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tx.id,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Dibayar ${tx.paidAmount.toRupiah()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Text(
                tx.total.toRupiah(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyReport() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Belum ada transaksi pada hari ini",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
