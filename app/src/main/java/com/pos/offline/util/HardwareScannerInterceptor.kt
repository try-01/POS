package com.pos.offline.util

import android.view.KeyCharacterMap
import android.view.KeyEvent

/**
 * Mendeteksi barcode scanner fisik (USB OTG / Bluetooth HID) yang muncul
 * ke Android sebagai KeyEvent sangat cepat, diakhiri tombol Enter.
 *
 * Dua sinyal dipakai bareng biar akurat:
 * 1. deviceId  -> keyboard on-screen (soft keyboard) SELALU device ID
 *    virtual (-1). Kalau event datang dari device ID asli, hampir pasti
 *    dari perangkat fisik (scanner), bukan ketikan kasir di layar.
 * 2. Timing    -> jaga-jaga tambahan: karakter scanner masuk sangat
 *    rapat (< maxCharGapMs), termasuk jeda ke tombol Enter-nya.
 *
 * PENTING: kelas ini hanya "menguping" event, TIDAK PERNAH men-consume-nya.
 * Pemanggil wajib tetap meneruskan setiap event ke super.dispatchKeyEvent(),
 * kalau tidak, semua input manual (search bar, form harga, dll) di seluruh
 * app akan berhenti berfungsi.
 *
 * Cara pakai di MainActivity.kt:
 *
 * private val scannerInterceptor = HardwareScannerInterceptor { barcode ->
 *     posViewModel.onBarcodeScanned(barcode)
 * }
 *
 * override fun dispatchKeyEvent(event: KeyEvent): Boolean {
 *     scannerInterceptor.onKeyEvent(event)
 *     return super.dispatchKeyEvent(event) // wajib, jangan pernah dihapus
 * }
 */
class HardwareScannerInterceptor(
    private val maxCharGapMs: Long = 80L,   // jeda maksimum antar karakter ala scanner
    private val minLength: Int = 6,         // UPC-E terpendek
    private val maxLength: Int = 20,        // aman untuk kode internal / Code128
    private val onBarcodeDetected: (String) -> Unit
) {
    private val buffer = StringBuilder()
    private var lastCharTime = 0L

    fun onKeyEvent(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN) return

        // Sinyal utama: keyboard layar selalu virtual device -> abaikan dari awal
        if (event.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD) return

        val now = System.currentTimeMillis()
        val isEnter = event.keyCode == KeyEvent.KEYCODE_ENTER ||
            event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

        if (isEnter) {
            val candidate = buffer.toString()
            val lastGapFast = buffer.isNotEmpty() && (now - lastCharTime) <= maxCharGapMs
            buffer.clear()

            val validBarcode = lastGapFast &&
                candidate.length in minLength..maxLength &&
                candidate.all { it.isDigit() }

            if (validBarcode) onBarcodeDetected(candidate)
            return
        }

        val char = event.unicodeChar.toChar()
        if (!char.isDigit()) {
            buffer.clear() // karakter non-digit -> bukan pola barcode, batalkan
            return
        }

        if (buffer.isNotEmpty() && (now - lastCharTime) > maxCharGapMs) {
            buffer.clear() // jeda kelamaan -> ini awal input baru
        }
        buffer.append(char)
        lastCharTime = now
    }
}
