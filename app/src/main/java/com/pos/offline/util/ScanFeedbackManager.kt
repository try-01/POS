package com.pos.offline.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manager khusus untuk menangani respons audio (Beep) dan haptic (Getaran)
 * saat pemindaian barcode berhasil.
 *
 * Menggunakan applicationContext untuk mencegah memory leak, serta mendukung
 * kustomisasi suara halus dan getaran haptic modern ala enterprise app.
 */
class ScanFeedbackManager(context: Context) {
    private val appContext = context.applicationContext

    // Menggunakan ToneGenerator standar Android (suara pip/beep halus)
    private var toneGenerator: ToneGenerator? = try {
        // Volume diatur 65% agar nyaman di telinga
        ToneGenerator(AudioManager.STREAM_MUSIC, 65)
    } catch (e: Exception) {
        null
    }

    // Mengambil service Vibrator sesuai versi API Android
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
     * Memicu umpan balik suara & getaran jika diizinkan oleh preferensi kasir.
     */
    fun triggerSuccessFeedback(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (soundEnabled) {
            playPleasantBeep()
        }
        if (vibrationEnabled) {
            playSmoothVibration()
        }
    }

    private fun playPleasantBeep() {
        try {
            // TONE_PROP_BEEP memberikan suara "pip" singkat (~80ms)
            // yang halus, modern, dan enak didengar
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (e: Exception) {
            // Mengabaikan error audio secara aman tanpa membuat app crash
        }
    }

    private fun playSmoothVibration() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: Menggunakan haptic effect 'CLICK' bawaan sistem (seperti ketukan keyboard)
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8-9: Getaran singkat 30 milidetik dengan amplitudo rendah (gentle tap)
                v.vibrate(VibrationEffect.createOneShot(30, 70))
            } else {
                // Android lama: Getaran singkat 30 milidetik
                @Suppress("DEPRECATION")
                v.vibrate(30)
            }
        } catch (e: Exception) {
            // Mengabaikan error haptic
        }
    }

    /**
     * Membebaskan resource audio saat tidak lagi dibutuhkan.
     */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
