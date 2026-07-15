package com.pos.offline.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.pos.offline.data.local.entity.PrinterConnectionType
import com.pos.offline.data.local.entity.PrinterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Implementasi DeviceConnection Bluetooth KUSTOM (BUKAN memakai
 * com.dantsu...connection.bluetooth.BluetoothConnection bawaan library) --
 * alasan teknis: BluetoothSocket di kelas bawaan bersifat PRIVATE tanpa
 * getter apa pun, sehingga TIDAK MUNGKIN dibatalkan paksa dari luar saat
 * connect() macet. BluetoothSocket.connect() juga tidak punya parameter
 * timeout bawaan & bisa blocking lama (device mati mendadak, sinyal lemah,
 * dll) -- kalau cuma "berhenti menunggu" di level coroutine tanpa menutup
 * socket-nya, thread di baliknya tetap jalan diam-diam sampai OS-level
 * timeout Android (bisa >10 detik), tidak sesuai target "timeout ~5 detik"
 * dan boros resource thread.
 *
 * Karena field outputStream/data di DeviceConnection (abstract base) BERSIFAT
 * PROTECTED, kita bisa extend langsung & simpan referensi BluetoothSocket
 * sendiri -- forceCloseIfStuck() dipanggil watchdog eksternal saat timeout,
 * menutup socket dari thread lain membuat connect() yang sedang blocking
 * langsung melempar IOException (perilaku resmi terdokumentasi Android),
 * sehingga thread benar-benar berhenti alih-alih menggantung tanpa batas.
 */
class CancellableBluetoothConnection(private val device: BluetoothDevice) : DeviceConnection() {

    @Volatile private var socket: BluetoothSocket? = null

    override fun isConnected(): Boolean =
        socket?.isConnected == true && super.isConnected()

