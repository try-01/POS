package com.pos.offline.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

private val rupiahFormatter: DecimalFormat = DecimalFormat(
    "#,###",
    DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))
).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

fun Long.toRupiah(): String = "Rp " + rupiahFormatter.format(this)

fun formatRupiah(amount: Long): String = amount.toRupiah()

/**
 * Pembulatan aritmatika standar (round-half-up) ke Rupiah terdekat.
 *
 * Dipakai untuk hasil kalkulasi diskon persen & pajak yang berpotensi
 * pecahan (mis. 7% dari Rp 10.333 = Rp 723,31 → dibulatkan Rp 723;
 * Rp 723,50 → Rp 724). `kotlin.math.roundToLong()` (berbasis `Math.round`)
 * sudah membulatkan .5 ke atas untuk angka positif — cocok karena nominal
 * diskon/pajak di aplikasi ini selalu >= 0.
 */
fun Double.roundToRupiah(): Long = this.roundToLong()