package com.pos.offline.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Kartu "glassmorphism" RINGAN & performant.
 *
 * Implementasi ini menyimulasikan kaca dengan:
 *   - background gradient setengah-transparan, dan
 *   - garis tepi (border) putih samar.
 * Kedua operasi murah di GPU dan tidak dievaluasi ulang per-frame saat scroll.
 *
 * CATATAN PERFORMA: menghindari `Modifier.blur` / `RenderEffect.createBlurEffect`
 * untuk backdrop blur. Blur real-time mahal di GPU dan membebani rendering daftar
 * yang scroll cepat (janky). Untuk efek frosted sejati, aktifkan blur HANYA pada
 * satu elemen statis (mis. panel bayangan di belakang sheet) — bukan di item list.
 *
 * @param contentPadding padding internal default.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tint = if (isDark) Color.White else Color.Black
    Box(
        modifier = modifier
            .clipShape(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.16f),
                        tint.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, tint.copy(alpha = 0.22f)),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

/** Konstanta pembantu luminance sederhana. */
private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

/** Ekstensi privat supaya nama API tetap bersih (mengganti import clip eksplisit). */
private fun Modifier.clipShape(shape: androidx.compose.ui.graphics.Shape) =
    this.then(androidx.compose.ui.draw.clip(shape))
