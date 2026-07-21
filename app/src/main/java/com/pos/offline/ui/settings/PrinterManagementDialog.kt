package com.pos.offline.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pos.offline.data.local.entity.PaperWidth
import com.pos.offline.data.local.entity.PrinterConnectionType
import com.pos.offline.data.local.entity.PrinterEntity
import com.pos.offline.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterManagementDialog(
    viewModel: PrinterViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val printers by viewModel.printers.collectAsState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Kelola Printer",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (printers.isEmpty()) {
                        Text(
                            "Belum ada printer ditambahkan. Tambahkan printer WiFi/LAN, " +
                                "Bluetooth, atau USB.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        printers.forEachIndexed { index, printer ->
                            val isTesting = uiState.testingPrinterIds.contains(printer.id)
                            PrinterRow(
                                printer = printer,
                                isFirst = index == 0,
                                isLast = index == printers.lastIndex,
                                isTesting = isTesting,
                                onEdit = { viewModel.openEditDialog(printer) },
                                onDelete = { viewModel.requestDelete(printer.id) },
                                onSetDefault = { viewModel.setAsDefault(printer) },
                                onMoveUp = { viewModel.movePriorityUp(printer) },
                                onMoveDown = { viewModel.movePriorityDown(printer) },
                                onTestPrint = { viewModel.testPrint(printer) },
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.openAddDialog() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Printer", fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (uiState.showFormDialog) {
        PrinterFormDialog(viewModel = viewModel)
    }

    if (uiState.pendingDeleteId != null) {
        val target = printers.find { it.id == uiState.pendingDeleteId }
        DeletePrinterConfirmDialog(
            printerLabel = target?.label.orEmpty(),
            onDismiss = { viewModel.cancelDelete() },
            onConfirm = { viewModel.confirmDelete() },
        )
    }
}

@Composable
private fun PrinterRow(
    printer: PrinterEntity,
    isFirst: Boolean,
    isLast: Boolean,
    isTesting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTestPrint: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        printer.connectionType.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(printer.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (printer.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                "UTAMA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onMoveUp, enabled = !isFirst && !isTesting, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Naikkan prioritas", modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast && !isTesting, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Turunkan prioritas", modifier = Modifier.size(14.dp))
                    }
                }
            }

            Text(
                text = printerSubtitle(printer),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!printer.isDefault) {
                    TextButton(onClick = onSetDefault, enabled = !isTesting) {
                        Text("Jadikan Utama", fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onTestPrint, enabled = !isTesting, modifier = Modifier.size(28.dp)) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = "Test Print",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(onClick = onEdit, enabled = !isTesting, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, enabled = !isTesting, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Hapus",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterFormDialog(viewModel: PrinterViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.formState
    val isEdit = form.id != null
    var showBluetoothPicker by remember { mutableStateOf(false) }
    var showUsbPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.closeFormDialog() },
        title = {
            Text(if (isEdit) "Edit Printer" else "Tambah Printer", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = form.label,
                    onValueChange = viewModel::updateFormLabel,
                    label = { Text("Nama Printer", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Jenis Koneksi", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = form.connectionType == PrinterConnectionType.WIFI,
                        onClick = { viewModel.updateFormConnectionType(PrinterConnectionType.WIFI) },
                        label = { Text("WiFi/LAN", fontSize = 11.sp) },
                    )
                    FilterChip(
                        selected = form.connectionType == PrinterConnectionType.BLUETOOTH,
                        onClick = { viewModel.updateFormConnectionType(PrinterConnectionType.BLUETOOTH) },
                        label = { Text("Bluetooth", fontSize = 11.sp) },
                    )
                    FilterChip(
                        selected = form.connectionType == PrinterConnectionType.USB,
                        onClick = { viewModel.updateFormConnectionType(PrinterConnectionType.USB) },
                        label = { Text("USB", fontSize = 11.sp) },
                    )
                }

                if (form.connectionType == PrinterConnectionType.WIFI) {
                    OutlinedTextField(
                        value = form.wifiIpAddress,
                        onValueChange = viewModel::updateFormWifiIp,
                        label = { Text("Alamat IP Printer", fontSize = 12.sp) },
                        placeholder = { Text("mis. 192.168.1.50", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = form.wifiPort,
                        onValueChange = viewModel::updateFormWifiPort,
                        label = { Text("Port", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (form.connectionType == PrinterConnectionType.BLUETOOTH) {
                    OutlinedButton(
                        onClick = { showBluetoothPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (form.bluetoothMacAddress.isBlank()) {
                                "Pilih Perangkat Bluetooth"
                            } else {
                                "Ganti Perangkat (${form.bluetoothMacAddress})"
                            },
                            fontSize = 12.sp,
                        )
                    }
                }

                if (form.connectionType == PrinterConnectionType.USB) {
                    OutlinedButton(
                        onClick = { showUsbPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        val vid = form.usbVendorId
                        val pid = form.usbProductId
                        Text(
                            if (vid == null || pid == null) {
                                "Pilih Perangkat USB"
                            } else {
                                "Ganti Perangkat (${String.format("%04x:%04x", vid, pid)})"
                            },
                            fontSize = 12.sp,
                        )
                    }
                }

                Text("Lebar Kertas", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = form.paperWidth == PaperWidth.MM_58,
                        onClick = { viewModel.updateFormPaperWidth(PaperWidth.MM_58) },
                        label = { Text("58mm", fontSize = 11.sp) },
                    )
                    FilterChip(
                        selected = form.paperWidth == PaperWidth.MM_80,
                        onClick = { viewModel.updateFormPaperWidth(PaperWidth.MM_80) },
                        label = { Text("80mm", fontSize = 11.sp) },
                    )
                }

                OutlinedTextField(
                    value = form.charPerLine,
                    onValueChange = viewModel::updateFormCharPerLine,
                    label = { Text("Karakter per Baris", fontSize = 12.sp) },
                    supportingText = {
                        Text(
                            "Default mengikuti lebar kertas. Ubah manual bila hasil " +
                                "cetak terlalu sempit/lebar.",
                            fontSize = 10.sp,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Deteksi Status Kertas", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Printer mendukung deteksi kertas habis", fontSize = 12.sp)
                        Text(
                            "Aktifkan HANYA jika Anda yakin printer ini mendukung status-query. " +
                                "Kebanyakan printer thermal umum (termasuk RPP02N) TIDAK mendukung " +
                                "-- deteksi otomatis belum tersedia, atur manual di sini.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = form.supportsStatusQuery,
                        onCheckedChange = viewModel::updateFormSupportsStatusQuery,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.saveForm() }, enabled = !uiState.isSaving) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (isEdit) "Simpan" else "Tambah", fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeFormDialog() }) {
                Text("Batal", fontSize = 13.sp)
            }
        },
    )

    if (showBluetoothPicker) {
        BluetoothPickerDialog(
            viewModel = viewModel,
            onDismiss = { showBluetoothPicker = false },
        )
    }

    if (showUsbPicker) {
        UsbPickerDialog(
            viewModel = viewModel,
            onDismiss = { showUsbPicker = false },
        )
    }
}

@Composable
private fun DeletePrinterConfirmDialog(
    printerLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Printer?", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Printer \"$printerLabel\" akan dihapus permanen dan tidak bisa " +
                    "dikembalikan. Lanjutkan?",
                fontSize = 13.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Ya, Hapus", fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal", fontSize = 13.sp)
            }
        },
    )
}

private fun printerSubtitle(printer: PrinterEntity): String =
    when (printer.connectionType) {
        PrinterConnectionType.WIFI -> {
            "${printer.wifiIpAddress}:${printer.wifiPort} • ${printer.charPerLine} kar/baris (${printer.paperWidth.label()})"
        }

        PrinterConnectionType.BLUETOOTH -> {
            "${printer.bluetoothMacAddress ?: "-"} • ${printer.charPerLine} kar/baris"
        }

        PrinterConnectionType.USB -> {
            val vid = printer.usbVendorId
            val pid = printer.usbProductId
            val idLabel = if (vid != null && pid != null) String.format("%04x:%04x", vid, pid) else "-"
            "USB $idLabel • ${printer.charPerLine} kar/baris"
        }
    }

private fun PaperWidth.label(): String =
    when (this) {
        PaperWidth.MM_58 -> "58mm"
        PaperWidth.MM_80 -> "80mm"
    }

private fun PrinterConnectionType.icon() =
    when (this) {
        PrinterConnectionType.WIFI -> Icons.Rounded.Wifi
        PrinterConnectionType.BLUETOOTH -> Icons.Rounded.Bluetooth
        PrinterConnectionType.USB -> Icons.Rounded.Usb
    }
