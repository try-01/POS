package com.pos.offline.ui.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pos.offline.data.local.entity.PrinterConnectionType
import com.pos.offline.data.local.entity.PrinterEntity

@Composable
fun PrinterPickerDialog(
    printers: List<PrinterEntity>,
    onSelect: (PrinterEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Printer") },
        text = {
            Column {
                Text(
                    "Struk akan dicetak ulang ke printer yang dipilih. Jika gagal, sistem TIDAK " +
                        "akan mencoba printer lain secara otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                LazyColumn {
                    items(printers, key = { it.id }) { printer ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(printer) }
                                    .padding(vertical = 12.dp),
                        ) {
                            Text(printer.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                connectionSummary(printer),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

private fun connectionSummary(printer: PrinterEntity): String =
    when (printer.connectionType) {
        PrinterConnectionType.WIFI -> "WiFi \u2022 ${printer.wifiIpAddress}:${printer.wifiPort}"
        PrinterConnectionType.BLUETOOTH -> "Bluetooth"
        PrinterConnectionType.USB -> "USB"
    }
