package com.kasirku.pos.ui.receipt

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Builder perintah byte standar ESC/POS (Epson/Star/POS thermal printers 58mm & 80mm).
 * Sangat ringan, tidak membutuhkan library eksternal, dan langsung mentransmisikan byte stream ke printer via Bluetooth.
 */
class EscPosCommandBuilder {
    private val buffer = ByteArrayOutputStream()

    companion object {
        private val ESC = 0x1B.toByte()
        private val GS = 0x1D.toByte()
        private val LF = 0x0A.toByte()

        // Commands
        private val INIT = byteArrayOf(ESC, 0x40.toByte())
        private val ALIGN_LEFT = byteArrayOf(ESC, 0x61.toByte(), 0x00)
        private val ALIGN_CENTER = byteArrayOf(ESC, 0x61.toByte(), 0x01)
        private val ALIGN_RIGHT = byteArrayOf(ESC, 0x61.toByte(), 0x02)
        private val BOLD_ON = byteArrayOf(ESC, 0x45.toByte(), 0x01)
        private val BOLD_OFF = byteArrayOf(ESC, 0x45.toByte(), 0x00)
        private val SIZE_DOUBLE = byteArrayOf(GS, 0x21.toByte(), 0x11)
        private val SIZE_NORMAL = byteArrayOf(GS, 0x21.toByte(), 0x00)
        private val CUT_PAPER = byteArrayOf(GS, 0x56.toByte(), 0x41.toByte(), 0x03)
    }

    init {
        buffer.write(INIT)
    }

    fun alignCenter(): EscPosCommandBuilder {
        buffer.write(ALIGN_CENTER)
        return this
    }

    fun alignLeft(): EscPosCommandBuilder {
        buffer.write(ALIGN_LEFT)
        return this
    }

    fun alignRight(): EscPosCommandBuilder {
        buffer.write(ALIGN_RIGHT)
        return this
    }

    fun bold(enable: Boolean): EscPosCommandBuilder {
        buffer.write(if (enable) BOLD_ON else BOLD_OFF)
        return this
    }

    fun doubleSize(enable: Boolean): EscPosCommandBuilder {
        buffer.write(if (enable) SIZE_DOUBLE else SIZE_NORMAL)
        return this
    }

    fun text(text: String): EscPosCommandBuilder {
        // Charset CP437 atau ISO-8859-1 umum untuk printer thermal
        val bytes = text.toByteArray(Charset.forName("ISO-8859-1"))
        buffer.write(bytes)
        return this
    }

    fun newLine(count: Int = 1): EscPosCommandBuilder {
        for (i in 0 until count) {
            buffer.write(LF.toInt())
        }
        return this
    }

    fun separator(char: String = "-", width: Int = 32): EscPosCommandBuilder {
        val line = char.repeat(width)
        return text(line).newLine()
    }

    fun cut(): EscPosCommandBuilder {
        buffer.write(CUT_PAPER)
        return this
    }

    fun getBytes(): ByteArray = buffer.toByteArray()
}
