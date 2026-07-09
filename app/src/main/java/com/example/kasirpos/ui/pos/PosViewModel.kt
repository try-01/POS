package com.example.kasirpos.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kasirpos.data.local.dao.CartWithProduct
import com.example.kasirpos.data.local.entity.ProductEntity
import com.example.kasirpos.data.local.entity.TransactionEntity
import com.example.kasirpos.data.repository.CartRepository
import com.example.kasirpos.data.repository.ProductRepository
import com.example.kasirpos.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── State Holder (immutable UI state) ──────────────────────────

data class PosUiState(
    val cartItems: List<CartWithProduct> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<ProductEntity> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val taxPercentage: Int = 0,       // 0 = tidak ada pajak
    val paymentMethod: String = "cash",
    val cashReceived: Long = 0,
    val showPaymentDialog: Boolean = false,
    val lastTransaction: TransactionEntity? = null,
    val showReceiptDialog: Boolean = false,
    val error: String? = null
) {
    // ── Derived properties (kalkulasi di state holder, sekali evaluasi) ──

    /** Subtotal = Σ (unitPrice × quantity) semua item */
    val subtotal: Long
        get() = cartItems.sumOf { it.unitPrice * it.quantity }

    /** Total diskon = Σ (discount) semua item */
    val totalDiscount: Long
        get() = cartItems.sumOf { it.discount }

    /** Setelah diskon (tidak boleh negatif) */
    val afterDiscount: Long
        get() = (subtotal - totalDiscount).coerceAtLeast(0)

    /** Nominal pajak */
    val taxAmount: Long
        get() = (afterDiscount * taxPercentage) / 100

    /** Grand total = setelah diskon + pajak */
    val grandTotal: Long
        get() = afterDiscount + taxAmount

    /** Kembalian = uang tunai diterima - grand total (≥ 0) */
    val change: Long
        get() = (cashReceived - grandTotal).coerceAtLeast(0)

    val totalItems: Int
        get() = cartItems.sumOf { it.quantity }

    val canCheckout: Boolean
        get() = cartItems.isNotEmpty()
}

// ── Events (one-shot) ──────────────────────────────────────────

sealed interface PosEvent {
    data class ShowSnackbar(val message: String) : PosEvent
    data class NavigateToReceipt(val transactionId: Long) : PosEvent
}

// ── ViewModel ──────────────────────────────────────────────────

class PosViewModel(
    private val productRepo: ProductRepository,
    private val cartRepo: CartRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    // UI state — StateFlow untuk lifecycle-aware observation
    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    // One-shot events — gunakan extraBufferCapacity agar emit tidak suspend
    private val _events = MutableSharedFlow<PosEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<PosEvent> = _events.asSharedFlow()

    // Job untuk membatalkan pencarian sebelumnya (cegah flow leak)
    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        observeCart()
    }

    // ── Observasi keranjang ────────────────────────────────────

    private fun observeCart() {
        viewModelScope.launch {
            cartRepo.cartWithProducts.collect { items ->
                _uiState.update { it.copy(
                    cartItems = items,
                    isLoading = false
                )}
            }
        }
    }

    // ── Pencarian produk ───────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }

        // Batalkan pencarian sebelumnya untuk mencegah flow leak
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            productRepo.searchByName(query).collect { results ->
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    // ── Aksi keranjang ─────────────────────────────────────────

    fun addToCart(product: ProductEntity) {
        viewModelScope.launch {
            if (product.stock <= 0) {
                _events.emit(PosEvent.ShowSnackbar("Stok ${product.name} habis"))
                return@launch
            }

            // Cek apakah produk SUDAH di keranjang — hitung total qty setelah increment
            val existingInCart = _uiState.value.cartItems.find { it.productId == product.id }
            if (existingInCart != null && existingInCart.quantity >= product.stock) {
                _events.emit(PosEvent.ShowSnackbar(
                    "Stok ${product.name} hanya ${product.stock} — sudah maksimal di keranjang"
                ))
                return@launch
            }

            cartRepo.addToCart(product.id, product.price)
            clearSearch()
        }
    }

    fun removeFromCart(productId: Long) {
        viewModelScope.launch {
            cartRepo.removeFromCart(productId)
        }
    }

    fun updateQuantity(productId: Long, newQty: Int) {
        viewModelScope.launch {
            if (newQty <= 0) {
                cartRepo.removeFromCart(productId)
            } else {
                // Mapping: cartRepo.updateQuantity menggunakan cartItemId,
                // tapi di UI kita gunakan productId. Cari dulu item by productId.
                cartRepo.updateQuantity(productId, newQty)
            }
        }
    }

    fun updateDiscount(productId: Long, discount: Long) {
        viewModelScope.launch {
            cartRepo.updateDiscount(productId, discount)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepo.clearCart()
        }
    }

    // ── Setter untuk payment dialog ────────────────────────────

    fun setTaxPercentage(value: Int) {
        _uiState.update { it.copy(taxPercentage = value.coerceIn(0, 100)) }
    }

    fun setCashReceived(value: Long) {
        _uiState.update { it.copy(cashReceived = value.coerceAtLeast(0)) }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun showPaymentDialog() {
        // Auto-fill cashReceived dengan grandTotal sebagai default
        _uiState.update { it.copy(
            showPaymentDialog = true,
            cashReceived = it.grandTotal
        )}
    }

    fun dismissPaymentDialog() {
        _uiState.update { it.copy(showPaymentDialog = false) }
    }

    fun dismissReceiptDialog() {
        _uiState.update { it.copy(showReceiptDialog = false) }
    }

    // ── Checkout ───────────────────────────────────────────────

    fun checkout() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.cashReceived < state.grandTotal) {
                _events.emit(PosEvent.ShowSnackbar("Uang tunai kurang Rp ${state.grandTotal - state.cashReceived}"))
                return@launch
            }
            try {
                val transaction = transactionRepo.checkout(
                    taxPercentage = state.taxPercentage,
                    paymentMethod = state.paymentMethod,
                    cashReceived = state.cashReceived
                )
                _uiState.update { it.copy(
                    showPaymentDialog = false,
                    lastTransaction = transaction,
                    showReceiptDialog = true,
                    cashReceived = 0,
                    taxPercentage = 0
                )}
                _events.emit(PosEvent.ShowSnackbar("Transaksi berhasil! Total: Rp ${transaction.grandTotal}"))
            } catch (e: Exception) {
                _events.emit(PosEvent.ShowSnackbar("Checkout gagal: ${e.message}"))
            }
        }
    }

    // ── Factory ────────────────────────────────────────────────

    class Factory(
        private val productRepo: ProductRepository,
        private val cartRepo: CartRepository,
        private val transactionRepo: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PosViewModel(productRepo, cartRepo, transactionRepo) as T
        }
    }
}
