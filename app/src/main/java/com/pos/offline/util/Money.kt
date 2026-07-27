package com.pos.offline.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

// FIXED: Gunakan ThreadLocal agar thread-safe saat dipanggil dari Dispatchers.IO (PDF/Excel)
// dan Main Thread (UI) secara bersamaan.
private val rupiahFormatter = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue(): DecimalFormat {
        return DecimalFormat(
            "#,###",
            DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))
        ).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }
}

fun Long.toRupiah(): String {
    val formatter = rupiahFormatter.get()
    return if (formatter != null) "Rp " + formatter.format(this) else "Rp $this"
}

fun formatRupiah(amount: Long): String = amount.toRupiah()

fun Double.roundToRupiah(): Long = this.roundToLong()

/**
 * Format kuantitas eceran: menampilkan desimal (misal "1.5")
 * dan menyembunyikan ".0" jika nilainya bilangan bulat (misal "1").
 */
fun Double.formatQuantity(): String {
    val isWholeNumber = this % 1.0 == 0.0
    return if (isWholeNumber) this.toLong().toString() else this.toString()
}