package com.example.kasirpos.domain.model

data class CartItem(
    val productId: Long,
    val productName: String,
    val productSku: String,
    val quantity: Int,
    val unitPrice: Long,
    val discount: Long
) {
    val effectivePrice: Long get() = (unitPrice - discount).coerceAtLeast(0)
    val lineTotal: Long get() = effectivePrice * quantity
}
