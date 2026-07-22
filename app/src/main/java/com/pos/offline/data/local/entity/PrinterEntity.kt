package com.pos.offline.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PrinterConnectionType { BLUETOOTH, WIFI, USB }

enum class PaperWidth {
    MM_58,
    MM_80,
    ;

    fun defaultCharPerLine(): Int =
        when (this) {
            MM_58 -> 32
            MM_80 -> 48
        }

    fun printableWidthMM(): Float =
        when (this) {
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
    val createdAt: Long = System.currentTimeMillis(),
    val statusQueryFailStreak: Int = 0,
    @ColumnInfo(name = "autoDisabledDueToNoResponse", defaultValue = "0")
    val autoDisabledDueToNoResponse: Boolean = false,
)