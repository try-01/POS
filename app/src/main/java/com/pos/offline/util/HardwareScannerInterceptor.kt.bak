package com.pos.offline.util

import android.view.KeyCharacterMap
import android.view.KeyEvent

class HardwareScannerInterceptor(
    private val maxCharGapMs: Long = 80L, // jeda maksimum antar karakter ala scanner
    private val minLength: Int = 6, // UPC-E terpendek
    private val maxLength: Int = 20, // aman untuk kode internal / Code128
    private val onBarcodeDetected: (String) -> Unit,
) {
    private val buffer = StringBuilder()
    private var lastCharTime = 0L

    fun onKeyEvent(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN) return

        if (event.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD) return

        val now = System.currentTimeMillis()
        val isEnter =
            event.keyCode == KeyEvent.KEYCODE_ENTER ||
                event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

        if (isEnter) {
            val candidate = buffer.toString()
            val lastGapFast = buffer.isNotEmpty() && (now - lastCharTime) <= maxCharGapMs
            buffer.clear()

            val validBarcode =
                lastGapFast &&
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
