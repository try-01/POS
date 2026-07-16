package com.pos.offline.ui.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StoreProfileDialog(
    viewModel: StoreProfileViewModel,
    onDismiss: () -> Unit
) {
    // FIX PERFORMA: Menggunakan lifecycle-aware state collection (menghemat memori & baterai)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form = uiState.formState

    // FIX UX: Menjadikan viewModel sebagai key agar siklus hidup Effect lebih aman
    LaunchedEffect(viewModel) {
        viewModel.loadFormFromCurrentProfile()
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.pickLogo(uri)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Profil Toko & Struk",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        // FIX ERROR: Memanggil icon via route lengkap receiver Icons.Rounded
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(), // FIX UX: Mencegah textfield tertutup oleh keyboard
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ---- Logo ----
                    Text("Logo Toko", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            LogoPreview(
                                logoBytes = form.logoBytes,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            if (uiState.isProcessingLogo) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            OutlinedButton(
                                onClick = { pickImageLauncher.launch("image/*") },
                                enabled = !uiState.isProcessingLogo
                            ) {
                                // FIX ERROR & BENTROK NAMA: Memanggil Icons.Rounded.Image tanpa alias ImageIcon
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (form.logoBytes == null) "Pilih Logo" else "Ganti Logo", fontSize = 12.sp)
                            }
                            if (form.logoBytes != null) {
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { viewModel.clearLogo() },
                                    enabled = !uiState.isProcessingLogo
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Hapus Logo", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Text(
                        "Logo akan otomatis disesuaikan ukurannya & diubah ke hitam-putih " +
                            "untuk dicetak di struk thermal.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // ---- Info Toko ----
                    OutlinedTextField(
                        value = form.storeName,
                        onValueChange = viewModel::updateStoreName,
                        label = { Text("Nama Toko", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.address,
                        onValueChange = viewModel::updateAddress,
                        label = { Text("Alamat", fontSize = 12.sp) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.footerNote,
                        onValueChange = viewModel::updateFooterNote,
                        label = { Text("Catatan Footer Struk", fontSize = 12.sp) },
                        placeholder = { Text("mis. Terima kasih telah berbelanja!", fontSize = 12.sp) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ---- Auto Print ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cetak Otomatis", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "Otomatis cetak struk ke printer utama setiap transaksi selesai.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = form.autoPrintEnabled,
                            onCheckedChange = viewModel::updateAutoPrintEnabled
                        )
                    }

                    Button(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving && !uiState.isProcessingLogo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Simpan", fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** Preview logo dari ByteArray -- dipakai juga di SettingsScreen untuk
 * thumbnail ringkasan, karena itu TIDAK private. Dekode ulang dari
 * ByteArray tiap kali [logoBytes] berubah (di-cache via remember key). */
@Composable
fun LogoPreview(logoBytes: ByteArray?, modifier: Modifier = Modifier) {
    // FIX PERFORMA: Memindahkan decode gambar ke Background Thread (Dispatchers.IO) agar UI tidak patah/freeze
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = logoBytes) {
        value = if (logoBytes != null) {
            withContext(Dispatchers.IO) {
                runCatching { 
                    BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)?.asImageBitmap() 
                }.getOrNull()
            }
        } else {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Logo toko",
            contentScale = ContentScale.Crop,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
