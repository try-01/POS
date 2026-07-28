package com.pos.offline.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository sederhana & ringan berbasis SharedPreferences untuk menyimpan
 * preferensi kasir terkait suara (Beep) dan getaran (Vibration) scanner.
 */
class ScanPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("scan_preferences", Context.MODE_PRIVATE)

    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, true))
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION_ENABLED, true))
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _isSoundEnabled.value = enabled
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        _isVibrationEnabled.value = enabled
    }

    companion object {
        private const val KEY_SOUND_ENABLED = "key_scan_sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "key_scan_vibration_enabled"
    }
}
