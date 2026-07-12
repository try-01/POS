package com.pos.offline.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation yang menampilkan separator ribuan ("100000" -> "100.000")
 * tanpa mengubah nilai asli yang disimpan di state.
 *
 * Dipindahkan ke lokasi shared (dari PosScreen.kt) agar bisa dipakai ulang oleh
 * InventoryScreen.kt tanpa duplikasi logika — satu sumber kebenaran untuk
 * format uang di seluruh aplikasi.
 *
 * Pemetaan offset dibangun dari array (bukan rumus aritmatika manual) supaya
 * tidak ada celah off-by-one yang bisa menghasilkan indeks negatif/di luar
 * jangkauan — penyebab crash saat jumlah digit kelipatan 3.
 */
object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val n = original.length
        if (n == 0) return TransformedText(text, OffsetMapping.Identity)

        val sb = StringBuilder()
        val orig2Trans = IntArray(n + 1)
        orig2Trans[0] = 0
        for (i in original.indices) {
            sb.append(original[i])
            val remaining = n - i - 1
            if (remaining > 0 && remaining % 3 == 0) sb.append('.')
            orig2Trans[i + 1] = sb.length
        }
        val formatted = sb.toString()

        val trans2Orig = IntArray(formatted.length + 1)
        var origIdx = 0
        trans2Orig[0] = 0
        for (t in 1..formatted.length) {
            if (formatted[t - 1] != '.') origIdx++
            trans2Orig[t] = origIdx.coerceAtMost(n)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                orig2Trans[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int): Int =
                trans2Orig[offset.coerceIn(0, formatted.length)]
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}