package com.pos.offline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.PaperWidth
import com.pos.offline.data.local.entity.PrinterConnectionType
import com.pos.offline.data.local.entity.PrinterEntity
import com.pos.offline.data.repository.PrinterRepository
import com.pos.offline.util.BluetoothDeviceInfo
import com.pos.offline.util.BluetoothPrinterHelper
import com.pos.offline.util.BondResult
import com.pos.offline.util.PrinterConnectionFactory
import com.pos.offline.util.TestPrintResult
import com.pos.offline.util.UsbDeviceInfo
import com.pos.offline.util.UsbPermissionResult
import com.pos.offline.util.UsbPrinterHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Data form tambah/edit printer. */
data class PrinterFormState(
    val id: Long? = null, // null = printer baru
    val label: String = "",
    val connectionType: PrinterConnectionType = PrinterConnectionType.WIFI,
    val paperWidth: PaperWidth = PaperWidth.MM_80,
    val charPerLine: String = PaperWidth.MM_80.defaultCharPerLine().toString(),
    val wifiIpAddress: String = "",
    val wifiPort: String = "9100", // default umum port raw ESC/POS jaringan
    val bluetoothMacAddress: String = "",
    val usbVendorId: Int? = null,
    val usbProductId: Int? = null,
    val supportsStatusQuery: Boolean = false
)

data class PrinterUiState(
    val showFormDialog: Boolean = false,
    val formState: PrinterFormState = PrinterFormState(),
    val isSaving: Boolean = false,
    val pendingDeleteId: Long? = null,
    /** Guard anti-dobel-klik Test Print PER PRINTER (Batch H3d) -- pakai
     *  Set<Long>, BUKAN flag boolean global, supaya user tetap bisa test
     *  print printer B selagi printer A masih diproses, tapi tetap dicegah
     *  klik ganda pada printer YANG SAMA. */
    val testingPrinterIds: Set<Long> = emptySet()
)

/** State picker Bluetooth -- terpisah dari [PrinterUiState] karena siklus
 *  hidupnya beda (aktif hanya selagi dialog pemilihan perangkat terbuka). */
data class BluetoothUiState(
    val bondedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val discoveredDevices: List<BluetoothDeviceInfo> = emptyList(),
    val isScanning: Boolean = false,
    val pairingTarget: BluetoothDeviceInfo? = null,
    val isPairing: Boolean = false
)

/** State picker USB (Batch H3c) -- terpisah karena model permission &
 *  siklus hidupnya beda total dari Bluetooth (tanpa discovery aktif, tanpa
 *  PIN, cukup listen attach/detach + request permission sekali klik). */
data class UsbUiState(
    val devices: List<UsbDeviceInfo> = emptyList(),
    val isRequestingPermission: Boolean = false
)

