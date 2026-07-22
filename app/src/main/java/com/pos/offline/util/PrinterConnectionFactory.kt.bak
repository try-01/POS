package com.pos.offline.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.EscPosPrinterCommands
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.pos.offline.data.local.entity.PrinterConnectionType
import com.pos.offline.data.local.entity.PrinterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CancellableBluetoothConnection(
    private val device: BluetoothDevice,
) : DeviceConnection() {
    @Volatile private var socket: BluetoothSocket? = null

    override fun isConnected(): Boolean = socket?.isConnected == true && super.isConnected()

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    override fun connect(): DeviceConnection {
        if (isConnected()) return this
        try {
            val uuid = resolveServiceUuid()
            val newSocket = device.createRfcommSocketToServiceRecord(uuid)
            socket = newSocket
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            newSocket.connect()
            outputStream = newSocket.outputStream
            data = ByteArray(0)
        } catch (e: Exception) {
            disconnect()
            throw EscPosConnectionException("Unable to connect to bluetooth device.")
        }
        return this
    }

    override fun disconnect(): DeviceConnection {
        data = ByteArray(0)
        outputStream?.let { runCatching { it.close() } }
        outputStream = null
        socket?.let { runCatching { it.close() } }
        socket = null
        return this
    }

    fun forceCloseIfStuck() {
        socket?.let { runCatching { it.close() } }
    }

    @SuppressLint("MissingPermission")
    private fun resolveServiceUuid(): UUID {
        val uuids = device.uuids
        if (!uuids.isNullOrEmpty()) {
            val sppMatch = uuids.firstOrNull { it.uuid == SPP_UUID }
            return sppMatch?.uuid ?: uuids[0].uuid
        }
        return SPP_UUID
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    }
}

sealed class TestPrintResult {
    object Success : TestPrintResult()

    data class Failure(
        val message: String,
    ) : TestPrintResult()
}

sealed class PrintResult {
    data class Success(
        val printer: PrinterEntity,
        val statusQueryFailed: Boolean = false,
    ) : PrintResult()

    data class Failure(
        val printer: PrinterEntity,
        val message: String,
    ) : PrintResult()
}

sealed class CashDrawerResult {
    object Success : CashDrawerResult()

    data class Failure(
        val message: String,
    ) : CashDrawerResult()
}

sealed class PaperStatusResult {
    object Ok : PaperStatusResult()
    object PaperOut : PaperStatusResult()
    object NoResponse : PaperStatusResult()
}

private sealed class ConnectionResolution {
    data class Ready(
        val connection: DeviceConnection,
        val targetLabel: String,
    ) : ConnectionResolution()

    data class Error(
        val message: String,
    ) : ConnectionResolution()
}

private sealed class JobOutcome {
    data class Success(
        val statusQueryFailed: Boolean = false,
    ) : JobOutcome()

    data class Failure(
        val message: String,
    ) : JobOutcome()
}

