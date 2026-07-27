package com.pos.offline.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.local.entity.DiscountType
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.repository.CartRepository
import com.pos.offline.data.repository.CashierRepository
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.InsufficientStockException
import com.pos.offline.data.repository.PrinterRepository
import com.pos.offline.data.repository.ProductRepository
import com.pos.offline.data.repository.ShiftEndOutcome
import com.pos.offline.data.repository.ShiftRepository
import com.pos.offline.data.repository.ShiftStartOutcome
import com.pos.offline.data.repository.ShiftSummary
import com.pos.offline.data.repository.StoreProfileRepository
import com.pos.offline.data.repository.TransactionRepository
import com.pos.offline.ui.receipt.PrintUiState
import com.pos.offline.util.CashDrawerResult
import com.pos.offline.util.PrintCoordinator
import com.pos.offline.util.PrinterConnectionFactory
import com.pos.offline.util.roundToRupiah
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Totals(
    val subtotal: Long = 0L,
    val discount: Long = 0L,
    val tax: Long = 0L,
    val total: Long = 0L,
    val discountCapped: Boolean = false,
)

sealed interface PosUiEvent {
    data class ShowMessage(
        val message: String,
    ) : PosUiEvent
}

/** Info untuk dialog peringatan stok non-blocking. Transaksi/penambahan cart
 * TETAP diteruskan (kebijakan soft-block) — dialog ini murni pemberitahuan,
 * bukan penghalang, mengikuti kebutuhan lapangan: stok fisik sering telat
 * di-update sementara pembeli sudah menunggu di kasir. */
data class StockWarningInfo(
    val productName: String,
    val currentStock: Double,
)

sealed interface CheckoutState {
    data object Idle : CheckoutState

    data object Processing : CheckoutState

    data class Success(
        val result: CheckoutResult,
    ) : CheckoutState

    data class Error(
        val message: String,
    ) : CheckoutState
}

