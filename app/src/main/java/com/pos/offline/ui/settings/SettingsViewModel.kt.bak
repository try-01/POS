package com.pos.offline.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.backup.BackupManager
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.repository.CashierRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SettingsUiEvent {
    data class ShowToast(val message: String) : SettingsUiEvent
    data object RestartApp : SettingsUiEvent
}

class SettingsViewModel(
    private val cashierRepository: CashierRepository
) : ViewModel() {

    val cashiers: StateFlow<List<CashierEntity>> = cashierRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvents = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 4)
    val uiEvents: SharedFlow<SettingsUiEvent> = _uiEvents.asSharedFlow()

    fun addCashier(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        // Asumsi CashierEntity punya default value id=0, pinHash=null, active=true
        val cashier = CashierEntity(name = name.trim(), pinHash = null, active = true)
        cashierRepository.insert(cashier)
        _uiEvents.emit(SettingsUiEvent.ShowToast("Kasir '${name.trim()}' ditambahkan"))
    }

    fun toggleCashierActive(cashier: CashierEntity) = viewModelScope.launch {
        cashierRepository.setActive(cashier.id, !cashier.active)
    }

    fun exportDatabase(context: Context, uri: Uri) = viewModelScope.launch {
        try {
            // Pindahkan ke IO agar file stream tidak blok Main Thread
            withContext(Dispatchers.IO) {
                BackupManager.exportDatabase(context, uri)
            }
            _uiEvents.emit(SettingsUiEvent.ShowToast("Backup berhasil disimpan"))
        } catch (e: Exception) {
            _uiEvents.emit(SettingsUiEvent.ShowToast("Gagal backup: ${e.message}"))
        }
    }

    fun importDatabase(context: Context, uri: Uri) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                BackupManager.importDatabase(context, uri)
            }
            _uiEvents.emit(SettingsUiEvent.ShowToast("Data berhasil dipulihkan"))
            _uiEvents.emit(SettingsUiEvent.RestartApp)
        } catch (e: Exception) {
            _uiEvents.emit(SettingsUiEvent.ShowToast("Gagal pulihkan: ${e.message}"))
        }
    }
}