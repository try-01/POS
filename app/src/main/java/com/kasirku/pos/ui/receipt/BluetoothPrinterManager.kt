package com.kasirku.pos.ui.receipt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.kasirku.pos.data.local.relation.TransactionWithItems
import com.kasirku.pos.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Manajer koneksi printer thermal Bluetooth offline.
 * Berjalan di background thread via Coroutines agar UI tidak pernah tersendat saat socket berproses.
 */
class BluetoothPrinterManager(private val context: Context) {

    // UUID standar Serial Port Profile (SPP) untuk semua printer thermal Bluetooth ESC/POS
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) return emptyList()

        return adapter.bondedDevices?.filter {
            // Filter perangkat bertipe Printer atau Miscellaneous (karena banyak printer portable cina terdeteksi misc)
            it.bluetoothClass?.majorDeviceClass == android.bluetooth.BluetoothClass.Device.Major.IMAGING ||
            it.name?.contains("Printer", ignoreCase = true) == true ||
            it.name?.contains("POS", ignoreCase = true) == true ||
            it.name?.contains("RP", ignoreCase = true) == true
        } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    suspend fun printReceipt(device: BluetoothDevice, transactionWithItems: TransactionWithItems, storeName: String = "KASIRKU STORE"): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            val tx = transactionWithItems.transaction
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("in", "ID")).format(Date(tx.createdAt))

            // Build ESC/POS bytes
            val builder = EscPosCommandBuilder()
                .alignCenter()
                .doubleSize(true).bold(true).text(storeName).newLine()
                .doubleSize(false).bold(false).text("POS 100% Offline & Ringan").newLine()
                .text("================================").newLine()
                .alignLeft()
                .text("No: ${tx.invoiceNumber}").newLine()
                .text("Waktu: $dateStr").newLine()
                .text("================================").newLine()

            for (item in transactionWithItems.items) {
                builder.text(item.productName).newLine()
                val lineRight = "${item.quantity}x @${CurrencyFormatter.format(item.priceAtSale)} = ${CurrencyFormatter.format(item.subtotal)}"
                builder.alignRight().text(lineRight).newLine().alignLeft()
            }

            builder.text("--------------------------------").newLine()
                .alignRight()
                .text("Subtotal : ${CurrencyFormatter.format(tx.subtotal)}").newLine()

            if (tx.discountAmount > 0) {
                builder.text("Diskon   : -${CurrencyFormatter.format(tx.discountAmount)}").newLine()
            }
            if (tx.taxAmount > 0) {
                builder.text("Pajak    : ${CurrencyFormatter.format(tx.taxAmount)}").newLine()
            }

            builder.bold(true).text("TOTAL    : ${CurrencyFormatter.format(tx.grandTotal)}").newLine().bold(false)
                .text("Tunai    : ${CurrencyFormatter.format(tx.paidAmount)}").newLine()
                .text("Kembali  : ${CurrencyFormatter.format(tx.changeAmount)}").newLine()
                .alignCenter()
                .newLine()
                .text("Terima Kasih Atas Kunjungan Anda").newLine()
                .text("Powered by KasirKu POS").newLine()
                .newLine(3)
                .cut()

            outputStream.write(builder.getBytes())
            outputStream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BluetoothPrinter", "Gagal cetak struk: ${e.message}", e)
            Result.failure(e)
        } finally {
            runCatching { outputStream?.close() }
            runCatching { socket?.close() }
        }
    }
}
