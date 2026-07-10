package com.pos.offline.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pos.offline.data.local.dao.CartDao
import com.pos.offline.data.local.dao.ProductDao
import com.pos.offline.data.local.dao.TransactionDao
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.TransactionItemEntity

/** Data contoh awal (name, sku, price, cost). */
private data class SeedProduct(
    val name: String, val sku: String, val price: Long, val cost: Long
)

/**
 * Database tunggal (singleton) untuk seluruh aplikasi.
 *
 * Menggunakan [Context.getApplicationContext] agar tidak ada referensi Activity/View
 * yang lolos (mencegah memory leak). Double-checked locking + @Volatile membuat
 * inisialisasi aman di banyak thread tanpa lock berat.
 *
 * MIGRASI EKSPLISIT: database memakai [Room.databaseBuilder.addMigrations]
 * (lihat [Migrations]), BUKAN `fallbackToDestructiveMigration`. Dengan begitu
 * data pengguna terjaga saat aplikasi di-update; bila skema berubah tanpa
 * migrasi yang terdaftar, Room melempar exception (fail-loud) sehingga korupsi
 * data tidak terjadi diam-diam.
 */
@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class
    ],
    version = 2,
    // exportSchema = true: Room mengekspor riwayat skema ke app/schemas (JSON)
    // melalui plugin "androidx.room" di build.gradle.kts. Berkas tersebut wajib
    // di-commit ke VCS supaya migrasi versi database dapat dibangkitkan & diuji.
    exportSchema = true
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        fun getInstance(context: Context): PosDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, // applicationContext = anti memory leak
                    PosDatabase::class.java,
                    "pos.db"
                )
                    .addCallback(SEED_CALLBACK)
                    // Daftarkan SEMUA migrasi eksplisit (upgrade versi).
                    // Upgrade tidak pernah destruktif → data pengguna tetap aman.
                    .addMigrations(*Migrations.ALL)
                    // Safety-net HANYA untuk DOWNGRADE (mis. sideload versi lama saat
                    // develop). Tidak ada fallbackToDestructiveMigration() untuk upgrade.
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }

        /**
         * Isi data contoh saat database pertama kali dibuat (first-run). Callback
         * ini berjalan pada thread database (bukan main) dan memakai [execSQL]
         * sinkron — tidak butuh coroutine, tidak menambah alokasi saat runtime.
         *
         * Catatan: onCreate HANYA berjalan pada fresh-install. Pada perangkat yang
         * upgrade v1→v2, data lama dipertahankan dan [Migrations.MIGRATION_1_2]
         * yang menambahkan kolom `cost` — bukan callback ini.
         */
        private val SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                // Stok contoh diseragamkan = 25; cost < price agar laba masuk akal.
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
