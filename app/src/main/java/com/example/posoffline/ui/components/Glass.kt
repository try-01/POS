package com.example.posoffline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lightweight glassmorphism primitives.
 *
 * Performance rules:
 *  - The blur effect is implemented by [Modifier.background] with a
 *    translucent gradient. We do NOT use Compose's `blur` modifier on
 *    entire screens (it is GPU-expensive and would hurt list scrolling).
 *  - We keep the corner radius small-ish (12-24dp) so the per-corner
 *    overdraw cost stays bounded.
 *  - `border` is a single 1px stroke; that one extra draw call is fine.
 *
 * The visual "blur" effect on top of the page background gradient is
 * actually cheap — we layer a translucent gradient on top of the body
 * background, which is already a radial-gradient color. This avoids
 * `RenderEffect.createBlurEffect` on Android 12+ (which is the expensive
 * path) for the common case.
 */

@Composable
fun Modifier.glass(
    corner: Dp = 20.dp,
    tint: Color = Color.White.copy(alpha = 0.04f),
    border: Color = Color.White.copy(alpha = 0.10f)
): Modifier = this
    .clip(RoundedCornerShape(corner))
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                tint
            )
        )
    )
    .border(1.dp, border, RoundedCornerShape(corner))

@Composable
fun Modifier.glassStrong(
    corner: Dp = 24.dp
): Modifier = this
    .clip(RoundedCornerShape(corner))
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    )
    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(corner))
