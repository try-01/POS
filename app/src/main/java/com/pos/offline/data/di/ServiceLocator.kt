package com.pos.offline.data.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pos.offline.data.local.PosDatabase
import com.pos.offline.data.repository.CartRepository
import com.pos.offline.data.repository.CashierRepository
import com.pos.offline.data.repository.ProductRepository
import com.pos.offline.data.repository.ShiftRepository
import com.pos.offline.data.repository.TransactionRepository
import com.pos.offline.ui.inventory.InventoryViewModel
import com.pos.offline.ui.pos.PosViewModel
import com.pos.offline.ui.report.ReportViewModel

class PosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}

/**
 * Dependency Injection manual (Service Locator) memakai `by lazy`.
 */
object ServiceLocator {
    private lateinit var appContext: Context

    private val db: PosDatabase by lazy { PosDatabase.getInstance(appContext) }

    private val productRepository: ProductRepository by lazy {
        ProductRepository(db.productDao())
    }
    private val cartRepository: CartRepository by lazy {
        CartRepository(db.cartDao())
    }
    private val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(db, db.transactionDao(), db.cartDao(), db.productDao())
    }
    // === BARU: fondasi fitur Kasir/Shift (Batch 1) ===
    private val cashierRepository: CashierRepository by lazy {
        CashierRepository(db.cashierDao())
    }
    private val shiftRepository: ShiftRepository by lazy {
        ShiftRepository(db.shiftDao())
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun posViewModelFactory(): ViewModelProvider.Factory = PosViewModelFactory(
        productRepository,
        cartRepository,
        transactionRepository
    )

    fun inventoryViewModelFactory(): ViewModelProvider.Factory =
        InventoryViewModelFactory(productRepository)

    fun reportViewModelFactory(): ViewModelProvider.Factory =
        ReportViewModelFactory(transactionRepository)

    // Repositori lain dapat dibuka di sini saat dibutuhkan.
    fun transactionRepository(): TransactionRepository = transactionRepository
    fun productRepository(): ProductRepository = productRepository
    fun cashierRepository(): CashierRepository = cashierRepository
    fun shiftRepository(): ShiftRepository = shiftRepository
}

class PosViewModelFactory(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PosViewModel(productRepository, cartRepository, transactionRepository) as T
}

class InventoryViewModelFactory(
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        InventoryViewModel(productRepository) as T
}

class ReportViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(transactionRepository) as T
}