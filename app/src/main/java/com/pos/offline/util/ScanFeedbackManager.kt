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

/**
 * Pilihan tingkat getaran yang ramah pengguna (seperti di pengaturan HP)
 */
enum class VibrationLevel {
    HALUS,
    SEDANG,
    KUAT;

    companion object {
        // Mengonversi String dari SharedPreferences/DataStore menjadi Enum dengan aman
        fun fromString(value: String?): VibrationLevel {
            return when (value?.uppercase()) {
                "HALUS", "LOW", "SOFT" -> HALUS
                "KUAT", "HIGH", "STRONG" -> KUAT
                else -> SEDANG // Default jika data belum diset atau bernilai "SEDANG"
            }
        }
    }
}

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
     * Memicu umpan balik saat scan berhasil
     */
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

    /**
     * Memicu umpan balik saat scan gagal
     */
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

    fun playBeep(volume: Int, durationMs: Int) {
        try {
            toneGenerator?.release()
            val validVol = volume.coerceIn(0, 100)
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, validVol)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, durationMs.coerceIn(50, 300))
        } catch (e: Exception) {
            Log.e(TAG, "Error playBeep: ${e.message}")
        }
    }

    fun playErrorBeep(volume: Int) {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volume.coerceIn(0, 100))
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (e: Exception) {
            Log.e(TAG, "Error playErrorBeep: ${e.message}")
        }
    }

    /**
     * Memicu getaran berdasarkan level (HALUS, SEDANG, KUAT)
     */
    fun playVibration(level: VibrationLevel, durationMs: Int = 50) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            // Menentukan amplitudo (kekuatan) dan durasi berdasarkan Enum level
            val (amplitude, effectiveDuration) = when (level) {
                VibrationLevel.HALUS -> Pair(70, 20L)                          // Getaran lembut & sangat singkat
                VibrationLevel.SEDANG -> Pair(160, durationMs.coerceIn(30, 60).toLong()) // Getaran standar
                VibrationLevel.KUAT -> Pair(VibrationEffect.DEFAULT_AMPLITUDE, durationMs.coerceIn(70, 150).toLong()) // Getaran kuat
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(effectiveDuration, amplitude)

                // Android 13+ (API 33+) - Tetap menggunakan USAGE_ALARM untuk bypass getaran sistem
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val vibrationAttributes = VibrationAttributes.createForUsage(
                        VibrationAttributes.USAGE_ALARM
                    )
                    v.vibrate(effect, vibrationAttributes)
                } else {
                    val audioAttributes = AudioAttributes.Builder()
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
            Log.e(TAG, "Gagal memicu getaran: ${e.message}")
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
