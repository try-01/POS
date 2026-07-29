package com.pos.offline.util
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong
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
fun Double.formatQuantity(): String {
    val isWholeNumber = this % 1.0 == 0.0
    return if (isWholeNumber) this.toLong().toString() else this.toString()
}
