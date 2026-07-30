package com.pos.offline.ui.pos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pos.offline.data.local.entity.CartItemEntity

/**
 * Menyimpan ephemeral UI state yang hanya relevan di level composable,
 * tidak perlu survive config change, dan tidak perlu dikonsumsi file lain.
 *
 * Dipisah dari [PosUiState] agar ViewModel tetap bersih dari concern UI murni.
 */
class PosLocalStateHolder {

    // ── Layout ────────────────────────────────────────────────────────────────

    /** Status expand/collapse CartPane di narrow layout */
    var isCartExpanded by mutableStateOf(false)
        private set

    fun toggleCart() {
        isCartExpanded = !isCartExpanded
    }

    fun setCartExpanded(expanded: Boolean) {
        isCartExpanded = expanded
    }

    // ── Cart dialogs ──────────────────────────────────────────────────────────

    /** Dialog konfirmasi "Kosongkan Keranjang?" */
    var showClearConfirm by mutableStateOf(false)
        private set

    fun showClearDialog() { showClearConfirm = true }
    fun dismissClearDialog() { showClearConfirm = false }

    /** Item yang sedang diedit jumlahnya */
    var qtyEditItem by mutableStateOf<CartItemEntity?>(null)
        private set

    fun startQtyEdit(item: CartItemEntity) { qtyEditItem = item }
    fun dismissQtyEdit() { qtyEditItem = null }

    /** Dialog peringatan pembayaran kurang */
    var showInsufficientPaymentDialog by mutableStateOf(false)
        private set

    fun showInsufficientPayment() { showInsufficientPaymentDialog = true }
    fun dismissInsufficientPayment() { showInsufficientPaymentDialog = false }
}

@Composable
fun rememberPosLocalState(): PosLocalStateHolder = remember { PosLocalStateHolder() }