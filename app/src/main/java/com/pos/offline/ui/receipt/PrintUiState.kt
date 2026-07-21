package com.pos.offline.ui.receipt

import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.util.ReceiptPrintOutcome

sealed interface PrintUiState {
    data object Idle : PrintUiState

    data class Printing(
        val checkoutResult: CheckoutResult,
    ) : PrintUiState

    data class Result(
        val outcome: ReceiptPrintOutcome,
        val checkoutResult: CheckoutResult,
    ) : PrintUiState
}

fun PrintUiState.forTransaction(transactionId: String): PrintUiState =
    when (this) {
        is PrintUiState.Printing -> if (checkoutResult.transaction.id == transactionId) this else PrintUiState.Idle
        is PrintUiState.Result -> if (checkoutResult.transaction.id == transactionId) this else PrintUiState.Idle
        PrintUiState.Idle -> PrintUiState.Idle
    }
