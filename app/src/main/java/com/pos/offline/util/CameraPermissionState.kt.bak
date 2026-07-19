package com.pos.offline.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class CameraPermissionState {
    NOT_REQUESTED, GRANTED, SHOW_RATIONALE, PERMANENTLY_DENIED
}

// Fungsi pengupas Context standar Compose untuk mencegah ClassCastException
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberCameraPermissionState(): Pair<CameraPermissionState, () -> Unit> {
    val context = LocalContext.current
    val activity = context.findActivity() // Menggunakan metode aman

    var state by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) CameraPermissionState.GRANTED
            else CameraPermissionState.NOT_REQUESTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        state = when {
            granted -> CameraPermissionState.GRANTED
            // Pastikan activity tidak null sebelum mengecek Rationale
            activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) ->
                CameraPermissionState.SHOW_RATIONALE   // Lapis 1: baru ditolak sekali
            else -> CameraPermissionState.PERMANENTLY_DENIED // Lapis 2
        }
    }

    return state to { launcher.launch(Manifest.permission.CAMERA) }
}

fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}