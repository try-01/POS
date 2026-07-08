package com.example.posoffline.ui.screen.pos

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posoffline.data.SettingsRepository
import com.example.posoffline.data.entity.TransactionEntity
import com.example.posoffline.data.entity.TransactionItem
import com.example.posoffline.receipt.EscPosBuilder
import com.example.posoffline.receipt.ReceiptRenderer
import com.example.posoffline.receipt.ReceiptShare
import com.example.posoffline.ui.theme.AppColors
import com.example.posoffline.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Modal receipt preview.
 *
 * Two outputs:
 *  - PNG bitmap rendered offscreen for sharing / printing.
 *  - ESC/POS plain text for the future BT-printer integration.
 */
@Composable
fun ReceiptDialog(
    tx: TransactionEntity,
    settings: SettingsRepository.Snapshot,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val items = remember(tx.itemsJson) {
        runCatching {
            Json.decodeFromString(ListSerializer(TransactionItem.serializer()), tx.itemsJson)
        }.getOrElse { emptyList() }
    }

    val bitmap by produceState<Bitmap?>(null, tx, settings) {
        value = withContext(Dispatchers.Default) {
            ReceiptRenderer.render(tx, items, settings, widthPx = 384)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                OutlinedButton(onClick = {
                    ReceiptShare.shareText(ctx, EscPosBuilder.build(tx, items, settings), tx.invoiceNo)
                }) { Text("⎙ ESC/POS") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { bitmap?.let { ReceiptShare.sharePng(ctx, it, tx.invoiceNo) } },
                    enabled = bitmap != null,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Indigo500)
                ) { Text("⬇ Unduh PNG") }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Tutup") }
        },
        title = {
            Column {
                Text("Transaksi Berhasil", color = AppColors.Slate50, fontWeight = FontWeight.SemiBold)
                Text(tx.invoiceNo, color = AppColors.Slate400, fontSize = 11.sp)
            }
        },
        text = {
            Column {
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Struk"
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Menyiapkan struk…", color = AppColors.Slate400) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Stat("Subtotal", Money.format(tx.subtotal, settings.currency), Modifier.weight(1f))
                    if (tx.discount > 0) Stat("Diskon", "-${Money.format(tx.discount, settings.currency)}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Stat("Pajak (${(tx.taxRate * 100).toInt()}%)", Money.format(tx.tax, settings.currency), Modifier.weight(1f))
                    Stat("Total", Money.format(tx.grandTotal, settings.currency), Modifier.weight(1f), big = true)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Stat("Bayar", Money.format(tx.paid, settings.currency), Modifier.weight(1f))
                    Stat("Kembali", Money.format(tx.change, settings.currency), Modifier.weight(1f))
                }
            }
        },
        containerColor = AppColors.Bg1,
        textContentColor = AppColors.Slate200,
        titleContentColor = AppColors.Slate50
    )
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, big: Boolean = false) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Text(label, color = AppColors.Slate400, fontSize = 10.sp)
        Text(
            value,
            color = if (big) AppColors.Slate50 else AppColors.Slate200,
            fontSize = if (big) 16.sp else 13.sp,
            fontWeight = if (big) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
