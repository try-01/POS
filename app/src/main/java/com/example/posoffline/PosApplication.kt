package com.example.posoffline

import android.app.Application
import com.example.posoffline.data.PosDatabase
import com.example.posoffline.data.SettingsRepository
import com.example.posoffline.data.repository.ProductRepository
import com.example.posoffline.data.repository.TransactionRepository
import com.example.posoffline.data.seed.SeedRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application root.
 *
 * We deliberately use manual dependency injection (a single AppContainer)
 * instead of Hilt/Koin to keep:
 *  - APK small (no codegen, no extra KSP processors)
 *  - Cold-start fast (no annotation scanning)
 *  - Memory low (no reflection / service locators)
 *
 * Anything that should live for the lifetime of the process is created
 * here exactly once and passed down via the [container] property.
 */
class PosApplication : Application() {

    /** Application-scoped coroutine scope. Uses [SupervisorJob] so a single
     *  failed job (e.g. seed insert) does not cancel the rest. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Manual DI container. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Seed sample data on first launch (idempotent).
        appScope.launch { SeedRunner(container.database).ensureSeeded() }
    }
}

class AppContainer(app: PosApplication) {
    val database: PosDatabase = PosDatabase.getInstance(app)
    val settingsRepository = SettingsRepository(app)

    val productRepository = ProductRepository(database.productDao())
    val transactionRepository = TransactionRepository(
        productDao = database.productDao(),
        transactionDao = database.transactionDao(),
        settings = settingsRepository
    )
}
