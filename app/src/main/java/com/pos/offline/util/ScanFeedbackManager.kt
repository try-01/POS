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

/**
 * Manager umpan balik suara & getaran dengan dukungan pengaturan volume,
 * intensitas getar dinamis (1-255 amplitude), dan kontrol durasi.
 * Kompatibel dari Android 8 (API 26) hingga Android 16 (API 36+).
 */
class ScanFeedbackManager(context: Context) {
    private val appContext = context.applicationContext
    private val TAG = "ScanFeedbackManager"

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    } catch (e: Exception) {
        Log.e(TAG, "Gagal inisialisasi ToneGenerator: ${e.message}")
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
        Log.e(TAG, "Gagal mendapatkan Vibrator Service: ${e.message}")
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
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, volume.coerceIn(0, 100))
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs.coerceIn(50, 300))
        } catch (e: Exception) {
            Log.e(TAG, "Error playBeep: ${e.message}")
        }
    }

    fun playVibration(intensity: Int, durationMs: Int) {
        val v = vibrator ?: run {
            Log.w(TAG, "Vibrator bernilai null")
            return
        }

        if (!v.hasVibrator()) {
            Log.w(TAG, "Perangkat tidak memiliki hardware getar")
            return
        }

        try {
            val mappedIntensity = intensity.coerceIn(1, 100)
            // Pastikan durasi minimal 30ms agar motor getar HP sempat berputar
            val duration = durationMs.coerceIn(30, 300).toLong()

            // DENGAN AudioAttributes: Memaksa OS Android 8-16 mengeksekusi getaran 
            // sebagai feedback pemindaian/bantuan aplikasi (sonification)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Hitung amplitudo (1..255)
                val amplitude = if (mappedIntensity >= 90) {
                    VibrationEffect.DEFAULT_AMPLITUDE
                } else {
                    // Petakan 1..100% ke amplitudo fisik 100..255
                    (100 + (mappedIntensity / 100f) * 155).toInt().coerceIn(1, 255)
                }

                val effect = VibrationEffect.createOneShot(duration, amplitude)
                v.vibrate(effect, audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
            Log.d(TAG, "Getaran berhasil dipicu ($duration ms, intensity: $mappedIntensity%)")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memicu getaran: ${e.message}", e)
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