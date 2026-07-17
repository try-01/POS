package com.pos.offline

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pos.offline.data.di.ServiceLocator
import com.pos.offline.ui.inventory.InventoryScreen
import com.pos.offline.ui.inventory.InventoryViewModel
import com.pos.offline.ui.pos.PosScreen
import com.pos.offline.ui.pos.PosViewModel
import com.pos.offline.ui.receipt.ReceiptManager
import com.pos.offline.ui.report.ReportScreen
import com.pos.offline.ui.report.ReportViewModel
import com.pos.offline.ui.settings.PrinterViewModel
import com.pos.offline.ui.settings.SettingsScreen
import com.pos.offline.ui.settings.SettingsViewModel
import com.pos.offline.ui.settings.StoreProfileViewModel
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
    val printerViewModel: PrinterViewModel =
        viewModel(factory = ServiceLocator.printerViewModelFactory())
    val storeProfileViewModel: StoreProfileViewModel =
        viewModel(factory = ServiceLocator.storeProfileViewModelFactory())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dest by rememberSaveable { mutableStateOf(Dest.POS) }

    // Memantau status shift aktif secara dinamis dari DB
    val openShift by posViewModel.openShift.collectAsStateWithLifecycle()

    // State Dialog Exit tersentralisasi di Root Level
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    // Intercept Back HP 1: Jika tidak berada di halaman Kasir, kembalikan ke Kasir
    BackHandler(enabled = dest != Dest.POS) {
        dest = Dest.POS
    }

    // Intercept Back HP 2: Jika sudah di Kasir dan dialog belum muncul, tampilkan dialog
    BackHandler(enabled = dest == Dest.POS && !showExitDialog) {
        showExitDialog = true
    }

    // Dialog Konfirmasi Keluar Tersentralisasi (Root Level)
    if (showExitDialog) {
        val shift = openShift
        if (shift != null) {
            // Skenario A: Masih ada shift aktif (Warning Keras)
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                icon = {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        "Ada Shift Kasir Aktif!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Shift kasir atas nama ${shift.cashierName} masih berjalan. " +
                                "Untuk keakuratan laporan keuangan dan laci kas (rekonsiliasi uang fisik), " +
                                "sangat disarankan untuk menutup shift terlebih dahulu di tab Kasir.",
                            fontSize = 13.sp
                        )
                        Text(
                            "Catatan: Jika Anda memilih 'Tetap Keluar', sesi shift akan tetap aktif menggantung " +
                                "dan harus ditutup secara normal saat aplikasi dibuka kembali.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    // Tutup Shift Dulu (Bobot Visual: Tinggi / Filled Button)
                    Button(
                        onClick = {
                            showExitDialog = false
                            dest = Dest.POS
                        }
                    ) {
                        Text("Tutup Shift Dulu", fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tetap Keluar (Bobot Visual: Rendah / Text Button dengan warna merah bahaya)
                        TextButton(
                            onClick = {
                                showExitDialog = false
                                (context as? android.app.Activity)?.finishAndRemoveTask()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Tetap Keluar", fontSize = 13.sp)
                        }
                        // Batal (Bobot Visual: Menengah / Outlined Button)
                        OutlinedButton(
                            onClick = { showExitDialog = false }
                        ) {
                            Text("Batal", fontSize = 13.sp)
                        }
                    }
                }
            )
        } else {
            // Skenario B: Tidak ada shift aktif (Konfirmasi Bersih)
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Text(
                        "Keluar Aplikasi?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Semua data transaksi dan laci kas Anda telah tersimpan dengan aman di database lokal. " +
                            "Sesi kasir Anda saat ini bersih (tidak ada shift berjalan). Keluar sekarang?",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            (context as? android.app.Activity)?.finishAndRemoveTask()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Keluar", fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Batal", fontSize = 13.sp)
                    }
                }
            )
        }
    }

    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SideNavRail(selected = dest, onSelect = { dest = it })
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .imePadding()
            ) {
                ScreenContent(
                    dest = dest,
                    posViewModel = posViewModel,
                    inventoryViewModel = inventoryViewModel,
                    reportViewModel = reportViewModel,
                    settingsViewModel = settingsViewModel,
                    printerViewModel = printerViewModel,
                    storeProfileViewModel = storeProfileViewModel,
                    context = context,
                    scope = scope,
                    onNavigateToSettings = { dest = Dest.SETTINGS },
                    onRequestExit = { showExitDialog = true },
                    isLandscape = true
                )
            }
        }
    } else {
        // Hitung tinggi System Navigation Bar (Gesture Bar)
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        
        // Animasikan padding bawah konten agar selalu sejajar dengan tinggi BottomNavBar
        // Saat keyboard aktif, padding jadi 0. Saat tidak aktif, padding sebesar tinggi BottomNavBar (56.dp + navBarHeight).
        val contentBottomPadding by animateDpAsState(
            targetValue = if (imeVisible) 0.dp else (56.dp + navBarHeight),
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "contentBottomPadding"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Konten Utama dengan padding bawah dinamis dan imePadding murni
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentBottomPadding)
                    .imePadding()
            ) {
                ScreenContent(
                    dest = dest,
                    posViewModel = posViewModel,
                    inventoryViewModel = inventoryViewModel,
                    reportViewModel = reportViewModel,
                    settingsViewModel = settingsViewModel,
                    printerViewModel = printerViewModel,
                    storeProfileViewModel = storeProfileViewModel,
                    context = context,
                    scope = scope,
                    onNavigateToSettings = { dest = Dest.SETTINGS },
                    onRequestExit = { showExitDialog = true },
                    isLandscape = false
                )
            }

            // BottomNavBar dilayangkan (overlay) di atas konten utama
            AnimatedVisibility(
                visible = !imeVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomNavBar(selected = dest, onSelect = { dest = it })
            }
        }
    }
}

