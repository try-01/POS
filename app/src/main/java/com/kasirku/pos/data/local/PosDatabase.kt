package com.kasirku.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kasirku.pos.data.local.dao.CartDao
import com.kasirku.pos.data.local.dao.ProductDao
import com.kasirku.pos.data.local.dao.TransactionDao
import com.kasirku.pos.data.local.entity.CartItemEntity
import com.kasirku.pos.data.local.entity.ProductEntity
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity

/**
 * Room Database utama dengan konfigurasi optimal
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
        const val DATABASE_NAME = "kasirku_pos.db"

        fun buildDatabase(context: Context): PosDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PosDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .build()
        }
    }
}
