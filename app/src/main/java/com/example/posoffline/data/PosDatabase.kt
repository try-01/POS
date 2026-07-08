package com.example.posoffline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.posoffline.data.dao.ProductDao
import com.example.posoffline.data.dao.TransactionDao
import com.example.posoffline.data.entity.ProductEntity
import com.example.posoffline.data.entity.TransactionEntity

/**
 * Room database.
 *
 * Singleton: built once per process via [getInstance]. We pass the application
 * context to avoid leaking activities (a classic Android memory leak source).
 *
 * Note on @TypeConverters:
 *  - The transaction's `itemsJson` field is already a `String`, so Room
 *    does not invoke any TypeConverter for it. The JSON encoding is done
 *    by `TransactionRepository.checkout()` *before* insert. That keeps the
 *    schema simple and avoids a converter that would have to round-trip
 *    through kotlinx.serialization inside the Room generated code.
 *
 * Why we disable destructive migration in production:
 *  - Sales data must never be silently lost on a schema bump. Use proper
 *    migrations instead. The seed data is idempotent, but a real shop
 *    expects migrations, not wipes.
 */
@Database(
    entities = [ProductEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PosDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DB_NAME = "pos-offline.db"

        @Volatile
        private var instance: PosDatabase? = null

        fun getInstance(context: Context): PosDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): PosDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PosDatabase::class.java,
                DB_NAME
            )
                // Do NOT call fallbackToDestructiveMigration in release.
                // We leave it off intentionally; for v1 schema this is a no-op.
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // PRAGMA tuning: WAL = better concurrency for read-heavy POS.
                        db.execSQL("PRAGMA journal_mode = WAL;")
                        db.execSQL("PRAGMA synchronous = NORMAL;")
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    }
                })
                .build()
        }
    }
}