    @SuppressLint("MissingPermission") // dijaga pemanggil: cek hasPermissions() di resolveBluetooth()
    @Suppress("DEPRECATION")
    override fun connect(): DeviceConnection {
        if (isConnected()) return this
        val uuid = resolveServiceUuid()
        try {
            val newSocket = device.createRfcommSocketToServiceRecord(uuid)
            socket = newSocket
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            newSocket.connect()
            outputStream = newSocket.outputStream
            data = ByteArray(0)
        } catch (e: IOException) {
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

    /** Dipanggil dari watchdog timeout eksternal untuk memaksa batalkan
     *  koneksi yang macet -- lihat penjelasan lengkap di dokumentasi kelas. */
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
    data class Failure(val message: String) : TestPrintResult()
}

private sealed class ConnectionResolution {
    data class Ready(val connection: DeviceConnection, val targetLabel: String) : ConnectionResolution()
    data class Error(val message: String) : ConnectionResolution()
}

/**
 * Factory + orkestrasi Test Print (Batch H3d). Cakupan sengaja MINIMAL --
 * satu printer, satu percobaan cetak per pemanggilan, TANPA cascade ke
 * printer cadangan (itu baru masuk scope PrintCoordinator di H6).
 *
 * Auto-detect supportsStatusQuery DITUNDA (keputusan disepakati bersama
 * user) -- DeviceConnection DantSu TIDAK menyediakan akses baca respons
 * (InputStream) lewat API publik/protected apa pun, library ini murni
 * satu-arah (write-only). Field supportsStatusQuery untuk saat ini murni
 * toggle manual di form edit printer.
 */
class PrinterConnectionFactory(
    private val bluetoothHelper: BluetoothPrinterHelper,
    private val usbHelper: UsbPrinterHelper
) {

    suspend fun testPrint(printer: PrinterEntity): TestPrintResult {
        val resolution = resolveConnection(printer)
        val ready = when (resolution) {
            is ConnectionResolution.Error -> return TestPrintResult.Failure(resolution.message)
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
                val escPosPrinter = withContext(Dispatchers.IO) {
                    EscPosPrinter(
                        ready.connection,
                        DEFAULT_PRINTER_DPI,
                        printer.paperWidth.printableWidthMM(),
                        printer.charPerLine
                    )
                }
                withContext(Dispatchers.IO) {
                    escPosPrinter.printFormattedTextAndCut(buildTestPrintMarkup(printer))
                }
                withContext(Dispatchers.IO) {
                    escPosPrinter.disconnectPrinter()
                }
                TestPrintResult.Success
            } catch (e: Exception) {
                runCatching { ready.connection.disconnect() }
                TestPrintResult.Failure(
                    "Terhubung ke printer, tetapi gagal mencetak: ${e.message ?: "kesalahan tidak diketahui"}"
                )
            }
        }

        return TestPrintResult.Failure(genericErrorMessage)
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
            "$ip:$port"
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
                "Izin Bluetooth tidak diberikan. Buka Pengaturan Aplikasi untuk mengaktifkan izin."
            )
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return ConnectionResolution.Error("Perangkat ini tidak mendukung Bluetooth.")
        val device = try {
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
        val usbManager = usbHelper.getSystemUsbManager()
            ?: return ConnectionResolution.Error("USB tidak didukung pada perangkat ini.")
        val device = usbHelper.findDeviceByVendorProduct(vendorId, productId)
            ?: return ConnectionResolution.Error("Perangkat USB tidak ditemukan. Pastikan kabel tersambung.")

        val permissionResult = usbHelper.requestPermission(device)
        if (permissionResult != UsbPermissionResult.Granted) {
            return ConnectionResolution.Error(
                "Izin akses USB tidak diberikan. Pastikan kabel tersambung dan izinkan akses saat diminta."
            )
        }

        return ConnectionResolution.Ready(UsbConnection(usbManager, device), printer.label)
    }

    /**
     * Membuka koneksi dengan batas waktu ~5 detik.
     *
     * 🐛 BUGFIX PENTING (ditemukan saat pengujian H3d, menyebabkan APP FORCE
     * CLOSE ~5 detik setelah Test Print saat printer Bluetooth mati/tidak
     * terjangkau): watchdog di bawah ini SENGAJA TIDAK memanggil
     * `connectJob.cancel()` setelah forceCloseIfStuck(). Kombinasi keduanya
     * (menutup paksa socket YANG MENYEBABKAN coroutine menyelesaikan diri
     * dengan EscPosConnectionException miliknya sendiri, DIBARENGI dengan
     * cancel() eksplisit pada job yang sama) adalah race condition fatal di
     * kotlinx.coroutines: sebuah `async` Job yang SUDAH diminta cancel()
     * tetapi kemudian menyelesaikan diri dengan exception LAIN (bukan
     * CancellationException) dianggap "exception tak tertangani" oleh
     * coroutine machinery -- dilempar ke Thread.UncaughtExceptionHandler
     * (menyebabkan force close), BUKAN diteruskan ke try/catch normal kita
     * di connectJob.await() di bawah.
     *
     * Perbaikannya: HANYA memaksa socket menutup diri lewat
     * forceCloseIfStuck(). Ini sudah cukup memicu IOException ASLI dari
     * dalam connect() itu sendiri (perilaku resmi Android: menutup socket
     * yang sedang di tengah connect() akan membuat connect() melempar
     * IOException) -- job lalu menyelesaikan diri SECARA ALAMI (bukan
     * dibatalkan paksa), sehingga exception-nya tertangkap normal lewat
     * connectJob.await() di bawah seperti seharusnya.
     *
     * Ini aman untuk ketiga jenis koneksi: WiFi (TcpConnection) sudah punya
     * timeout native sendiri lewat parameter constructor yang dihormati JVM,
     * USB (UsbConnection.connect()) pada dasarnya cepat (buka device handle,
     * bukan network call) sehingga nyaris mustahil sampai menyentuh watchdog
     * ini sama sekali.
     */
    private suspend fun connectWithTimeout(connection: DeviceConnection): Boolean = coroutineScope {
        val connectJob = async(Dispatchers.IO) { connection.connect() }
        val watchdog = launch(Dispatchers.IO) {
            delay(CONNECT_TIMEOUT_MS)
            if (connectJob.isActive) {
                (connection as? CancellableBluetoothConnection)?.forceCloseIfStuck()
                // TIDAK ADA connectJob.cancel() DI SINI -- lihat penjelasan
                // panjang di atas. Biarkan job menyelesaikan diri alami.
            }
        }
        try {
            connectJob.await()
            watchdog.cancel()
            true
        } catch (e: Exception) {
            watchdog.cancel()
            false
        }
    }

    private fun connectionErrorMessage(printer: PrinterEntity, targetLabel: String): String =
        when (printer.connectionType) {
            PrinterConnectionType.WIFI ->
                "Tidak dapat terhubung ke $targetLabel. Pastikan printer menyala dan alamat IP " +
                    "masih sama (IP printer bisa berubah jika direstart tanpa IP statis)."
            PrinterConnectionType.BLUETOOTH ->
                "Tidak dapat terhubung ke \"$targetLabel\". Pastikan printer menyala dan dalam jangkauan."
            PrinterConnectionType.USB ->
                "Tidak dapat terhubung ke perangkat USB. Pastikan kabel tersambung dengan baik, " +
                    "izin akses masih diberikan, dan printer mendukung mode USB Printer Class standar."
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
        private const val RETRY_ATTEMPTS_TOTAL = 3 // 1 percobaan awal + 2 retry
        private const val RETRY_DELAY_MS = 1_500L
    }
}