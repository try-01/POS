package com.example.kasirpos.data.local.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kasirpos.data.local.dao.ProductDao
import com.example.kasirpos.data.local.entity.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Seeder untuk memasukkan data demo produk saat database pertama kali dibuat.
 * Dipanggil dari [AppDatabase.Callback.onCreate].
 *
 * Dua method disediakan:
 * - [seedViaRawSql] → digunakan oleh Room Callback (sebelum INSTANCE tersedia)
 * - [seed]          → digunakan manual lewat DAO (opsional)
 */
object DatabaseSeeder {

    /** Daftar produk demo — 15 item, mencakup minuman, makanan, snack */
    private val DEMO_PRODUCTS = listOf(
        ProductEntity(name = "Kopi Hitam Tubruk",     sku = "DRK001", price = 8_000,   stock = 45),
        ProductEntity(name = "Es Teh Manis",           sku = "DRK002", price = 5_000,   stock = 60),
        ProductEntity(name = "Nasi Goreng Special",    sku = "FD001",  price = 18_000,  stock = 20),
        ProductEntity(name = "Mie Goreng",             sku = "FD002",  price = 15_000,  stock = 25),
        ProductEntity(name = "Ayam Goreng Krispi",     sku = "FD003",  price = 12_000,  stock = 15),
        ProductEntity(name = "Roti Bakar Coklat",      sku = "SNK001", price = 10_000,  stock = 30),
        ProductEntity(name = "Kentang Goreng",         sku = "SNK002", price = 9_000,   stock = 22),
        ProductEntity(name = "Air Mineral 600ml",      sku = "DRK003", price = 4_000,   stock = 80),
        ProductEntity(name = "Jus Alpukat",            sku = "DRK004", price = 12_000,  stock = 18),
        ProductEntity(name = "Sate Ayam (10 tusuk)",   sku = "FD004",  price = 25_000,  stock = 12),
        ProductEntity(name = "Es Krim Cone",           sku = "SNK003", price = 7_000,   stock = 35),
        ProductEntity(name = "Keripik Pisang",         sku = "SNK004", price = 6_000,   stock = 40),
        ProductEntity(name = "Teh Tarik Panas",        sku = "DRK005", price = 7_000,   stock = 50),
        ProductEntity(name = "Indomie Goreng Telor",   sku = "FD005",  price = 13_000,  stock = 28),
        ProductEntity(name = "Nasi Putih",             sku = "FD006",  price = 5_000,   stock = 55),
    )

    /**
     * Seed via raw [SupportSQLiteDatabase] — digunakan di dalam Room Callback
     * saat [AppDatabase] INSTANCE belum tersedia.
     */
    fun seedViaRawSql(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        for ((i, p) in DEMO_PRODUCTS.withIndex()) {
            db.execSQL(
                """INSERT OR IGNORE INTO products 
                   (id, name, sku, price, stock, imageUri, createdAt, updatedAt) 
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                arrayOf(
                    (i + 1).toLong(), p.name, p.sku, p.price, p.stock,
                    null, now, now
                )
            )
        }
    }

    /** Seed via DAO — untuk pemanggilan manual dari luar Callback */
    fun seed(dao: ProductDao) {
        CoroutineScope(Dispatchers.IO).launch {
            if (dao.count() == 0) {
                DEMO_PRODUCTS.forEach { dao.upsert(it) }
            }
        }
    }
}