@Composable
private fun ScreenContent(
    dest: Dest,
    posViewModel: PosViewModel,
    inventoryViewModel: InventoryViewModel,
    reportViewModel: ReportViewModel,
    settingsViewModel: SettingsViewModel,
    printerViewModel: PrinterViewModel,
    storeProfileViewModel: StoreProfileViewModel,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onNavigateToSettings: () -> Unit,
    onRequestExit: () -> Unit,
    isLandscape: Boolean
) {
    when (dest) {
        Dest.POS -> PosScreen(
            viewModel = posViewModel,
            forceWideLayout = isLandscape,
            onNavigateToSettings = onNavigateToSettings,
            onSharePdfFile = { file ->
                context.startActivity(ReceiptManager.buildPdfShareIntent(context, file))
            },
            onExportPdf = { result ->
                val file = ReceiptManager.exportToPdf(context, result)
                Toast.makeText(context, "Struk tersimpan: ${file.name}", Toast.LENGTH_LONG).show()
            }
        )
        Dest.INVENTORY -> InventoryScreen(viewModel = inventoryViewModel)
        Dest.REPORT -> ReportScreen(
            viewModel = reportViewModel,
            onNavigateToSettings = onNavigateToSettings,
            onSharePdfFile = { file ->
                context.startActivity(ReceiptManager.buildPdfShareIntent(context, file))
            },
            onExportPdf = { result ->
                val file = ReceiptManager.exportToPdf(context, result)
                Toast.makeText(context, "Struk tersimpan: ${file.name}", Toast.LENGTH_LONG).show()
            },
            onShare = { result ->
                val intent = ReceiptManager.buildShareIntent(context, result)
                context.startActivity(intent)
            }
        )
        Dest.SETTINGS -> SettingsScreen(
            viewModel = settingsViewModel,
            printerViewModel = printerViewModel,
            storeProfileViewModel = storeProfileViewModel,
            onExitClick = onRequestExit
        )
    }
}

@Composable
private fun BottomNavBar(selected: Dest, onSelect: (Dest) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
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

@Composable
private fun SideNavRail(selected: Dest, onSelect: (Dest) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(64.dp)
            .systemBarsPadding(),
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