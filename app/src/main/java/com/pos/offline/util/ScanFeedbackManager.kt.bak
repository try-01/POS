package com.pos.offline.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

enum class VibrationLevel {
    HALUS,
    SEDANG,
    KUAT,
    ;

    companion object {
        fun fromString(value: String?): VibrationLevel =
            when (value?.uppercase()) {
                "HALUS", "LOW", "SOFT" -> HALUS
                "KUAT", "HIGH", "STRONG" -> KUAT
                else -> SEDANG
            }
    }
}

class ScanFeedbackManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val TAG = "ScanFeedbackManager"

    private var toneGenerator: ToneGenerator? =
        try {
            ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            null
        }

    private val vibrator: Vibrator? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }

    fun triggerSuccessFeedback(
        soundEnabled: Boolean,
        soundVolume: Int,
        soundDurationMs: Int,
        vibrationEnabled: Boolean,
        vibrationLevel: VibrationLevel = VibrationLevel.SEDANG,
        vibrationDurationMs: Int = 50,
    ) {
        if (soundEnabled && soundVolume > 0) {
            playBeep(soundVolume, soundDurationMs)
        }
        if (vibrationEnabled) {
            playVibration(vibrationLevel, vibrationDurationMs)
        }
    }

    fun triggerFailureFeedback(
        soundEnabled: Boolean,
        soundVolume: Int,
        vibrationEnabled: Boolean,
        vibrationLevel: VibrationLevel = VibrationLevel.SEDANG,
    ) {
        if (soundEnabled && soundVolume > 0) {
            playErrorBeep(soundVolume)
        }
        if (vibrationEnabled) {
            playVibration(vibrationLevel, durationMs = 120)
        }
    }

    fun playBeep(
        volume: Int,
        durationMs: Int,
    ) {
        try {
            toneGenerator?.release()
            val validVol = volume.coerceIn(0, 100)
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, validVol)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, durationMs.coerceIn(50, 300))
        } catch (e: Exception) {
        }
    }

    fun playErrorBeep(volume: Int) {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volume.coerceIn(0, 100))
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (e: Exception) {
        }
    }

    fun playVibration(
        level: VibrationLevel,
        durationMs: Int = 50,
    ) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            val (amplitude, effectiveDuration) =
                when (level) {
                    VibrationLevel.HALUS -> Pair(70, 20L)
                    VibrationLevel.SEDANG -> Pair(160, durationMs.coerceIn(30, 60).toLong())
                    VibrationLevel.KUAT -> Pair(VibrationEffect.DEFAULT_AMPLITUDE, durationMs.coerceIn(70, 150).toLong())
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(effectiveDuration, amplitude)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val vibrationAttributes =
                        VibrationAttributes.createForUsage(
                            VibrationAttributes.USAGE_ALARM,
                        )
                    v.vibrate(effect, vibrationAttributes)
                } else {
                    val audioAttributes =
                        AudioAttributes
                            .Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    @Suppress("DEPRECATION")
                    v.vibrate(effect, audioAttributes)
                }
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effectiveDuration)
            }
        } catch (e: Exception) {
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
        }
    }
}
