package com.example.kasirpos.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kasirpos.data.local.dao.CartDao
import com.example.kasirpos.data.local.dao.ProductDao
import com.example.kasirpos.data.local.dao.TransactionDao
import com.example.kasirpos.data.local.entity.CartItemEntity
import com.example.kasirpos.data.local.entity.ProductEntity
import com.example.kasirpos.data.local.entity.TransactionEntity
import com.example.kasirpos.data.local.entity.TransactionItemEntity

@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class
    ],
    version = 1,
    exportSchema = false // Matikan schema export untuk menghemat ukuran APK
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Singleton thread-safe dengan double-checked locking.
         * Gunakan [Context.applicationContext] untuk menghindari memory leak.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "kasirpos.db"
            )
                // WAL journal mode — performa tulis lebih baik, minim lock kontensi
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                // Fallback destruktif hanya jika versi berubah (development safe)
                .fallbackToDestructiveMigration()
                // Seeder: isi data demo saat DB pertama kali dibuat
                .addCallback(object : Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        DatabaseSeeder.seedViaRawSql(db)
                    }
                })
                .build()
        }
    }
}
