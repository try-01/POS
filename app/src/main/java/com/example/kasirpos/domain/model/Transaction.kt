package com.example.kasirpos.domain.model

data class Transaction(
    val id: Long,
    val subtotal: Long,
    val totalDiscount: Long,
    val taxAmount: Long,
    val grandTotal: Long,
    val paymentMethod: String,
    val cashReceived: Long,
    val change: Long,
    val createdAt: Long
)
