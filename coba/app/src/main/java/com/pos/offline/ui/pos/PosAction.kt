package com.pos.offline.ui.pos
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.repository.CheckoutResult
sealed interface PosAction {
    data class Search(
        val query: String,
    ) : PosAction
    data class SelectCategory(
        val category: String?,
    ) : PosAction
    data class AddToCart(
        val product: ProductEntity,
    ) : PosAction
    data class IncreaseQty(
        val item: CartItemEntity,
    ) : PosAction
    data class DecreaseQty(
        val item: CartItemEntity,
    ) : PosAction
    data class SetQuantity(
        val item: CartItemEntity,
        val qty: Double,
    ) : PosAction
    data class RemoveFromCart(
        val item: CartItemEntity,
    ) : PosAction
    data object ClearCart : PosAction
    data object ToggleDiscountType : PosAction
    data class SetDiscountValue(
        val value: Double,
    ) : PosAction
    data class SetTaxRate(
        val rate: Double,
    ) : PosAction
    data class SetPaid(
        val amount: Long,
    ) : PosAction
    data class SetChangeGivenOverride(
        val value: Long?,
    ) : PosAction
    data class SetChangeGivenInCash(
        val value: Boolean,
    ) : PosAction
    data class SetPaymentMethod(
        val method: PaymentMethod,
    ) : PosAction
    data object Checkout : PosAction
    data object ResetCheckout : PosAction
    data class PrintReceipt(
        val result: CheckoutResult,
    ) : PosAction
    data class ToggleOpenDrawer(
        val enabled: Boolean,
    ) : PosAction
    data object OpenStartShiftDialog : PosAction
    data object DismissStartShiftDialog : PosAction
    data object OpenShiftListDialog : PosAction
    data object DismissShiftListDialog : PosAction
    data class OpenEndShiftDialog(
        val shift: ShiftEntity,
    ) : PosAction
    data object DismissEndShiftDialog : PosAction
    data class StartShift(
        val cashierId: Long,
        val startingCash: Long,
    ) : PosAction
    data class EndShift(
        val actualCash: Long,
    ) : PosAction
    data class SelectActiveShift(
        val shiftId: Long,
    ) : PosAction
    data object OpenCashDrawer : PosAction
    data object DismissStockWarning : PosAction
}
