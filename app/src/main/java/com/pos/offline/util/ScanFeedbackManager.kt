package com.pos.offline.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manager umpan balik suara & getaran dengan dukungan pengaturan volume,
 * intensitas getar dinamis (1-255 amplitude), dan kontrol durasi.
 */
class ScanFeedbackManager(context: Context) {
    private val appContext = context.applicationContext

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    } catch (e: Exception) {
        null
    }

    private val vibrator: Vibrator? = try {
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
        vibrationIntensity: Int,
        vibrationDurationMs: Int,
    ) {
        if (soundEnabled && soundVolume > 0) {
            playBeep(soundVolume, soundDurationMs)
        }
        if (vibrationEnabled && vibrationIntensity > 0) {
            playVibration(vibrationIntensity, vibrationDurationMs)
        }
    }

    fun playBeep(volume: Int, durationMs: Int) {
        try {
            // Re-instantiate tone generator sesuai skala volume 0-100%
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, volume.coerceIn(0, 100))
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs.coerceIn(50, 300))
        } catch (e: Exception) {
            // Ignore safely
        }
    }

    fun playVibration(intensity: Int, durationMs: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            val duration = durationMs.coerceIn(20, 200).toLong()
            // Konversi 1-100% ke skala amplitudo Android (1 - 255)
            val amplitude = ((intensity.coerceIn(1, 100) / 100f) * 255).toInt().coerceIn(1, 255)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        } catch (e: Exception) {
            // Ignore safely
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
