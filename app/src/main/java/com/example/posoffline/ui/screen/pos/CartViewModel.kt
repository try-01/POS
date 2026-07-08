package com.example.posoffline.ui.screen.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.posoffline.data.entity.ProductEntity
import com.example.posoffline.data.repository.TransactionRepository
import com.example.posoffline.domain.model.CartLine
import com.example.posoffline.domain.model.PaymentMethod
import com.example.posoffline.domain.model.Totals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the shopping cart state and pricing math.
 *
 * Architecture notes:
 *  - State is exposed as [StateFlow] — Compose's `collectAsStateWithLifecycle`
 *    is the Compose-side equivalent of Flow.collect.
 *  - All money math is in integer rupiah. The View layer only formats.
 *  - The cart is held in a single [StateFlow] so the entire UI is consistent:
 *    a +/− on a line cannot race a "set discount" event.
 */
class CartViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    data class CartState(
        val lines: List<CartLine> = emptyList(),
        val cartDiscount: Long = 0L,
        val taxRate: Double = 0.11
    )

    data class CartUiState(
        val state: CartState = CartState(),
        val totals: Totals = Totals(0, 0, 0, 0),
        val itemCount: Int = 0,
        val isCheckingOut: Boolean = false
    )

    private val _cart = MutableStateFlow(CartState())
    val cart: StateFlow<CartState> = _cart.asStateFlow()

    /** Combined UI state. `combine` recomputes totals whenever any input changes. */
    val ui: StateFlow<CartUiState> =
        combine(_cart, MutableStateFlow(0L)) { c, _ ->
            val totals = transactionRepository.computeTotals(c.lines, c.cartDiscount, c.taxRate)
            val itemCount = c.lines.sumOf { it.qty }
            CartUiState(c, totals, itemCount, isCheckingOut = false)
        }.stateIn(
            scope = viewModelScope,
            // WhileSubscribed(5_000) keeps the StateFlow alive across short
            // configuration changes (rotation) without leaking.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CartUiState()
        )

    /* -------------------- mutations -------------------- */

    fun addProduct(p: ProductEntity, qty: Int = 1) {
        _cart.update { c ->
            val idx = c.lines.indexOfFirst { it.productId == p.id }
            val next = c.lines.toMutableList()
            if (idx >= 0) {
                val cur = next[idx]
                if (cur.qty + qty > p.stock) return@update c
                next[idx] = cur.copy(qty = cur.qty + qty)
            } else {
                if (qty > p.stock) return@update c
                next.add(
                    CartLine(
                        productId = p.id,
                        name = p.name,
                        price = p.price,
                        qty = qty,
                        discount = 0L
                    )
                )
            }
            c.copy(lines = next)
        }
    }

    fun incQty(productId: String, stockCap: Int) {
        _cart.update { c ->
            c.copy(
                lines = c.lines.map {
                    if (it.productId == productId && it.qty < stockCap)
                        it.copy(qty = it.qty + 1) else it
                }
            )
        }
    }

    fun decQty(productId: String) {
        _cart.update { c ->
            c.copy(
                lines = c.lines
                    .map { if (it.productId == productId) it.copy(qty = it.qty - 1) else it }
                    .filter { it.qty > 0 }
            )
        }
    }

    fun setQty(productId: String, qty: Int, stockCap: Int) {
        _cart.update { c ->
            c.copy(
                lines = c.lines
                    .map {
                        if (it.productId == productId)
                            it.copy(qty = qty.coerceIn(0, stockCap)) else it
                    }
                    .filter { it.qty > 0 }
            )
        }
    }

    fun setLineDiscount(productId: String, discount: Long) {
        _cart.update { c ->
            c.copy(
                lines = c.lines.map {
                    if (it.productId == productId)
                        it.copy(discount = discount.coerceAtLeast(0L)) else it
                }
            )
        }
    }

    fun removeLine(productId: String) {
        _cart.update { c -> c.copy(lines = c.lines.filter { it.productId != productId }) }
    }

    fun setCartDiscount(d: Long) {
        _cart.update { it.copy(cartDiscount = d.coerceAtLeast(0L)) }
    }

    fun setTaxRate(r: Double) {
        _cart.update { it.copy(taxRate = r.coerceAtLeast(0.0)) }
    }

    fun clear() {
        _cart.update { it.copy(lines = emptyList(), cartDiscount = 0L) }
    }

    /* -------------------- checkout -------------------- */

    fun checkout(
        paid: Long,
        method: PaymentMethod,
        onResult: (Result<com.example.posoffline.data.entity.TransactionEntity>) -> Unit
    ) {
        val snapshot = _cart.value
        if (snapshot.lines.isEmpty()) {
            onResult(Result.failure(IllegalStateException("Keranjang kosong")))
            return
        }
        viewModelScope.launch {
            try {
                val tx = transactionRepository.checkout(
                    lines = snapshot.lines,
                    cartDiscount = snapshot.cartDiscount,
                    paid = paid,
                    paymentMethod = method
                )
                clear()
                onResult(Result.success(tx))
            } catch (t: Throwable) {
                onResult(Result.failure(t))
            }
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CartViewModel::class.java))
            return CartViewModel(transactionRepository) as T
        }
    }
}
