package com.pos.offline.data.local.entity

enum class PaymentMethod(
    val label: String,
) {
    CASH("Tunai"),
    QRIS("QRIS"),
    TRANSFER("Transfer Bank"),
    OTHER("Lainnya"),
    ;

    companion object {
        fun fromStorage(value: String): PaymentMethod = entries.find { it.name == value } ?: CASH
    }
}
