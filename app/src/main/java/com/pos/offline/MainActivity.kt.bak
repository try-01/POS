package com.pos.offline

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pos.offline.data.di.ServiceLocator
import com.pos.offline.ui.inventory.InventoryScreen
import com.pos.offline.ui.inventory.InventoryViewModel
import com.pos.offline.ui.pos.PosScreen
import com.pos.offline.ui.pos.PosViewModel
import com.pos.offline.ui.receipt.ReceiptManager
import com.pos.offline.ui.report.ReportScreen
import com.pos.offline.ui.report.ReportViewModel
import com.pos.offline.ui.theme.PosTheme
import kotlinx.coroutines.launch

private enum class Dest(val label: String) {
    POS("Kasir"), INVENTORY("Inventaris"), REPORT("Laporan")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PosTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val posViewModel: PosViewModel = viewModel(factory = ServiceLocator.posViewModelFactory())
    val inventoryViewModel: InventoryViewModel =
        viewModel(factory = ServiceLocator.inventoryViewModelFactory())
    val reportViewModel: ReportViewModel =
        viewModel(factory = ServiceLocator.reportViewModelFactory())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dest by remember { mutableStateOf(Dest.POS) }

    // Deteksi status keyboard di level root — dipakai untuk menyembunyikan
    // Bottom Nav (bukan ikut naik) supaya konten (mis. keranjang kasir)
    // mendapat ruang maksimal saat keyboard aktif.
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    Column(Modifier.fillMaxSize()) {
        // Konten layar mengisi sisa ruang di atas bottom navigation.
        // imePadding() dipasang di sini (bukan di Column terluar) supaya saat
        // Bottom Nav disembunyikan, Box ini otomatis melebar mengisi ruang
        // ekstra tersebut sebelum dipangkas ulang oleh tinggi keyboard.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
        ) {
            when (dest) {
                Dest.POS -> PosScreen(
                    viewModel = posViewModel,
                    onPrintBluetooth = { result ->
                        scope.launch {
                            val ok = ReceiptManager.printToFirstBonded(context, result)
                            Toast.makeText(
                                context,
                                if (ok) "Mengirim ke printer…"
                                else "Gagal: pasang & izinkan printer Bluetooth",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onExportPdf = { result ->
                        val file = ReceiptManager.exportToPdf(context, result)
                        Toast.makeText(context, "Struk tersimpan: ${file.name}", Toast.LENGTH_LONG).show()
                    }
                )

                Dest.INVENTORY -> InventoryScreen(viewModel = inventoryViewModel)
                Dest.REPORT -> ReportScreen(viewModel = reportViewModel)
            }
        }

        // Bottom Nav disembunyikan (bukan ikut naik) saat keyboard aktif,
        // supaya layar kasir dapat ruang vertikal maksimal untuk mengetik
        // diskon/pajak/bayar tanpa terganggu navigasi di bawahnya.
        AnimatedVisibility(
            visible = !imeVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Dest.entries.forEach { item ->
                        val selected = dest == item
                        val color = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { dest = item },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (item) {
                                    Dest.POS -> Icons.Rounded.ShoppingCart
                                    Dest.INVENTORY -> Icons.Rounded.Inventory2
                                    Dest.REPORT -> Icons.Rounded.Assessment
                                },
                                contentDescription = item.label,
                                tint = color,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = item.label,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}