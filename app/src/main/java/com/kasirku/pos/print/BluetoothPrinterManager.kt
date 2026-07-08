package com.kasirku.pos.print

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class BluetoothPrinterManager(private val context: Context) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PAPER_WIDTH = 32
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun scanPairedDevices(): List<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            bluetoothSocket = socket
            outputStream = socket.outputStream
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (_: Exception) { }
        outputStream = null
        bluetoothSocket = null
    }

    val isConnected: Boolean get() = bluetoothSocket?.isConnected == true

    suspend fun printReceipt(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        storeName: String = "KASIRKU POS"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val os = outputStream ?: return@withContext Result.failure(Exception("Printer tidak terhubung"))
            val esc = EscPosCommands

            os.write(esc.INIT)
            os.write(esc.ALIGN_CENTER)
            os.write(esc.BOLD_ON)
            os.write(esc.FONT_SIZE_2X)
            os.printLine("★ \$storeName ★")
            os.write(esc.FONT_SIZE_NORMAL)
            os.write(esc.BOLD_OFF)
            os.printLine("Toko Serba Ada")
            os.printLine("")

            os.write(esc.ALIGN_LEFT)
            os.printLine(transaction.invoiceNumber)
            os.printLine(formatDate(transaction.transactionDate))
            os.printLine(dashLine())

            items.forEach { item ->
                os.printLine(item.productName)
                os.printLine(padRight("  \${item.quantity}x \${formatShortPrice(item.unitPrice)}", formatShortPrice(item.subtotal)))
            }
            os.printLine(dashLine())

            os.printLine(padRight("Subtotal", formatShortPrice(transaction.subtotal)))
            if (transaction.taxPercent > 0) {
                os.printLine(padRight("Pajak (\${transaction.taxPercent.toInt()}%)", formatShortPrice(transaction.taxAmount)))
            }
            os.printLine(dashLine())
            os.write(esc.BOLD_ON)
            os.printLine(padRight("TOTAL", formatShortPrice(transaction.totalAmount)))
            os.write(esc.BOLD_OFF)
            os.printLine(padRight("Bayar", formatShortPrice(transaction.paymentAmount)))
            os.printLine(padRight("Kembali", formatShortPrice(transaction.changeAmount)))
            os.printLine(dashLine())

            os.write(esc.ALIGN_CENTER)
            os.printLine("Terima kasih!")
            os.printLine("")
            os.write(esc.FEED_LINES)
            os.write(esc.CUT_PAPER)
            os.flush()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun OutputStream.printLine(text: String) {
        write(text.toByteArray(Charsets.UTF_8))
        write(EscPosCommands.LINE_FEED)
    }

    private fun dashLine(): String = "-".repeat(PAPER_WIDTH)

    private fun padRight(left: String, right: String): String {
        val spaces = PAPER_WIDTH - left.length - right.length
        return if (spaces > 0) left + " ".repeat(spaces) + right else "\$left \$right"
    }

    private fun formatShortPrice(amount: Long): String = "Rp\${String.format("%,d", amount).replace(',', '.')}"

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
