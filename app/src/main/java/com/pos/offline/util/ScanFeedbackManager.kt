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

    /**
     * Umpan balik saat SCAN BERHASIL (Produk Ditemukan)
     */
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

    /**
     * Umpan balik saat SCAN GAGAL / PRODUK TIDAK DITEMUKAN
     */
    fun triggerFailureFeedback(
        soundEnabled: Boolean,
        soundVolume: Int,
        vibrationEnabled: Boolean,
        vibrationIntensity: Int,
    ) {
        if (soundEnabled && soundVolume > 0) {
            playErrorBeep(soundVolume)
        }
        if (vibrationEnabled) {
            // Getaran Peringatan Beruntun Singkat
            playVibration(vibrationIntensity, durationMs = 120)
        }
    }

    fun playBeep(volume: Int, durationMs: Int) {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volume.coerceIn(0, 100))
            // TONE_PROP_BEEP2 = Nada Sukses Tinggi & Nyaring
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, durationMs.coerceIn(50, 300))
        } catch (e: Exception) {
            Log.e(TAG, "Error playBeep: ${e.message}")
        }
    }

    /**
     * NADA ERROR (Negative Acknowledgment)
     * Mengeluarkan suara khas scanner gagal ("Tet-Tet" / Bip ganda nada rendah)
     */
    fun playErrorBeep(volume: Int) {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volume.coerceIn(0, 100))
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (e: Exception) {
            Log.e(TAG, "Error playErrorBeep: ${e.message}")
        }
    }

    fun playVibration(intensity: Int, durationMs: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            val (amplitude, effectiveDuration) = when {
                intensity <= 35 -> Pair(80, 20L)
                intensity <= 70 -> Pair(170, durationMs.coerceIn(40, 70).toLong())
                else -> Pair(VibrationEffect.DEFAULT_AMPLITUDE, durationMs.coerceIn(90, 200).toLong())
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(effectiveDuration, amplitude)
                v.vibrate(effect, audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effectiveDuration)
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