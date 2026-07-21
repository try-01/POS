package com.pos.offline.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

data class UsbDeviceInfo(
    val deviceName: String,
    val label: String,
    val vendorId: Int,
    val productId: Int,
)

sealed class UsbPermissionResult {
    object Granted : UsbPermissionResult()

    object Denied : UsbPermissionResult()
}

class UsbPrinterHelper(
    private val appContext: Context,
) {
    private val usbManager: UsbManager?
        get() = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager

    fun isUsbSupported(): Boolean = usbManager != null

    fun getDeviceList(): List<UsbDeviceInfo> {
        val manager = usbManager ?: return emptyList()
        return manager.deviceList.values.map { it.toInfo() }
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager?.hasPermission(device) == true

    fun findDeviceByName(deviceName: String): UsbDevice? = usbManager?.deviceList?.get(deviceName)

    fun findDeviceByVendorProduct(
        vendorId: Int,
        productId: Int,
    ): UsbDevice? =
        usbManager?.deviceList?.values?.firstOrNull {
            it.vendorId == vendorId && it.productId == productId
        }

    fun getSystemUsbManager(): UsbManager? = usbManager

    fun observeAttachDetach(): Flow<Unit> =
        callbackFlow {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        when (intent.action) {
                            UsbManager.ACTION_USB_DEVICE_ATTACHED,
                            UsbManager.ACTION_USB_DEVICE_DETACHED,
                            -> trySend(Unit)
                        }
                    }
                }
            val filter =
                IntentFilter().apply {
                    addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                    addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                }

            ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

            awaitClose {
                runCatching { appContext.unregisterReceiver(receiver) }
            }
        }

    suspend fun requestPermission(device: UsbDevice): UsbPermissionResult {
        val manager = usbManager ?: return UsbPermissionResult.Denied
        if (manager.hasPermission(device)) return UsbPermissionResult.Granted

        return suspendCancellableCoroutine { cont ->
            val action = "${appContext.packageName}.USB_PERMISSION"
            lateinit var receiver: BroadcastReceiver
            receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        if (intent.action != action) return
                        runCatching { appContext.unregisterReceiver(receiver) }
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (cont.isActive) {
                            cont.resumeWith(
                                Result.success(
                                    if (granted) UsbPermissionResult.Granted else UsbPermissionResult.Denied,
                                ),
                            )
                        }
                    }
                }

            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(action),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )

            cont.invokeOnCancellation {
                runCatching { appContext.unregisterReceiver(receiver) }
            }

            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            val permissionIntent =
                PendingIntent.getBroadcast(
                    appContext,
                    0,
                    Intent(action).setPackage(appContext.packageName),
                    flags,
                )

            manager.requestPermission(device, permissionIntent)
        }
    }

    private fun UsbDevice.toInfo(): UsbDeviceInfo {
        val name =
            productName?.takeIf { it.isNotBlank() }
                ?: String.format("USB %04x:%04x", vendorId, productId)
        return UsbDeviceInfo(
            deviceName = deviceName,
            label = name,
            vendorId = vendorId,
            productId = productId,
        )
    }
}
