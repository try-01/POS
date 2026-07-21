package com.pos.offline.ui.pos

import android.util.Log
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
import com.pos.offline.data.repository.ShiftRepository
import com.pos.offline.data.repository.ShiftSummary
import com.pos.offline.data.repository.StoreProfileRepository
import com.pos.offline.data.repository.TransactionRepository
import com.pos.offline.ui.receipt.PrintUiState
import com.pos.offline.util.CashDrawerResult
import com.pos.offline.util.PrintCoordinator
import com.pos.offline.util.PrinterConnectionFactory
import com.pos.offline.util.roundToRupiah
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    init {
        viewModelScope.launch {
            categories.collect { list ->
                val current = _selectedCategory.value
                if (current != null && current !in list) {
                    _selectedCategory.value = null
                }
            }
        }
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

    val openShift: StateFlow<ShiftEntity?> =
        shiftRepository.openShift
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val openShifts: StateFlow<List<ShiftEntity>> =
        shiftRepository.openShifts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private var lastScannedBarcode: String = ""
    private var lastScannedTimestamp: Long = 0L
    private val scanCooldownMs = 600L // saring burst kamera, gak block scan sengaja

    private fun sanitizeScannedCode(raw: String): String? {
        val cleaned = raw.trim().filter { c -> c.isLetterOrDigit() || c in "-_./: #" }.take(128)
        return cleaned.ifBlank { null }
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

    fun startShift(
        cashierId: Long,
        startingCash: Long,
    ) = viewModelScope.launch {
        val cashier = activeCashiers.value.find { it.id == cashierId } ?: return@launch
        shiftRepository.startShift(cashierId, cashier.name, startingCash)
        _showStartShiftDialog.value = false
        _uiEvents.emit(PosUiEvent.ShowMessage("Shift dimulai untuk ${cashier.name}."))
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
            val shift = _endShiftTarget.value ?: return@launch
            shiftRepository.endShift(shift.id, actualCash)
            _showEndShiftDialog.value = false
            _shiftSummary.value = null
            _endShiftTarget.value = null
            _uiEvents.emit(PosUiEvent.ShowMessage("Shift ditutup untuk ${shift.cashierName}."))
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
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
    }

    // Pisahkan logika inti ke suspend function agar bisa mengembalikan status (Boolean)
    private suspend fun tryAddToCart(product: ProductEntity): Boolean {
        val currentQtyInCart = cart.value.find { it.productId == product.id }?.quantity ?: 0
        if (currentQtyInCart + 1 > product.stock) {
            _uiEvents.emit(
                PosUiEvent.ShowMessage("Stok \"${product.name}\" tidak mencukupi (tersisa ${product.stock})."),
            )
            return false // Gagal karena kehabisan stok
        }
        cartRepository.add(product.id, product.name, product.price)
        return true // Sukses
    }

    // Fungsi utama yang dipanggil oleh klik di UI (Tetap mempertahankan signature aslinya)
    fun addToCart(product: ProductEntity) =
        viewModelScope.launch {
            tryAddToCart(product)
        }

    fun setQuantityDirect(
        item: CartItemEntity,
        newQuantity: Int,
    ) = viewModelScope.launch {
        if (newQuantity <= 0) {
            cartRepository.remove(item.productId)
            return@launch
        }
        val product = productRepository.getById(item.productId)
        val stock = product?.stock
        if (stock != null && newQuantity > stock) {
            _uiEvents.emit(
                PosUiEvent.ShowMessage("Stok \"${item.name}\" tidak mencukupi (tersisa $stock)."),
            )
            return@launch
        }
        cartRepository.setQuantity(item.productId, newQuantity)
    }

    fun increaseQty(item: CartItemEntity) =
        viewModelScope.launch {
            val product = productRepository.getById(item.productId)
            val stock = product?.stock
            if (stock != null && item.quantity + 1 > stock) {
                _uiEvents.emit(
                    PosUiEvent.ShowMessage("Stok \"${item.name}\" tidak mencukupi (tersisa $stock)."),
                )
                return@launch
            }
            cartRepository.setQuantity(item.productId, item.quantity + 1)
        }

    fun decreaseQty(item: CartItemEntity) =
        viewModelScope.launch {
            cartRepository.setQuantity(item.productId, item.quantity - 1)
        }

    fun removeFromCart(item: CartItemEntity) =
        viewModelScope.launch {
            cartRepository.remove(item.productId)
        }

    fun clearCart() = viewModelScope.launch { cartRepository.clear() }

    fun checkout() =
        viewModelScope.launch {
            val currentCart = cart.value
            if (currentCart.isEmpty()) return@launch

            _checkoutState.value = CheckoutState.Processing
            _printUiState.value = PrintUiState.Idle
            _checkoutState.value =
                try {
                    val shift = openShift.value
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
                        )
                    _discountType.value = DiscountType.NOMINAL
                    _discountValue.value = 0.0
                    _taxRate.value = 0.0 // Reset tax rate agar tidak terbawa transaksi berikutnya
                    _paid.value = 0L
                    _paymentMethod.value = PaymentMethod.CASH
                    CheckoutState.Success(result)
                } catch (e: InsufficientStockException) {
                    CheckoutState.Error("Stok '${e.productName}' tidak mencukupi.")
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
        private const val TAG = "PosViewModel"

        fun computeTotals(
            items: List<CartItemEntity>,
            discountType: DiscountType,
            discountValue: Double,
            taxRate: Double,
        ): Totals {
            val subtotal = items.sumOf { it.unitPrice * it.quantity.toLong() }

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
