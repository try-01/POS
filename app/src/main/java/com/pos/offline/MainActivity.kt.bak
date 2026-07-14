package com.pos.offline

import android.content.res.Configuration
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pos.offline.data.di.ServiceLocator
import com.pos.offline.ui.inventory.InventoryScreen
import com.pos.offline.ui.inventory.InventoryViewModel
import com.pos.offline.ui.pos.PosScreen
import com.pos.offline.ui.pos.PosViewModel
import com.pos.offline.ui.receipt.ReceiptManager
import com.pos.offline.ui.report.ReportScreen
import com.pos.offline.ui.report.ReportViewModel
import com.pos.offline.ui.settings.SettingsScreen
import com.pos.offline.ui.settings.SettingsViewModel
import com.pos.offline.ui.theme.PosTheme
import kotlinx.coroutines.launch

private enum class Dest(val label: String) {
    POS("Kasir"), INVENTORY("Inventaris"), REPORT("Laporan"), SETTINGS("Pengaturan")
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
    val settingsViewModel: SettingsViewModel =
        viewModel(factory = ServiceLocator.settingsViewModelFactory())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dest by remember { mutableStateOf(Dest.POS) }

    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

if (isLandscape) {
    Row(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .imePadding()
        ) {
            ScreenContent(dest, posViewModel, inventoryViewModel, reportViewModel, settingsViewModel, context, scope, isLandscape = true)
        }
        SideNavRail(selected = dest, onSelect = { dest = it })
    }
} else {
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
        ) {
            ScreenContent(dest, posViewModel, inventoryViewModel, reportViewModel, settingsViewModel, context, scope, isLandscape = false)
        }

        AnimatedVisibility(
            visible = !imeVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            BottomNavBar(selected = dest, onSelect = { dest = it })
        }
    }
}
}

/** Konten layar aktif (Kasir/Inventaris/Laporan/Pengaturan) — dipisah agar tidak duplikat antara mode potret & landscape. */
@Composable
private fun ScreenContent(
    dest: Dest,
    posViewModel: PosViewModel,
    inventoryViewModel: InventoryViewModel,
    reportViewModel: ReportViewModel,
    settingsViewModel: SettingsViewModel,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    isLandscape: Boolean
) {
    when (dest) {
        Dest.POS -> PosScreen(
            viewModel = posViewModel,
            forceWideLayout = isLandscape,
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
        Dest.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
    }
}

/** Bottom Nav — dipakai di mode potret. */
@Composable
private fun BottomNavBar(selected: Dest, onSelect: (Dest) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Dest.entries.forEach { item ->
                val isSelected = selected == item
                val color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon(),
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Rail navigasi vertikal tipis untuk mode landscape. Selalu terlihat di sisi
 * kanan layar, tidak memakan ruang horizontal signifikan (lebar tetap ~64dp),
 * dan tidak perlu di-expand — konsisten dengan prinsip "1 tap ke mana saja".
 */
@Composable
private fun SideNavRail(selected: Dest, onSelect: (Dest) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(64.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Dest.entries.forEach { item ->
                val isSelected = selected == item
                val color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                              else androidx.compose.ui.graphics.Color.Transparent

                Column(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onSelect(item) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon(),
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = color,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun Dest.icon() = when (this) {
    Dest.POS -> Icons.Rounded.ShoppingCart
    Dest.INVENTORY -> Icons.Rounded.Inventory2
    Dest.REPORT -> Icons.Rounded.Assessment
    Dest.SETTINGS -> Icons.Rounded.Settings
}