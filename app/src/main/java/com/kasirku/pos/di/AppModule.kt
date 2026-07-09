package com.kasirku.pos.di

import android.content.Context
import com.kasirku.pos.data.local.AppDatabase
import com.kasirku.pos.data.local.dao.ProductDao
import com.kasirku.pos.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modul Hilt: menyediakan instance Database & DAO sebagai Singleton yang di-scope ke
 * SingletonComponent (seumur hidup aplikasi). Ini penting untuk mencegah memory leak —
 * hanya ADA SATU instance Room database aktif sepanjang aplikasi berjalan.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    // CartRepository & ProductRepository/TransactionRepository tidak perlu di-provide manual
    // di sini karena sudah memakai @Inject constructor + @Singleton pada class-nya masing-masing
    // (Hilt otomatis mengenali & menyediakannya).
}
