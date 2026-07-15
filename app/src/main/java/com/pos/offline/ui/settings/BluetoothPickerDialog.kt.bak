package com.pos.offline.ui.settings

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pos.offline.util.BluetoothDeviceInfo
import com.pos.offline.util.PermissionUtils

/**
 * Dialog pemilihan printer Bluetooth: gerbang permission -> gerbang
 * Bluetooth-on -> daftar perangkat ter-pairing -> pencarian perangkat baru
 * -> PIN dialog hybrid. Menutup diri sendiri otomatis saat pairing sukses
 * (lewat [PrinterViewModel.pairingSuccess]) atau saat memilih perangkat
 * yang sudah ter-pairing.
 */
@Composable
fun BluetoothPickerDialog(
    viewModel: PrinterViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.bluetoothUiState.collectAsState()
    var hasPermission by remember { mutableStateOf(viewModel.hasBluetoothPermissions()) }
    var btEnabled by remember { mutableStateOf(viewModel.isBluetoothEnabled()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        if (hasPermission) viewModel.refreshBondedDevices()
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        btEnabled = viewModel.isBluetoothEnabled()
        if (hasPermission && btEnabled) viewModel.refreshBondedDevices()
    }

    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.refreshBondedDevices()
    }

    LaunchedEffect(Unit) {
        viewModel.pairingSuccess.collect { onDismiss() }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetBluetoothPicker() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Printer Bluetooth", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    !hasPermission -> {
                        Text(
                            "Aplikasi memerlukan izin Bluetooth untuk mencari & " +
                                "memasangkan printer.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { permissionLauncher.launch(PermissionUtils.requiredBluetoothPermissions()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Izinkan Akses Bluetooth", fontSize = 13.sp)
                        }
                    }
                    !btEnabled -> {
                        Text(
                            "Bluetooth perangkat sedang nonaktif. Aktifkan terlebih dahulu.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aktifkan Bluetooth", fontSize = 13.sp)
                        }
                    }
                    else -> {
                        Text("Perangkat Terpasang", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (state.bondedDevices.isEmpty()) {
                            Text(
                                "Belum ada printer ter-pairing di pengaturan Bluetooth HP ini.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.bondedDevices.forEach { device ->
                                DeviceRow(device = device, trailing = null) {
                                    viewModel.selectBondedDevice(device)
                                    onDismiss()
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Perangkat Baru", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            TextButton(
                                onClick = {
                                    if (state.isScanning) viewModel.stopDiscovery() else viewModel.startDiscovery()
                                }
                            ) {
                                Text(if (state.isScanning) "Berhenti" else "Cari Perangkat", fontSize = 11.sp)
                            }
                        }
                        if (state.isScanning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Mencari perangkat di sekitar…", fontSize = 11.sp)
                            }
                        }
                        val newDevices = state.discoveredDevices.filterNot { found ->
                            state.bondedDevices.any { it.address == found.address }
                        }
                        newDevices.forEach { device ->
                            DeviceRow(device = device, trailing = "Pasang") {
                                viewModel.requestPairing(device)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", fontSize = 13.sp) }
        }
    )

    state.pairingTarget?.let { target ->
        BluetoothPinDialog(
            device = target,
            isPairing = state.isPairing,
            onDismiss = { viewModel.cancelPairing() },
            onConfirm = { pin -> viewModel.confirmPairing(pin) }
        )
    }
}

@Composable
private fun DeviceRow(device: BluetoothDeviceInfo, trailing: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(device.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(device.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Text(trailing, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BluetoothPinDialog(
    device: BluetoothDeviceInfo,
    isPairing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isPairing) onDismiss() },
        title = { Text("Masukkan PIN", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pasangkan dengan \"${device.name}\". PIN printer thermal " +
                        "umumnya 0000 atau 1234, namun bisa berbeda tiap unit.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pin = "0000" }, enabled = !isPairing) {
                        Text("0000", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { pin = "1234" }, enabled = !isPairing) {
                        Text("1234", fontSize = 12.sp)
                    }
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() } },
                    label = { Text("PIN Kustom", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !isPairing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pin) }, enabled = !isPairing && pin.isNotBlank()) {
                if (isPairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Pasangkan", fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPairing) { Text("Batal", fontSize = 13.sp) }
        }
    )
}