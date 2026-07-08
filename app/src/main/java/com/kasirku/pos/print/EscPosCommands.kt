package com.kasirku.pos.print

/**
 * ESC/POS Command Set untuk thermal printer
 */
object EscPosCommands {
    val INIT = byteArrayOf(0x1B, 0x40)
    val LINE_FEED = byteArrayOf(0x0A)
    val FEED_LINES = byteArrayOf(0x1B, 0x64, 0x03)
    val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x01)

    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)

    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)

    val FONT_SIZE_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    val FONT_SIZE_2X = byteArrayOf(0x1D, 0x21, 0x11)
}
