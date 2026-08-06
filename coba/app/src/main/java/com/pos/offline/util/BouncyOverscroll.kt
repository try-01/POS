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
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.runtime.Composable
import io.iamjosephmj.flinger.configs.FlingConfiguration
import io.iamjosephmj.flinger.flings.flingBehavior
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

fun Modifier.bouncyOverscroll(
    orientation: Orientation = Orientation.Vertical
): Modifier = composed {
    val translation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val connection = remember(orientation) {
        var animJob: Job? = null

        fun springBackToZero(initialVelocity: Float = 0f) {
            animJob?.cancel()
            animJob = scope.launch {
                translation.animateTo(
                    targetValue = 0f,
                    initialVelocity = initialVelocity,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = translation.value
                val availableDelta = if (orientation == Orientation.Vertical) available.y else available.x

                // Gunakan threshold > 0.5f agar tidak menginterupsi scroll biasa
                if (abs(current) > 0.5f && sign(availableDelta) != sign(current)) {
                    animJob?.cancel()
                    
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

                if (availableDelta != 0f && source == NestedScrollSource.UserInput) {
                    animJob?.cancel()
                    val resistance = availableDelta * 0.22f
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
                // HANYA interupsi jika BENAR-BENAR sedang dalam posisi overscroll (> 0.5f)
                if (abs(current) > 0.5f) {
                    val availableVelocity = if (orientation == Orientation.Vertical) available.y else available.x
                    springBackToZero(initialVelocity = availableVelocity * 0.15f)
                }
                // Selalu kembalikan Velocity.Zero agar momentum fling list TIDAK TERPOTONG
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val availableVelocity = if (orientation == Orientation.Vertical) available.y else available.x

                if (availableVelocity != 0f) {
                    val initialVel = (availableVelocity * 0.2f).coerceIn(-1500f, 1500f)
                    springBackToZero(initialVelocity = initialVel)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    // Safety net dengan threshold
    LaunchedEffect(translation.value) {
        if (abs(translation.value) > 0.5f && !translation.isRunning) {
            translation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else if (abs(translation.value) <= 0.5f && translation.value != 0f && !translation.isRunning) {
            translation.snapTo(0f)
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer {
            val current = translation.value
            val absCurrent = abs(current)

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
@Composable
fun iosGlideFlingBehavior(): FlingBehavior = flingBehavior(
    scrollConfiguration = FlingConfiguration.Builder()
        .scrollViewFriction(0.012f)     // Angka ideal: luncuran panjang tapi tetap terkontrol
        .decelerationFriction(0.025f)   // Perlambatan halus tanpa remah
        .splineInflection(0.15f)        // Kurva momentum khas iOS
        .numberOfSplinePoints(150)
        .build()
)