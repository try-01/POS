package com.pos.offline.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pos.offline.data.local.dao.CartDao
import com.pos.offline.data.local.dao.CashierDao
import com.pos.offline.data.local.dao.ProductDao
import com.pos.offline.data.local.dao.ShiftDao
import com.pos.offline.data.local.dao.TransactionDao
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.ShiftEntity
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.TransactionItemEntity

private data class SeedProduct(
    val name: String, val sku: String, val price: Long, val cost: Long
)

/**
 * Database tunggal (singleton) untuk seluruh aplikasi.
 *
 * v5 menambahkan kolom `discountType`/`discountValue` pada `transactions`
 * (lihat [Migrations.MIGRATION_4_5]) — snapshot audit tipe & nilai mentah
 * diskon (Nominal/Persen) yang diketik kasir. Kolom `discount` (nominal
 * final) TIDAK berubah maknanya, tetap satu-satunya sumber kebenaran untuk
 * kalkulasi & laporan.
 */
@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        CashierEntity::class,
        ShiftEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cashierDao(): CashierDao
    abstract fun shiftDao(): ShiftDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        fun getInstance(context: Context): PosDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "pos.db"
                )
                    .addCallback(SEED_CALLBACK)
                    .addMigrations(*Migrations.ALL)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }

        fun closeActiveInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        private val SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                val samples = listOf(
                    SeedProduct("Kopi Hitam", "SKU-001", 8_000L, 3_000L),
                    SeedProduct("Kopi Susu", "SKU-002", 12_000L, 5_000L),
                    SeedProduct("Es Teh Manis", "SKU-003", 5_000L, 1_500L),
                    SeedProduct("Roti Bakar", "SKU-004", 15_000L, 7_000L),
                    SeedProduct("Mie Goreng", "SKU-005", 18_000L, 9_000L),
                    SeedProduct("Nasi Goreng", "SKU-006", 20_000L, 10_000L),
                    SeedProduct("Air Mineral", "SKU-007", 4_000L, 2_000L),
                    SeedProduct("Gorengan", "SKU-008", 3_000L, 1_000L)
                )
                samples.forEach { s ->
                    db.execSQL(
                        "INSERT INTO products " +
                            "(name, sku, price, cost, stock, active, createdAt, updatedAt) " +
                            "VALUES (?, ?, ?, ?, ?, 1, ?, ?)",
                        arrayOf<Any>(s.name, s.sku, s.price, s.cost, 25, now, now)
                    )
                }
            }
        }
    }
}