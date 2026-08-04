package com.pos.offline.ui.components

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import android.view.WindowManager
import android.content.ContextWrapper
import android.content.Context
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.pos.offline.util.CameraPermissionState
import com.pos.offline.util.ScanFeedbackManager
import com.pos.offline.util.ScanPreferencesRepository
import com.pos.offline.util.VibrationLevel
import com.pos.offline.util.openAppSettings
import com.pos.offline.util.rememberCameraPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.compose.ui.geometry.Size as GeometrySize

@Composable
fun BarcodeScannerCamera(
    onBarcodeScanned: suspend (String) -> Boolean,
    isMultiScanMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner =
        remember {
            BarcodeScanning.getClient(
                BarcodeScannerOptions
                    .Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                    ).build(),
            )
        }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTime by remember { mutableLongStateOf(0L) }
    // Tambahkan 3 baris ini:
    var pendingCode by remember { mutableStateOf<String?>(null) }
    var pendingCodeCount by remember { mutableIntStateOf(0) }
    var scanVisualState by remember { mutableStateOf(ScanVisualState.IDLE) }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    fun attemptBind() {
        val provider = cameraProvider ?: return
        val previewView = previewViewRef ?: return
        try {
            if (executor.isShutdown) return
            val resolutionStrategy =
                ResolutionStrategy(
                    Size(720, 1280),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                )
            val resolutionSelector =
                ResolutionSelector
                    .Builder()
                    .setResolutionStrategy(resolutionStrategy)
                    .build()
            val preview =
                Preview
                    .Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
            val analysis =
                ImageAnalysis
                    .Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
            analysis.setAnalyzer(executor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                try {
                    val rotationDegrees = proxy.imageInfo.rotationDegrees
                    val input = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                    scanner
                        .process(input)
                        .addOnSuccessListener { barcodes ->
                            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                            val imgW = if (isRotated) proxy.height.toFloat() else proxy.width.toFloat()
                            val imgH = if (isRotated) proxy.width.toFloat() else proxy.height.toFloat()

                            /**
                            val targetBarcode =
                                barcodes.firstOrNull { barcode ->
                                    val raw = barcode.rawValue
                                    if (raw.isNullOrBlank()) return@firstOrNull false
                                    val rect = barcode.boundingBox ?: return@firstOrNull false
                                    val normLeft = rect.left / imgW
                                    val normRight = rect.right / imgW
                                    val normTop = rect.top / imgH
                                    val normBottom = rect.bottom / imgH
                                    
                                    // AREA KETAT DIPERBARUI: 
                                    // Sumbu X diperketat menjadi 25% (0.25f) hingga 75% (0.75f)
                                    // Sumbu Y tetap 35% (0.35f) hingga 65% (0.65f)
                                    normLeft >= 0.35f && normRight <= 0.65f &&
                                        normTop >= 0.40f && normBottom <= 0.60f
                                }
                            **/
                            val targetBarcode =
                                barcodes.firstOrNull { barcode ->
                                    val raw = barcode.rawValue
                                    if (raw.isNullOrBlank()) return@firstOrNull false
                                    val rect = barcode.boundingBox ?: return@firstOrNull false

                                    // 1. Ambil ukuran layar fisik (PreviewView) saat ini
                                    val viewW = previewView.width.toFloat()
                                    val viewH = previewView.height.toFloat()
                                    // Cegah pembagian dengan nol jika layar belum selesai dirender
                                    if (viewW == 0f || viewH == 0f) return@firstOrNull false

                                    // 2. Ambil ukuran gambar mentah dari sensor kamera
                                    val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                                    val imgW = if (isRotated) proxy.height.toFloat() else proxy.width.toFloat()
                                    val imgH = if (isRotated) proxy.width.toFloat() else proxy.height.toFloat()

                                    // 3. Hitung rasio skala (Center-Crop)
                                    // Kamera akan memperbesar gambar sampai sisi terkecilnya menutupi layar
                                    val scale = maxOf(viewW / imgW, viewH / imgH)
                                    val scaledImgW = imgW * scale
                                    val scaledImgH = imgH * scale

                                    // 4. Hitung Offset (Area gambar yang meluber keluar layar dan dipotong)
                                    val offsetX = (scaledImgW - viewW) / 2f
                                    val offsetY = (scaledImgH - viewH) / 2f

                                    // 5. Konversi koordinat barcode dari kamera ke koordinat layar fisik
                                    val screenLeft = (rect.left * scale) - offsetX
                                    val screenRight = (rect.right * scale) - offsetX
                                    val screenTop = (rect.top * scale) - offsetY
                                    val screenBottom = (rect.bottom * scale) - offsetY

                                    // 6. Hitung batas kotak visual kita
                                    // Lebar kotak adalah 75% layar, berarti tersisa 25% (12.5% Kiri, 12.5% Kanan)
                                    // Tinggi kotak adalah 35% layar, berarti tersisa 65% (32.5% Atas, 32.5% Bawah)
                                    // Kita beri toleransi 2.5% agar pengguna tidak kesulitan jika tangan sedikit gemetar
                                    val boxLeft = viewW * 0.15f
                                    val boxRight = viewW * 0.85f
                                    val boxTop = viewH * 0.35f
                                    val boxBottom = viewH * 0.65f

                                    // 7. Syarat Mutlak: Barcode harus ada di DALAM kotak visual secara adaptif!
                                    screenLeft >= boxLeft && screenRight <= boxRight &&
                                        screenTop >= boxTop && screenBottom <= boxBottom
                                }

                            if (targetBarcode != null) {
                                val code = targetBarcode.rawValue!!
                                
                                // ATURAN 2-FRAME: Pastikan kode yang terbaca konsisten
                                if (code == pendingCode) {
                                    pendingCodeCount++
                                    // Jika kamera membaca teks yang persis sama 2x berturut-turut (fokus sudah stabil)
                                    if (pendingCodeCount >= 2) { 
                                        val currentTime = System.currentTimeMillis()
                                        val isSameCode = (code == lastScannedCode)
                                        // Waktu jeda (cooldown) untuk kode yang sama: 2 detik (2000L)
                                        val isTimeElapsed = (currentTime - lastScannedTime) > 2000L
                                        
                                        if (!isSameCode || isTimeElapsed) {
                                            lastScannedCode = code
                                            lastScannedTime = currentTime
                                            coroutineScope.launch {
                                                val isSuccess = onBarcodeScanned(code)
                                                scanVisualState = if (isSuccess) ScanVisualState.SUCCESS else ScanVisualState.ERROR
                                                delay(400L)
                                                scanVisualState = ScanVisualState.IDLE
                                            }
                                        }
                                    }
                                } else {
                                    // Jika kode terbaca berbeda (misal karena blur), reset sistem antrean
                                    pendingCode = code
                                    pendingCodeCount = 1
                                }
                            } else {
                                // Jika tidak ada barcode di dalam kotak visual, bersihkan antrean
                                pendingCode = null
                                pendingCodeCount = 0
                            }
                        }.addOnCompleteListener {
                            proxy.close()
                        }
                } catch (e: Exception) {
                    proxy.close()
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            cameraError = null
        } catch (e: Exception) {
            cameraError = "Gagal membuka kamera."
        }
    }

    DisposableEffect(Unit) {
        lastScannedCode = null
        lastScannedTime = 0L
        pendingCode = null
        pendingCodeCount = 0
        onDispose {
            lastScannedCode = null
            lastScannedTime = 0L
            cameraProvider?.unbindAll()
            executor.shutdown()
            scanner.close()
            scanVisualState = ScanVisualState.IDLE
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c: Context ->
                val previewView =
                    PreviewView(c).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                previewViewRef = previewView
                val providerFuture = ProcessCameraProvider.getInstance(c)
                providerFuture.addListener({
                    try {
                        cameraProvider = providerFuture.get()
                        attemptBind()
                    } catch (e: Exception) {
                        cameraError = "Gagal membuka kamera."
                    }
                }, ContextCompat.getMainExecutor(c))
                previewView
            },
        )
        ScannerViewfinder(
            scanState = scanVisualState, // Kirimkan status animasi
            modifier = Modifier.fillMaxSize()
        )
        cameraError?.let { message ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(text = message, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { attemptBind() }) {
                        Text("Coba Lagi")
                    }
                }
            }
        }
    }
}

