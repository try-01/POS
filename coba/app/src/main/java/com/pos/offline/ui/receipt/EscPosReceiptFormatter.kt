package com.pos.offline.ui.receipt
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.pos.offline.data.local.entity.StoreProfileEntity
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.TransactionItemEntity
import com.pos.offline.data.repository.CheckoutResult
object EscPosReceiptFormatter {
    private const val MAX_IMAGE_HEIGHT_PX = 256
    fun build(
        printer: EscPosPrinter,
        checkoutResult: CheckoutResult,
        storeProfile: StoreProfileEntity,
    ): List<String> = build(printer, checkoutResult.transaction, checkoutResult.items, storeProfile)
    fun build(
        printer: EscPosPrinter,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        storeProfile: StoreProfileEntity,
    ): List<String> {
        val logoHex = buildLogoHex(printer, storeProfile.logoBytes)
        val logoMarkup = logoHex?.let { "[C]<img>$it</img>\n" }
        val checkoutResult = CheckoutResult(transaction, items)
        val lines =
            ReceiptManager.buildLines(
                result = checkoutResult,
                storeProfile = storeProfile,
                dividerWidth = printer.printerNbrCharactersPerLine,
            )
        val markup = ReceiptManager.linesToEscPosMarkup(lines)
        return listOfNotNull(logoMarkup, markup)
    }
    private fun buildLogoHex(
        printer: EscPosPrinter,
        logoBytes: ByteArray?,
    ): String? {
        if (logoBytes == null) return null
        val original =
            try {
                BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)
            } catch (t: Throwable) {
                null
            } ?: return null
        val resized =
            if (original.height > MAX_IMAGE_HEIGHT_PX) {
                val ratio = MAX_IMAGE_HEIGHT_PX.toFloat() / original.height
                val newWidth = (original.width * ratio).toInt().coerceAtLeast(1)
                try {
                    Bitmap.createScaledBitmap(original, newWidth, MAX_IMAGE_HEIGHT_PX, true)
                } catch (t: Throwable) {
                    original
                }
            } else {
                original
            }
        val hex =
            try {
                PrinterTextParserImg.bitmapToHexadecimalString(printer, resized, true)
            } catch (t: Throwable) {
                null
            }
        if (resized !== original) {
            runCatching { resized.recycle() }
        }
        runCatching { original.recycle() }
        return hex
    }
}
