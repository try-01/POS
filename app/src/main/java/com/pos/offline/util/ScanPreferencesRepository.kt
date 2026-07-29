package com.pos.offline.util
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class ScanPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("scan_preferences", Context.MODE_PRIVATE)
    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, false))
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()
    private val _isVibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION_ENABLED, false))
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()
    private val _soundVolume = MutableStateFlow(prefs.getInt(KEY_SOUND_VOLUME, 65))
    val soundVolume: StateFlow<Int> = _soundVolume.asStateFlow()
    private val _soundDurationMs = MutableStateFlow(prefs.getInt(KEY_SOUND_DURATION, 80))
    val soundDurationMs: StateFlow<Int> = _soundDurationMs.asStateFlow()
    private val _vibrationIntensity = MutableStateFlow(prefs.getInt(KEY_VIBRATION_INTENSITY, 45))
    val vibrationIntensity: StateFlow<Int> = _vibrationIntensity.asStateFlow()
    private val _vibrationDurationMs = MutableStateFlow(prefs.getInt(KEY_VIBRATION_DURATION, 35))
    val vibrationDurationMs: StateFlow<Int> = _vibrationDurationMs.asStateFlow()
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _isSoundEnabled.value = enabled
    }
    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        _isVibrationEnabled.value = enabled
    }
    fun setSoundVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        prefs.edit().putInt(KEY_SOUND_VOLUME, clamped).apply()
        _soundVolume.value = clamped
    }
    fun setSoundDurationMs(duration: Int) {
        val clamped = duration.coerceIn(50, 300)
        prefs.edit().putInt(KEY_SOUND_DURATION, clamped).apply()
        _soundDurationMs.value = clamped
    }
    fun setVibrationIntensity(intensity: Int) {
        val clamped = intensity.coerceIn(0, 100)
        prefs.edit().putInt(KEY_VIBRATION_INTENSITY, clamped).apply()
        _vibrationIntensity.value = clamped
    }
    fun setVibrationDurationMs(duration: Int) {
        val clamped = duration.coerceIn(20, 200)
        prefs.edit().putInt(KEY_VIBRATION_DURATION, clamped).apply()
        _vibrationDurationMs.value = clamped
    }
    companion object {
        private const val KEY_SOUND_ENABLED = "key_scan_sound_enabled"
        private const val KEY_SOUND_VOLUME = "key_scan_sound_volume"
        private const val KEY_SOUND_DURATION = "key_scan_sound_duration"
        private const val KEY_VIBRATION_ENABLED = "key_scan_vibration_enabled"
        private const val KEY_VIBRATION_INTENSITY = "key_scan_vibration_intensity"
        private const val KEY_VIBRATION_DURATION = "key_scan_vibration_duration"
    }
}
