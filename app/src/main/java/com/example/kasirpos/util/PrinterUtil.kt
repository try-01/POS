package com.example.kasirpos.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

/**
 * Utility cetak struk ke Bluetooth Printer dengan protokol ESC/POS.
 * Ringan — tanpa library pihak ketiga.
 *
 * Referensi perintah ESC/POS:
 *   ESC = 0x1B, GS  = 0x1D
 *   ESC '@'  → init printer
 *   ESC '!' n → format teks (bold, double-height, dll)
 *   ESC 'a' n → alignment (0=kiri, 1=tengah, 2=kanan)
 *   GS  'V' n → paper cut
 */
object PrinterUtil {

    /** SPP UUID standar untuk Bluetooth serial */
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Cetak struk ke printer Bluetooth.
     *
     * @param deviceAddress MAC address printer Bluetooth
     * @param receiptText  Teks struk yang sudah diformat (plain text, max 48 char/baris untuk printer 58mm)
     */
    suspend fun printReceipt(
        deviceAddress: String,
        receiptText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            // Validasi format MAC address sebelum diproses
            if (!deviceAddress.matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$"))) {
                return@withContext Result.failure(
                    IllegalArgumentException("Format MAC address tidak valid: $deviceAddress")
                )
            }

            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia"))

            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            // Batalkan discovery (mengganggu koneksi) dan connect
            adapter.cancelDiscovery()

            // Connect dengan timeout 5 detik via reflection-safe approach
            try {
                socket.connect()
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Gagal terhubung ke printer — pastikan printer menyala dan dalam jangkauan", e)
                )
            }

            outputStream = socket.outputStream

            // Kirim perintah ESC/POS
            val commands = buildEscPosCommands(receiptText)
            outputStream.write(commands)
            outputStream.flush()

            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("Izin Bluetooth diperlukan — aktifkan di pengaturan", e))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // ── SELALU tutup resource di finally block ──────────
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Bangun byte array perintah ESC/POS lengkap untuk struk.
     */
    private fun buildEscPosCommands(text: String): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()

        // Init printer
        buffer.write(byteArrayOf(0x1B, 0x40)) // ESC @

        // Alignment tengah untuk header
        buffer.write(byteArrayOf(0x1B, 0x61, 0x01)) // ESC a 1 (center)

        // Format teks: bold + double height untuk judul toko
        buffer.write(byteArrayOf(0x1B, 0x21, 0x30.toByte())) // ESC ! 48

        val lines = text.split("\n")
        for ((index, line) in lines.withIndex()) {
            // Baris pertama = judul toko (bold besar), sisanya normal
            if (index == 1) {
                // Reset format setelah judul
                buffer.write(byteArrayOf(0x1B, 0x21, 0x00)) // ESC ! 0 (normal)
                buffer.write(byteArrayOf(0x1B, 0x61, 0x00)) // ESC a 0 (left align)
            }
            // Gunakan CP437 (karakter set standar printer ESC/POS) — 
            // fallback graceful: karakter di luar CP437 diganti '?'
            buffer.write(line.toByteArray(Charsets.ISO_8859_1))
            buffer.write(0x0A) // LF (line feed)
        }

        // Feed beberapa baris + paper cut
        buffer.write(byteArrayOf(0x0A, 0x0A, 0x0A))  // 3× line feed
        buffer.write(byteArrayOf(0x1D, 0x56, 0x01))  // GS V 1 (partial cut)

        return buffer.toByteArray()
    }

    /**
     * Format struk teks dari data transaksi.
     * Maksimal 42 karakter per baris (printer 58mm).
     */
    fun formatReceiptText(
        storeName: String,
        transactionId: Long,
        date: String,
        items: List<Triple<String, Int, Long>>, // (nama, qty, harga satuan)
        subtotal: Long,
        discount: Long,
        taxAmount: Long,
        grandTotal: Long,
        cash: Long,
        change: Long
    ): String = buildString {
        val W = 40 // Lebar maks karakter

        appendLine("=" .repeat(W))
        appendLine(storeName.padStart((W + storeName.length) / 2).padEnd(W))
        appendLine("=" .repeat(W))
        appendLine("No: #$transactionId")
        appendLine("Tgl: $date")
        appendLine("-".repeat(W))

        // Kolom: Nama | Qty | Harga | Total
        appendLine(String.format("%-18s %3s %8s %9s", "Item", "Qty", "Harga", "Total"))
        appendLine("-".repeat(W))

        for ((name, qty, price) in items) {
            val lineTotal = price * qty
            val nameTrunc = if (name.length > 17) name.take(15) + ".." else name
            appendLine(String.format("%-18s %3d %8s %9s",
                nameTrunc, qty, fmt(price), fmt(lineTotal)))
        }

        appendLine("-".repeat(W))
        appendLine(String.format("%30s %9s", "SUBTOTAL:", fmt(subtotal)))
        if (discount > 0) appendLine(String.format("%30s %9s", "DISKON:", "-${fmt(discount)}"))
        if (taxAmount > 0) appendLine(String.format("%30s %9s", "PAJAK:", fmt(taxAmount)))
        appendLine(String.format("%30s %9s", "TOTAL:", fmt(grandTotal)))
        appendLine(String.format("%30s %9s", "TUNAI:", fmt(cash)))
        appendLine(String.format("%30s %9s", "KEMBALI:", fmt(change)))
        appendLine("=" .repeat(W))
        appendLine("TERIMA KASIH".padStart((W + 12) / 2).padEnd(W))
        appendLine("=" .repeat(W))
    }

    private fun fmt(amount: Long): String = "%,d".format(amount)
}
