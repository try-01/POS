package com.pos.offline.util
import android.view.KeyCharacterMap
import android.view.KeyEvent
class HardwareScannerInterceptor(
    private val maxCharGapMs: Long = 80L, 
    private val minLength: Int = 6, 
    private val maxLength: Int = 20, 
    private val onBarcodeDetected: (String) -> Unit,
) {
    private val buffer = StringBuilder()
    private var lastCharTime = 0L
fun onKeyEvent(event: KeyEvent): Boolean {
      if (event.action != KeyEvent.ACTION_DOWN || event.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD) return false
      val now = System.currentTimeMillis()
      val isEnter = event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
      if (isEnter) {
          val candidate = buffer.toString()
          buffer.clear()
          val validBarcode = candidate.length in minLength..maxLength && candidate.all { it.isLetterOrDigit() || it == '-' }
          if (validBarcode) {
              onBarcodeDetected(candidate)
              return true
          }
          return false
      }
      val charCode = event.unicodeChar
      if (charCode == 0) return false
      val char = charCode.toChar()
      if (!char.isLetterOrDigit() && char != '-') {
          buffer.clear()
          return false
      }
      if (buffer.isNotEmpty() && (now - lastCharTime) > maxCharGapMs) buffer.clear()
      buffer.append(char)
      lastCharTime = now
      return true
  }
}
