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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class CameraPermissionState {
    NOT_REQUESTED,
    GRANTED,
    SHOW_RATIONALE,
    PERMANENTLY_DENIED,
}

tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
fun rememberCameraPermissionState(): Pair<CameraPermissionState, () -> Unit> {
    val ctx = LocalContext.current
    val act = ctx.findActivity()

    var state by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                CameraPermissionState.GRANTED
            } else {
                CameraPermissionState.NOT_REQUESTED
            },
        )
    }

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            state =
                when {
                    granted -> {
                        CameraPermissionState.GRANTED
                    }

                    act != null && ActivityCompat.shouldShowRequestPermissionRationale(act, Manifest.permission.CAMERA) -> {
                        CameraPermissionState.SHOW_RATIONALE
                    }

                    else -> {
                        CameraPermissionState.PERMANENTLY_DENIED
                    }
                }
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                ) {
                    state = CameraPermissionState.GRANTED
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state to { launcher.launch(Manifest.permission.CAMERA) }
}

fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    context.startActivity(intent)
}
