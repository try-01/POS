package com.example.posoffline.domain.model

/** One line in the shopping cart. */
data class CartLine(
    val productId: String,
    val name: String,
    val price: Long, // rupiah, integer
    val qty: Int,
    val discount: Long // per-line discount in rupiah
)

/** Derived totals. Computed in the ViewModel, displayed by the View. */
data class Totals(
    val subtotal: Long,
    val discount: Long,
    val tax: Long,
    val grandTotal: Long
)

/** Payment methods accepted by the POS. */
enum class PaymentMethod { CASH, QRIS, CARD }
