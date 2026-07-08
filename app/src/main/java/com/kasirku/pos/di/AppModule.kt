package com.kasirku.pos.di

import android.content.Context
import com.kasirku.pos.data.local.PosDatabase
import com.kasirku.pos.data.local.dao.CartDao
import com.kasirku.pos.data.local.dao.ProductDao
import com.kasirku.pos.data.local.dao.TransactionDao
import com.kasirku.pos.export.PdfReceiptExporter
import com.kasirku.pos.print.BluetoothPrinterManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module - Dependency Injection Configuration
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PosDatabase {
        return PosDatabase.buildDatabase(context)
    }

    @Provides
    @Singleton
    fun provideProductDao(db: PosDatabase): ProductDao = db.productDao()

    @Provides
    @Singleton
    fun provideCartDao(db: PosDatabase): CartDao = db.cartDao()

    @Provides
    @Singleton
    fun provideTransactionDao(db: PosDatabase): TransactionDao = db.transactionDao()

    @Provides
    @Singleton
    fun provideBluetoothPrinterManager(
        @ApplicationContext context: Context
    ): BluetoothPrinterManager = BluetoothPrinterManager(context)

    @Provides
    @Singleton
    fun providePdfReceiptExporter(
        @ApplicationContext context: Context
    ): PdfReceiptExporter = PdfReceiptExporter(context)
}
