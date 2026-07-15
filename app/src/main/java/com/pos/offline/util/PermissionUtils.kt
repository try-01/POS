package com.pos.offline.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utilitas cek/permintaan runtime permission untuk fitur printer Bluetooth (Batch H).
 *
 * KONTEKS:
 * - Pada Android 11 (API 30) ke bawah, `BLUETOOTH` & `BLUETOOTH_ADMIN` berstatus
 *   "normal" (otomatis diberikan saat install) -- lihat AndroidManifest.xml
 *   (dideklarasikan dgn `android:maxSdkVersion="30"`), jadi TIDAK perlu diminta
 *   saat runtime di versi Android tsb.
 * - Mulai Android 12 (API 31), Google memecah izin Bluetooth klasik menjadi
 *   dua izin "dangerous" terpisah yang WAJIB diminta saat runtime:
 *     - BLUETOOTH_SCAN    -> dipakai utk mencari printer BARU yang belum
 *       ter-pairing (discovery, dipakai di alur pairing custom-PIN Batch H3).
 *     - BLUETOOTH_CONNECT -> dipakai utk membaca daftar device yang sudah
 *       ter-pairing (getBondedDevices), createBond(), dan membuka
 *       BluetoothSocket ke printer utk mengirim data cetak.
 * - `BLUETOOTH_SCAN` sudah dideklarasikan dengan flag `neverForLocation` di
 *   manifest -> app menyatakan tidak memakai hasil scan utk menyimpulkan
 *   lokasi fisik pengguna, sehingga TIDAK perlu `ACCESS_FINE_LOCATION`
 *   sekalipun secara umum BLE scan biasanya terikat izin lokasi. Ini valid
 *   karena di sini kita hanya scan Bluetooth Classic (printer thermal),
 *   bukan Bluetooth Low Energy.
 *
 * CATATAN USB (bukan bagian utilitas ini): permission USB TIDAK memakai
 * mekanisme runtime-permission standar Android (tidak ada string di
 * `Manifest.permission` untuk itu). Izin akses ke device USB tertentu
 * diberikan per-device lewat `UsbManager.requestPermission(device, pendingIntent)`
 * dan dicek lewat `UsbManager.hasPermission(device)` -- akan diimplementasikan
 * langsung di alur pemilihan device USB pada Batch H3/H6, bukan di sini.
 */
object PermissionUtils {

    /** Daftar permission Bluetooth yang WAJIB diminta lewat runtime request,
     *  sesuai level API perangkat. Array kosong di API < 31 (tidak ada yang
     *  perlu diminta runtime -- sudah berstatus normal-permission). */
    fun requiredBluetoothPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }

    /** True kalau semua permission Bluetooth yang relevan sudah diberikan.
     *  Selalu true di API < 31 (bersifat normal-permission, otomatis granted
     *  saat install). Dipakai untuk decide apakah perlu memanggil
     *  ActivityResultLauncher.launch(requiredBluetoothPermissions()) sebelum
     *  membuka halaman scan/pairing printer (Batch H3). */
    fun hasBluetoothPermissions(context: Context): Boolean =
        requiredBluetoothPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
}