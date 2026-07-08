package com.kasirku.pos.ui.screens.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.pos.data.local.entity.CartItemEntity
import com.kasirku.pos.data.local.entity.ProductEntity
import com.kasirku.pos.data.repository.CartRepository
import com.kasirku.pos.data.repository.ProductRepository
import com.kasirku.pos.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) productRepository.allProducts
            else productRepository.searchProducts(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cartItems: StateFlow<List<CartItemEntity>> = cartRepository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cartItemCount: StateFlow<Int> = cartRepository.cartItemCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val cartSubtotal: StateFlow<Long> = cartRepository.cartSubtotal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _taxPercent = MutableStateFlow(0.0)
    val taxPercent: StateFlow<Double> = _taxPercent.asStateFlow()

    private val _isCheckoutLoading = MutableStateFlow(false)
    val isCheckoutLoading: StateFlow<Boolean> = _isCheckoutLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PosUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: ProductEntity) {
        viewModelScope.launch {
            val currentInCart = cartRepository.getCartItemByProductId(product.id)
            val currentQty = currentInCart?.quantity ?: 0

            if (currentQty >= product.stock) {
                _uiEvent.emit(PosUiEvent.ShowError("Stok \${product.name} tidak cukup!"))
                return@launch
            }

            cartRepository.addToCart(product)
            _uiEvent.emit(PosUiEvent.ShowSuccess("\${product.name} ditambahkan"))
        }
    }

    fun updateCartItemQuantity(item: CartItemEntity, newQty: Int) {
        viewModelScope.launch {
            cartRepository.updateQuantity(item, newQty)
        }
    }

    fun removeCartItem(itemId: Long) {
        viewModelScope.launch {
            cartRepository.removeFromCart(itemId)
        }
    }

    fun setTaxPercent(percent: Double) {
        _taxPercent.value = percent.coerceIn(0.0, 100.0)
    }

    fun checkout(paymentAmount: Long) {
        viewModelScope.launch {
            _isCheckoutLoading.value = true
            try {
                val items = cartItems.value
                if (items.isEmpty()) {
                    _uiEvent.emit(PosUiEvent.ShowError("Keranjang kosong!"))
                    return@launch
                }

                val result = transactionRepository.checkout(
                    cartItems = items,
                    taxPercent = _taxPercent.value,
                    paymentAmount = paymentAmount
                )

                result.onSuccess { txId ->
                    cartRepository.clearCart()
                    _uiEvent.emit(PosUiEvent.CheckoutSuccess(txId))
                }.onFailure { error ->
                    _uiEvent.emit(PosUiEvent.ShowError(error.message ?: "Checkout gagal"))
                }
            } finally {
                _isCheckoutLoading.value = false
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clearCart()
            _uiEvent.emit(PosUiEvent.ShowSuccess("Keranjang dikosongkan"))
        }
    }
}

sealed class PosUiEvent {
    data class ShowSuccess(val message: String) : PosUiEvent()
    data class ShowError(val message: String) : PosUiEvent()
    data class CheckoutSuccess(val transactionId: Long) : PosUiEvent()
}
