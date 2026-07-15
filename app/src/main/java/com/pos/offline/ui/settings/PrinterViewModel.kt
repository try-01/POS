package com.pos.offline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.PaperWidth
import com.pos.offline.data.local.entity.PrinterConnectionType
import com.pos.offline.data.local.entity.PrinterEntity
import com.pos.offline.data.repository.PrinterRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Data form tambah/edit printer. Batch H3a hanya mendukung WIFI secara
 *  fungsional -- BLUETOOTH/USB ditunda ke H3b/H3c (lihat validasi di saveForm). */
data class PrinterFormState(
    val id: Long? = null, // null = printer baru
    val label: String = "",
    val connectionType: PrinterConnectionType = PrinterConnectionType.WIFI,
    val paperWidth: PaperWidth = PaperWidth.MM_80,
    val charPerLine: String = PaperWidth.MM_80.defaultCharPerLine().toString(),
    val wifiIpAddress: String = "",
    val wifiPort: String = "9100" // default umum port raw ESC/POS jaringan
)

data class PrinterUiState(
    val showFormDialog: Boolean = false,
    val formState: PrinterFormState = PrinterFormState(),
    val isSaving: Boolean = false,
    val pendingDeleteId: Long? = null
)

class PrinterViewModel(
    private val printerRepository: PrinterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val printers: StateFlow<List<PrinterEntity>> = printerRepository.allPrinters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Dialog form ----

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(
            showFormDialog = true,
            formState = PrinterFormState()
        )
    }

    fun openEditDialog(printer: PrinterEntity) {
        _uiState.value = _uiState.value.copy(
            showFormDialog = true,
            formState = PrinterFormState(
                id = printer.id,
                label = printer.label,
                connectionType = printer.connectionType,
                paperWidth = printer.paperWidth,
                charPerLine = printer.charPerLine.toString(),
                wifiIpAddress = printer.wifiIpAddress ?: "",
                wifiPort = printer.wifiPort?.toString() ?: "9100"
            )
        )
    }

    fun closeFormDialog() {
        _uiState.value = _uiState.value.copy(showFormDialog = false)
    }

    fun updateFormLabel(value: String) = updateForm { it.copy(label = value) }

    fun updateFormConnectionType(type: PrinterConnectionType) =
        updateForm { it.copy(connectionType = type) }

    /** Ganti lebar kertas otomatis mengganti nilai default charPerLine --
     *  user tetap bisa override manual sesudahnya lewat updateFormCharPerLine. */
    fun updateFormPaperWidth(width: PaperWidth) =
        updateForm { it.copy(paperWidth = width, charPerLine = width.defaultCharPerLine().toString()) }

    fun updateFormCharPerLine(value: String) =
        updateForm { it.copy(charPerLine = value.filter { c -> c.isDigit() }) }

    fun updateFormWifiIp(value: String) = updateForm { it.copy(wifiIpAddress = value) }

    fun updateFormWifiPort(value: String) =
        updateForm { it.copy(wifiPort = value.filter { c -> c.isDigit() }) }

    private inline fun updateForm(block: (PrinterFormState) -> PrinterFormState) {
        _uiState.value = _uiState.value.copy(formState = block(_uiState.value.formState))
    }

    fun saveForm() {
        val form = _uiState.value.formState

        if (form.connectionType != PrinterConnectionType.WIFI) {
            emitMessage("Jenis koneksi ini belum tersedia pada pembaruan ini.")
            return
        }
        val label = form.label.trim()
        if (label.isEmpty()) {
            emitMessage("Nama printer tidak boleh kosong.")
            return
        }
        val ip = form.wifiIpAddress.trim()
        if (ip.isEmpty() || !isValidIpv4(ip)) {
            emitMessage("Alamat IP tidak valid. Contoh format: 192.168.1.50")
            return
        }
        val port = form.wifiPort.toIntOrNull()
        if (port == null || port !in 1..65535) {
            emitMessage("Port tidak valid (harus angka 1-65535).")
            return
        }
        val charPerLine = form.charPerLine.toIntOrNull()
        if (charPerLine == null || charPerLine <= 0) {
            emitMessage("Karakter per baris tidak valid.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val existingId = form.id
            if (existingId == null) {
                val existing = printerRepository.getAllOrderedByPriority()
                val nextPriority = (existing.maxOfOrNull { it.priority } ?: -1) + 1
                val entity = PrinterEntity(
                    label = label,
                    connectionType = form.connectionType,
                    isDefault = existing.isEmpty(), // printer pertama otomatis jadi default
                    priority = nextPriority,
                    charPerLine = charPerLine,
                    paperWidth = form.paperWidth,
                    wifiIpAddress = ip,
                    wifiPort = port
                )
                printerRepository.add(entity)
                emitMessage("Printer \"$label\" ditambahkan.")
            } else {
                val current = printerRepository.getById(existingId)
                if (current == null) {
                    emitMessage("Printer tidak ditemukan (mungkin sudah dihapus).")
                } else {
                    val updated = current.copy(
                        label = label,
                        paperWidth = form.paperWidth,
                        charPerLine = charPerLine,
                        wifiIpAddress = ip,
                        wifiPort = port
                    )
                    printerRepository.update(updated)
                    emitMessage("Printer \"$label\" diperbarui.")
                }
            }

            _uiState.value = _uiState.value.copy(isSaving = false, showFormDialog = false)
        }
    }

    // ---- Set default & reorder priority ----

    fun setAsDefault(printer: PrinterEntity) {
        viewModelScope.launch {
            printerRepository.setAsDefault(printer)
            emitMessage("\"${printer.label}\" dijadikan printer utama.")
        }
    }

    fun movePriorityUp(printer: PrinterEntity) = swapPriority(printer, moveUp = true)
    fun movePriorityDown(printer: PrinterEntity) = swapPriority(printer, moveUp = false)

    private fun swapPriority(printer: PrinterEntity, moveUp: Boolean) {
        viewModelScope.launch {
            val all = printerRepository.getAllOrderedByPriority()
            val index = all.indexOfFirst { it.id == printer.id }
            if (index == -1) return@launch
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex !in all.indices) return@launch

            val a = all[index]
            val b = all[targetIndex]
            printerRepository.update(a.copy(priority = b.priority))
            printerRepository.update(b.copy(priority = a.priority))
        }
    }

    // ---- Hapus (hard delete) ----

    fun requestDelete(id: Long) {
        _uiState.value = _uiState.value.copy(pendingDeleteId = id)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(pendingDeleteId = null)
    }

    fun confirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        viewModelScope.launch {
            val printer = printerRepository.getById(id)
            if (printer != null) {
                printerRepository.delete(printer)
                emitMessage("Printer \"${printer.label}\" dihapus.")

                // Jaga invarian: kalau yang dihapus adalah default dan masih
                // ada printer lain tersisa, promosikan otomatis printer dgn
                // priority terkecil sebagai default baru.
                if (printer.isDefault) {
                    val remaining = printerRepository.getAllOrderedByPriority()
                    remaining.firstOrNull()?.let { printerRepository.setAsDefault(it) }
                }
            }
            _uiState.value = _uiState.value.copy(pendingDeleteId = null)
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch { _messages.emit(message) }
    }

    companion object {
        private val IPV4_REGEX = Regex(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )

        private fun isValidIpv4(ip: String): Boolean = IPV4_REGEX.matches(ip)
    }
}