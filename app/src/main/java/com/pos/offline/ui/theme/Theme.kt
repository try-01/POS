package com.pos.offline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------- Palet (tema gelap elegan, kontras tinggi, minim silau untuk kasir) ----------
private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FCF97),
    onPrimary = Color(0xFF003919),
    secondary = Color(0xFF7FD8FF),
    background = Color(0xFF0B1020),
    surface = Color(0xFF121A2E),
    onBackground = Color(0xFFE6ECF5),
    onSurface = Color(0xFFE6ECF5),
    error = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E9E5A),
    onPrimary = Color.White,
    secondary = Color(0xFF1E88E5),
    background = Color(0xFFF2F5FB),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF101826),
    onSurface = Color(0xFF101826),
    error = Color(0xFFD32F2F)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

/**
 * Tema aplikasi. Default mengikuti sistem (gelap/terang).
 * Dynamic color sengaja TIDAK diaktifkan agar warna glassmorphism konsisten di
 * semua perangkat (mengurangi beban pemrosesan warna & menjaga kontras struk).
 */
@Composable
fun PosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
