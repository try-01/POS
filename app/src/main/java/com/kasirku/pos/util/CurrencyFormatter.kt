package com.kasirku.pos.util

import java.text.NumberFormat
import java.util.Locale

/** Formatter mata uang Rupiah, dipakai konsisten di seluruh layar (Kasir, Riwayat, Struk). */
object CurrencyFormatter {
    private val idrFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    fun format(amount: Double): String = idrFormat.format(amount)
}
