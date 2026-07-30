package com.pos.offline.ui.pos

import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.local.entity.DiscountType
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.data.repository.ShiftSummary
import com.pos.offline.ui.receipt.PrintUiState

// ─── Root UI State ────────────────────────────────────────────────────────────

data class PosUiState(
    val catalog: CatalogState = CatalogState(),
    val cart: CartState = CartState(),
    val payment: PaymentState = PaymentState(),
    val checkout: CheckoutState = CheckoutState(),
    val shift: ShiftState = ShiftState(),
)

// ─── Catalog ──────────────────────────────────────────────────────────────────

data class CatalogState(
    val products: List<ProductEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    /** productId → qty di keranjang, untuk hitung sisa stok di ProductCard */
    val cartQtyByProductId: Map<Long, Double> = emptyMap(),
    /** productId → stok aktual, untuk validasi increase di CartRow */
    val stockByProductId: Map<Long, Double> = emptyMap(),
)

// ─── Cart ─────────────────────────────────────────────────────────────────────

data class CartState(
    val items: List<CartItemEntity> = emptyList(),
    val totals: Totals = Totals(),
    val isEmpty: Boolean = true,
)

// ─── Payment ──────────────────────────────────────────────────────────────────

data class PaymentState(
    val method: PaymentMethod = PaymentMethod.CASH,
    val discountType: DiscountType = DiscountType.NOMINAL,
    val discountValue: Double = 0.0,
    val taxRate: Double = 0.0,
    val paid: Long = 0L,
    /** derived: paid - totals.total, bisa negatif (kurang bayar) */
    val change: Long = 0L,
    val changeGivenOverride: Long? = null,
    val changeGivenInCash: Boolean = true,
)

// ─── Checkout ─────────────────────────────────────────────────────────────────

data class CheckoutState(
    val flow: CheckoutFlow = CheckoutFlow.Idle,
    val printUiState: PrintUiState = PrintUiState.Idle,
    val openDrawerOnPrint: Boolean = false,
    /** derived: apakah sedang processing checkout */
    val isProcessing: Boolean = false,
)

sealed interface CheckoutFlow {
    data object Idle : CheckoutFlow
    data object Processing : CheckoutFlow
    data class Success(val result: CheckoutResult) : CheckoutFlow
    data class Error(val message: String) : CheckoutFlow
}

// ─── Shift ────────────────────────────────────────────────────────────────────

data class ShiftState(
    val activeShift: ShiftEntity? = null,
    val openShifts: List<ShiftEntity> = emptyList(),
    val activeCashiers: List<CashierEntity> = emptyList(),
    val shiftSummary: ShiftSummary? = null,
    val stockWarning: StockWarningInfo? = null,
    // dialog visibility
    val showStartShiftDialog: Boolean = false,
    val showEndShiftDialog: Boolean = false,
    val showShiftListDialog: Boolean = false,
    // processing flags
    val isStartingShift: Boolean = false,
    val isEndingShift: Boolean = false,
    val isOpeningDrawer: Boolean = false,
)

// ─── Shared models (tetap di file ini agar satu paket) ───────────────────────

data class Totals(
    val subtotal: Long = 0L,
    val discount: Long = 0L,
    val tax: Long = 0L,
    val total: Long = 0L,
    val discountCapped: Boolean = false,
)

sealed interface PosUiEvent {
    data class ShowMessage(val message: String) : PosUiEvent
}

data class StockWarningInfo(
    val productName: String,
    val currentStock: Double,
)