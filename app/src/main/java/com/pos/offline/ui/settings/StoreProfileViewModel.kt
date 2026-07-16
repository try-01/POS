package com.pos.offline.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.StoreProfileEntity
import com.pos.offline.data.repository.StoreProfileRepository
import com.pos.offline.util.LogoImageProcessor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Data form edit profil toko. Custom equals()/hashCode() (pola sama seperti
 *  StoreProfileEntity) diperlukan karena data class dengan properti
 *  ByteArray secara default membandingkan REFERENSI, bukan ISI -- Kotlin
 *  compiler bahkan memberi warning eksplisit soal ini. Tanpa perbaikan ini,
 *  StateFlow bisa menganggap dua form state dengan logo identik sebagai
 *  "berbeda" (tidak fatal, hanya recomposition berlebih), tapi lebih benar
 *  untuk diperbaiki dari awal mengikuti pola yang sudah ada di entity. */
data class StoreProfileFormState(
    val storeName: String = "",
    val address: String = "",
    val footerNote: String = "",
    val logoBytes: ByteArray? = null,
    val autoPrintEnabled: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoreProfileFormState) return false
        return storeName == other.storeName &&
            address == other.address &&
            footerNote == other.footerNote &&
            autoPrintEnabled == other.autoPrintEnabled &&
            (logoBytes?.contentEquals(other.logoBytes) ?: (other.logoBytes == null))
    }

    override fun hashCode(): Int {
        var result = storeName.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + footerNote.hashCode()
        result = 31 * result + autoPrintEnabled.hashCode()
        result = 31 * result + (logoBytes?.contentHashCode() ?: 0)
        return result
    }
}

data class StoreProfileUiState(
    val formState: StoreProfileFormState = StoreProfileFormState(),
    val isProcessingLogo: Boolean = false,
    val isSaving: Boolean = false
)

class StoreProfileViewModel(
    private val storeProfileRepository: StoreProfileRepository,
    private val logoImageProcessor: LogoImageProcessor
) : ViewModel() {

    /** Dipakai untuk ringkasan di SettingsScreen (nama toko, status
     *  auto-print, thumbnail logo) DAN sebagai sumber data awal saat form
     *  edit dibuka -- lihat [loadFormFromCurrentProfile]. */
    val profile: StateFlow<StoreProfileEntity> = storeProfileRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoreProfileEntity())

    private val _uiState = MutableStateFlow(StoreProfileUiState())
    val uiState: StateFlow<StoreProfileUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Muat ulang form dari data tersimpan terakhir -- dipanggil setiap
     *  dialog profil toko dibuka, supaya form selalu mencerminkan data
     *  terbaru (bukan bekas edit sesi sebelumnya yang belum disimpan). */
    fun loadFormFromCurrentProfile() {
        val current = profile.value
        _uiState.value = StoreProfileUiState(
            formState = StoreProfileFormState(
                storeName = current.storeName,
                address = current.address,
                footerNote = current.footerNote,
                logoBytes = current.logoBytes,
                autoPrintEnabled = current.autoPrintEnabled
            )
        )
    }

    fun updateStoreName(value: String) = updateForm { it.copy(storeName = value) }
    fun updateAddress(value: String) = updateForm { it.copy(address = value) }
    fun updateFooterNote(value: String) = updateForm { it.copy(footerNote = value) }
    fun updateAutoPrintEnabled(value: Boolean) = updateForm { it.copy(autoPrintEnabled = value) }

    fun pickLogo(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingLogo = true)
            val bytes = logoImageProcessor.process(uri)
            if (bytes == null) {
                emitMessage("Gagal memproses gambar. Coba pilih gambar lain.")
            } else {
                updateForm { it.copy(logoBytes = bytes) }
            }
            _uiState.value = _uiState.value.copy(isProcessingLogo = false)
        }
    }

    fun clearLogo() = updateForm { it.copy(logoBytes = null) }

    private inline fun updateForm(block: (StoreProfileFormState) -> StoreProfileFormState) {
        _uiState.value = _uiState.value.copy(formState = block(_uiState.value.formState))
    }

    fun save() {
        val form = _uiState.value.formState
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            storeProfileRepository.save(
                StoreProfileEntity(
                    storeName = form.storeName.trim(),
                    address = form.address.trim(),
                    footerNote = form.footerNote.trim(),
                    logoBytes = form.logoBytes,
                    autoPrintEnabled = form.autoPrintEnabled
                )
            )
            emitMessage("Profil toko disimpan.")
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch { _messages.emit(message) }
    }
}