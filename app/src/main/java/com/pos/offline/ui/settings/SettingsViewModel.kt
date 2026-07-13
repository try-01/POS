package com.pos.offline.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.backup.BackupManager
import com.pos.offline.data.backup.BackupOutcome
import com.pos.offline.data.backup.RestoreOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    // Uri file hasil ACTION_OPEN_DOCUMENT, menunggu konfirmasi user sebelum
    // benar-benar ditimpakan ke database aktif.
    val pendingRestoreUri: Uri? = null
) {
    val isBusy: Boolean get() = isExporting || isImporting
}

/**
 * Backup/Restore murni memanggil [BackupManager] (object stateless) — jadi
 * ViewModel ini SENGAJA tidak menerima dependency apa pun di constructor.
 * Kelola Kasir (Batch 3B) akan menambah CashierRepository sebagai parameter
 * konstruktor saat itu tiba; factory-nya sudah disiapkan di ServiceLocator
 * agar perubahan nanti tidak mengubah pola pemanggilan di MainActivity.
 *
 * `Context` TIDAK disimpan sebagai field (mencegah leak) — selalu diteruskan
 * per-pemanggilan dari Composable yang mengambilnya via `LocalContext.current`.
 */
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Event sekali-pakai untuk ditampilkan sebagai Toast oleh Screen. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Dipanggil setelah user pilih lokasi simpan lewat ACTION_CREATE_DOCUMENT. */
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

    /** Dipanggil setelah user pilih file lewat ACTION_OPEN_DOCUMENT — belum eksekusi, tunggu konfirmasi. */
    fun requestRestore(uri: Uri) {
        _uiState.value = _uiState.value.copy(pendingRestoreUri = uri)
    }

    fun cancelRestore() {
        _uiState.value = _uiState.value.copy(pendingRestoreUri = null)
    }

    /**
     * Dipanggil setelah user menekan "Ya, Timpa & Pulihkan" di dialog konfirmasi.
     * [onRestartRequired] HANYA dipanggil kalau restore benar-benar sukses —
     * eksekusi `BackupManager.restartApp()` sengaja dilempar balik ke Screen
     * karena butuh Activity/Application context yang masih hidup saat itu.
     */
    fun confirmRestore(context: Context, onRestartRequired: () -> Unit) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, pendingRestoreUri = null)
            when (val result = BackupManager.validateAndRestore(context, uri)) {
                is RestoreOutcome.Success -> {
                    // Tidak perlu set isImporting = false: proses aplikasi
                    // akan segera restart, state ini tidak relevan lagi.
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
}