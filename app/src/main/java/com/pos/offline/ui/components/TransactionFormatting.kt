package com.pos.offline.ui.components

import com.pos.offline.data.local.entity.DiscountType
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.util.toRupiah

fun paymentMethodLabel(raw: String): String =
    when (raw) {
        PaymentMethod.CASH.name -> "Tunai"
        PaymentMethod.QRIS.name -> "QRIS"
        PaymentMethod.TRANSFER.name -> "Transfer"
        PaymentMethod.OTHER.name -> "Lainnya"
        else -> raw
    }

fun formatPercentTrim(value: Double): String {
    val i = value.toLong()
    return if (value == i.toDouble()) i.toString() else value.toString()
}

fun TransactionEntity.discountRowLabel(): String? {
    if (discount <= 0L) return null
    return if (discountType == DiscountType.PERCENT.name) {
        "Diskon (${formatPercentTrim(discountValue)}%)"
    } else {
        "Diskon"
    }
}

fun TransactionEntity.discountInlineLabel(): String? {
    if (discount <= 0L) return null
    return if (discountType == DiscountType.PERCENT.name) {
        "Diskon: ${formatPercentTrim(discountValue)}% (${discount.toRupiah()})"
    } else {
        "Diskon: ${discount.toRupiah()}"
    }
}
