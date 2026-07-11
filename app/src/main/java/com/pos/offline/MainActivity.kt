package com.pos.offline

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.NavigationBar
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

/** Tujuan navigasi. Ringan: beralih via state, tanpa library navigasi tambahan. */
private enum class Dest(val label: String) {
    POS("Kasir"), INVENTORY("Inventaris"), REPORT("Laporan")
}

/**
 * Activity tunggal yang menjadi host Jetpack Compose.
 * ViewModel dibuat via [ServiceLocator] factory (DI manual) — bukan Hilt —
 * agar start-up cepat & footprint RAM kecil.
 */
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

/**
 * Akar UI: bottom navigation 3 tujuan (Kasir, Inventaris, Laporan).
 *
 * Menggunakan Column + NavigationBar (bukan Scaffold bersarang) agar penanganan
 * inset sistem (status bar / nav bar) tetap satu lapis dan tidak dobel-padding.
 *
 * Ketiga ViewModel dibuat sekali di sini (scoped ke Activity) sehingga state
 * terjaga saat berpindah tab. Layar yang tidak tampil otomatis berhenti
 * mengoleksi Flow (hemat baterai) berkat [collectAsStateWithLifecycle] + WhileSubscribed.
 */
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

    Column(Modifier.fillMaxSize()) {
        // Konten layar mengisi sisa ruang di atas bottom navigation.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (dest) {
                Dest.POS -> PosScreen(
                    viewModel = posViewModel,
                    onPrintBluetooth = { result ->
                        // Cetak Bluetooth di background; butuh izin runtime
                        // BLUETOOTH_CONNECT + printer yang sudah dipasang (paired).
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

        NavigationBar {
        // --- NAVIGATION BAR KUSTOM SUPER COMPACT ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp) 
                .height(48.dp),
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
                        // Ikon sedikit dikecilkan agar pas di ruang 44.dp
                        modifier = Modifier.size(18.dp) 
                    )
                    // Hapus Spacer agar teks menempel rapi di bawah ikon
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
