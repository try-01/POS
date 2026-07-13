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
 * Dependency Injection manual (Service Locator).
 * Custom getter dipakai menggantikan `by lazy` agar instance bisa di-reset
 * saat proses Backup/Restore database fisik.
 */
object ServiceLocator {
    private lateinit var appContext: Context

    private var _db: PosDatabase? = null
    private val db: PosDatabase
        get() = _db ?: PosDatabase.getInstance(appContext).also { _db = it }

    private var _productRepository: ProductRepository? = null
    private val productRepository: ProductRepository
        get() = _productRepository ?: ProductRepository(db.productDao()).also { _productRepository = it }

    private var _cartRepository: CartRepository? = null
    private val cartRepository: CartRepository
        get() = _cartRepository ?: CartRepository(db.cartDao()).also { _cartRepository = it }

    private var _transactionRepository: TransactionRepository? = null
    private val transactionRepository: TransactionRepository
        get() = _transactionRepository ?: TransactionRepository(db, db.transactionDao(), db.cartDao(), db.productDao()).also { _transactionRepository = it }

    private var _cashierRepository: CashierRepository? = null
    private val cashierRepository: CashierRepository
        get() = _cashierRepository ?: CashierRepository(db.cashierDao()).also { _cashierRepository = it }

    private var _shiftRepository: ShiftRepository? = null
    private val shiftRepository: ShiftRepository
        get() = _shiftRepository ?: ShiftRepository(db.shiftDao()).also { _shiftRepository = it }

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

    fun transactionRepository(): TransactionRepository = transactionRepository
    fun productRepository(): ProductRepository = productRepository
    fun cashierRepository(): CashierRepository = cashierRepository
    fun shiftRepository(): ShiftRepository = shiftRepository

    /**
     * Menutup koneksi database dan mereset cache repository di ServiceLocator.
     * Memaksa WAL checkpoint sebelum close agar tidak ada transaksi nanggung hilang.
     */
    fun closeDatabase() {
        _db?.let { database ->
            // Force WAL checkpoint TRUNCATE: memindahkan semua data dari pos.db-wal
            // ke pos.db utama, lalu mengosongkan file wal. Wajib sebelum backup fisik.
            database.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)")
                .use { it.moveToFirst() }

            PosDatabase.closeAndClearInstance()

            _db = null
            _productRepository = null
            _cartRepository = null
            _transactionRepository = null
            _cashierRepository = null
            _shiftRepository = null
        }
    }
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