package com.example.kasirpos

import android.app.Application
import com.example.kasirpos.data.local.database.AppDatabase
import com.example.kasirpos.data.repository.CartRepository
import com.example.kasirpos.data.repository.ProductRepository
import com.example.kasirpos.data.repository.TransactionRepository

/**
 * Application class — inisialisasi singleton database & repository.
 * Menggunakan lazy initialization agar tidak blocking cold start.
 */
class KasirApp : Application() {

    // Database singleton — diinisialisasi pertama kali dibutuhkan
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    // Repository — lightweight, cukup dibuat sekali
    val productRepository: ProductRepository by lazy {
        ProductRepository(database.productDao())
    }
    val cartRepository: CartRepository by lazy {
        CartRepository(database.cartDao())
    }
    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(
            database.transactionDao(),
            database.cartDao(),
            database.productDao(),
            database // Untuk atomic `withTransaction` di checkout
        )
    }
}
