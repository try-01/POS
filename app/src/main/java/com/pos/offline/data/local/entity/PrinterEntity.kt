package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Jenis koneksi ke printer thermal fisik. Prioritas fallback disepakati:
 *  Bluetooth -> WiFi/LAN -> USB (lihat PrintCoordinator, Batch H6). */
enum class PrinterConnectionType { BLUETOOTH, WIFI, USB }

/** Metadata asal-usul lebar kertas -- HANYA dipakai untuk menentukan nilai
 *  default [PrinterEntity.charPerLine] saat printer baru dibuat. Nilai
 *  charPerLine sesungguhnya disimpan independen supaya bisa di-override
 *  manual tanpa perlu migrasi skema baru. */
enum class PaperWidth {
    MM_58, MM_80;

    fun defaultCharPerLine(): Int = when (this) {
        MM_58 -> 32
        MM_80 -> 48
    }
}

@Entity(tableName = "printers")
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val connectionType: PrinterConnectionType,
    /** Printer utama. Hanya boleh ada 1 baris dengan nilai true -- ditegakkan
     *  di PrinterRepository (clearDefaultExcept / clearAllDefault). */
    val isDefault: Boolean = false,
    /** Urutan fallback ke printer cadangan. 0 = paling diutamakan. */
    val priority: Int = 0,
    val charPerLine: Int,
    val paperWidth: PaperWidth,
    /** Auto-detect saat Test Print (Batch H3) via perintah mentah DLE EOT 4
     *  (bypass API tinggi DantSu yang write-only), timeout ~1 detik.
     *  Ada toggle override manual di UI edit kalau auto-detect keliru. */
    val supportsStatusQuery: Boolean = false,
    val bluetoothMacAddress: String? = null,
    val wifiIpAddress: String? = null,
    val wifiPort: Int? = null,
    /** Identifier printer USB pakai kombinasi vendorId:productId (bukan
     *  device path yang bisa berubah antar colok-ulang). */
    val usbVendorId: Int? = null,
    val usbProductId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)