package com.example.posoffline.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App-wide dark theme. POS is almost always used indoors at night, so we
 * keep the dark scheme as the default and skip light mode to save bytes.
 */
private val DarkScheme = darkColorScheme(
    primary = AppColors.Indigo500,
    onPrimary = AppColors.Slate50,
    primaryContainer = AppColors.Indigo600,
    onPrimaryContainer = AppColors.Slate50,
    secondary = AppColors.Fuchsia500,
    onSecondary = AppColors.Slate50,
    tertiary = AppColors.Sky400,
    onTertiary = AppColors.Slate50,
    background = AppColors.Bg0,
    onBackground = AppColors.Slate200,
    surface = AppColors.Bg1,
    onSurface = AppColors.Slate200,
    surfaceVariant = AppColors.Bg2,
    onSurfaceVariant = AppColors.Slate400,
    outline = AppColors.Slate500,
    error = AppColors.Rose300
)

@Composable
fun OfflinePosTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppColors.Bg0.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography(),
        content = content
    )
}
