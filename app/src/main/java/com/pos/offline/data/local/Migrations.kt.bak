package com.pos.offline.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE products ADD COLUMN cost INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'"
            )
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN cashierId INTEGER"
            )
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN cashierName TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN shiftId INTEGER"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `cashiers` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `pinHash` TEXT,
                    `active` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shifts` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `cashierId` INTEGER NOT NULL,
                    `cashierName` TEXT NOT NULL,
                    `startingCash` INTEGER NOT NULL,
                    `startedAt` INTEGER NOT NULL,
                    `endingCashExpected` INTEGER,
                    `endingCashActual` INTEGER,
                    `endedAt` INTEGER,
                    `note` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_shifts_cashierId` ON `shifts` (`cashierId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_shifts_endedAt` ON `shifts` (`endedAt`)"
            )
        }
    }
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE transaction_items ADD COLUMN unitCost INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN discountType TEXT NOT NULL DEFAULT 'NOMINAL'"
            )
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN discountValue REAL NOT NULL DEFAULT 0.0"
            )
        }
    }
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'"
            )
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN voidedAt INTEGER"
            )
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN voidReason TEXT"
            )
            db.execSQL(
                "ALTER TABLE transaction_items ADD COLUMN productId INTEGER"
            )
        }
    }

    /**
     * Batch E — Retur Produk.
     * - transactions.returnId (nullable): FK logis ke returns.id. Nullable, TANPA
     *   default khusus (SQLite ALTER TABLE ADD COLUMN nullable otomatis NULL untuk
     *   baris lama) — konsisten dengan pola voidedAt/voidReason di MIGRATION_5_6.
     * - Tabel `returns`/`return_items` baru: CREATE TABLE langsung cocok dengan
     *   definisi entity (tidak ada baris lama yang perlu diisi default), jadi
     *   tidak butuh anotasi @ColumnInfo(defaultValue=...) di sisi Kotlin.
     * - returns.note: TEXT NOT NULL DEFAULT '' — catatan bebas alasan retur.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN returnId INTEGER"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `returns` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `transactionId` TEXT NOT NULL,
                    `returnedAt` INTEGER NOT NULL,
                    `shiftId` INTEGER,
                    `cashierId` INTEGER,
                    `cashierName` TEXT NOT NULL,
                    `refundAmount` INTEGER NOT NULL,
                    `refundMethod` TEXT NOT NULL,
                    `note` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_returns_transactionId` ON `returns` (`transactionId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_returns_returnedAt` ON `returns` (`returnedAt`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_returns_shiftId` ON `returns` (`shiftId`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `return_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `returnId` INTEGER NOT NULL,
                    `transactionItemId` INTEGER NOT NULL,
                    `productId` INTEGER,
                    `productName` TEXT NOT NULL,
                    `unitPrice` INTEGER NOT NULL,
                    `quantityReturned` INTEGER NOT NULL,
                    `restocked` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_return_items_returnId` ON `return_items` (`returnId`)"
            )
        }
    }

    /** Daftar semua migrasi yang terdaftar pada [androidx.room.RoomDatabase]. */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
    )
}