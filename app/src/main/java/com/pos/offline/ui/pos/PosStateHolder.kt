package com.pos.offline.ui.pos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pos.offline.data.local.entity.CartItemEntity

class PosLocalStateHolder {
    var isCartExpanded by mutableStateOf(false)
        private set

    fun toggleCart() {
        isCartExpanded = !isCartExpanded
    }

    fun updateCartExpanded(expanded: Boolean) {
        isCartExpanded = expanded
    }

    var showClearConfirm by mutableStateOf(false)
        private set

    fun showClearDialog() {
        showClearConfirm = true
    }

    fun dismissClearDialog() {
        showClearConfirm = false
    }

    var qtyEditItem by mutableStateOf<CartItemEntity?>(null)
        private set

    fun startQtyEdit(item: CartItemEntity) {
        qtyEditItem = item
    }

    fun dismissQtyEdit() {
        qtyEditItem = null
    }

    var showInsufficientPaymentDialog by mutableStateOf(false)
        private set

    fun showInsufficientPayment() {
        showInsufficientPaymentDialog = true
    }

    fun dismissInsufficientPayment() {
        showInsufficientPaymentDialog = false
    }
}

@Composable
fun rememberPosLocalState(): PosLocalStateHolder = remember { PosLocalStateHolder() }
