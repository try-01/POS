package com.kasirku.pos.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.pos.data.local.entity.ProductEntity
import com.kasirku.pos.data.repository.CartRepository
import com.kasirku.pos.data.repository.CheckoutResult
import com.kasirku.pos.data.repository.ProductRepository
import com.kasirku.pos.data.repository.TransactionRepository
import com.kasirku.pos.domain.model.CartItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Rincian kalkulasi total belanja, dipisah agar mudah ditampilkan baris per baris di UI. */
data class CartTotals(
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double = 0.0
)

/** State tunggal yang dikonsumsi Compose UI — satu sumber kebenaran (single source of truth) untuk layar Kasir. */
data class PosUiState(
    val products: List<ProductEntity> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val totals: CartTotals = CartTotals(),
    val isProcessingCheckout: Boolean = false
)

/** Event sekali-jalan (one-shot) untuk notifikasi hasil checkout, dikirim lewat SharedFlow agar tidak "replay" saat rotasi layar. */
sealed class CheckoutEvent {
    data class Success(val invoiceNumber: String) : CheckoutEvent()
    data class Failed(val reason: String) : CheckoutEvent()
}

@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    // Contoh sederhana: diskon & pajak berupa persentase global.
    // Bisa dikembangkan lebih lanjut menjadi diskon per-item jika diperlukan.
    private val discountPercent = MutableStateFlow(0.0)
    private val taxPercent = MutableStateFlow(10.0) // contoh default PPN 10%

    private val _checkoutEvent = MutableSharedFlow<CheckoutEvent>()
    val checkoutEvent = _checkoutEvent.asSharedFlow()

    /**
     * `debounce(300)` mencegah query Room dijalankan pada SETIAP huruf yang diketik user;
     * query baru hanya dieksekusi 300ms setelah user berhenti mengetik.
     * `flatMapLatest` otomatis membatalkan (cancel) query pencarian sebelumnya jika ada
     * query baru masuk — mencegah race condition & pemborosan resource CPU.
     */
    private val productsFlow = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query -> productRepository.observeProducts(query) }

    /**
     * KALKULASI TOTAL BELANJA (reaktif, dihitung ulang otomatis tiap kali salah satu
     * input berubah — isi keranjang, persen diskon, atau persen pajak):
     *
     *   1. subtotal      = Σ (harga_item × qty_item) untuk semua baris di keranjang
     *   2. discount      = subtotal × (persenDiskon / 100)
     *   3. taxableAmount = subtotal − discount   (dasar pengenaan pajak, setelah diskon)
     *   4. tax           = taxableAmount × (persenPajak / 100)
     *   5. grandTotal    = taxableAmount + tax   (nominal akhir yang harus dibayar pelanggan)
     */
    private val totalsFlow = combine(
        cartRepository.cartItems, discountPercent, taxPercent
    ) { items, discPct, taxPct ->
        val subtotal = items.sumOf { it.subtotal }
        val discount = subtotal * (discPct / 100.0)
        val taxableAmount = (subtotal - discount).coerceAtLeast(0.0)
        val tax = taxableAmount * (taxPct / 100.0)
        val grandTotal = taxableAmount + tax
        CartTotals(subtotal = subtotal, discount = discount, tax = tax, grandTotal = grandTotal)
    }

    val uiState: StateFlow<PosUiState> = combine(
        productsFlow, cartRepository.cartItems, searchQuery, totalsFlow
    ) { products, cart, query, totals ->
        PosUiState(products = products, cartItems = cart, searchQuery = query, totals = totals)
    }.stateIn(
        scope = viewModelScope,
        // Flow berhenti berjalan 5 detik setelah tidak ada lagi observer (mis. layar di-background),
        // lalu otomatis restart saat observer muncul kembali -> tidak ada resource yang terbuang sia-sia.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PosUiState()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun addToCart(product: ProductEntity) = cartRepository.addProduct(product)

    fun increaseQuantity(item: CartItem) =
        cartRepository.updateQuantity(item.productId, item.quantity + 1)

    fun decreaseQuantity(item: CartItem) =
        cartRepository.updateQuantity(item.productId, item.quantity - 1)

    fun removeFromCart(productId: Long) = cartRepository.removeItem(productId)

    fun setDiscountPercent(value: Double) {
        discountPercent.value = value.coerceIn(0.0, 100.0)
    }

    fun setTaxPercent(value: Double) {
        taxPercent.value = value.coerceIn(0.0, 100.0)
    }

    /** Eksekusi checkout: dijalankan di background (viewModelScope -> Dispatchers.IO via Repository). */
    fun checkout(paidAmount: Double, paymentMethod: String = "CASH") {
        val items = cartRepository.currentItems()
        if (items.isEmpty()) return

        viewModelScope.launch {
            val totals = totalsFlow.first()
            val result = transactionRepository.checkout(
                cartItems = items,
                discountAmount = totals.discount,
                taxAmount = totals.tax,
                paidAmount = paidAmount,
                paymentMethod = paymentMethod
            )
            when (result) {
                is CheckoutResult.Success -> {
                    cartRepository.clear()
                    _checkoutEvent.emit(CheckoutEvent.Success(result.invoiceNumber))
                }
                is CheckoutResult.InsufficientStock ->
                    _checkoutEvent.emit(CheckoutEvent.Failed("Stok \"${result.productName}\" tidak mencukupi"))
                is CheckoutResult.Error ->
                    _checkoutEvent.emit(CheckoutEvent.Failed(result.message))
            }
        }
    }
}
