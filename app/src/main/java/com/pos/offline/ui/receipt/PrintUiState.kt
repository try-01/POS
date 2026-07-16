package com.pos.offline.ui.receipt

import com.pos.offline.data.repository.CheckoutResult
import com.pos.offline.util.ReceiptPrintOutcome

/**
 * State cetak struk thermal (H7), sengaja TERPISAH dari `CheckoutState` (PosViewModel) —
 * checkout adalah transaksi inti (stok berkurang, data tersimpan permanen), sedangkan print
 * hanyalah side-effect yang boleh gagal tanpa mempengaruhi validitas transaksi yang sudah tercatat.
 *
 * Ditampilkan sebagai banner PERSISTEN di dalam dialog (SuccessDialog / TransactionDetailDialog),
 * BUKAN Snackbar sekali-tampil — Snackbar milik Scaffold tidak reliable terlihat/ter-tap selama
 * AlertDialog (window terpisah) masih terbuka, padahal dialog kita sengaja tidak auto-close
 * setelah tombol "Cetak" ditekan.
 *
 * `checkoutResult` disertakan di SETIAP varian (bukan cuma Result) supaya UI bisa memfilter
 * status ini hanya untuk transaksi yang sedang ditampilkan (lihat [forTransaction]) — mencegah
 * status cetak transaksi A "bocor" ditampilkan saat user sudah pindah melihat transaksi B.
 */
sealed interface PrintUiState {
    data object Idle : PrintUiState
    data class Printing(val checkoutResult: CheckoutResult) : PrintUiState
    data class Result(val outcome: ReceiptPrintOutcome, val checkoutResult: CheckoutResult) : PrintUiState
}

/** Filter state ini supaya hanya relevan untuk [transactionId] tertentu, selain itu jadi Idle. */
fun PrintUiState.forTransaction(transactionId: String): PrintUiState = when (this) {
    is PrintUiState.Printing -> if (checkoutResult.transaction.id == transactionId) this else PrintUiState.Idle
    is PrintUiState.Result -> if (checkoutResult.transaction.id == transactionId) this else PrintUiState.Idle
    PrintUiState.Idle -> PrintUiState.Idle
}