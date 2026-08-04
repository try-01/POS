package com.pos.offline.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.CartItemEntity
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
import com.pos.offline.data.repository.StoreProfileRepository
import com.pos.offline.data.repository.TransactionRepository
import com.pos.offline.ui.receipt.PrintUiState
import com.pos.offline.util.CashDrawerResult
import com.pos.offline.util.PrintCoordinator
import com.pos.offline.util.PrinterConnectionFactory
import com.pos.offline.util.roundToRupiah
import com.pos.offline.util.sanitizeScannedCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _discountType = MutableStateFlow(DiscountType.NOMINAL)
    private val _discountValue = MutableStateFlow(0.0)
    private val _taxRate = MutableStateFlow(0.0)
    private val _paid = MutableStateFlow(0L)
    private val _changeGivenOverride = MutableStateFlow<Long?>(null)
    private val _changeGivenInCash = MutableStateFlow(true)
    private val _paymentMethod = MutableStateFlow(PaymentMethod.CASH)
    private val _checkoutFlow = MutableStateFlow<CheckoutFlow>(CheckoutFlow.Idle)
    private val _printUiState = MutableStateFlow<PrintUiState>(PrintUiState.Idle)
    private val _openDrawerOnPrint = MutableStateFlow(false)
    private val _activeShiftId = MutableStateFlow<Long?>(null)
    private val _shiftSummary = MutableStateFlow<com.pos.offline.data.repository.ShiftSummary?>(null)
    private val _endShiftTarget = MutableStateFlow<ShiftEntity?>(null)
    private val _stockWarning = MutableStateFlow<StockWarningInfo?>(null)
    private val _isStartingShift = MutableStateFlow(false)
    private val _isEndingShift = MutableStateFlow(false)
    private val _isOpeningDrawer = MutableStateFlow(false)
    private val _showStartShiftDialog = MutableStateFlow(false)
    private val _showEndShiftDialog = MutableStateFlow(false)
    private val _showShiftListDialog = MutableStateFlow(false)

    private val searchResults =
        _searchQuery
            .debounce(180)
            .distinctUntilChanged()
            .flatMapLatest { productRepository.search(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allProducts =
        combine(
            searchResults,
            _selectedCategory,
        ) { list, category ->
            if (category == null) list else list.filter { it.category == category }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allCategories =
        productRepository
            .observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val cartItems =
        cartRepository.cartItems
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val activeCashiersFlow =
        cashierRepository.activeCashiers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val openShiftsFlow =
        shiftRepository.openShifts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val activeShiftFlow =
        combine(openShiftsFlow, _activeShiftId) { shifts, activeId ->
            shifts.find { it.id == activeId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val totalsFlow =
        combine(
            cartItems,
            _discountType,
            _discountValue,
            _taxRate,
        ) { items, discType, discValue, rate ->
            computeTotals(items, discType, discValue, rate)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Totals())

    val openShift: StateFlow<ShiftEntity?> =
        shiftRepository.openShift
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<PosUiState> =
        combine(
            combine(allProducts, allCategories, _selectedCategory, _searchQuery, cartItems) {
                products,
                categories,
                selectedCategory,
                searchQuery,
                cart,
                ->
                CatalogState(
                    products = products,
                    categories = categories,
                    selectedCategory = selectedCategory,
                    searchQuery = searchQuery,
                    cartQtyByProductId = cart.associate { it.productId to it.quantity },
                    stockByProductId = products.associate { it.id to it.stock },
                )
            },
            combine(cartItems, totalsFlow) { items, totals ->
                CartState(
                    items = items,
                    totals = totals,
                    isEmpty = items.isEmpty(),
                )
            },
            combine(
                _paymentMethod,
                _discountType,
                _discountValue,
                _taxRate,
                _paid,
                _changeGivenOverride,
                _changeGivenInCash,
                totalsFlow,
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val method = args[0] as PaymentMethod
                val discType = args[1] as DiscountType
                val discValue = args[2] as Double
                val taxRate = args[3] as Double
                val paid = args[4] as Long
                val changeOverride = args[5] as Long?
                val changeInCash = args[6] as Boolean
                val totals = args[7] as Totals
                PaymentState(
                    method = method,
                    discountType = discType,
                    discountValue = discValue,
                    taxRate = taxRate,
                    paid = paid,
                    change = paid - totals.total,
                    changeGivenOverride = changeOverride,
                    changeGivenInCash = changeInCash,
                )
            },
            combine(_checkoutFlow, _printUiState, _openDrawerOnPrint) { flow, print, openDrawer ->
                CheckoutState(
                    flow = flow,
                    printUiState = print,
                    openDrawerOnPrint = openDrawer,
                    isProcessing = flow is CheckoutFlow.Processing,
                )
            },
            combine(
                activeShiftFlow,
                openShiftsFlow,
                activeCashiersFlow,
                _shiftSummary,
                _stockWarning,
                _showStartShiftDialog,
                _showEndShiftDialog,
                _showShiftListDialog,
                _isStartingShift,
                _isEndingShift,
                _isOpeningDrawer,
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                ShiftState(
                    activeShift = args[0] as ShiftEntity?,
                    openShifts = args[1] as List<ShiftEntity>,
                    activeCashiers = args[2] as List<com.pos.offline.data.local.entity.CashierEntity>,
                    shiftSummary = args[3] as com.pos.offline.data.repository.ShiftSummary?,
                    stockWarning = args[4] as StockWarningInfo?,
                    showStartShiftDialog = args[5] as Boolean,
                    showEndShiftDialog = args[6] as Boolean,
                    showShiftListDialog = args[7] as Boolean,
                    isStartingShift = args[8] as Boolean,
                    isEndingShift = args[9] as Boolean,
                    isOpeningDrawer = args[10] as Boolean,
                )
            },
        ) { catalog, cart, payment, checkout, shift ->
            PosUiState(
                catalog = catalog,
                cart = cart,
                payment = payment,
                checkout = checkout,
                shift = shift,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PosUiState(),
        )

    private val _uiEvents = MutableSharedFlow<PosUiEvent>(extraBufferCapacity = 4)
    val uiEvents: SharedFlow<PosUiEvent> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            allCategories.collect { list ->
                val current = _selectedCategory.value
                if (current != null && current !in list) {
                    _selectedCategory.value = null
                }
            }
        }
        viewModelScope.launch {
            openShiftsFlow.collect { shifts ->
                val currentActiveId = _activeShiftId.value
                when {
                    shifts.isEmpty() -> {
                        if (currentActiveId != null) _activeShiftId.value = null
                    }

                    currentActiveId != null && shifts.none { it.id == currentActiveId } -> {
                        _activeShiftId.value =
                            if (shifts.size == 1) shifts.first().id else null
                    }

                    currentActiveId == null && shifts.size == 1 -> {
                        _activeShiftId.value = shifts.first().id
                    }
                }
            }
        }
    }

    fun onAction(action: PosAction) {
        when (action) {
            is PosAction.Search -> {
                _searchQuery.value = action.query
            }

            is PosAction.SelectCategory -> {
                _selectedCategory.value = action.category
            }

            is PosAction.AddToCart -> {
                viewModelScope.launch { tryAddToCart(action.product) }
            }

            is PosAction.IncreaseQty -> {
                increaseQty(action.item)
            }

            is PosAction.DecreaseQty -> {
                decreaseQty(action.item)
            }

            is PosAction.SetQuantity -> {
                setQuantityDirect(action.item, action.qty)
            }

            is PosAction.RemoveFromCart -> {
                viewModelScope.launch {
                    cartRepository.remove(action.item.productId)
                }
            }

            PosAction.ClearCart -> {
                clearCart()
            }

            PosAction.ToggleDiscountType -> {
                _discountType.value =
                    if (_discountType.value == DiscountType.NOMINAL) {
                        DiscountType.PERCENT
                    } else {
                        DiscountType.NOMINAL
                    }
                _discountValue.value = 0.0
            }

            is PosAction.SetDiscountValue -> {
                _discountValue.value =
                    when (_discountType.value) {
                        DiscountType.NOMINAL -> action.value.coerceAtLeast(0.0)
                        DiscountType.PERCENT -> action.value.coerceIn(0.0, 100.0)
                    }
            }

            is PosAction.SetTaxRate -> {
                _taxRate.value = action.rate.coerceIn(0.0, 1.0)
            }

            is PosAction.SetPaid -> {
                _paid.value = action.amount.coerceAtLeast(0L)
                _changeGivenOverride.value = null
                _changeGivenInCash.value = true
            }

            is PosAction.SetChangeGivenOverride -> {
                _changeGivenOverride.value = action.value
            }

            is PosAction.SetChangeGivenInCash -> {
                _changeGivenInCash.value = action.value
            }

            is PosAction.SetPaymentMethod -> {
                _paymentMethod.value = action.method
                _changeGivenInCash.value = true
            }

            PosAction.Checkout -> {
                checkout()
            }

            PosAction.ResetCheckout -> {
                _checkoutFlow.value = CheckoutFlow.Idle
                _printUiState.value = PrintUiState.Idle
            }

            is PosAction.PrintReceipt -> {
                printReceipt(action.result)
            }

            is PosAction.ToggleOpenDrawer -> {
                _openDrawerOnPrint.value = action.enabled
            }

            PosAction.OpenStartShiftDialog -> {
                _showStartShiftDialog.value = true
            }

            PosAction.DismissStartShiftDialog -> {
                _showStartShiftDialog.value = false
            }

            PosAction.OpenShiftListDialog -> {
                _showShiftListDialog.value = true
            }

            PosAction.DismissShiftListDialog -> {
                _showShiftListDialog.value = false
            }

            is PosAction.OpenEndShiftDialog -> {
                viewModelScope.launch {
                    _endShiftTarget.value = action.shift
                    _shiftSummary.value = shiftRepository.getShiftSummary(action.shift.id)
                    _showEndShiftDialog.value = true
                }
            }

            PosAction.DismissEndShiftDialog -> {
                _showEndShiftDialog.value = false
                _shiftSummary.value = null
                _endShiftTarget.value = null
            }

            is PosAction.StartShift -> {
                startShift(action.cashierId, action.startingCash)
            }

            is PosAction.EndShift -> {
                endShift(action.actualCash)
            }

            is PosAction.SelectActiveShift -> {
                _activeShiftId.value = action.shiftId
            }

            PosAction.OpenCashDrawer -> {
                openCashDrawerManually()
            }

            PosAction.DismissStockWarning -> {
                _stockWarning.value = null
            }
        }
    }

    suspend fun onBarcodeScanned(raw: String): String? { // Ubah Boolean menjadi String?
        val barcode = sanitizeScannedCode(raw)
        if (barcode == null) {
            _uiEvents.emit(PosUiEvent.ShowMessage("Gagal memindai kode. Coba pindai ulang."))
            return null // Ubah false menjadi null
        }
        val product = productRepository.getProductByBarcode(barcode)
        if (product == null) {
            _uiEvents.emit(PosUiEvent.ShowMessage("Produk tidak ditemukan!"))
            return null // Ubah false menjadi null
        }
        val success = tryAddToCart(product)
        if (success) {
            _uiEvents.emit(PosUiEvent.ShowMessage("${product.name} ditambahkan ke keranjang"))
            return product.name // Kembalikan NAMA PRODUK sebagai tanda sukses
        }
        return null
    }

    private suspend fun tryAddToCart(product: ProductEntity): Boolean {
        val result =
            cartRepository.changeQuantity(
                productId = product.id,
                name = product.name,
                unitPrice = product.price,
                delta = 1.0,
                maxStock = product.stock,
            )
        if (result.crossedIntoExcess) {
            _stockWarning.value = StockWarningInfo(product.name, product.stock)
        }
        return true
    }

    private fun increaseQty(item: CartItemEntity) =
        viewModelScope.launch {
            val product = productRepository.getById(item.productId)
            val stock = product?.stock

            cartRepository.changeQuantity(
                productId = item.productId,
                name = item.name,
                unitPrice = item.unitPrice,
                delta = 1.0,
                maxStock = null,
            )

            if (stock != null && (item.quantity + 1.0) > stock) {
                _stockWarning.value = StockWarningInfo(item.name, stock)
            }
        }

    private fun decreaseQty(item: CartItemEntity) =
        viewModelScope.launch {
            cartRepository.changeQuantity(
                productId = item.productId,
                name = item.name,
                unitPrice = item.unitPrice,
                delta = -1.0,
            )
        }

    private fun setQuantityDirect(
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
            _stockWarning.value = StockWarningInfo(item.name, stock)
        }
        cartRepository.setQuantity(item.productId, newQuantity)
    }

    private fun clearCart() =
        viewModelScope.launch {
            cartRepository.clear()
            _discountType.value = DiscountType.NOMINAL
            _discountValue.value = 0.0
            _paid.value = 0L
            _changeGivenOverride.value = null
            _changeGivenInCash.value = true
        }

    private fun checkout() =
        viewModelScope.launch {
            if (_checkoutFlow.value is CheckoutFlow.Processing) return@launch
            val currentCart = cartItems.value
            if (currentCart.isEmpty()) return@launch

            val shiftsNow = openShiftsFlow.value
            val shift = activeShiftFlow.value

            if (shift == null && shiftsNow.size > 1) {
                _checkoutFlow.value =
                    CheckoutFlow.Error(
                        "Ada beberapa shift kasir aktif. Pilih kasir yang bertugas di terminal ini terlebih dahulu.",
                    )
                return@launch
            }

            _checkoutFlow.value = CheckoutFlow.Processing
            _printUiState.value = PrintUiState.Idle
            _stockWarning.value = null

            try {
                val currentTotals = computeTotals(currentCart, _discountType.value, _discountValue.value, _taxRate.value)
                val currentTotal = currentTotals.total
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

                _paid.value = 0L
                _changeGivenOverride.value = null
                _changeGivenInCash.value = true
                _paymentMethod.value = PaymentMethod.CASH

                _checkoutFlow.value = CheckoutFlow.Success(result)
            } catch (e: InsufficientStockException) {
                _checkoutFlow.value =
                    CheckoutFlow.Error(
                        "Produk '${e.productName}' tidak ditemukan (kemungkinan baru saja dihapus). " +
                            "Transaksi dibatalkan, silakan cek ulang keranjang.",
                    )
            } catch (e: Exception) {
                _checkoutFlow.value =
                    CheckoutFlow.Error(
                        "Gagal memproses: ${e.message ?: "kesalahan tak dikenal"}",
                    )
            }

            (_checkoutFlow.value as? CheckoutFlow.Success)?.let { success ->
                maybeAutoPrint(success.result)
            }
        }

    private suspend fun maybeAutoPrint(result: CheckoutResult) {
        val profile = storeProfileRepository.get()
        if (profile.autoPrintEnabled) {
            printReceipt(result)
        }
    }

    private fun printReceipt(result: CheckoutResult) {
        if (_printUiState.value is PrintUiState.Printing) return
        viewModelScope.launch {
            _printUiState.value = PrintUiState.Printing(result)
            val openDrawer = _openDrawerOnPrint.value
            val outcome = printCoordinator.printReceiptAuto(result, openDrawer)
            _printUiState.value = PrintUiState.Result(outcome, result)
        }
    }

    private fun startShift(
        cashierId: Long,
        startingCash: Long,
    ) = viewModelScope.launch {
        if (_isStartingShift.value) return@launch
        _isStartingShift.value = true
        try {
            val cashier = activeCashiersFlow.value.find { it.id == cashierId } ?: return@launch
            when (val outcome = shiftRepository.startShift(cashierId, cashier.name, startingCash)) {
                is ShiftStartOutcome.Success -> {
                    _showStartShiftDialog.value = false
                    _activeShiftId.value = outcome.shiftId
                    _uiEvents.emit(PosUiEvent.ShowMessage("Shift dimulai untuk ${cashier.name}."))
                }

                ShiftStartOutcome.AlreadyOpenForCashier -> {
                    _uiEvents.emit(
                        PosUiEvent.ShowMessage(
                            "${cashier.name} sudah memiliki shift yang sedang berjalan.",
                        ),
                    )
                }
            }
        } finally {
            _isStartingShift.value = false
        }
    }

    private fun endShift(actualCash: Long) =
        viewModelScope.launch {
            if (_isEndingShift.value) return@launch
            val shift = _endShiftTarget.value ?: return@launch
            _isEndingShift.value = true
            try {
                when (shiftRepository.endShift(shift.id, actualCash)) {
                    is ShiftEndOutcome.Success -> {
                        if (_activeShiftId.value == shift.id) _activeShiftId.value = null
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

    private fun openCashDrawerManually() {
        if (_isOpeningDrawer.value) return
        viewModelScope.launch {
            _isOpeningDrawer.value = true
            try {
                val printer = printerRepository.getDefault()
                if (printer == null) {
                    _uiEvents.emit(
                        PosUiEvent.ShowMessage(
                            "Printer belum diatur. Atur printer default di tab Pengaturan.",
                        ),
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
            val taxableBase = (subtotal - discountAmount).coerceAtLeast(0L)
            val tax = (taxableBase * taxRate).roundToRupiah()
            val total = taxableBase + tax
            return Totals(subtotal, discountAmount, tax, total, discountCapped)
        }
    }
}
