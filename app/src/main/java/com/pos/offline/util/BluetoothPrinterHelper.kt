package com.pos.offline.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Parcelable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

data class BluetoothDeviceInfo(val name: String, val address: String)

sealed class BondResult {
    object AlreadyBonded : BondResult()
    object Success : BondResult()
    object Failed : BondResult()
}

class BluetoothPrinterHelper(private val appContext: Context) {

    private val adapter: BluetoothAdapter?
        get() = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun isAdapterAvailable(): Boolean = adapter != null

    fun isAdapterEnabled(): Boolean = adapter?.isEnabled == true

    fun hasPermissions(): Boolean = PermissionUtils.hasBluetoothPermissions(appContext)

    @SuppressLint("MissingPermission") // dijaga pemanggil via hasPermissions()
    fun getBondedDevices(): List<BluetoothDeviceInfo> {
        if (!hasPermissions()) return emptyList()
        return try {
            adapter?.bondedDevices?.map { it.toInfo() } ?: emptyList()
        } catch (t: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverDevices(): Flow<BluetoothDeviceInfo> = callbackFlow {
        val bt = adapter
        if (bt == null || !hasPermissions()) {
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != BluetoothDevice.ACTION_FOUND) return
                val device = intent.getParcelableExtraCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    ?: return
                trySend(device.toInfo())
            }
        }
        ContextCompat.registerReceiver(
            appContext, receiver, IntentFilter(BluetoothDevice.ACTION_FOUND),
            ContextCompat.RECEIVER_EXPORTED
        )

        try {
            if (bt.isDiscovering) bt.cancelDiscovery()
            bt.startDiscovery()
        } catch (t: SecurityException) {
            close()
        }

        awaitClose {
            runCatching { appContext.unregisterReceiver(receiver) }
            runCatching { if (bt.isDiscovering) bt.cancelDiscovery() }
        }
    }

    fun cancelDiscovery() {
        try {
            adapter?.let { if (it.isDiscovering) it.cancelDiscovery() }
        } catch (t: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun pairDevice(address: String, pin: String): BondResult {
        val bt = adapter ?: return BondResult.Failed
        if (!hasPermissions()) return BondResult.Failed

        val device = try {
            bt.getRemoteDevice(address)
        } catch (t: IllegalArgumentException) {
            return BondResult.Failed
        }

        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            return BondResult.AlreadyBonded
        }

        return suspendCancellableCoroutine { cont ->
            lateinit var receiver: BroadcastReceiver
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                            val target = intent.getParcelableExtraCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            if (target?.address != device.address) return
                            val reflectionOk = trySetPinViaReflection(target, pin)
                            if (reflectionOk) {
                                try {
                                    abortBroadcast()
                                } catch (t: Throwable) {
                                }
                            }
                        }
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                            val target = intent.getParcelableExtraCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            if (target?.address != device.address) return
                            when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)) {
                                BluetoothDevice.BOND_BONDED -> {
                                    runCatching { appContext.unregisterReceiver(receiver) }
                                    if (cont.isActive) cont.resumeWith(Result.success(BondResult.Success))
                                }
                                BluetoothDevice.BOND_NONE -> {
                                    runCatching { appContext.unregisterReceiver(receiver) }
                                    if (cont.isActive) cont.resumeWith(Result.success(BondResult.Failed))
                                }
                            }
                        }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }
            ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

            cont.invokeOnCancellation {
                runCatching { appContext.unregisterReceiver(receiver) }
            }

            val started = try {
                device.createBond()
            } catch (t: SecurityException) {
                false
            }
            if (!started) {
                runCatching { appContext.unregisterReceiver(receiver) }
                if (cont.isActive) cont.resumeWith(Result.success(BondResult.Failed))
            }
        }
    }

    private fun trySetPinViaReflection(device: BluetoothDevice, pin: String): Boolean = try {
        val method = device.javaClass.getDeclaredMethod("setPin", ByteArray::class.java)
        method.isAccessible = true
        (method.invoke(device, pin.toByteArray(Charsets.UTF_8)) as? Boolean) ?: false
    } catch (t: Throwable) {
        false
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toInfo(): BluetoothDeviceInfo {
        val deviceName = try { name } catch (t: SecurityException) { null }
        return BluetoothDeviceInfo(
            name = deviceName?.takeIf { it.isNotBlank() } ?: address,
            address = address
        )
    }
}

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra(key)
    }