class PrinterViewModel(
    private val printerRepository: PrinterRepository,
    private val bluetoothHelper: BluetoothPrinterHelper,
    private val usbHelper: UsbPrinterHelper,
    private val connectionFactory: PrinterConnectionFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()

    private val _bluetoothUiState = MutableStateFlow(BluetoothUiState())
    val bluetoothUiState: StateFlow<BluetoothUiState> = _bluetoothUiState.asStateFlow()

    private val _usbUiState = MutableStateFlow(UsbUiState())
    val usbUiState: StateFlow<UsbUiState> = _usbUiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Event one-shot: emit sekali saat pairing+pemilihan perangkat sukses,
     *  dipakai UI utk menutup dialog picker otomatis (pola sama seperti
     *  [messages], supaya tidak ke-collect ulang saat rekomposisi/rotasi). */
    private val _pairingSuccess = MutableSharedFlow<BluetoothDeviceInfo>(extraBufferCapacity = 1)
    val pairingSuccess: SharedFlow<BluetoothDeviceInfo> = _pairingSuccess.asSharedFlow()

    /** Event one-shot serupa [pairingSuccess], untuk penutupan otomatis
     *  dialog picker USB setelah izin akses diberikan. */
    private val _usbSelectionSuccess = MutableSharedFlow<UsbDeviceInfo>(extraBufferCapacity = 1)
    val usbSelectionSuccess: SharedFlow<UsbDeviceInfo> = _usbSelectionSuccess.asSharedFlow()

    val printers: StateFlow<List<PrinterEntity>> = printerRepository.allPrinters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var discoveryJob: Job? = null
    private var usbAttachmentJob: Job? = null

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
                wifiPort = printer.wifiPort?.toString() ?: "9100",
                bluetoothMacAddress = printer.bluetoothMacAddress ?: "",
                usbVendorId = printer.usbVendorId,
                usbProductId = printer.usbProductId,
                supportsStatusQuery = printer.supportsStatusQuery
            )
        )
    }

    fun closeFormDialog() {
        _uiState.value = _uiState.value.copy(showFormDialog = false)
    }

    fun updateFormLabel(value: String) = updateForm { it.copy(label = value) }

    fun updateFormConnectionType(type: PrinterConnectionType) =
        updateForm { it.copy(connectionType = type) }

    fun updateFormPaperWidth(width: PaperWidth) =
        updateForm { it.copy(paperWidth = width, charPerLine = width.defaultCharPerLine().toString()) }

    fun updateFormCharPerLine(value: String) =
        updateForm { it.copy(charPerLine = value.filter { c -> c.isDigit() }) }

    fun updateFormWifiIp(value: String) = updateForm { it.copy(wifiIpAddress = value) }

    fun updateFormWifiPort(value: String) =
        updateForm { it.copy(wifiPort = value.filter { c -> c.isDigit() }) }

    fun updateFormSupportsStatusQuery(value: Boolean) =
        updateForm { it.copy(supportsStatusQuery = value) }

    private inline fun updateForm(block: (PrinterFormState) -> PrinterFormState) {
        _uiState.value = _uiState.value.copy(formState = block(_uiState.value.formState))
    }

    fun saveForm() {
        val form = _uiState.value.formState

        val label = form.label.trim()
        if (label.isEmpty()) {
            emitMessage("Nama printer tidak boleh kosong.")
            return
        }

        var wifiIp: String? = null
        var wifiPort: Int? = null
        var btAddress: String? = null
        var usbVendorId: Int? = null
        var usbProductId: Int? = null

        when (form.connectionType) {
            PrinterConnectionType.WIFI -> {
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
                wifiIp = ip
                wifiPort = port
            }
            PrinterConnectionType.BLUETOOTH -> {
                if (form.bluetoothMacAddress.isBlank()) {
                    emitMessage("Pilih perangkat Bluetooth terlebih dahulu.")
                    return
                }
                btAddress = form.bluetoothMacAddress
            }
            PrinterConnectionType.USB -> {
                val vendorId = form.usbVendorId
                val productId = form.usbProductId
                if (vendorId == null || productId == null) {
                    emitMessage("Pilih perangkat USB terlebih dahulu.")
                    return
                }
                usbVendorId = vendorId
                usbProductId = productId
            }
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
                    isDefault = existing.isEmpty(),
                    priority = nextPriority,
                    charPerLine = charPerLine,
                    paperWidth = form.paperWidth,
                    supportsStatusQuery = form.supportsStatusQuery,
                    wifiIpAddress = wifiIp,
                    wifiPort = wifiPort,
                    bluetoothMacAddress = btAddress,
                    usbVendorId = usbVendorId,
                    usbProductId = usbProductId
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
                        connectionType = form.connectionType,
                        paperWidth = form.paperWidth,
                        charPerLine = charPerLine,
                        supportsStatusQuery = form.supportsStatusQuery,
                        wifiIpAddress = wifiIp,
                        wifiPort = wifiPort,
                        bluetoothMacAddress = btAddress,
                        usbVendorId = usbVendorId,
                        usbProductId = usbProductId
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

                if (printer.isDefault) {
                    val remaining = printerRepository.getAllOrderedByPriority()
                    remaining.firstOrNull()?.let { printerRepository.setAsDefault(it) }
                }
            }
            _uiState.value = _uiState.value.copy(pendingDeleteId = null)
        }
    }

    // ---- Test Print (Batch H3d) ----

    fun testPrint(printer: PrinterEntity) {
        if (_uiState.value.testingPrinterIds.contains(printer.id)) return // guard anti-dobel-klik
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                testingPrinterIds = _uiState.value.testingPrinterIds + printer.id
            )
            // Jaring pengaman terakhir -- membungkus SELURUH pemanggilan
            // dengan try/catch. Idealnya PrinterConnectionFactory sudah
            // menangani semua exception secara internal, tapi ini disiapkan
            // sebagai lapisan pertahanan terakhir supaya kesalahan tak
            // terduga apa pun (dari library eksternal/hardware) berakhir
            // sebagai pesan error, BUKAN membuat aplikasi force close.
            val resultMessage = try {
                when (val result = connectionFactory.testPrint(printer)) {
                    is TestPrintResult.Success -> "Test print ke \"${printer.label}\" berhasil."
                    is TestPrintResult.Failure -> result.message
                }
            } catch (e: Exception) {
                "Test print gagal karena kesalahan tak terduga: ${e.message ?: "tidak diketahui"}"
            }
            emitMessage(resultMessage)
            _uiState.value = _uiState.value.copy(
                testingPrinterIds = _uiState.value.testingPrinterIds - printer.id
            )
        }
    }

    // ---- Bluetooth: pairing & discovery (Batch H3b) ----

    fun hasBluetoothPermissions(): Boolean = bluetoothHelper.hasPermissions()

    fun isBluetoothEnabled(): Boolean = bluetoothHelper.isAdapterEnabled()

    fun refreshBondedDevices() {
        _bluetoothUiState.value = _bluetoothUiState.value.copy(
            bondedDevices = bluetoothHelper.getBondedDevices()
        )
    }

    fun startDiscovery() {
        if (discoveryJob?.isActive == true) return
        _bluetoothUiState.value = _bluetoothUiState.value.copy(isScanning = true, discoveredDevices = emptyList())
        discoveryJob = viewModelScope.launch {
            bluetoothHelper.discoverDevices().collect { found ->
                val current = _bluetoothUiState.value.discoveredDevices
                if (current.none { it.address == found.address }) {
                    _bluetoothUiState.value = _bluetoothUiState.value.copy(discoveredDevices = current + found)
                }
            }
        }
        // Discovery Bluetooth Classic bawaan Android otomatis berhenti
        // ~12 detik -- selaraskan indikator "isScanning" dengan batas itu.
        viewModelScope.launch {
            delay(13_000)
            if (discoveryJob?.isActive == true) stopDiscovery()
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        bluetoothHelper.cancelDiscovery()
        _bluetoothUiState.value = _bluetoothUiState.value.copy(isScanning = false)
    }

    fun requestPairing(device: BluetoothDeviceInfo) {
        _bluetoothUiState.value = _bluetoothUiState.value.copy(pairingTarget = device)
    }

    fun cancelPairing() {
        _bluetoothUiState.value = _bluetoothUiState.value.copy(pairingTarget = null)
    }

    fun confirmPairing(pin: String) {
        val target = _bluetoothUiState.value.pairingTarget ?: return
        viewModelScope.launch {
            _bluetoothUiState.value = _bluetoothUiState.value.copy(isPairing = true)
            when (bluetoothHelper.pairDevice(target.address, pin)) {
                BondResult.Success, BondResult.AlreadyBonded -> {
                    selectBluetoothDevice(target)
                    emitMessage("Berhasil memasangkan \"${target.name}\".")
                    _bluetoothUiState.value = BluetoothUiState()
                    _pairingSuccess.emit(target)
                }
                BondResult.Failed -> {
                    emitMessage(
                        "Gagal memasangkan perangkat. Periksa PIN & coba lagi, " +
                            "atau ikuti dialog pairing sistem bila muncul."
                    )
                    _bluetoothUiState.value = _bluetoothUiState.value.copy(isPairing = false, pairingTarget = null)
                }
            }
        }
    }

    /** Pilih perangkat yang SUDAH ter-pairing (tanpa perlu PIN lagi). */
    fun selectBondedDevice(device: BluetoothDeviceInfo) {
        selectBluetoothDevice(device)
    }

    fun resetBluetoothPicker() {
        stopDiscovery()
        _bluetoothUiState.value = BluetoothUiState()
    }

    private fun selectBluetoothDevice(device: BluetoothDeviceInfo) {
        updateForm {
            it.copy(
                bluetoothMacAddress = device.address,
                label = it.label.ifBlank { device.name }
            )
        }
    }

    // ---- USB (Batch H3c) ----

    fun refreshUsbDevices() {
        _usbUiState.value = _usbUiState.value.copy(devices = usbHelper.getDeviceList())
    }

    fun startObservingUsbAttachment() {
        if (usbAttachmentJob?.isActive == true) return
        usbAttachmentJob = viewModelScope.launch {
            usbHelper.observeAttachDetach().collect {
                refreshUsbDevices()
            }
        }
    }

    fun stopObservingUsbAttachment() {
        usbAttachmentJob?.cancel()
        usbAttachmentJob = null
    }

    /** Pilih device USB -- langsung minta izin akses (sekali klik), TIDAK
     *  ada langkah PIN seperti Bluetooth karena model permission USB murni
     *  soal izin akses OS, bukan pairing perangkat. */
    fun selectUsbDevice(device: UsbDeviceInfo) {
        viewModelScope.launch {
            _usbUiState.value = _usbUiState.value.copy(isRequestingPermission = true)

            val rawDevice = usbHelper.findDeviceByName(device.deviceName)
            if (rawDevice == null) {
                emitMessage("Perangkat USB tidak ditemukan (mungkin sudah dicabut).")
                _usbUiState.value = _usbUiState.value.copy(isRequestingPermission = false)
                return@launch
            }

            when (usbHelper.requestPermission(rawDevice)) {
                UsbPermissionResult.Granted -> {
                    updateForm {
                        it.copy(
                            usbVendorId = device.vendorId,
                            usbProductId = device.productId,
                            label = it.label.ifBlank { device.label }
                        )
                    }
                    emitMessage("Izin akses USB \"${device.label}\" diberikan.")
                    _usbUiState.value = UsbUiState()
                    _usbSelectionSuccess.emit(device)
                }
                UsbPermissionResult.Denied -> {
                    emitMessage("Izin akses USB ditolak. Tidak bisa menggunakan perangkat ini.")
                    _usbUiState.value = _usbUiState.value.copy(isRequestingPermission = false)
                }
            }
        }
    }

    fun resetUsbPicker() {
        stopObservingUsbAttachment()
        _usbUiState.value = UsbUiState()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothHelper.cancelDiscovery()
        usbAttachmentJob?.cancel()
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