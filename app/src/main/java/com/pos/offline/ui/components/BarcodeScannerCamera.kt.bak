package com.pos.offline.ui.components

import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.pos.offline.util.CameraPermissionState
import com.pos.offline.util.openAppSettings
import com.pos.offline.util.rememberCameraPermissionState
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun BarcodeScannerCamera(
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) } // Handler untuk pindah ke Main Thread

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128
                )
                .build()
        )
    }
    val scanned = remember { AtomicBoolean(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { c ->
            val previewView = PreviewView(c)
            val providerFuture = ProcessCameraProvider.getInstance(c)
            providerFuture.addListener({
                val provider = providerFuture.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage == null || scanned.get()) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    scanner.process(input)
                        .addOnSuccessListener { barcodes ->
                            barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.let { code ->
                                if (scanned.compareAndSet(false, true)) {
                                    mainHandler.post { onBarcodeScanned(code) }
                                }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                }
            }, ContextCompat.getMainExecutor(c))
            previewView
        }
    )
}

@Composable
fun rememberBarcodeScanner(onScanned: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val onScannedState = rememberUpdatedState(onScanned)

    val (permState, requestPermission) = rememberCameraPermissionState()
    var showScanner by remember { mutableStateOf(false) }
    var pendingOpen by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(permState) {
        if (pendingOpen) {
            when (permState) {
                CameraPermissionState.GRANTED -> {
                    showScanner = true
                    pendingOpen = false
                }
                CameraPermissionState.PERMANENTLY_DENIED -> {
                    showDeniedDialog = true
                    pendingOpen = false
                }
                else -> Unit
            }
        }
    }

    if (showDeniedDialog) {
        val permanentlyDenied = permState == CameraPermissionState.PERMANENTLY_DENIED
        AlertDialog(
            onDismissRequest = { showDeniedDialog = false },
            title = {
                Text(if (permanentlyDenied) "Izin Kamera Diblokir" else "Izin Kamera Diperlukan")
            },
            text = {
                Text(
                    if (permanentlyDenied) {
                        "Akses kamera untuk scan barcode ditolak permanen. Aktifkan manual lewat Pengaturan aplikasi."
                    } else {
                        "Akses kamera dibutuhkan untuk memindai barcode secara langsung."
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDeniedDialog = false
                    pendingOpen = true
                    if (permanentlyDenied) {
                        openAppSettings(context) // FIX: reuse fungsi yang sudah ada, tidak ditulis ulang
                    } else {
                        requestPermission()
                    }
                }) {
                    Text(if (permanentlyDenied) "Buka Pengaturan" else "Izinkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeniedDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(Modifier.fillMaxSize()) {
                BarcodeScannerCamera(
                    onBarcodeScanned = { code ->
                        showScanner = false
                        onScannedState.value(code)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { showScanner = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                }
            }
        }
    }

    return {
        when (permState) {
            CameraPermissionState.GRANTED -> showScanner = true
            CameraPermissionState.SHOW_RATIONALE, CameraPermissionState.PERMANENTLY_DENIED -> {
                showDeniedDialog = true
            }
            else -> {
                pendingOpen = true
                requestPermission()
            }
        }
    }
}