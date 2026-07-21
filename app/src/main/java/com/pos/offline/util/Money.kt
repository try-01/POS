package com.pos.offline.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

private val rupiahFormatter: DecimalFormat =
    DecimalFormat(
        "#,###",
        DecimalFormatSymbols(Locale.forLanguageTag("id-ID")),
    ).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

fun Long.toRupiah(): String = "Rp " + rupiahFormatter.format(this)

fun formatRupiah(amount: Long): String = amount.toRupiah()

fun Double.roundToRupiah(): Long = this.roundToLong()
