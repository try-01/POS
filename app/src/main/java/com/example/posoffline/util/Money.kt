package com.example.posoffline.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Money helpers.
 *
 * POS systems must avoid floating-point drift in money math. We store all
 * monetary values as `Long` rupiah and only format at the display boundary.
 * This matches the way mature POS systems (and most banking software) work.
 */
object Money {

    private val idLocale = Locale("id", "ID")
    private val formatter: NumberFormat = NumberFormat.getInstance(idLocale).apply {
        maximumFractionDigits = 0
    }

    fun format(value: Long, currency: String = "Rp"): String {
        val grouped = formatter.format(value)
        return "$currency $grouped"
    }

    /**
     * Parse a user-typed rupiah string.
     * Accepts "10.000", "10000", "Rp 10.000", "rp 10,000" etc.
     * Returns 0 for empty / unparseable input.
     */
    fun parseInput(raw: String): Long {
        if (raw.isBlank()) return 0L
        val cleaned = raw.replace(Regex("[^0-9]"), "")
        return cleaned.toLongOrNull() ?: 0L
    }
}
