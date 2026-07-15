package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PrinterConnectionType { BLUETOOTH, WIFI, USB }

enum class PaperWidth {
    MM_58, MM_80;

    fun defaultCharPerLine(): Int = when (this) {
        MM_58 -> 32
        MM_80 -> 48
    }

    /** Lebar AREA CETAK RIIL dalam mm (BUKAN lebar kertas nominal) --
     *  dipakai sebagai parameter printerWidthMM saat membangun EscPosPrinter
     *  DantSu, supaya perhitungan lebar pixel (gambar/QR code, di H5 nanti)
     *  akurat. Lebar kertas 58mm/80mm SELALU lebih besar dari area cetak
     *  riil karena margin mekanis printhead. Nilai disepakati bersama user
     *  (bukan hasil pengukuran spesifik RPP02N) -- bisa disesuaikan lagi
     *  nanti kalau ditemukan printer dengan area cetak signifikan berbeda. */
    fun printableWidthMM(): Float = when (this) {
        MM_58 -> 48f
        MM_80 -> 72f
    }
}

@Entity(tableName = "printers")
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val connectionType: PrinterConnectionType,
    val isDefault: Boolean = false,
    val priority: Int = 0,
    val charPerLine: Int,
    val paperWidth: PaperWidth,
    val supportsStatusQuery: Boolean = false,
    val bluetoothMacAddress: String? = null,
    val wifiIpAddress: String? = null,
    val wifiPort: Int? = null,
    val usbVendorId: Int? = null,
    val usbProductId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)