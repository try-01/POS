package com.pos.offline.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.backup.BackupManager
import com.pos.offline.data.backup.BackupOutcome
import com.pos.offline.data.backup.RestoreGuard
import com.pos.offline.data.backup.RestoreOutcome
import com.pos.offline.data.backup.ShareOutcome
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.repository.CashierRepository
import com.pos.offline.data.repository.ShiftRepository
import com.pos.offline.util.ScanFeedbackManager
import com.pos.offline.util.ScanPreferencesRepository
import com.pos.offline.util.VibrationLevel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isSharing: Boolean = false,
    val isAddingCashier: Boolean = false,
    val pendingRestoreUri: Uri? = null,
    val showAddCashierDialog: Boolean = false,
) {
    val isBusy: Boolean get() = isExporting || isImporting || isSharing
}

class SettingsViewModel(
    private val appContext: Context,
    private val cashierRepository: CashierRepository,
    private val shiftRepository: ShiftRepository,
    private val scanPreferencesRepository: ScanPreferencesRepository = ScanPreferencesRepository(appContext),
) : ViewModel() {
    private val feedbackManager = ScanFeedbackManager(appContext)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private val _shareIntent = MutableSharedFlow<android.content.Intent>(extraBufferCapacity = 1)
    val shareIntent: SharedFlow<android.content.Intent> = _shareIntent.asSharedFlow()

    val cashiers: StateFlow<List<CashierEntity>> =
        cashierRepository.allCashiers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isSoundEnabled: StateFlow<Boolean> = scanPreferencesRepository.isSoundEnabled
    val soundVolume: StateFlow<Int> = scanPreferencesRepository.soundVolume
    val soundDurationMs: StateFlow<Int> = scanPreferencesRepository.soundDurationMs

    val isVibrationEnabled: StateFlow<Boolean> = scanPreferencesRepository.isVibrationEnabled

    val vibrationLevel: StateFlow<VibrationLevel> =
        scanPreferencesRepository.vibrationIntensity
            .map { intensity ->
                when {
                    intensity <= 35 -> VibrationLevel.HALUS
                    intensity <= 70 -> VibrationLevel.SEDANG
                    else -> VibrationLevel.KUAT
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VibrationLevel.SEDANG)

    val vibrationDurationMs: StateFlow<Int> = scanPreferencesRepository.vibrationDurationMs

    fun setSoundEnabled(enabled: Boolean) {
        scanPreferencesRepository.setSoundEnabled(enabled)
        if (enabled) testSoundPreview()
    }

    fun setSoundVolume(volume: Int) {
        scanPreferencesRepository.setSoundVolume(volume)
    }

    fun setSoundDurationMs(duration: Int) {
        scanPreferencesRepository.setSoundDurationMs(duration)
    }

    fun testSoundPreview() {
        feedbackManager.playBeep(soundVolume.value, soundDurationMs.value)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        scanPreferencesRepository.setVibrationEnabled(enabled)
        if (enabled) testVibrationPreview()
    }

    fun setVibrationLevel(level: VibrationLevel) {
        val intensityValue =
            when (level) {
                VibrationLevel.HALUS -> 30
                VibrationLevel.SEDANG -> 65
                VibrationLevel.KUAT -> 100
            }
        scanPreferencesRepository.setVibrationIntensity(intensityValue)
    }

    fun setVibrationDurationMs(duration: Int) {
        scanPreferencesRepository.setVibrationDurationMs(duration)
    }

    fun testVibrationPreview() {
        feedbackManager.playVibration(vibrationLevel.value, vibrationDurationMs.value)
    }

    override fun onCleared() {
        super.onCleared()
        feedbackManager.release()
    }

    fun exportDatabase(destinationUri: Uri) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                when (val result = BackupManager.exportDatabase(appContext, destinationUri)) {
                    is BackupOutcome.Success -> _messages.emit("Cadangan berhasil disimpan.")
                    is BackupOutcome.Error -> _messages.emit("Gagal membuat cadangan: ${result.throwable.message}")
                }
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }

    fun shareDatabase() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true)
            try {
                when (val result = BackupManager.prepareShareableCopy(appContext)) {
                    is ShareOutcome.Success -> {
                        _shareIntent.emit(BackupManager.buildShareIntent(appContext, result.file))
                    }

                    is ShareOutcome.Error -> {
                        _messages.emit("Gagal menyiapkan cadangan untuk dibagikan: ${result.throwable.message}")
                    }
                }
            } finally {
                _uiState.value = _uiState.value.copy(isSharing = false)
            }
        }
    }

    fun requestRestore(uri: Uri) {
        _uiState.value = _uiState.value.copy(pendingRestoreUri = uri)
    }

    fun cancelRestore() {
        _uiState.value = _uiState.value.copy(pendingRestoreUri = null)
    }

    fun confirmRestore(onRestartRequired: () -> Unit) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, pendingRestoreUri = null)
            RestoreGuard.begin()
            try {
                when (val result = BackupManager.validateAndRestore(appContext, uri)) {
                    is RestoreOutcome.Success -> {
                        onRestartRequired()
                    }

                    is RestoreOutcome.InvalidFile -> {
                        RestoreGuard.end()
                        _messages.emit("File tidak valid: ${result.reason}")
                        _uiState.value = _uiState.value.copy(isImporting = false)
                    }

                    is RestoreOutcome.Error -> {
                        _messages.emit("Gagal memulihkan: ${result.throwable.message}")
                        if (result.requiresRestart) {
                            onRestartRequired()
                        } else {
                            RestoreGuard.end()
                            _uiState.value = _uiState.value.copy(isImporting = false)
                        }
                    }
                }
            } catch (e: Exception) {
                _messages.emit("Gagal memulihkan: ${e.message ?: "kesalahan tak dikenal"}")
                onRestartRequired()
            }
        }
    }

    fun openAddCashierDialog() {
        _uiState.value = _uiState.value.copy(showAddCashierDialog = true)
    }

    fun closeAddCashierDialog() {
        _uiState.value = _uiState.value.copy(showAddCashierDialog = false)
    }

    fun addCashier(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _messages.emit("Nama kasir tidak boleh kosong.") }
            return
        }
        if (_uiState.value.isAddingCashier) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingCashier = true)
            try {
                cashierRepository.save(CashierEntity(name = trimmed))
                _messages.emit("Kasir \"$trimmed\" ditambahkan.")
                _uiState.value = _uiState.value.copy(showAddCashierDialog = false)
            } catch (e: Exception) {
                _messages.emit("Gagal menambahkan kasir: ${e.message ?: "kesalahan tak dikenal"}")
            } finally {
                _uiState.value = _uiState.value.copy(isAddingCashier = false)
            }
        }
    }

    fun setCashierActive(
        id: Long,
        active: Boolean,
    ) {
        viewModelScope.launch {
            if (!active && shiftRepository.hasOpenShift(id)) {
                _messages.emit(
                    "Tidak bisa menonaktifkan kasir ini karena masih memiliki " +
                        "shift yang berjalan. Tutup shift-nya terlebih dahulu.",
                )
                return@launch
            }
            cashierRepository.setActive(id, active)
        }
    }
}