class PrinterConnectionFactory(
    private val bluetoothHelper: BluetoothPrinterHelper,
    private val usbHelper: UsbPrinterHelper,
) {
    suspend fun testPrint(printer: PrinterEntity): TestPrintResult {
        val outcome = executePrintJob(printer, false) { listOf(buildTestPrintMarkup(printer)) }
        return when (outcome) {
            is JobOutcome.Success -> TestPrintResult.Success
            is JobOutcome.Failure -> TestPrintResult.Failure(outcome.message)
        }
    }

    suspend fun printReceipt(
        printer: PrinterEntity,
        openCashDrawer: Boolean = false,
        markupBuilder: (EscPosPrinter) -> List<String>,
    ): PrintResult {
        val outcome = executePrintJob(printer, openCashDrawer, markupBuilder)
        return when (outcome) {
            is JobOutcome.Success -> PrintResult.Success(printer, outcome.statusQueryFailed)
            is JobOutcome.Failure -> PrintResult.Failure(printer, outcome.message)
        }
    }

    suspend fun openCashDrawer(printer: PrinterEntity): CashDrawerResult {
        val resolution = resolveConnection(printer)
        val ready =
            when (resolution) {
                is ConnectionResolution.Error -> return CashDrawerResult.Failure(resolution.message)
                is ConnectionResolution.Ready -> resolution
            }

        val genericErrorMessage = connectionErrorMessage(printer, ready.targetLabel)

        repeat(RETRY_ATTEMPTS_TOTAL) { attempt ->
            if (attempt > 0) delay(RETRY_DELAY_MS)

            val connected = connectWithTimeout(ready.connection)
            if (!connected) {
                return@repeat
            }

            return try {
                withContext(Dispatchers.IO) {
                    val commands = EscPosPrinterCommands(ready.connection)
                    commands.connect()
                    commands.openCashBox()
                    commands.disconnect()
                }
                CashDrawerResult.Success
            } catch (e: Exception) {
                runCatching { ready.connection.disconnect() }
                CashDrawerResult.Failure(
                    "Gagal membuka laci: ${e.message ?: "kesalahan tidak diketahui"}",
                )
            }
        }

        return CashDrawerResult.Failure(genericErrorMessage)
    }

    private suspend fun executePrintJob(
        printer: PrinterEntity,
        openCashDrawer: Boolean = false,
        markupBuilder: (EscPosPrinter) -> List<String>,
    ): JobOutcome {
        val resolution = resolveConnection(printer)
        val ready =
            when (resolution) {
                is ConnectionResolution.Error -> return JobOutcome.Failure(resolution.message)
                is ConnectionResolution.Ready -> resolution
            }

        val genericErrorMessage = connectionErrorMessage(printer, ready.targetLabel)

        repeat(RETRY_ATTEMPTS_TOTAL) { attempt ->
            if (attempt > 0) delay(RETRY_DELAY_MS)

            val connected = connectWithTimeout(ready.connection)
            if (!connected) {
                return@repeat
            }

            return try {
                var statusQueryFailed = false
                if (attempt == 0 && printer.supportsStatusQuery) {
                    when (preCheckPaperStatus(printer)) {
                        PaperStatusResult.PaperOut -> return JobOutcome.Failure("Printer melaporkan kertas habis.")
                        PaperStatusResult.NoResponse -> statusQueryFailed = true
                        PaperStatusResult.Ok -> {}
                    }
                    delay(500)
                }

                val escPosPrinter =
                    withContext(Dispatchers.IO) {
                        if (openCashDrawer) {
                            try {
                                val commands = EscPosPrinterCommands(ready.connection)
                                commands.openCashBox()
                                Thread.sleep(250)
                            } catch (e: Exception) {
                            }
                        }
                        EscPosPrinter(
                            ready.connection,
                            DEFAULT_PRINTER_DPI,
                            printer.paperWidth.printableWidthMM(),
                            printer.charPerLine,
                        ).useEscAsteriskCommand(true)
                    }

                val markups = markupBuilder(escPosPrinter)
                withContext(Dispatchers.IO) {
                    markups.forEachIndexed { index, markup ->
                        if (index == markups.lastIndex) {
                            escPosPrinter.printFormattedTextAndCut(markup)
                        } else {
                            escPosPrinter.printFormattedText(markup)
                            delay(1500)
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    escPosPrinter.disconnectPrinter()
                }
                JobOutcome.Success(statusQueryFailed = statusQueryFailed)
            } catch (e: Exception) {
                runCatching { ready.connection.disconnect() }
                JobOutcome.Failure(
                    "Terhubung ke printer, tetapi gagal mencetak: ${e.message ?: "kesalahan tidak diketahui"}",
                )
            }
        }

        return JobOutcome.Failure(genericErrorMessage)
    }

    @SuppressLint("MissingPermission")
    private suspend fun preCheckPaperStatus(printer: PrinterEntity): PaperStatusResult = withContext(Dispatchers.IO) {
        val cmd = byteArrayOf(0x1D, 0x72, 0x01)
        try {
            when (printer.connectionType) {
                PrinterConnectionType.WIFI -> {
                    val socket = Socket()
                    try {
                        socket.connect(InetSocketAddress(printer.wifiIpAddress, printer.wifiPort), 1500)
                        socket.soTimeout = 1500
                        socket.getOutputStream().apply { write(cmd); flush() }
                        val status = socket.getInputStream().read()
                        when { 
                            status == -1 -> PaperStatusResult.NoResponse
                            (status and 0x04) == 0 -> PaperStatusResult.Ok
                            else -> PaperStatusResult.PaperOut 
                        }
                    } finally { runCatching { socket.close() } }
                }
                PrinterConnectionType.BLUETOOTH -> {
                    val address = printer.bluetoothMacAddress ?: return@withContext PaperStatusResult.NoResponse
                    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext PaperStatusResult.NoResponse
                    val device = adapter.getRemoteDevice(address)
                    val uuid = device.uuids?.firstOrNull { it.uuid == SPP_UUID }?.uuid ?: SPP_UUID

                    val socket = withTimeoutOrNull(1500) {
                        val s = device.createRfcommSocketToServiceRecord(uuid)
                        adapter.cancelDiscovery()
                        s.connect()
                        s
                    } ?: return@withContext PaperStatusResult.NoResponse

                    try {
                        socket.outputStream.write(cmd)
                        socket.outputStream.flush()
                        val status = withTimeoutOrNull(1500) { socket.inputStream.read() } ?: -1
                        when { 
                            status == -1 -> PaperStatusResult.NoResponse
                            (status and 0x04) == 0 -> PaperStatusResult.Ok
                            else -> PaperStatusResult.PaperOut 
                        }
                    } finally {
                        runCatching { socket.close() }
                    }
                }
                PrinterConnectionType.USB -> PaperStatusResult.Ok
            }
        } catch (e: Exception) {
            PaperStatusResult.NoResponse
        }
    }

    private suspend fun resolveConnection(printer: PrinterEntity): ConnectionResolution =
        when (printer.connectionType) {
            PrinterConnectionType.WIFI -> resolveWifi(printer)
            PrinterConnectionType.BLUETOOTH -> resolveBluetooth(printer)
            PrinterConnectionType.USB -> resolveUsb(printer)
        }

    private fun resolveWifi(printer: PrinterEntity): ConnectionResolution {
        val ip = printer.wifiIpAddress
        val port = printer.wifiPort
        if (ip.isNullOrBlank() || port == null) {
            return ConnectionResolution.Error("Konfigurasi WiFi printer tidak lengkap.")
        }
        return ConnectionResolution.Ready(
            TcpConnection(ip, port, CONNECT_TIMEOUT_MS.toInt()),
            "$ip:$port",
        )
    }

    @Suppress("DEPRECATION")
    private fun resolveBluetooth(printer: PrinterEntity): ConnectionResolution {
        val address = printer.bluetoothMacAddress
        if (address.isNullOrBlank()) {
            return ConnectionResolution.Error("Alamat perangkat Bluetooth belum diatur pada printer ini.")
        }
        if (!bluetoothHelper.hasPermissions()) {
            return ConnectionResolution.Error(
                "Izin Bluetooth tidak diberikan. Buka Pengaturan Aplikasi untuk mengaktifkan izin.",
            )
        }
        val adapter =
            BluetoothAdapter.getDefaultAdapter()
                ?: return ConnectionResolution.Error("Perangkat ini tidak mendukung Bluetooth.")
        if (!adapter.isEnabled) {
            return ConnectionResolution.Error(
                "Bluetooth ponsel sedang mati. Nyalakan Bluetooth terlebih dahulu.",
            )
        }

        val device =
            try {
                adapter.getRemoteDevice(address)
            } catch (e: IllegalArgumentException) {
                return ConnectionResolution.Error("Alamat Bluetooth pada printer ini tidak valid.")
            }
        return ConnectionResolution.Ready(CancellableBluetoothConnection(device), printer.label)
    }

    private suspend fun resolveUsb(printer: PrinterEntity): ConnectionResolution {
        val vendorId = printer.usbVendorId
        val productId = printer.usbProductId
        if (vendorId == null || productId == null) {
            return ConnectionResolution.Error("Konfigurasi perangkat USB printer ini tidak lengkap.")
        }
        val usbManager =
            usbHelper.getSystemUsbManager()
                ?: return ConnectionResolution.Error("USB tidak didukung pada perangkat ini.")
        val device =
            usbHelper.findDeviceByVendorProduct(vendorId, productId)
                ?: return ConnectionResolution.Error("Perangkat USB tidak ditemukan. Pastikan kabel tersambung.")

        val permissionResult = usbHelper.requestPermission(device)
        if (permissionResult != UsbPermissionResult.Granted) {
            return ConnectionResolution.Error(
                "Izin akses USB tidak diberikan. Pastikan kabel tersambung dan izinkan akses saat diminta.",
            )
        }

        return ConnectionResolution.Ready(UsbConnection(usbManager, device), printer.label)
    }

    private suspend fun connectWithTimeout(connection: DeviceConnection): Boolean =
        supervisorScope {
            val connectJob = async(Dispatchers.IO) { connection.connect() }
            val watchdog =
                launch(Dispatchers.IO) {
                    delay(CONNECT_TIMEOUT_MS)
                    if (connectJob.isActive) {
                        (connection as? CancellableBluetoothConnection)?.forceCloseIfStuck()
                        connectJob.cancel()
                    }
                }
            val success =
                try {
                    connectJob.await()
                    true
                } catch (e: Exception) {
                    false
                }
            watchdog.cancel()
            success
        }

    private fun connectionErrorMessage(
        printer: PrinterEntity,
        targetLabel: String,
    ): String =
        when (printer.connectionType) {
            PrinterConnectionType.WIFI -> {
                "Tidak dapat terhubung ke $targetLabel. Pastikan printer menyala dan alamat IP " +
                    "masih sama (IP printer bisa berubah jika direstart tanpa IP statis)."
            }

            PrinterConnectionType.BLUETOOTH -> {
                "Tidak dapat terhubung ke \"$targetLabel\". Pastikan printer menyala dan dalam jangkauan."
            }

            PrinterConnectionType.USB -> {
                "Tidak dapat terhubung ke perangkat USB. Pastikan kabel tersambung dengan baik, " +
                    "izin akses masih diberikan, dan printer mendukung mode USB Printer Class standar."
            }
        }

    private fun buildTestPrintMarkup(printer: PrinterEntity): String {
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("id-ID")).format(Date())
        return buildString {
            append("[C]<b>TEST PRINT KASIR OFFLINE</b>\n")
            append("[C]${printer.label}\n")
            append("[L]--------------------------------\n")
            append("[C]Printer berhasil terhubung!\n")
            append("[L]\n")
            append("[L]Waktu: $timestamp\n")
            append("[L]--------------------------------\n")
        }
    }

    companion object {
        private const val DEFAULT_PRINTER_DPI = 203
        private const val CONNECT_TIMEOUT_MS = 5_000L
        private const val RETRY_ATTEMPTS_TOTAL = 3
        private const val RETRY_DELAY_MS = 1_500L
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    }
}