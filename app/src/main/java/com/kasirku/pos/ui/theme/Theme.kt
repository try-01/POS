package com.kasirku.pos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PosPrimary = Color(0xFF2F6FED)
val PosPrimaryDark = Color(0xFF6E9BFF)
val PosSecondary = Color(0xFF00C2A8)
val PosBackgroundLight = Color(0xFFF5F7FB)
val PosBackgroundDark = Color(0xFF10131A)

// Warna permukaan semi-transparan -> dasar dari efek "glassmorphism ringan" tanpa blur nyata.
val PosSurfaceGlassLight = Color(0xCCFFFFFF)
val PosSurfaceGlassDark = Color(0xCC1B1F27)

private val LightColors = lightColorScheme(
    primary = PosPrimary,
    secondary = PosSecondary,
    background = PosBackgroundLight,
    surface = PosSurfaceGlassLight
)

private val DarkColors = darkColorScheme(
    primary = PosPrimaryDark,
    secondary = PosSecondary,
    background = PosBackgroundDark,
    surface = PosSurfaceGlassDark
)

@Composable
fun KasirKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // manfaatkan Material You di Android 12+ (termasuk Android 16)
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
