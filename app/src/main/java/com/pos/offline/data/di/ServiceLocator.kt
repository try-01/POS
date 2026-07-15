package com.pos.offline.data.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pos.offline.data.local.PosDatabase
import com.pos.offline.data.repository.CartRepository
import com.pos.offline.data.repository.CashierRepository
import com.pos.offline.data.repository.PrinterRepository
import com.pos.offline.data.repository.ProductRepository
import com.pos.offline.data.repository.ReturnRepository
import com.pos.offline.data.repository.ShiftRepository
import com.pos.offline.data.repository.StoreProfileRepository
import com.pos.offline.data.repository.TransactionRepository
import com.pos.offline.ui.inventory.InventoryViewModel
import com.pos.offline.ui.pos.PosViewModel
import com.pos.offline.ui.report.ReportViewModel
import com.pos.offline.ui.settings.PrinterViewModel
import com.pos.offline.ui.settings.SettingsViewModel
import com.pos.offline.util.BluetoothPrinterHelper

class PosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}

object ServiceLocator {
    private lateinit var appContext: Context

    private val db: PosDatabase by lazy { PosDatabase.getInstance(appContext) }

    private val productRepository: ProductRepository by lazy {
        ProductRepository(db.productDao())
    }
    private val cartRepository: CartRepository by lazy {
        CartRepository(db.cartDao())
    }
    private val cashierRepository: CashierRepository by lazy {
        CashierRepository(db.cashierDao())
    }
    private val shiftRepository: ShiftRepository by lazy {
        ShiftRepository(db.shiftDao())
    }
    private val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(db, db.transactionDao(), db.cartDao(), db.productDao(), shiftRepository)
    }
    private val returnRepository: ReturnRepository by lazy {
        ReturnRepository(db, db.returnDao(), db.transactionDao(), db.productDao())
    }
    private val printerRepository: PrinterRepository by lazy {
        PrinterRepository(db.printerDao())
    }
    private val storeProfileRepository: StoreProfileRepository by lazy {
        StoreProfileRepository(db.storeProfileDao())
    }
    private val bluetoothPrinterHelper: BluetoothPrinterHelper by lazy {
        BluetoothPrinterHelper(appContext)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    fun posViewModelFactory(): ViewModelProvider.Factory = PosViewModelFactory(
        productRepository,
        cartRepository,
        transactionRepository,
        cashierRepository,
        shiftRepository
    )

    fun inventoryViewModelFactory(): ViewModelProvider.Factory =
        InventoryViewModelFactory(productRepository)
    fun reportViewModelFactory(): ViewModelProvider.Factory =
        ReportViewModelFactory(transactionRepository, shiftRepository, returnRepository)
    fun settingsViewModelFactory(): ViewModelProvider.Factory =
        SettingsViewModelFactory(cashierRepository, shiftRepository)
    fun printerViewModelFactory(): ViewModelProvider.Factory =
        PrinterViewModelFactory(printerRepository, bluetoothPrinterHelper)

    fun transactionRepository(): TransactionRepository = transactionRepository
    fun productRepository(): ProductRepository = productRepository
    fun cashierRepository(): CashierRepository = cashierRepository
    fun shiftRepository(): ShiftRepository = shiftRepository
    fun returnRepository(): ReturnRepository = returnRepository
    fun printerRepository(): PrinterRepository = printerRepository
    fun storeProfileRepository(): StoreProfileRepository = storeProfileRepository
}

class PosViewModelFactory(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository,
    private val cashierRepository: CashierRepository,
    private val shiftRepository: ShiftRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PosViewModel(
            productRepository,
            cartRepository,
            transactionRepository,
            cashierRepository,
            shiftRepository
        ) as T
}

class InventoryViewModelFactory(
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        InventoryViewModel(productRepository) as T
}

class ReportViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val shiftRepository: ShiftRepository,
    private val returnRepository: ReturnRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(transactionRepository, shiftRepository, returnRepository) as T
}

class SettingsViewModelFactory(
    private val cashierRepository: CashierRepository,
    private val shiftRepository: ShiftRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(cashierRepository, shiftRepository) as T
}

class PrinterViewModelFactory(
    private val printerRepository: PrinterRepository,
    private val bluetoothPrinterHelper: BluetoothPrinterHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PrinterViewModel(printerRepository, bluetoothPrinterHelper) as T
}