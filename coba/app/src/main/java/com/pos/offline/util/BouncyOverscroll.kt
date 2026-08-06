package com.pos.offline.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

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
        var animJob: Job? = null

        // Fungsi terpusat untuk memantulkan kembali konten ke titik 0f
        fun springBackToZero(initialVelocity: Float = 0f) {
            animJob?.cancel()
            animJob = scope.launch {
                translation.animateTo(
                    targetValue = 0f,
                    initialVelocity = initialVelocity,
                    animationSpec = spring(
                        dampingRatio = 0.55f, // Makin kecil = makin bouncy (0.55f sangat pas untuk iOS feel)
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = translation.value
                val availableDelta = if (orientation == Orientation.Vertical) available.y else available.x

                // 1. Jika sedang dalam posisi overscroll (mentok) dan pengguna menggeser jari balik ke 0
                if (current != 0f && sign(availableDelta) != sign(current)) {
                    animJob?.cancel() // Batalkan animasi mental jika ada sentuhan jari baru
                    
                    val maxConsumed = if (current > 0) {
                        availableDelta.coerceAtLeast(-current)
                    } else {
                        availableDelta.coerceAtMost(-current)
                    }

                    scope.launch {
                        translation.snapTo(translation.value + maxConsumed)
                    }

                    return if (orientation == Orientation.Vertical) {
                        Offset(0f, maxConsumed)
                    } else {
                        Offset(maxConsumed, 0f)
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

                // 2. Tangani tarik paksa melebihi batas (efek kelenturan karet)
                if (availableDelta != 0f && source == NestedScrollSource.UserInput) {
                    animJob?.cancel()
                    val resistance = availableDelta * 0.22f // Tingkat kelenturan karet
                    scope.launch {
                        val target = (translation.value + resistance).coerceIn(-280f, 280f)
                        translation.snapTo(target)
                    }
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val current = translation.value
                if (current != 0f) {
                    val availableVelocity = if (orientation == Orientation.Vertical) available.y else available.x
                    
                    // Kembalikan ke 0f dengan membawa momentum kecepatan jari
                    springBackToZero(initialVelocity = availableVelocity * 0.15f)

                    // KUNCI PERBAIKAN: Kembalikan Velocity.Zero agar tidak "mencuri" momentum 
                    // dan list utama tidak mengunci/stuck.
                    return Velocity.Zero
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val availableVelocity = if (orientation == Orientation.Vertical) available.y else available.x

                // 3. Jika scroll kencang membentur dinding/ujung list
                if (availableVelocity != 0f) {
                    val initialVel = (availableVelocity * 0.2f).coerceIn(-1500f, 1500f)
                    springBackToZero(initialVelocity = initialVel)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    // SAFETY NET: Pemantau otomatis. 
    // Jika karena suatu hal kustom animasi terhenti/nyangkut di posisi non-zero saat idle,
    // LaunchedEffect ini menjamin konten AKAN SELALU memantul kembali ke 0f.
    LaunchedEffect(translation.value) {
        if (translation.value != 0f && !translation.isRunning) {
            translation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer {
            val current = translation.value
            val absCurrent = abs(current)

            // Efek sedikit membesar/mengecil saat ditarik mentok (maksimal 2.5%)
            val scaleFactor = 1f + (absCurrent * 0.0001f).coerceAtMost(0.025f)
            scaleX = scaleFactor
            scaleY = scaleFactor

            if (orientation == Orientation.Vertical) {
                translationY = current
            } else {
                translationX = current
            }
        }
}