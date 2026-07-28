package com.pos.offline.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // FIX: Menggunakan import Lifecycle-aware
import com.pos.offline.data.backup.BackupManager
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    printerViewModel: PrinterViewModel,
    storeProfileViewModel: StoreProfileViewModel,
    onExitClick: () -> Unit,
) {
    val context = LocalContext.current
    
    // FIX: Mengubah semua .collectAsState() menjadi .collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cashiers by viewModel.cashiers.collectAsStateWithLifecycle()
    val printers by printerViewModel.printers.collectAsStateWithLifecycle()
    val storeProfile by storeProfileViewModel.profile.collectAsStateWithLifecycle()

    val isSoundEnabled by viewModel.isSoundEnabled.collectAsStateWithLifecycle()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()

    var showPrinterDialog by remember { mutableStateOf(false) }
    var showStoreProfileDialog by remember { mutableStateOf(false) }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri -> if (uri != null) viewModel.exportDatabase(uri) }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri -> if (uri != null) viewModel.requestRestore(uri) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        printerViewModel.messages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        storeProfileViewModel.messages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareIntent.collect { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Tidak ada aplikasi untuk membagikan file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (uiState.pendingRestoreUri != null) {
        RestoreConfirmDialog(
            onDismiss = { viewModel.cancelRestore() },
            onConfirm = {
                viewModel.confirmRestore {
                    BackupManager.restartApp(context)
                }
            },
        )
    }

    if (uiState.showAddCashierDialog) {
        AddCashierDialog(
            onDismiss = { viewModel.closeAddCashierDialog() },
            onConfirm = { name -> viewModel.addCashier(name) },
        )
    }

    if (showPrinterDialog) {
        PrinterManagementDialog(
            viewModel = printerViewModel,
            onDismiss = { showPrinterDialog = false },
        )
    }

    if (showStoreProfileDialog) {
        StoreProfileDialog(
            viewModel = storeProfileViewModel,
            onDismiss = { showStoreProfileDialog = false },
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Pengaturan",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ================= UMPAN BALIK PEMINDAI (SUARA & GETARAN) =================
            SectionLabel("Umpan Balik Pemindai (Scanner Feedback)")

            FuturisticFeedbackControls(
                isSoundEnabled = isSoundEnabled,
                isVibrationEnabled = isVibrationEnabled,
                onSoundToggle = { viewModel.setSoundEnabled(it) },
                onVibrationToggle = { viewModel.setVibrationEnabled(it) },
            )

            // ================= CADANGKAN & PULIHKAN =================
            SectionLabel("Cadangkan & Pulihkan")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Cadangan tersimpan sebagai satu berkas (.db). Simpan " +
                                "ke folder pilihan Anda, atau bagikan langsung ke " +
                                "WhatsApp/Email/Drive — aplikasi tidak menyinkronkan " +
                                "data secara otomatis.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { exportLauncher.launch(BackupManager.suggestedBackupFileName()) },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Simpan", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.shareDatabase() },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (uiState.isSharing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Bagikan", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Memulihkan…", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pulihkan Cadangan (Restore)", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ================= KELOLA KASIR =================
            SectionLabel("Kelola Kasir")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cashiers.isEmpty()) {
                        Text(
                            "Belum ada kasir. Fitur ini opsional — aplikasi tetap " +
                                "bisa dipakai tanpa memilih kasir/shift sama sekali.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        cashiers.forEach { cashier ->
                            CashierRow(
                                cashier = cashier,
                                onToggleActive = { active ->
                                    viewModel.setCashierActive(cashier.id, active)
                                },
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.openAddCashierDialog() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Kasir", fontSize = 13.sp)
                    }
                }
            }

            // ================= PROFIL TOKO & STRUK =================
            SectionLabel("Profil Toko & Struk")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (storeProfile.storeName.isBlank() && storeProfile.logoBytes == null) {
                        Text(
                            "Profil toko belum diatur. Nama toko, alamat, & logo akan " +
                                "tampil di struk cetak.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LogoPreview(
                                logoBytes = storeProfile.logoBytes,
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    storeProfile.storeName.ifBlank { "(Nama toko belum diisi)" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    if (storeProfile.autoPrintEnabled) "Cetak otomatis aktif" else "Cetak otomatis nonaktif",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showStoreProfileDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Profil Toko", fontSize = 13.sp)
                    }
                }
            }

            // ================= PRINTER STRUK =================
            SectionLabel("Printer Struk")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (printers.isEmpty()) {
                        Text(
                            "Belum ada printer thermal ditambahkan. Struk tetap bisa " +
                                "dicetak/dibagikan sebagai PDF tanpa printer fisik.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val defaultPrinter = printers.find { it.isDefault }
                        Text(
                            "Printer utama: ${defaultPrinter?.label ?: "-"}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "${printers.size} printer tersimpan",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedButton(
                        onClick = { showPrinterDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kelola Printer", fontSize = 13.sp)
                    }
                }
            }

            // ================= SESI APLIKASI =================
            SectionLabel("Sesi Aplikasi")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Keluar dari aplikasi kasir dengan aman. Semua data penjualan Anda tetap tersimpan utuh di memori lokal perangkat.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onExitClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Keluar Aplikasi",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Keluar Aplikasi", fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun FuturisticFeedbackControls(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Respon Scan Real-Time",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold
                )
            }

            FuturisticSliderItem(
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                title = "Suara Beep Halus",
                subtitle = if (isSoundEnabled) "Aktif (Nada Beep POS 65%)" else "Muted (Senyap)",
                isEnabled = isSoundEnabled,
                onToggle = onSoundToggle
            )

            FuturisticSliderItem(
                icon = Icons.Rounded.Vibration,
                title = "Getaran Haptic Smooth",
                subtitle = if (isVibrationEnabled) "Aktif (Sentuhan Haptic Soft)" else "Nonaktif",
                isEnabled = isVibrationEnabled,
                onToggle = onVibrationToggle
            )
        }
    }
}

