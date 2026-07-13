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
import com.pos.offline.data.repository.ProductRepository
import com.pos.offline.data.repository.ShiftRepository
import com.pos.offline.data.repository.ShiftSummary
import com.pos.offline.data.repository.TransactionRepository
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

/**
 * Ringkasan kalkulasi keranjang (nilai murni, mudah di-unit-test).
 *
 * [discountCapped] = true kalau nilai diskon mentah (terutama mode NOMINAL)
 * melebihi [subtotal] sehingga dipangkas — dipakai UI untuk menampilkan
 * peringatan inline non-blocking di bawah field diskon.
 */
data class Totals(
    val subtotal: Long = 0L,
    val discount: Long = 0L,
    val tax: Long = 0L,
    val total: Long = 0L,
    val discountCapped: Boolean = false
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
 *
 * BATCH DISKON % : [discount] Long lama diganti [discountType]+[discountValue]
 * — nilai MENTAH yang diketik kasir (bukan hasil konversi). Konversi ke
 * nominal final dilakukan di [computeTotals] (untuk tampilan real-time) dan
 * di [TransactionRepository.checkout] (untuk persist) — DUA tempat ini WAJIB
 * pakai rumus yang identik, lihat komentar masing-masing.
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

    // ---------- Diskon: tipe (Nominal/Persen) + nilai mentah ----------
    private val _discountType = MutableStateFlow(DiscountType.NOMINAL)
    val discountType: StateFlow<DiscountType> = _discountType.asStateFlow()

    private val _discountValue = MutableStateFlow(0.0)
    val discountValue: StateFlow<Double> = _discountValue.asStateFlow()

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
    val totals: StateFlow<Totals> = combine(
        cart, _discountType, _discountValue, _taxRate
    ) { items, discType, discValue, rate ->
        computeTotals(items, discType, discValue, rate)
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

    /**
     * Set nilai diskon MENTAH sesuai tipe yang sedang aktif.
     * NOMINAL: hanya dibatasi >= 0 (pemangkasan ke subtotal terjadi saat
     * kalkulasi total, lihat [Totals.discountCapped] untuk peringatan UI).
     * PERCENT: dibatasi 0..100 (diskon > 100% tidak masuk akal secara bisnis).
     */
    fun setDiscountValue(raw: Double) {
        _discountValue.value = when (_discountType.value) {
            DiscountType.NOMINAL -> raw.coerceAtLeast(0.0)
            DiscountType.PERCENT -> raw.coerceIn(0.0, 100.0)
        }
    }

    /**
     * Balik tipe diskon (Nominal <-> Persen) dan RESET nilai ke 0.
     * Reset disengaja: angka yang sama (mis. "50") punya arti yang jauh
     * berbeda antara Rp 50 dan 50% — mencegah salah tafsir tak sengaja.
     */
    fun toggleDiscountType() {
        _discountType.value = if (_discountType.value == DiscountType.NOMINAL) {
            DiscountType.PERCENT
        } else {
            DiscountType.NOMINAL
        }
        _discountValue.value = 0.0
    }

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
     *
     * BATCH DISKON %: [discountType]/[discountValue] dikirim mentah ke
     * repository — konversi ke nominal final dilakukan DI SANA (bukan di
     * sini), supaya ada satu tempat tunggal yang menentukan nominal final
     * yang benar-benar disimpan.
     */
    fun checkout() = viewModelScope.launch {
        val currentCart = cart.value
        if (currentCart.isEmpty()) return@launch

        _checkoutState.value = CheckoutState.Processing
        _checkoutState.value = try {
            val shift = openShift.value
            val currentTotal = totals.value.total
            val effectivePaid = if (_paid.value <= 0L) currentTotal else _paid.value
            val result = transactionRepository.checkout(
                cart = currentCart,
                discountType = _discountType.value,
                discountValue = _discountValue.value,
                taxRate = _taxRate.value,
                paid = effectivePaid,
                paymentMethod = _paymentMethod.value,
                cashierId = shift?.cashierId,
                cashierName = shift?.cashierName ?: "",
                shiftId = shift?.id
            )
            _discountType.value = DiscountType.NOMINAL
            _discountValue.value = 0.0
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
        /**
         * Kalkulasi total real-time untuk tampilan UI.
         *
         * URUTAN (sesuai aturan PPN): subtotal (bruto) → diskon → DPP →
         * pajak dari DPP → total. HARUS identik dengan rumus di
         * [TransactionRepository.checkout] — kalau salah satu diubah,
         * yang satu lagi wajib ikut diubah.
         */
        fun computeTotals(
            items: List<CartItemEntity>,
            discountType: DiscountType,
            discountValue: Double,
            taxRate: Double
        ): Totals {
            val subtotal = items.sumOf { it.unitPrice * it.quantity.toLong() }

            val rawDiscountAmount = (when (discountType) {
                DiscountType.NOMINAL -> discountValue.roundToRupiah()
                DiscountType.PERCENT -> (subtotal * (discountValue / 100.0)).roundToRupiah()
            }).coerceAtLeast(0L)
            val discountAmount = rawDiscountAmount.coerceAtMost(subtotal)
            val discountCapped = rawDiscountAmount > subtotal && subtotal > 0L

            val taxableBase = (subtotal - discountAmount).coerceAtLeast(0L) // DPP
            val tax = (taxableBase * taxRate).roundToRupiah()
            val total = taxableBase + tax

            return Totals(subtotal, discountAmount, tax, total, discountCapped)
        }
    }
}