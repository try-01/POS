package com.pos.offline.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    private const val PREFS_NAME = "permission_prefs"
    private const val KEY_BLUETOOTH_REQUESTED = "bluetooth_permission_requested"

    /** Status hasil pengecekan izin Bluetooth, dipakai UI untuk memutuskan
     *  aksi yang tepat -- KARENA memanggil launcher.launch() saat kondisi
     *  sebenarnya [PermanentlyDenied] akan percuma (sistem tidak menampilkan
     *  dialog apa pun, hanya "flicker" activity permission yang langsung
     *  menutup diri sendiri). Lihat catatan bug H3b untuk detail. */
    sealed class BluetoothPermissionState {
        object Granted : BluetoothPermissionState()
        object CanRequest : BluetoothPermissionState()
        object PermanentlyDenied : BluetoothPermissionState()
    }

    /** Daftar permission Bluetooth yang WAJIB diminta lewat runtime request,
     *  sesuai level API perangkat. Array kosong di API < 31 (tidak ada yang
     *  perlu diminta runtime -- sudah berstatus normal-permission). */
    fun requiredBluetoothPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }

    fun hasBluetoothPermissions(context: Context): Boolean =
        requiredBluetoothPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

    /**
     * Tandai bahwa dialog request permission Bluetooth SUDAH PERNAH
     * ditampilkan (apa pun hasilnya). Disimpan manual di SharedPreferences
     * karena Android TIDAK menyediakan API untuk membedakan "belum pernah
     * ditanya" vs "ditolak permanen" -- keduanya sama-sama membuat
     * shouldShowRequestPermissionRationale() == false. Tanpa flag ini, kita
     * tidak bisa membedakan kapan aman memanggil launch() lagi vs kapan
     * harus mengarahkan user ke Pengaturan Aplikasi.
     */
    fun markBluetoothPermissionRequested(context: Context) {
        prefs(context).edit().putBoolean(KEY_BLUETOOTH_REQUESTED, true).apply()
    }

    fun wasBluetoothPermissionRequestedBefore(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLUETOOTH_REQUESTED, false)

    /**
     * Tentukan status permission Bluetooth saat ini secara lengkap.
     *
     * Catatan penting: sebagian OEM (termasuk Xiaomi HyperOS, dikonfirmasi
     * oleh user di Android 16/HyperOS 3) bisa menandai permission grup
     * "Nearby devices" sebagai tidak-boleh-ditanya-lagi HANYA SETELAH SATU
     * kali penolakan, tanpa user pernah mencentang "Jangan tanya lagi" --
     * berbeda dari perilaku standar AOSP (biasanya butuh 2x penolakan).
     * Fungsi ini TIDAK bergantung pada asumsi jumlah penolakan tertentu --
     * ia murni mengikuti apa kata shouldShowRequestPermissionRationale()
     * dikombinasikan dengan flag "sudah pernah ditanya", sehingga otomatis
     * benar di semua OEM/versi Android tanpa perlu hardcode kebijakan OEM.
     */
    fun currentBluetoothPermissionState(context: Context): BluetoothPermissionState {
        if (hasBluetoothPermissions(context)) return BluetoothPermissionState.Granted

        val required = requiredBluetoothPermissions()
        if (required.isEmpty()) return BluetoothPermissionState.Granted // API < 31

        val activity = findActivity(context)
            ?: return BluetoothPermissionState.CanRequest // fallback aman

        val canShowRationale = required.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        val requestedBefore = wasBluetoothPermissionRequestedBefore(context)

        return when {
            canShowRationale -> BluetoothPermissionState.CanRequest
            requestedBefore -> BluetoothPermissionState.PermanentlyDenied
            else -> BluetoothPermissionState.CanRequest // belum pernah ditanya sama sekali
        }
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}