package com.kasirku.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kasirku.pos.data.local.dao.ProductDao
import com.kasirku.pos.data.local.dao.TransactionDao
import com.kasirku.pos.data.local.entity.ProductEntity
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kasirku.db"
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed data awal saat database baru pertama kali dibentuk
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                seedInitialProducts(database.productDao())
                            }
                        }
                    }
                })
                .build()
        }

        private suspend fun seedInitialProducts(dao: ProductDao) {
            val sampleProducts = listOf(
                ProductEntity(sku = "KOP-001", name = "Kopi Susu Gula Aren", sellPrice = 22000.0, costPrice = 10000.0, stock = 45, category = "Minuman"),
                ProductEntity(sku = "KOP-002", name = "Americano Ice", sellPrice = 18000.0, costPrice = 7000.0, stock = 60, category = "Minuman"),
                ProductEntity(sku = "MAKH-01", name = "Nasi Goreng Spesial KasirKu", sellPrice = 35000.0, costPrice = 16000.0, stock = 25, category = "Makanan"),
                ProductEntity(sku = "SNK-001", name = "Croissant Butter", sellPrice = 24000.0, costPrice = 12000.0, stock = 18, category = "Camilan"),
                ProductEntity(sku = "SNK-002", name = "Kentang Goreng Crispy", sellPrice = 20000.0, costPrice = 8000.0, stock = 30, category = "Camilan"),
                ProductEntity(sku = "MIN-001", name = "Es Teh Manis Melati", sellPrice = 10000.0, costPrice = 3000.0, stock = 100, category = "Minuman"),
                ProductEntity(sku = "MIN-002", name = "Air Mineral 600ml", sellPrice = 6000.0, costPrice = 2500.0, stock = 80, category = "Minuman"),
                ProductEntity(sku = "MAKH-02", name = "Mie Goreng Katsu Ayam", sellPrice = 38000.0, costPrice = 18000.0, stock = 15, category = "Makanan")
            )
            for (p in sampleProducts) {
                runCatching { dao.insert(p) }
            }
        }
    }
}