@OptIn(kotlinx.coroutines.FlowPreview::class, ExperimentalCoroutinesApi::class)
class PosViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository,
    private val cashierRepository: CashierRepository,
    private val shiftRepository: ShiftRepository,
    private val printCoordinator: PrintCoordinator,
    private val storeProfileRepository: StoreProfileRepository,
    private val printerRepository: PrinterRepository,
    private val printerConnectionFactory: PrinterConnectionFactory,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _discountType = MutableStateFlow(DiscountType.NOMINAL)
    val discountType: StateFlow<DiscountType> = _discountType.asStateFlow()

    private val _discountValue = MutableStateFlow(0.0)
    val discountValue: StateFlow<Double> = _discountValue.asStateFlow()

    private val _taxRate = MutableStateFlow(0.0)
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    private val _paid = MutableStateFlow(0L)
    val paid: StateFlow<Long> = _paid.asStateFlow()

    /**
     * Override manual "kembalian yang benar-benar diserahkan" ke pembeli.
     * `null` = default, mengikuti nilai kembalian penuh secara otomatis
     * (perilaku lama, backward-compatible). Diisi eksplisit oleh kasir lewat
     * UI hanya saat mereka ingin menyisakan sebagian sebagai tip. Nilai ini
     * BOLEH menjadi "stale" relatif terhadap total/pembayaran terbaru --
     * clamp final ke rentang valid [0, max(change,0)] SELALU dihitung ulang
     * di TransactionRepository.checkout() dari data checkout sebenarnya,
     * bukan dari state UI yang sempat tersimpan di sini.
     */
    private val _changeGivenOverride = MutableStateFlow<Long?>(null)
    val changeGivenOverride: StateFlow<Long?> = _changeGivenOverride.asStateFlow()

    /**
     * Hanya relevan untuk QRIS saat change > 0: apakah nominal kembalian
     * benar-benar diserahkan sebagai uang TUNAI FISIK dari laci. Default
     * true (skenario paling umum saat QRIS overpay: pembeli ingin pegang
     * cash). Untuk CASH, nilai ini tidak berpengaruh (selalu dipaksa true
     * di TransactionRepository).
     */
    private val _changeGivenInCash = MutableStateFlow(true)
    val changeGivenInCash: StateFlow<Boolean> = _changeGivenInCash.asStateFlow()

    private val _paymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val paymentMethod: StateFlow<PaymentMethod> = _paymentMethod.asStateFlow()

    private val searchResults: StateFlow<List<ProductEntity>> =
        _searchQuery
            .debounce(180)
            .distinctUntilChanged()
            .flatMapLatest { productRepository.search(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null) // null = "Semua"
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _stockWarning = MutableStateFlow<StockWarningInfo?>(null)
    val stockWarning: StateFlow<StockWarningInfo?> = _stockWarning.asStateFlow()

    val categories: StateFlow<List<String>> =
        productRepository
            .observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> =
        combine(
            searchResults,
            _selectedCategory,
        ) { list, category ->
            if (category == null) list else list.filter { it.category == category }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    val cart: StateFlow<List<CartItemEntity>> =
        cartRepository.cartItems
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totals: StateFlow<Totals> =
        combine(
            cart,
            _discountType,
            _discountValue,
            _taxRate,
        ) { items, discType, discValue, rate ->
            computeTotals(items, discType, discValue, rate)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Totals())

    private val _uiEvents = MutableSharedFlow<PosUiEvent>(extraBufferCapacity = 4)
    val uiEvents: SharedFlow<PosUiEvent> = _uiEvents.asSharedFlow()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    private val _printUiState = MutableStateFlow<PrintUiState>(PrintUiState.Idle)
    val printUiState: StateFlow<PrintUiState> = _printUiState.asStateFlow()

    private val _isOpeningDrawer = MutableStateFlow(false)
    val isOpeningDrawer: StateFlow<Boolean> = _isOpeningDrawer.asStateFlow()

    private val _openDrawerOnPrint = MutableStateFlow(false)
    val openDrawerOnPrint: StateFlow<Boolean> = _openDrawerOnPrint.asStateFlow()

    val activeCashiers: StateFlow<List<CashierEntity>> =
        cashierRepository.activeCashiers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** @deprecated secara arsitektur untuk keperluan checkout (lihat [activeShift]).
     * Dipertahankan hanya sebagai info "shift terbaru dibuka" untuk kebutuhan tampilan lain. */
    val openShift: StateFlow<ShiftEntity?> =
        shiftRepository.openShift
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val openShifts: StateFlow<List<ShiftEntity>> =
        shiftRepository.openShifts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** ID shift yang secara eksplisit dipilih sebagai "kasir yang bertugas di
     * terminal ini". Sumber kebenaran untuk atribusi transaksi pada checkout,
     * BUKAN [openShift] (yang cuma "shift terbaru dibuka" secara global). */
    private val _activeShiftId = MutableStateFlow<Long?>(null)

    /** Shift yang sedang dipakai untuk checkout di sesi ini. Auto-terisi kalau
     * cuma ada 1 shift terbuka (kasus kasir tunggal, backward-compatible).
     * Kalau >1 shift terbuka bersamaan (multi-kasir), WAJIB dipilih manual via
     * [selectActiveShift] sebelum checkout — mencegah transaksi salah
     * teratribusi ke kasir yang tidak sedang bertugas di terminal ini. */
    val activeShift: StateFlow<ShiftEntity?> =
        combine(openShifts, _activeShiftId) { shifts, activeId ->
            shifts.find { it.id == activeId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showStartShiftDialog = MutableStateFlow(false)
    val showStartShiftDialog: StateFlow<Boolean> = _showStartShiftDialog.asStateFlow()

    private val _showEndShiftDialog = MutableStateFlow(false)
    val showEndShiftDialog: StateFlow<Boolean> = _showEndShiftDialog.asStateFlow()

    private val _shiftSummary = MutableStateFlow<ShiftSummary?>(null)
    val shiftSummary: StateFlow<ShiftSummary?> = _shiftSummary.asStateFlow()

    private val _endShiftTarget = MutableStateFlow<ShiftEntity?>(null)
    val endShiftTarget: StateFlow<ShiftEntity?> = _endShiftTarget.asStateFlow()

    private val _showShiftListDialog = MutableStateFlow(false)
    val showShiftListDialog: StateFlow<Boolean> = _showShiftListDialog.asStateFlow()

    private val _isStartingShift = MutableStateFlow(false)
    val isStartingShift: StateFlow<Boolean> = _isStartingShift.asStateFlow()

    private val _isEndingShift = MutableStateFlow(false)
    val isEndingShift: StateFlow<Boolean> = _isEndingShift.asStateFlow()

    private var lastScannedBarcode: String = ""
    private var lastScannedTimestamp: Long = 0L
    private val scanCooldownMs = 600L // saring burst kamera, gak block scan sengaja

    private fun sanitizeScannedCode(raw: String): String? {
        val cleaned = raw.trim().filter { c -> c.isLetterOrDigit() || c in "-_./: #" }.take(128)
        return cleaned.ifBlank { null }
    }

    init {
        viewModelScope.launch {
            categories.collect { list ->
                val current = _selectedCategory.value
                if (current != null && current !in list) {
                    _selectedCategory.value = null
                }
            }
        }
        // Mekanisme auto-select shift aktif untuk mendukung multi-kasir dengan
        // aman: hanya auto-pilih kalau tidak ambigu (0 atau 1 shift terbuka).
        // Kalau >1 shift terbuka, kasir WAJIB memilih manual (selectActiveShift).
        viewModelScope.launch {
            openShifts.collect { shifts ->
                val currentActiveId = _activeShiftId.value
                when {
                    shifts.isEmpty() -> {
                        if (currentActiveId != null) _activeShiftId.value = null
                    }

                    currentActiveId != null && shifts.none { it.id == currentActiveId } -> {
                        // Shift yang tadinya aktif di sesi ini sudah ditutup
                        // (oleh diri sendiri atau proses lain). Jangan diam-diam
                        // pindah ke kasir lain kecuali memang cuma tersisa 1.
                        _activeShiftId.value = if (shifts.size == 1) shifts.first().id else null
                    }

                    currentActiveId == null && shifts.size == 1 -> {
                        _activeShiftId.value = shifts.first().id
                    }
                    // currentActiveId == null && shifts.size > 1 -> tetap null,
                    // paksa kasir memilih shift aktif secara eksplisit.
                }
            }
        }
    }

    fun onBarcodeScanned(raw: String) {
        val barcode = sanitizeScannedCode(raw)
        if (barcode == null) {
            viewModelScope.launch {
                _uiEvents.emit(PosUiEvent.ShowMessage("Gagal memindai kode. Coba pindai ulang."))
            }
            return
        }

        val now = System.currentTimeMillis()
        if (barcode == lastScannedBarcode && (now - lastScannedTimestamp) < scanCooldownMs) {
            return
        }
        lastScannedBarcode = barcode
        lastScannedTimestamp = now
        viewModelScope.launch {
            val product = productRepository.getProductByBarcode(barcode)
            if (product == null) {
                _uiEvents.emit(PosUiEvent.ShowMessage("Produk tidak ditemukan!"))
                return@launch
            }
            val success = tryAddToCart(product)
            if (success) {
                _uiEvents.emit(PosUiEvent.ShowMessage("${product.name} ditambahkan ke keranjang"))
            }
        }
    }

    fun toggleOpenDrawerOnPrint(enabled: Boolean) {
        _openDrawerOnPrint.value = enabled
    }

    fun openShiftListDialog() {
        _showShiftListDialog.value = true
    }

    fun dismissShiftListDialog() {
        _showShiftListDialog.value = false
    }

    fun openStartShiftDialog() {
        _showStartShiftDialog.value = true
    }

    fun dismissStartShiftDialog() {
        _showStartShiftDialog.value = false
    }

    /** Dipanggil saat kasir secara eksplisit memilih shift mana yang sedang ia
     * operasikan di terminal ini. WAJIB dipanggil sebelum checkout jika ada
     * >1 shift terbuka bersamaan (multi-kasir). */
    fun selectActiveShift(shiftId: Long) {
        _activeShiftId.value = shiftId
    }

    fun startShift(
        cashierId: Long,
        startingCash: Long,
    ) = viewModelScope.launch {
        if (_isStartingShift.value) return@launch
        _isStartingShift.value = true
        try {
            val cashier = activeCashiers.value.find { it.id == cashierId } ?: return@launch
            when (val outcome = shiftRepository.startShift(cashierId, cashier.name, startingCash)) {
                is ShiftStartOutcome.Success -> {
                    _showStartShiftDialog.value = false
                    // Kasir yang baru saja membuka shift diasumsikan yang
                    // sekarang bertugas di terminal ini -> jadikan aktif.
                    _activeShiftId.value = outcome.shiftId
                    _uiEvents.emit(PosUiEvent.ShowMessage("Shift dimulai untuk ${cashier.name}."))
                }

                ShiftStartOutcome.AlreadyOpenForCashier -> {
                    _uiEvents.emit(
                        PosUiEvent.ShowMessage("${cashier.name} sudah memiliki shift yang sedang berjalan."),
                    )
                }
            }
        } finally {
            _isStartingShift.value = false
        }
    }

    fun openEndShiftDialog(shift: ShiftEntity) =
        viewModelScope.launch {
            _endShiftTarget.value = shift
            _shiftSummary.value = shiftRepository.getShiftSummary(shift.id)
            _showEndShiftDialog.value = true
        }

    fun dismissEndShiftDialog() {
        _showEndShiftDialog.value = false
        _shiftSummary.value = null
        _endShiftTarget.value = null
    }

    fun endShift(actualCash: Long) =
        viewModelScope.launch {
            if (_isEndingShift.value) return@launch
            val shift = _endShiftTarget.value ?: return@launch
            _isEndingShift.value = true
            try {
                when (val outcome = shiftRepository.endShift(shift.id, actualCash)) {
                    is ShiftEndOutcome.Success -> {
                        if (_activeShiftId.value == shift.id) {
                            _activeShiftId.value = null
                        }
                        _uiEvents.emit(PosUiEvent.ShowMessage("Shift ditutup untuk ${shift.cashierName}."))
                    }

                    ShiftEndOutcome.AlreadyClosed -> {
                        _uiEvents.emit(PosUiEvent.ShowMessage("Shift ini sudah ditutup sebelumnya."))
                    }

                    ShiftEndOutcome.NotFound -> {
                        _uiEvents.emit(PosUiEvent.ShowMessage("Shift tidak ditemukan."))
                    }
                }
                _showEndShiftDialog.value = false
                _shiftSummary.value = null
                _endShiftTarget.value = null
            } finally {
                _isEndingShift.value = false
            }
        }

    fun search(q: String) {
        _searchQuery.value = q
    }

    fun setDiscountValue(raw: Double) {
        _discountValue.value =
            when (_discountType.value) {
                DiscountType.NOMINAL -> raw.coerceAtLeast(0.0)
                DiscountType.PERCENT -> raw.coerceIn(0.0, 100.0)
            }
    }

    fun toggleDiscountType() {
        _discountType.value =
            if (_discountType.value == DiscountType.NOMINAL) {
                DiscountType.PERCENT
            } else {
                DiscountType.NOMINAL
            }
        _discountValue.value = 0.0
    }

    fun setTaxRate(rate: Double) {
        _taxRate.value = rate.coerceIn(0.0, 1.0)
    }

    fun setPaid(value: Long) {
        _paid.value = value.coerceAtLeast(0L)
        // Auto-reset: nominal "Bayar" berubah -> `change` & rentang valid
        // changeGiven ikut berubah total. Override lama (mis. kasir sudah
        // menurunkan kembalian jadi tip) sudah tidak relevan terhadap
        // pembayaran baru ini -> kembali ke default (kembalian penuh)
        // sampai kasir menyesuaikan ulang secara sadar.
        _changeGivenOverride.value = null
        _changeGivenInCash.value = true
    }

    /** Dipanggil dari UI saat kasir mengubah nilai "Kembalian Diberikan"
     * secara manual. `null` berarti kembali ke default (kembalian penuh). */
    fun setChangeGivenOverride(value: Long?) {
        _changeGivenOverride.value = value
    }

    /** Dipanggil dari UI saat kasir mengubah toggle "Kembalian tunai dari
     * laci?" — hanya relevan untuk QRIS saat change > 0. */
    fun setChangeGivenInCash(value: Boolean) {
        _changeGivenInCash.value = value
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
        // Reset ke default saat metode bayar berganti — mencegah state toggle
        // "nyangkut" dari transaksi/metode sebelumnya membingungkan konteks
        // yang baru (mis. pindah dari QRIS ke CASH lalu balik ke QRIS lagi).
        _changeGivenInCash.value = true
    }

    // Kebijakan SOFT-BLOCK: penambahan SELALU berhasil, tidak pernah ditolak
    // karena stok. Kalau qty melewati stok tercatat untuk pertama kali,
    // tampilkan dialog peringatan (bukan penolakan) via _stockWarning.
    private suspend fun tryAddToCart(product: ProductEntity): Boolean {
        val result =
            cartRepository.changeQuantity(
                productId = product.id,
                name = product.name,
                unitPrice = product.price,
                delta = 1,
                maxStock = product.stock,
            )
        if (result.crossedIntoExcess) {
            _stockWarning.value = StockWarningInfo(product.name, product.stock)
        }
        return true // Selalu sukses; stok tidak lagi memblokir penambahan.
    }

    // Fungsi utama yang dipanggil oleh klik di UI (Tetap mempertahankan signature aslinya)
    fun addToCart(product: ProductEntity) =
        viewModelScope.launch {
            tryAddToCart(product)
        }

    // Input manual (ketik langsung) juga SOFT-BLOCK sekarang: tidak lagi
    // ditolak, hanya diberi peringatan kalau melewati stok tercatat.
    fun setQuantityDirect(
        item: CartItemEntity,
        newQuantity: Double,
    ) = viewModelScope.launch {
        if (newQuantity <= 0.0) {
            cartRepository.remove(item.productId)
            return@launch
        }
        val product = productRepository.getById(item.productId)
        val stock = product?.stock
        if (stock != null && newQuantity > stock && item.quantity <= stock) {
            // Hanya tampilkan dialog saat MELEWATI batas untuk pertama kali
            // (konsisten dengan crossedIntoExcess pada jalur tap +/-).
            _stockWarning.value = StockWarningInfo(item.name, stock)
        }
        cartRepository.setQuantity(item.productId, newQuantity)
    }

    fun increaseQty(item: CartItemEntity) =
        viewModelScope.launch {
            val stock = productRepository.getById(item.productId)?.stock
            val result =
                cartRepository.changeQuantity(
                    productId = item.productId,
                    name = item.name,
                    unitPrice = item.unitPrice,
                    delta = 1.0,
                    maxStock = stock,
                )
            if (result.crossedIntoExcess) {
                _stockWarning.value = StockWarningInfo(item.name, stock ?: 0.0)
            }
        }

    fun decreaseQty(item: CartItemEntity) =
        viewModelScope.launch {
            cartRepository.changeQuantity(
                productId = item.productId,
                name = item.name,
                unitPrice = item.unitPrice,
                delta = -1,
            )
        }

    fun dismissStockWarning() {
        _stockWarning.value = null
    }

    fun removeFromCart(item: CartItemEntity) =
        viewModelScope.launch {
            cartRepository.remove(item.productId)
        }

    fun clearCart() = viewModelScope.launch { cartRepository.clear() }

    fun checkout() =
        viewModelScope.launch {
            if (_checkoutState.value is CheckoutState.Processing) return@launch

            val currentCart = cart.value
            if (currentCart.isEmpty()) return@launch

            val shiftsNow = openShifts.value
            val shift = activeShift.value

            // Guard multi-kasir: kalau ada >1 shift terbuka tapi belum ada
            // yang dipilih sbg kasir aktif di sesi ini, JANGAN asal pakai
            // shift lain — mencegah transaksi salah atribusi ke kasir yang
            // tidak sedang bertugas di terminal ini.
            if (shift == null && shiftsNow.size > 1) {
                _checkoutState.value =
                    CheckoutState.Error(
                        "Ada beberapa shift kasir aktif. Pilih kasir yang bertugas di terminal ini terlebih dahulu.",
                    )
                return@launch
            }

            _checkoutState.value = CheckoutState.Processing
            _printUiState.value = PrintUiState.Idle
            _stockWarning.value = null
                try {
                    val currentTotal = totals.value.total
                    val effectivePaid = if (_paid.value <= 0L) currentTotal else _paid.value
                    val result =
                        transactionRepository.checkout(
                            cart = currentCart,
                            discountType = _discountType.value,
                            discountValue = _discountValue.value,
                            taxRate = _taxRate.value,
                            paid = effectivePaid,
                            paymentMethod = _paymentMethod.value,
                            cashierId = shift?.cashierId,
                            cashierName = shift?.cashierName ?: "",
                            shiftId = shift?.id,
                            changeGivenOverride = _changeGivenOverride.value,
                            changeGivenInCash = _changeGivenInCash.value,
                        )
                    _discountType.value = DiscountType.NOMINAL
                    _discountValue.value = 0.0
                    _taxRate.value = 0.0 // Reset tax rate agar tidak terbawa transaksi berikutnya
                    _paid.value = 0L
                    _changeGivenOverride.value = null
                    _changeGivenInCash.value = true
                    _paymentMethod.value = PaymentMethod.CASH
                    CheckoutState.Success(result)
                } catch (e: InsufficientStockException) {
                    // Sejak kebijakan stok soft-block, exception ini HANYA terpicu
                    // saat produk sudah tidak ada di database (dihapus permanen
                    // secara konkuren tepat saat checkout berlangsung) — bukan lagi
                    // "stok kurang", karena decrementStock kini selalu berhasil
                    // untuk id yang masih ada, walau hasilnya stok jadi negatif.
                    CheckoutState.Error(
                        "Produk '${e.productName}' tidak ditemukan (kemungkinan baru saja dihapus). Transaksi dibatalkan, silakan cek ulang keranjang.",
                    )
                } catch (e: Exception) {
                    CheckoutState.Error("Gagal memproses: ${e.message ?: "kesalahan tak dikenal"}")
                }

            (_checkoutState.value as? CheckoutState.Success)?.let { success ->
                maybeAutoPrint(success.result)
            }
        }

    private suspend fun maybeAutoPrint(result: CheckoutResult) {
        val profile = storeProfileRepository.get()
        if (profile.autoPrintEnabled) {
            printReceipt(result)
        }
    }

    fun openCashDrawerManually() {
        if (_isOpeningDrawer.value) return
        viewModelScope.launch {
            _isOpeningDrawer.value = true
            try {
                val printer = printerRepository.getDefault()
                if (printer == null) {
                    _uiEvents.emit(
                        PosUiEvent.ShowMessage("Printer belum diatur. Atur printer default di tab Pengaturan."),
                    )
                    return@launch
                }
                when (val outcome = printerConnectionFactory.openCashDrawer(printer)) {
                    is CashDrawerResult.Success -> {
                        _uiEvents.emit(PosUiEvent.ShowMessage("Laci kasir dibuka."))
                    }

                    is CashDrawerResult.Failure -> {
                        _uiEvents.emit(PosUiEvent.ShowMessage(outcome.message))
                    }
                }
            } finally {
                _isOpeningDrawer.value = false
            }
        }
    }

    fun printReceipt(result: CheckoutResult) {
        if (_printUiState.value is PrintUiState.Printing) return
        viewModelScope.launch {
            _printUiState.value = PrintUiState.Printing(result)
            val openDrawer = _openDrawerOnPrint.value
            val outcome = printCoordinator.printReceiptAuto(result, openDrawer)
            _printUiState.value = PrintUiState.Result(outcome, result)
        }
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
        _printUiState.value = PrintUiState.Idle
    }

    companion object {
        fun computeTotals(
            items: List<CartItemEntity>,
            discountType: DiscountType,
            discountValue: Double,
            taxRate: Double,
        ): Totals {
            val subtotal = items.sumOf { kotlin.math.round(it.unitPrice * it.quantity).toLong() }

            val rawDiscountAmount =
                (
                    when (discountType) {
                        DiscountType.NOMINAL -> discountValue.roundToRupiah()
                        DiscountType.PERCENT -> (subtotal * (discountValue / 100.0)).roundToRupiah()
                    }
                ).coerceAtLeast(0L)
            val discountAmount = rawDiscountAmount.coerceAtMost(subtotal)
            val discountCapped = rawDiscountAmount > subtotal && subtotal > 0L

            val taxableBase = (subtotal - discountAmount).coerceAtLeast(0L) // DPP
            val tax = (taxableBase * taxRate).roundToRupiah()
            val total = taxableBase + tax

            return Totals(subtotal, discountAmount, tax, total, discountCapped)
        }
    }
}