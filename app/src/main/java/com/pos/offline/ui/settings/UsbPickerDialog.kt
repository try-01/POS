package com.pos.offline.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pos.offline.util.UsbDeviceInfo

@Composable
fun UsbPickerDialog(
    viewModel: PrinterViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.usbUiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshUsbDevices()
    }

    DisposableEffect(Unit) {
        viewModel.startObservingUsbAttachment()
        onDispose { viewModel.resetUsbPicker() }
    }

    LaunchedEffect(Unit) {
        viewModel.usbSelectionSuccess.collect { onDismiss() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Printer USB", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Pastikan printer tersambung via kabel USB/OTG ke perangkat ini.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.isRequestingPermission) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Menunggu izin akses USB…", fontSize = 11.sp)
                    }
                }

                if (state.devices.isEmpty()) {
                    Text(
                        "Tidak ada perangkat USB terdeteksi.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.devices.forEach { device ->
                        UsbDeviceRow(
                            device = device,
                            enabled = !state.isRequestingPermission,
                            onClick = { viewModel.selectUsbDevice(device) }
                        )
                    }
                }

                TextButton(
                    onClick = { viewModel.refreshUsbDevices() },
                    enabled = !state.isRequestingPermission
                ) {
                    Text("Segarkan Daftar", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", fontSize = 13.sp) }
        }
    )
}

@Composable
private fun UsbDeviceRow(device: UsbDeviceInfo, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(device.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                String.format("VID:PID %04x:%04x", device.vendorId, device.productId),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("Pilih", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}