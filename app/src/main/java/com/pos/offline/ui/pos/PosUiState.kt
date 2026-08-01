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

data class PosUiState(
    val catalog: CatalogState = CatalogState(),
    val cart: CartState = CartState(),
    val payment: PaymentState = PaymentState(),
    val checkout: CheckoutState = CheckoutState(),
    val shift: ShiftState = ShiftState(),
)

data class CatalogState(
    val products: List<ProductEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val cartQtyByProductId: Map<Long, Double> = emptyMap(),
    val stockByProductId: Map<Long, Double> = emptyMap(),
)

data class CartState(
    val items: List<CartItemEntity> = emptyList(),
    val totals: Totals = Totals(),
    val isEmpty: Boolean = true,
)

data class PaymentState(
    val method: PaymentMethod = PaymentMethod.CASH,
    val discountType: DiscountType = DiscountType.NOMINAL,
    val discountValue: Double = 0.0,
    val taxRate: Double = 0.0,
    val paid: Long = 0L,
    val change: Long = 0L,
    val changeGivenOverride: Long? = null,
    val changeGivenInCash: Boolean = true,
)

data class CheckoutState(
    val flow: CheckoutFlow = CheckoutFlow.Idle,
    val printUiState: PrintUiState = PrintUiState.Idle,
    val openDrawerOnPrint: Boolean = false,
    val isProcessing: Boolean = false,
)

sealed interface CheckoutFlow {
    data object Idle : CheckoutFlow

    data object Processing : CheckoutFlow

    data class Success(
        val result: CheckoutResult,
    ) : CheckoutFlow

    data class Error(
        val message: String,
    ) : CheckoutFlow
}

data class ShiftState(
    val activeShift: ShiftEntity? = null,
    val openShifts: List<ShiftEntity> = emptyList(),
    val activeCashiers: List<CashierEntity> = emptyList(),
    val shiftSummary: ShiftSummary? = null,
    val stockWarning: StockWarningInfo? = null,
    val showStartShiftDialog: Boolean = false,
    val showEndShiftDialog: Boolean = false,
    val showShiftListDialog: Boolean = false,
    val isStartingShift: Boolean = false,
    val isEndingShift: Boolean = false,
    val isOpeningDrawer: Boolean = false,
)

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

data class StockWarningInfo(
    val productName: String,
    val currentStock: Double,
)
