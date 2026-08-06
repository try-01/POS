package com.pos.offline.util // Sesuaikan dengan package Anda

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
 * Harus digunakan bersamaan dengan [LocalOverscrollConfiguration provides null].
 */
fun Modifier.bouncyOverscroll(
    orientation: Orientation = Orientation.Vertical
): Modifier = composed {
    val translation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val connection = remember(orientation) {
        object : NestedScrollConnection {
            
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val availableDelta = if (orientation == Orientation.Vertical) available.y else available.x
                // Jika sedang memantul (karet ditarik), gunakan scroll untuk melawan pantulan
                if (translation.value != 0f) {
                    val consumed = if (translation.value > 0) {
                        availableDelta.coerceAtMost(0f)
                    } else {
                        availableDelta.coerceAtLeast(0f)
                    }
                    scope.launch { 
                        translation.snapTo(translation.value + consumed * 0.3f) 
                    }
                    return if (orientation == Orientation.Vertical) Offset(0f, consumed) else Offset(consumed, 0f)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val availableDelta = if (orientation == Orientation.Vertical) available.y else available.x
                // Jika ada sisa scroll (menabrak ujung), ubah menjadi tarikan (translasi)
                if (availableDelta != 0f) {
                    scope.launch { 
                        translation.snapTo(translation.value + availableDelta * 0.2f) 
                    }
                    return if (orientation == Orientation.Vertical) Offset(0f, availableDelta) else Offset(availableDelta, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Saat jari dilepas, kembalikan posisi pantulan ke 0 menggunakan efek pegas
                if (translation.value != 0f) {
                    scope.launch { 
                        translation.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) 
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val availableVelocity = if (orientation == Orientation.Vertical) available.y else available.x
                // Jika masih ada sisa kecepatan Flinger saat menabrak ujung, buat pantulan benturan
                if (availableVelocity != 0f) {
                    scope.launch {
                        translation.animateTo(
                            targetValue = availableVelocity * 0.04f, 
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                        translation.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                    return if (orientation == Orientation.Vertical) Velocity(x = 0f, y = availableVelocity) else Velocity(x = availableVelocity, y = 0f)
                }
                return Velocity.Zero
            }
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer {
            if (orientation == Orientation.Vertical) {
                translationY = translation.value
            } else {
                translationX = translation.value
            }
        }
}
