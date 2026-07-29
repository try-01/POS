package com.pos.offline.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class ScanFeedbackManager(context: Context) {
    private val appContext = context.applicationContext
    private val TAG = "ScanFeedbackManager"

    // Menggunakan STREAM_ALARM agar gain suara dari speaker HP keluar secara maksimal
    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_ALARM, 100)
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
        if (vibrationEnabled) {
            playVibration(vibrationIntensity, vibrationDurationMs)
        }
    }

    fun playBeep(volume: Int, durationMs: Int) {
        try {
            toneGenerator?.release()
            // Gunakan STREAM_ALARM agar suara tetap keras meskipun volume media HP pelan
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volume.coerceIn(0, 100))
            
            // TONE_PROP_BEEP2 adalah nada high-frequency (tajam & nyaring)
            // yang dirancang khusus untuk menembus kebisingan latar belakang (road noise)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, durationMs.coerceIn(50, 300))
        } catch (e: Exception) {
            Log.e(TAG, "Error playBeep: ${e.message}")
        }
    }

    fun playVibration(intensity: Int, durationMs: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            val duration = durationMs.coerceIn(30, 300).toLong()

            val amplitude = when {
                intensity <= 35 -> 110
                intensity <= 70 -> 180
                else -> VibrationEffect.DEFAULT_AMPLITUDE
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(duration, amplitude)
                v.vibrate(effect, audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memicu getaran: ${e.message}")
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