@Composable
private fun FuturisticSliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    val targetValue = if (isEnabled) 1.0f else 0.0f
    var sliderPosition by remember(isEnabled) { mutableFloatStateOf(targetValue) }
    val animatedPosition by animateFloatAsState(targetValue = sliderPosition, label = "sliderPos")

    val accentColor by animateColorAsState(
        targetValue = if (isEnabled) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "accentColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) activeColor else Color.Gray.copy(alpha = 0.5f))
            )
            Spacer(Modifier.width(8.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = activeColor
                )
            )
        }

        Spacer(Modifier.height(4.dp))
        Slider(
            value = animatedPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
                if (newValue > 0.5f && !isEnabled) {
                    onToggle(true)
                } else if (newValue <= 0.5f && isEnabled) {
                    onToggle(false)
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = inactiveColor
            ),
            modifier = Modifier.height(20.dp)
        )
    }
}

@Composable
private fun CashierRow(
    cashier: CashierEntity,
    onToggleActive: (Boolean) -> Unit,
) {
    val contentColor =
        if (cashier.active) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = cashier.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Medium,
                color = contentColor,
            )
            Text(
                text = if (cashier.active) "Aktif" else "Nonaktif",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color =
                    if (cashier.active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
            )
        }
        Switch(
            checked = cashier.active,
            onCheckedChange = onToggleActive,
            colors = SwitchDefaults.colors(),
        )
    }
}

@Composable
private fun AddCashierDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Kasir", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Kasir", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
                Text("Tambah", fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", fontSize = 13.sp)
            }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 2.dp),
    )
}

@Composable
private fun RestoreConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pulihkan Cadangan?", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Memulihkan cadangan akan MENGGANTI SELURUH data saat ini " +
                    "(produk, transaksi, kasir) dan menutup aplikasi. " +
                    "Tindakan ini tidak bisa dibatalkan. Lanjutkan?",
                fontSize = 13.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Ya, Timpa & Pulihkan", fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal", fontSize = 13.sp)
            }
        },
    )
}
