package com.pos.offline.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.backup.BackupManager
import com.pos.offline.data.backup.BackupOutcome
import com.pos.offline.data.backup.RestoreOutcome
import com.pos.offline.data.backup.ShareOutcome
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.repository.CashierRepository
import com.pos.offline.data.repository.ShiftRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isSharing: Boolean = false,
    val pendingRestoreUri: Uri? = null,
    val showAddCashierDialog: Boolean = false
) {
    val isBusy: Boolean get() = isExporting || isImporting || isSharing
}

class SettingsViewModel(
    private val cashierRepository: CashierRepository,
    private val shiftRepository: ShiftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val cashiers: StateFlow<List<CashierEntity>> = cashierRepository.allCashiers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    fun exportDatabase(context: Context, destinationUri: Uri) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            when (val result = BackupManager.exportDatabase(context, destinationUri)) {
                is BackupOutcome.Success ->
                    _messages.emit("Cadangan berhasil disimpan.")
                is BackupOutcome.Error ->
                    _messages.emit("Gagal membuat cadangan: ${result.throwable.message}")
            }
            _uiState.value = _uiState.value.copy(isExporting = false)
        }
    }

    fun shareDatabase(context: Context) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true)
            when (val result = BackupManager.prepareShareableCopy(context)) {
                is ShareOutcome.Success -> {
                    val intent = BackupManager.buildShareIntent(context, result.file)
                    context.startActivity(intent)
                }
                is ShareOutcome.Error ->
                    _messages.emit("Gagal menyiapkan cadangan untuk dibagikan: ${result.throwable.message}")
            }
            _uiState.value = _uiState.value.copy(isSharing = false)
        }
    }

    fun requestRestore(uri: Uri) {
        _uiState.value = _uiState.value.copy(pendingRestoreUri = uri)
    }

    fun cancelRestore() {
        _uiState.value = _uiState.value.copy(pendingRestoreUri = null)
    }

    fun confirmRestore(context: Context, onRestartRequired: () -> Unit) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, pendingRestoreUri = null)
            when (val result = BackupManager.validateAndRestore(context, uri)) {
                is RestoreOutcome.Success -> {
                    onRestartRequired()
                }
                is RestoreOutcome.InvalidFile -> {
                    _messages.emit("File tidak valid: ${result.reason}")
                    _uiState.value = _uiState.value.copy(isImporting = false)
                }
                is RestoreOutcome.Error -> {
                    _messages.emit("Gagal memulihkan: ${result.throwable.message}")
                    _uiState.value = _uiState.value.copy(isImporting = false)
                }
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
        viewModelScope.launch {
            cashierRepository.save(CashierEntity(name = trimmed))
            _messages.emit("Kasir \"$trimmed\" ditambahkan.")
        }
        _uiState.value = _uiState.value.copy(showAddCashierDialog = false)
    }

    fun setCashierActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            if (!active && shiftRepository.hasOpenShift(id)) {
                _messages.emit(
                    "Tidak bisa menonaktifkan kasir ini karena masih memiliki " +
                        "shift yang berjalan. Tutup shift-nya terlebih dahulu."
                )
                return@launch
            }
            cashierRepository.setActive(id, active)
        }
    }
}