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

/**
 * Database tunggal (singleton) untuk seluruh aplikasi.
 *
 * Menggunakan [Context.getApplicationContext] agar tidak ada referensi Activity/View
 * yang lolos (mencegah memory leak). Double-checked locking + @Volatile membuat
 * inisialisasi aman di banyak thread tanpa lock berat.
 *
 * `fallbackToDestructiveMigration` dipakai untuk fase awal; ganti ke migrasi
 * eksplisit saat skema sudah stabil di produksi.
 */
@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class
    ],
    version = 1,
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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }

        /**
         * Isi data contoh saat database pertama kali dibuat (first-run). Callback
         * ini berjalan pada thread database (bukan main) dan memakai [execSQL]
         * sinkron — tidak butuh coroutine, tidak menambah alokasi saat runtime.
         * Tujuannya agar aplikasi langsung bisa dipakai tanpa layar kosong.
         */
        private val SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                // Triple<name, sku, price>; stok contoh diseragamkan = 25.
                val samples = listOf(
                    Triple("Kopi Hitam", "SKU-001", 8_000L),
                    Triple("Kopi Susu", "SKU-002", 12_000L),
                    Triple("Es Teh Manis", "SKU-003", 5_000L),
                    Triple("Roti Bakar", "SKU-004", 15_000L),
                    Triple("Mie Goreng", "SKU-005", 18_000L),
                    Triple("Nasi Goreng", "SKU-006", 20_000L),
                    Triple("Air Mineral", "SKU-007", 4_000L),
                    Triple("Gorengan", "SKU-008", 3_000L)
                )
                samples.forEach { (name, sku, price) ->
                    db.execSQL(
                        "INSERT INTO products " +
                            "(name, sku, price, stock, active, createdAt, updatedAt) " +
                            "VALUES (?, ?, ?, ?, 1, ?, ?)",
                        arrayOf<Any>(name, sku, price, 25, now, now)
                    )
                }
            }
        }
    }
}
