package com.pos.offline.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.repository.CartRepository
import com.pos.offline.data.repository.CashierRepository
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.InsufficientStockException
import com.pos.offline.data.repository.ProductRepository
import com.pos.offline.data.repository.ShiftRepository
import com.pos.offline.data.repository.ShiftSummary
import com.pos.offline.data.repository.TransactionRepository
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

/** Ringkasan kalkulasi keranjang (nilai murni, mudah di-unit-test). */
data class Totals(
    val subtotal: Long = 0L,
    val discount: Long = 0L,
    val tax: Long = 0L,
    val total: Long = 0L
)

/** Event UI sekali-jalan (bukan state) — cocok untuk Snackbar/Toast, tidak "nempel" saat rotasi. */
sealed interface PosUiEvent {
    data class ShowMessage(val message: String) : PosUiEvent
}

/** Status proses checkout (unidirectional: UI hanya membaca, VM yang menulis). */
sealed interface CheckoutState {
    data object Idle : CheckoutState
    data object Processing : CheckoutState
    data class Success(val result: CheckoutResult) : CheckoutState
    data class Error(val message: String) : CheckoutState
}

/**
 * ViewModel layar Kasir.
 *
 * BATCH 3D menambah state [paymentMethod] — dipilih via toggle di
 * TotalsSummary SEBELUM tombol Bayar ditekan, dibaca oleh [checkout] saat
 * dipanggil (bukan dikirim sebagai parameter dari Composable, konsisten
 * dengan pola atribusi shift/kasir di Batch 3C). QRIS di sini murni
 * PENCATATAN — tidak ada integrasi payment gateway sungguhan.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class, ExperimentalCoroutinesApi::class)
class PosViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository,
    private val cashierRepository: CashierRepository,
    private val shiftRepository: ShiftRepository
) : ViewModel() {

    // ---------- Input state (sesi, tidak di-persist) ----------
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _discount = MutableStateFlow(0L)
    val discount: StateFlow<Long> = _discount.asStateFlow()

    private val _taxRate = MutableStateFlow(0.0)
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    private val _paid = MutableStateFlow(0L)
    val paid: StateFlow<Long> = _paid.asStateFlow()

    // ---------- BATCH 3D: metode bayar ----------
    private val _paymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val paymentMethod: StateFlow<PaymentMethod> = _paymentMethod.asStateFlow()

    // ---------- Daftar produk (reaktif, debounce) ----------
    val products: StateFlow<List<ProductEntity>> = _searchQuery
        .debounce(180)
        .distinctUntilChanged()
        .flatMapLatest { productRepository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Keranjang (reaktif dari DB) ----------
    val cart: StateFlow<List<CartItemEntity>> = cartRepository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Total turunan ----------
    val totals: StateFlow<Totals> = combine(cart, _discount, _taxRate) { items, disc, rate ->
        computeTotals(items, disc, rate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Totals())

    // ---------- Event UI sekali-jalan ----------
    private val _uiEvents = MutableSharedFlow<PosUiEvent>(extraBufferCapacity = 4)
    val uiEvents: SharedFlow<PosUiEvent> = _uiEvents.asSharedFlow()

    // ---------- Status checkout ----------
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    // =====================================================================
    // Kasir & Shift (Batch 3C)
    // =====================================================================

    val activeCashiers: StateFlow<List<CashierEntity>> = cashierRepository.activeCashiers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openShift: StateFlow<ShiftEntity?> = shiftRepository.openShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showStartShiftDialog = MutableStateFlow(false)
    val showStartShiftDialog: StateFlow<Boolean> = _showStartShiftDialog.asStateFlow()

    private val _showEndShiftDialog = MutableStateFlow(false)
    val showEndShiftDialog: StateFlow<Boolean> = _showEndShiftDialog.asStateFlow()

    private val _shiftSummary = MutableStateFlow<ShiftSummary?>(null)
    val shiftSummary: StateFlow<ShiftSummary?> = _shiftSummary.asStateFlow()

    fun openStartShiftDialog() { _showStartShiftDialog.value = true }
    fun dismissStartShiftDialog() { _showStartShiftDialog.value = false }

    fun startShift(cashierId: Long, startingCash: Long) = viewModelScope.launch {
        val cashier = activeCashiers.value.find { it.id == cashierId } ?: return@launch
        shiftRepository.startShift(cashierId, cashier.name, startingCash)
        _showStartShiftDialog.value = false
        _uiEvents.emit(PosUiEvent.ShowMessage("Shift dimulai untuk ${cashier.name}."))
    }

    fun openEndShiftDialog() = viewModelScope.launch {
        val shift = openShift.value ?: return@launch
        _shiftSummary.value = shiftRepository.getShiftSummary(shift.id)
        _showEndShiftDialog.value = true
    }

    fun dismissEndShiftDialog() {
        _showEndShiftDialog.value = false
        _shiftSummary.value = null
    }

    fun endShift(actualCash: Long) = viewModelScope.launch {
        val shift = openShift.value ?: return@launch
        shiftRepository.endShift(shift.id, actualCash)
        _showEndShiftDialog.value = false
        _shiftSummary.value = null
        _uiEvents.emit(PosUiEvent.ShowMessage("Shift ditutup."))
    }

    // ---------- Aksi UI (keranjang) ----------
    fun search(q: String) { _searchQuery.value = q }
    fun setDiscount(value: Long) { _discount.value = value.coerceAtLeast(0L) }
    fun setTaxRate(rate: Double) { _taxRate.value = rate.coerceIn(0.0, 1.0) }
    fun setPaid(value: Long) { _paid.value = value.coerceAtLeast(0L) }

    /** BATCH 3D: dipanggil dari toggle Tunai/QRIS di TotalsSummary. */
    fun setPaymentMethod(method: PaymentMethod) { _paymentMethod.value = method }

    fun addToCart(product: ProductEntity) = viewModelScope.launch {
        val currentQtyInCart = cart.value.find { it.productId == product.id }?.quantity ?: 0
        if (currentQtyInCart + 1 > product.stock) {
            _uiEvents.emit(
                PosUiEvent.ShowMessage("Stok \"${product.name}\" tidak mencukupi (tersisa ${product.stock}).")
            )
            return@launch
        }
        cartRepository.add(product.id, product.name, product.price)
    }

    fun setQuantityDirect(item: CartItemEntity, newQuantity: Int) = viewModelScope.launch {
        if (newQuantity <= 0) {
            cartRepository.remove(item.productId)
            return@launch
        }
        val product = productRepository.getById(item.productId)
        val stock = product?.stock
        if (stock != null && newQuantity > stock) {
            _uiEvents.emit(
                PosUiEvent.ShowMessage("Stok \"${item.name}\" tidak mencukupi (tersisa $stock).")
            )
            return@launch
        }
        cartRepository.setQuantity(item.productId, newQuantity)
    }

    fun increaseQty(item: CartItemEntity) = viewModelScope.launch {
        val product = productRepository.getById(item.productId)
        val stock = product?.stock
        if (stock != null && item.quantity + 1 > stock) {
            _uiEvents.emit(
                PosUiEvent.ShowMessage("Stok \"${item.name}\" tidak mencukupi (tersisa $stock).")
            )
            return@launch
        }
        cartRepository.setQuantity(item.productId, item.quantity + 1)
    }

    fun decreaseQty(item: CartItemEntity) = viewModelScope.launch {
        cartRepository.setQuantity(item.productId, item.quantity - 1)
    }

    fun removeFromCart(item: CartItemEntity) = viewModelScope.launch {
        cartRepository.remove(item.productId)
    }

    fun clearCart() = viewModelScope.launch { cartRepository.clear() }

    /**
     * Jalankan checkout di background; update state untuk UI.
     *
     * BATCH 3D: [paymentMethod] kini dibaca dari state internal (hasil
     * toggle Tunai/QRIS), bukan lagi selalu CASH. Direset ke CASH setelah
     * sukses — sama seperti discount/paid — dengan asumsi mayoritas
     * transaksi berikutnya kemungkinan tunai lagi.
     */
    fun checkout() = viewModelScope.launch {
        val currentCart = cart.value
        if (currentCart.isEmpty()) return@launch

        _checkoutState.value = CheckoutState.Processing
        _checkoutState.value = try {
            val shift = openShift.value
            val result = transactionRepository.checkout(
                cart = currentCart,
                discount = _discount.value,
                taxRate = _taxRate.value,
                paid = _paid.value,
                paymentMethod = _paymentMethod.value,
                cashierId = shift?.cashierId,
                cashierName = shift?.cashierName ?: "",
                shiftId = shift?.id
            )
            _discount.value = 0L
            _paid.value = 0L
            _paymentMethod.value = PaymentMethod.CASH
            CheckoutState.Success(result)
        } catch (e: InsufficientStockException) {
            CheckoutState.Error("Stok '${e.productName}' tidak mencukupi.")
        } catch (e: Exception) {
            CheckoutState.Error("Gagal memproses: ${e.message ?: "kesalahan tak dikenal"}")
        }
    }

    fun resetCheckoutState() { _checkoutState.value = CheckoutState.Idle }

    companion object {
        fun computeTotals(items: List<CartItemEntity>, discount: Long, taxRate: Double): Totals {
            val subtotal = items.sumOf { it.unitPrice * it.quantity.toLong() }
            val discountAmount = discount.coerceIn(0L, subtotal)
            val taxableBase = (subtotal - discountAmount).coerceAtLeast(0L)
            val tax = (taxableBase * taxRate).toLong()
            val total = taxableBase + tax
            return Totals(subtotal, discountAmount, tax, total)
        }
    }
}