@Composable
fun rememberBarcodeScanner(onScanned: suspend (String) -> String?): () -> Unit {
    val context = LocalContext.current
    val onScannedState = rememberUpdatedState(onScanned)
    val feedbackManager = remember { ScanFeedbackManager(context) }
    val prefsRepository = remember { ScanPreferencesRepository(context) }
    val isSoundEnabled by prefsRepository.isSoundEnabled.collectAsStateWithLifecycle()
    val soundVolume by prefsRepository.soundVolume.collectAsStateWithLifecycle()
    val soundDurationMs by prefsRepository.soundDurationMs.collectAsStateWithLifecycle()
    val isVibrationEnabled by prefsRepository.isVibrationEnabled.collectAsStateWithLifecycle()
    val vibrationIntensity by prefsRepository.vibrationIntensity.collectAsStateWithLifecycle()
    val vibrationDurationMs by prefsRepository.vibrationDurationMs.collectAsStateWithLifecycle()

    val vibrationLevel =
        remember(vibrationIntensity) {
            when {
                vibrationIntensity <= 35 -> VibrationLevel.HALUS
                vibrationIntensity <= 70 -> VibrationLevel.SEDANG
                else -> VibrationLevel.KUAT
            }
        }

    val (permState, requestPermission) = rememberCameraPermissionState()
    var showScanner by remember { mutableStateOf(false) }
    var pendingOpen by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }
    var isMultiScanMode by remember { mutableStateOf(false) }
    var scannedCountBatch by remember { mutableIntStateOf(0) }
    var lastScannedCodeText by remember { mutableStateOf("") }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scanErrorMessage) {
        if (scanErrorMessage != null) {
            delay(2500L)
            scanErrorMessage = null
        }
    }

    LaunchedEffect(showScanner) {
        if (showScanner) {
            isMultiScanMode = false
            scannedCountBatch = 0
            lastScannedCodeText = ""
            scanErrorMessage = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            feedbackManager.release()
        }
    }

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

                else -> {
                    Unit
                }
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
                    },
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDeniedDialog = false
                    pendingOpen = true
                    if (permanentlyDenied) {
                        openAppSettings(context)
                    } else {
                        requestPermission()
                    }
                }) {
                    Text(if (permanentlyDenied) "Buka Pengaturan" else "Izinkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeniedDialog = false }) { Text("Tutup") }
            },
        )
    }

    if (showScanner) {
    Dialog(
        onDismissRequest = { showScanner = false },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        // --- FIX: Paksa window benar-benar MATCH_PARENT dan transparan ---
        val view = LocalView.current
        
        // Menggunakan LaunchedEffect(view) agar kode ini HANYA dipanggil satu kali 
        // saat layar Dialog pertama kali dimuat, bukan pada setiap perubahan UI.
        LaunchedEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                
                // 1. Set ukuran Window menjadi penuh (Full Screen)
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                )
                
                // 2. Izinkan UI digambar di belakang system bar (status bar & navigation bar)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                
                // 3. Tambahkan flag khusus agar kita bisa mengubah warna background system bar
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                
                // 4. Secara eksplisit atur warna status bar dan navigasi bawah menjadi transparan
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
        }


        BackHandler { showScanner = false }
        val (statusBarHeight, navBarHeight) = rememberRealSystemBarInsets()
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            BarcodeScannerCamera(
                isMultiScanMode = isMultiScanMode,
                onBarcodeScanned = { code ->
                    val scannedName = onScannedState.value(code)
                    val isSuccess = scannedName != null
                    // val isSuccess = onScannedState.value(code)
                    if (isSuccess) {
                        scanErrorMessage = null

                        feedbackManager.triggerSuccessFeedback(
                            soundEnabled = isSoundEnabled,
                            soundVolume = soundVolume,
                            soundDurationMs = soundDurationMs,
                            vibrationEnabled = isVibrationEnabled,
                            vibrationLevel = vibrationLevel,
                            vibrationDurationMs = vibrationDurationMs,
                        )
                        scannedCountBatch++
                        lastScannedCodeText = scannedName!! 
                        
                        if (!isMultiScanMode) {
                            showScanner = false
                        }
                    } else {
                        scanErrorMessage = "Produk tidak ditemukan ($code)"

                        feedbackManager.triggerFailureFeedback(
                            soundEnabled = isSoundEnabled,
                            soundVolume = soundVolume,
                            vibrationEnabled = isVibrationEnabled,
                            vibrationLevel = vibrationLevel,
                        )
                    }
                    isSuccess
                },
                modifier = Modifier.fillMaxSize(),
            )
            AnimatedVisibility(
                visible = scannedCountBatch > 0 && scanErrorMessage == null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$scannedCountBatch Item Masuk Keranjang",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = scanErrorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 12.dp, end = 60.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = scanErrorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            IconButton(
                onClick = { showScanner = false },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color.White)
            }
            Card(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = navBarHeight + 30.dp),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.85f),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Mode Multi-Scan (Beruntun)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (isMultiScanMode) "Kamera tetap terbuka setelah scan" else "Kamera otomatis tutup 1x scan",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        Switch(
                            checked = isMultiScanMode,
                            onCheckedChange = { isMultiScanMode = it },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }
                    AnimatedVisibility(visible = lastScannedCodeText.isNotEmpty()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Terakhir: $lastScannedCodeText",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Button(
                        onClick = { showScanner = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (scannedCountBatch > 0) "Selesai ($scannedCountBatch Item Dipindai)" else "Selesai",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
    }
    return {
        when (permState) {
            CameraPermissionState.GRANTED -> {
                showScanner = true
            }

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

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun rememberRealSystemBarInsets(): Pair<Dp, Dp> {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    return remember(configuration, density) {
        val activity = context.findActivity()
        val insets = activity?.window?.decorView?.let { ViewCompat.getRootWindowInsets(it) }
        val statusPx = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        val navPx = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        with(density) { statusPx.toDp() to navPx.toDp() }
    }
}

@Composable
private fun ScannerViewfinder(
    scanState: ScanVisualState,
    modifier: Modifier = Modifier
) {
    // Animasi Warna: Transisi mulus antara Putih, Hijau, dan Merah
    val animatedColor by animateColorAsState(
        targetValue = when (scanState) {
            ScanVisualState.IDLE -> Color.White
            ScanVisualState.SUCCESS -> Color(0xFF4CAF50) // Warna Hijau Sukses
            ScanVisualState.ERROR -> Color(0xFFF44336) // Warna Merah Gagal
        },
        animationSpec = tween(durationMillis = 200),
        label = "colorAnimation"
    )

    // Animasi Ketebalan: Berubah dari 4.dp ke 9.dp menciptakan efek "berkedut"
    val animatedStrokeDp by animateDpAsState(
        targetValue = when (scanState) {
            ScanVisualState.IDLE -> 4.dp
            else -> 9.dp // Menebal saat sukses/gagal
        },
        animationSpec = tween(durationMillis = 200),
        label = "strokeAnimation"
    )

    Box(
        modifier = modifier.drawWithCache {
            val boxWidth = size.width * 0.75f
            val boxHeight = size.height * 0.35f
            val left = (size.width - boxWidth) / 2f
            val top = (size.height - boxHeight) / 2f
            val right = left + boxWidth
            val bottom = top + boxHeight
            
            val cornerRadiusPx = 16.dp.toPx()
            val cornerLength = 40.dp.toPx()
            
            val outerRect = Rect(0f, 0f, size.width, size.height)
            val boxRect = RoundRect(
                rect = Rect(left, top, right, bottom),
                cornerRadius = CornerRadius(cornerRadiusPx)
            )
            
            val overlayPath = Path().apply { addRect(outerRect) }
            val cutoutPath = Path().apply { addRoundRect(boxRect) }
            val dimmedPath = Path.combine(
                operation = PathOperation.Difference,
                path1 = overlayPath,
                path2 = cutoutPath,
            )

            onDrawWithContent {
                drawContent()
                drawPath(dimmedPath, Color.Black.copy(alpha = 0.55f))
                
                // Ambil nilai animasi saat ini
                val color = animatedColor
                val strokeWidthPx = animatedStrokeDp.toPx()
                val strokeStyle = Stroke(width = strokeWidthPx)
                
                // --- Gambar Sudut Kiri Atas ---
                drawLine(color, Offset(left, top + cornerRadiusPx), Offset(left, top + cornerLength), strokeWidthPx)
                drawLine(color, Offset(left + cornerRadiusPx, top), Offset(left + cornerLength, top), strokeWidthPx)
                drawArc(color, startAngle = 180f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(left, top), size = GeometrySize(cornerRadiusPx * 2, cornerRadiusPx * 2), style = strokeStyle)

                // --- Gambar Sudut Kanan Atas ---
                drawLine(color, Offset(right, top + cornerRadiusPx), Offset(right, top + cornerLength), strokeWidthPx)
                drawLine(color, Offset(right - cornerRadiusPx, top), Offset(right - cornerLength, top), strokeWidthPx)
                drawArc(color, startAngle = 270f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(right - cornerRadiusPx * 2, top), size = GeometrySize(cornerRadiusPx * 2, cornerRadiusPx * 2), style = strokeStyle)

                // --- Gambar Sudut Kiri Bawah ---
                drawLine(color, Offset(left, bottom - cornerRadiusPx), Offset(left, bottom - cornerLength), strokeWidthPx)
                drawLine(color, Offset(left + cornerRadiusPx, bottom), Offset(left + cornerLength, bottom), strokeWidthPx)
                drawArc(color, startAngle = 90f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(left, bottom - cornerRadiusPx * 2), size = GeometrySize(cornerRadiusPx * 2, cornerRadiusPx * 2), style = strokeStyle)

                // --- Gambar Sudut Kanan Bawah ---
                drawLine(color, Offset(right, bottom - cornerRadiusPx), Offset(right, bottom - cornerLength), strokeWidthPx)
                drawLine(color, Offset(right - cornerRadiusPx, bottom), Offset(right - cornerLength, bottom), strokeWidthPx)
                drawArc(color, startAngle = 0f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(right - cornerRadiusPx * 2, bottom - cornerRadiusPx * 2), size = GeometrySize(cornerRadiusPx * 2, cornerRadiusPx * 2), style = strokeStyle)
            }
        }
    )
}

enum class ScanVisualState {
    IDLE,
    SUCCESS,
    ERROR
}
