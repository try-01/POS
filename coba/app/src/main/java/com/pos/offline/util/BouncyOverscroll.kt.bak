package com.pos.offline.util // Sesuaikan dengan nama package utilitas Anda

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

/**
 * Modifier kustom untuk memberikan efek pantulan ujung (rubber-band) gaya iOS.
 * Dipadukan dengan animasi membesar/mengecil (scale).
 */
fun Modifier.bouncyOverscroll(
    orientation: Orientation = Orientation.Vertical
): Modifier = composed {
    val translation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val connection = remember(orientation) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = translation.value
                val availableDelta = if (orientation == Orientation.Vertical) available.y else available.x
                
                // 1. Jika sedang memantul dan pengguna menggeser jari ke arah berlawanan (menuju 0)
                if (current != 0f) {
                    val sign = kotlin.math.sign(current)
                    if (kotlin.math.sign(availableDelta) != sign) {
                        // Kunci Perbaikan Bug: Gunakan proporsi 1:1 (tanpa dikali fraksi) 
                        // agar layar mengikuti jari secara akurat dan tidak terlepas.
                        val maxConsumed = if (current > 0) {
                            availableDelta.coerceAtLeast(-current)
                        } else {
                            availableDelta.coerceAtMost(-current)
                        }
                        
                        scope.launch {
                            translation.snapTo(translation.value + maxConsumed)
                        }
                        return if (orientation == Orientation.Vertical) Offset(0f, maxConsumed) else Offset(maxConsumed, 0f)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val availableDelta = if (orientation == Orientation.Vertical) available.y else available.x
                
                // 2. Tangani saat ditarik paksa melebihi batas (menciptakan efek kelenturan)
                if (availableDelta != 0f) {
                    val resistance = availableDelta * 0.25f // Tingkat kelenturan karet (0.25 = berat)
                    scope.launch {
                        // Batas absolut 350f menjamin konten TIDAK AKAN PERNAH bablas hilang dari layar
                        val target = (translation.value + resistance).coerceIn(-350f, 350f)
                        translation.snapTo(target)
                    }
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // 3. Saat jari dilepas, lepaskan karet agar memantul kembali ke posisi 0
                if (translation.value != 0f) {
                    scope.launch {
                        translation.animateTo(
                            targetValue = 0f, 
                            animationSpec = spring(
                                dampingRatio = 0.5f, // Makin kecil = makin bouncy
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    // Beri tahu sistem bahwa kita "menelan" sisa momentum, agar list tidak lanjut scroll sendiri
                    return available 
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val availableVelocity = if (orientation == Orientation.Vertical) available.y else available.x
                
                // 4. Jika list digulir kencang (fling) dan menabrak batas layar secara keras
                if (availableVelocity != 0f) {
                    scope.launch {
                        // Buat hentakan (membentur ujung)
                        val bounceImpact = (availableVelocity * 0.04f).coerceIn(-120f, 120f)
                        translation.animateTo(
                            targetValue = bounceImpact,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy, 
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                        // Pantulkan kembali ke titik awal (0)
                        translation.animateTo(
                            targetValue = 0f, 
                            animationSpec = spring(
                                dampingRatio = 0.5f, 
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer {
            val current = translation.value
            val absCurrent = kotlin.math.abs(current)
            
            // Animasi membesar dan mengecil (Maksimal membesar 3% dari ukuran asli)
            val scaleFactor = 1f + (absCurrent * 0.00015f).coerceAtMost(0.03f)
            scaleX = scaleFactor
            scaleY = scaleFactor

            // Animasi translasi (geser mengikuti jari/pantulan)
            if (orientation == Orientation.Vertical) {
                translationY = current
            } else {
                translationX = current
            }
        }
}
