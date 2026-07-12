package com.pos.offline.data.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pos.offline.data.local.PosDatabase
import com.pos.offline.data.repository.CartRepository
import com.pos.offline.data.repository.ProductRepository
import com.pos.offline.data.repository.TransactionRepository
import com.pos.offline.ui.inventory.InventoryViewModel
import com.pos.offline.ui.pos.PosViewModel
import com.pos.offline.ui.report.ReportViewModel

/**
 * Aplikasi entry-point. Menginisialisasi [ServiceLocator] sekali di onCreate.
 * Memakai applicationContext → tidak menyimpan referensi Activity (anti leak).
 */
class PosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}

/**
 * Dependency Injection manual (Service Locator) memakai `by lazy`.
 *
 * Alasan memilih ini ketimbang Hilt/Koin: tidak ada reflection/runtime container →
 * start-up lebih cepat & jejak RAM lebih kecil (sesuai tujuan "sangat ringan").
 * Database DAO dibuat satu kali lalu dipakai ulang (singleton efektif).
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

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /** Factory ViewModel standar untuk dipakai Compose `viewModel(factory = ...)`. */
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
}

/** Factory tipis yang menyuntikkan repository ke dalam [PosViewModel]. */
class PosViewModelFactory(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PosViewModel(productRepository, cartRepository, transactionRepository) as T
}

/** Factory untuk [InventoryViewModel] — hanya butuh ProductRepository. */
class InventoryViewModelFactory(
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        InventoryViewModel(productRepository) as T
}

/** Factory untuk [ReportViewModel] — hanya butuh TransactionRepository. */
class ReportViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(transactionRepository) as T
}
