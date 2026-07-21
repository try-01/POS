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

    sealed class BluetoothPermissionState {
        object Granted : BluetoothPermissionState()

        object CanRequest : BluetoothPermissionState()

        object PermanentlyDenied : BluetoothPermissionState()
    }

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

    fun markBluetoothPermissionRequested(context: Context) {
        prefs(context).edit().putBoolean(KEY_BLUETOOTH_REQUESTED, true).apply()
    }

    fun wasBluetoothPermissionRequestedBefore(context: Context): Boolean = prefs(context).getBoolean(KEY_BLUETOOTH_REQUESTED, false)

    fun currentBluetoothPermissionState(context: Context): BluetoothPermissionState {
        if (hasBluetoothPermissions(context)) return BluetoothPermissionState.Granted

        val required = requiredBluetoothPermissions()
        if (required.isEmpty()) return BluetoothPermissionState.Granted // API < 31

        val activity =
            findActivity(context)
                ?: return BluetoothPermissionState.CanRequest // fallback aman

        val canShowRationale =
            required.any {
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

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
