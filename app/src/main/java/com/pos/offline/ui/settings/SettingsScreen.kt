package com.pos.offline.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pos.offline.data.backup.BackupManager
import com.pos.offline.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // --- SAF launchers ---
    // Mime "application/octet-stream" untuk export (generik biner, dialog
    // "Simpan sebagai" akan pakai nama file yang kita usulkan).
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) viewModel.exportDatabase(context, uri) }

    // Mime "*/*" untuk import — sengaja TIDAK difilter ke tipe spesifik,
    // karena banyak perangkat tidak mengenali asosiasi mime SQLite dan bisa
    // menyembunyikan file backup dari picker kalau difilter ketat.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.requestRestore(uri) }

    // Toast sekali-pakai dari ViewModel (konsisten dgn pola print/export PDF).
    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    if (uiState.pendingRestoreUri != null) {
        RestoreConfirmDialog(
            onDismiss = { viewModel.cancelRestore() },
            onConfirm = {
                viewModel.confirmRestore(context) {
                    BackupManager.restartApp(context)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pengaturan",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("Cadangkan & Pulihkan")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Cadangan tersimpan sebagai satu berkas (.db). Simpan " +
                                "di tempat aman (mis. Google Drive/folder pribadi) — " +
                                "aplikasi tidak menyinkronkan data secara otomatis.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { exportLauncher.launch(BackupManager.suggestedBackupFileName()) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Menyimpan…", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Buat Cadangan (Export)", fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth()
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

            SectionLabel("Kasir & Shift")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Kelola Kasir — segera hadir",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 2.dp)
    )
}

@Composable
private fun RestoreConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pulihkan Cadangan?", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Memulihkan cadangan akan MENGGANTI SELURUH data saat ini " +
                    "(produk, transaksi, kasir) dan menutup aplikasi. " +
                    "Tindakan ini tidak bisa dibatalkan. Lanjutkan?",
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Ya, Timpa & Pulihkan", fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal", fontSize = 13.sp)
            }
        }
    )
}