package com.kasirku.pos.ui.screens.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasirku.pos.ui.components.formatRupiah
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    transactionId: Long,
    viewModel: ReceiptViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val transaction by viewModel.transaction.collectAsState()
    val items by viewModel.items.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ReceiptUiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
                is ReceiptUiEvent.ShowError -> snackbarHostState.showSnackbar("❌ \${event.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🧾 Struk") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        transaction?.let { tx ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("★ KASIRKU POS ★", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Toko Serba Ada", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(tx.invoiceNumber, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tx.transactionDate)),
                            style = MaterialTheme.typography.bodySmall
                        )
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))

                        items.forEach { item ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(item.productName, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text("\${item.quantity}x", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall)
                                Text(formatRupiah(item.subtotal), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal")
                            Text(formatRupiah(tx.subtotal))
                        }
                        if (tx.taxAmount > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pajak (\${tx.taxPercent.toInt()}%)")
                                Text(formatRupiah(tx.taxAmount))
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL", fontWeight = FontWeight.Bold)
                            Text(formatRupiah(tx.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bayar")
                            Text(formatRupiah(tx.paymentAmount))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kembali")
                            Text(formatRupiah(tx.changeAmount))
                        }

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("Terima kasih! 🙏", textAlign = TextAlign.Center)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::printReceipt, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Print, null, Modifier.padding(end = 8.dp))
                        Text("Cetak")
                    }
                    Button(onClick = viewModel::exportPdf, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PictureAsPdf, null, Modifier.padding(end = 8.dp))
                        Text("Export PDF")
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
