package com.example.kasirpos.domain.model

/**
 * Domain model — murni Kotlin, tidak bergantung pada Room/Android.
 * Digunakan untuk mapping Entity ↔ UI jika perlu transformasi.
 */
data class Product(
    val id: Long,
    val name: String,
    val sku: String,
    val price: Long,
    val stock: Int,
    val imageUri: String?
)
