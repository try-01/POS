package com.pos.offline.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE products ADD COLUMN cost INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'",
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN cashierId INTEGER",
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN cashierName TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN shiftId INTEGER",
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
                    """.trimIndent(),
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
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shifts_cashierId` ON `shifts` (`cashierId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shifts_endedAt` ON `shifts` (`endedAt`)",
                )
            }
        }
    val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transaction_items ADD COLUMN unitCost INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN discountType TEXT NOT NULL DEFAULT 'NOMINAL'",
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN discountValue REAL NOT NULL DEFAULT 0.0",
                )
            }
        }
    val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'",
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN voidedAt INTEGER",
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN voidReason TEXT",
                )
                db.execSQL(
                    "ALTER TABLE transaction_items ADD COLUMN productId INTEGER",
                )
            }
        }
    val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN returnId INTEGER",
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
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_returns_transactionId` ON `returns` (`transactionId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_returns_returnedAt` ON `returns` (`returnedAt`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_returns_shiftId` ON `returns` (`shiftId`)",
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
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_return_items_returnId` ON `return_items` (`returnId`)",
                )
            }
        }
    val MIGRATION_7_8 =
        object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `printers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `label` TEXT NOT NULL,
                        `connectionType` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `priority` INTEGER NOT NULL,
                        `charPerLine` INTEGER NOT NULL,
                        `paperWidth` TEXT NOT NULL,
                        `supportsStatusQuery` INTEGER NOT NULL,
                        `bluetoothMacAddress` TEXT,
                        `wifiIpAddress` TEXT,
                        `wifiPort` INTEGER,
                        `usbVendorId` INTEGER,
                        `usbProductId` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `store_profile` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `storeName` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `footerNote` TEXT NOT NULL,
                        `logoBytes` BLOB,
                        `autoPrintEnabled` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `store_profile`
                        (`id`, `storeName`, `address`, `footerNote`, `logoBytes`, `autoPrintEnabled`)
                    VALUES (1, '', '', '', NULL, 0)
                    """.trimIndent(),
                )
            }
        }
    val MIGRATION_8_9 =
        object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE products ADD COLUMN barcode TEXT",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)",
                )
            }
        }
    val MIGRATION_9_10 =
        object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE products ADD COLUMN category TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_products_category` ON `products` (`category`)",
                )
            }
        }
        val MIGRATION_10_11 =
        object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE printers ADD COLUMN statusQueryFailStreak INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE printers ADD COLUMN autoDisabledDueToNoResponse INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
        
        val MIGRATION_11_12 =
        object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_createdAt` ON `transactions` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_transactionId` ON `transaction_items` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_productId` ON `transaction_items` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_returns_returnedAt` ON `returns` (`returnedAt`)")
            }
        }
    val MIGRATION_12_13 =
        object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Kembalian yang BENAR-BENAR diserahkan ke pembeli (fisik).
                // Selisih `change - changeGiven` (saat change > 0) = tip yang
                // sengaja tidak diambil pembeli.
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN changeGiven INTEGER NOT NULL DEFAULT 0",
                )
                // Backfill data lama: sebelum fitur "change negatif/tip" ada,
                // kolom `change` SELALU >= 0 dan SELALU diberikan penuh sbg
                // kembalian. Maka changeGiven lama = change lama (identik
                // dengan perilaku sebelumnya, tidak ada tip pada data historis).
                db.execSQL(
                    "UPDATE transactions SET changeGiven = CASE WHEN change > 0 THEN change ELSE 0 END",
                )
            }
        }
    val MIGRATION_13_14 =
        object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Menandai apakah `changeGiven` pada transaksi ini benar-benar
                // KELUAR SEBAGAI UANG TUNAI FISIK dari laci. Relevan HANYA untuk
                // transaksi QRIS dengan changeGiven > 0 — skenario nyata di
                // lapangan: pembeli bayar lebih via QRIS (mis. tidak bawa cash),
                // lalu kembaliannya diminta dalam bentuk uang tunai. Untuk CASH,
                // nilai ini SELALU true (trivial — kembalian tunai pasti tunai),
                // dipaksa juga di level TransactionRepository sbg defense-in-depth.
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN changeGivenInCash INTEGER NOT NULL DEFAULT 1",
                )
                // Backfill data historis: sebelum fitur ini ada, tidak ada cara
                // mengetahui apakah kembalian QRIS lama diberikan tunai atau
                // tidak. Default KONSERVATIF = false untuk QRIS historis, agar
                // TIDAK mengubah retroaktif expectedCashInDrawer pada shift lama
                // yang sudah ditutup (shift lama sudah punya snapshot
                // endingCashExpected tersendiri di tabel shifts).
                db.execSQL(
                    "UPDATE transactions SET changeGivenInCash = 0 WHERE paymentMethod = 'QRIS'",
                )
            }
        }
    val MIGRATION_14_15 =
        object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. TABEL PRODUCTS (stock: INTEGER -> REAL)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `products_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `sku` TEXT NOT NULL,
                        `barcode` TEXT,
                        `price` INTEGER NOT NULL,
                        `cost` INTEGER NOT NULL DEFAULT 0,
                        `stock` REAL NOT NULL,
                        `active` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `category` TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `products_new` (`id`, `name`, `sku`, `barcode`, `price`, `cost`, `stock`, `active`, `createdAt`, `updatedAt`, `category`)
                    SELECT `id`, `name`, `sku`, `barcode`, `price`, `cost`, CAST(`stock` AS REAL), `active`, `createdAt`, `updatedAt`, `category`
                    FROM `products`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `products`")
                db.execSQL("ALTER TABLE `products_new` RENAME TO `products`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_sku` ON `products` (`sku`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category` ON `products` (`category`)")

                // 2. TABEL CART_ITEMS (quantity: INTEGER -> REAL)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cart_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `unitPrice` INTEGER NOT NULL,
                        `quantity` REAL NOT NULL DEFAULT 1,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `cart_items_new` (`id`, `productId`, `name`, `unitPrice`, `quantity`)
                    SELECT `id`, `productId`, `name`, `unitPrice`, CAST(`quantity` AS REAL)
                    FROM `cart_items`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `cart_items`")
                db.execSQL("ALTER TABLE `cart_items_new` RENAME TO `cart_items`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cart_items_productId` ON `cart_items` (`productId`)")

                // 3. TABEL TRANSACTION_ITEMS (quantity: INTEGER -> REAL)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transactionId` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `unitPrice` INTEGER NOT NULL,
                        `quantity` REAL NOT NULL,
                        `lineTotal` INTEGER NOT NULL,
                        `unitCost` INTEGER NOT NULL DEFAULT 0,
                        `productId` INTEGER
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `transaction_items_new` (`id`, `transactionId`, `productName`, `unitPrice`, `quantity`, `lineTotal`, `unitCost`, `productId`)
                    SELECT `id`, `transactionId`, `productName`, `unitPrice`, CAST(`quantity` AS REAL), `lineTotal`, `unitCost`, `productId`
                    FROM `transaction_items`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `transaction_items`")
                db.execSQL("ALTER TABLE `transaction_items_new` RENAME TO `transaction_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_transactionId` ON `transaction_items` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_productId` ON `transaction_items` (`productId`)")

                // 4. TABEL RETURN_ITEMS (quantityReturned: INTEGER -> REAL)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `return_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `returnId` INTEGER NOT NULL,
                        `transactionItemId` INTEGER NOT NULL,
                        `productId` INTEGER,
                        `productName` TEXT NOT NULL,
                        `unitPrice` INTEGER NOT NULL,
                        `quantityReturned` REAL NOT NULL,
                        `restocked` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `return_items_new` (`id`, `returnId`, `transactionItemId`, `productId`, `productName`, `unitPrice`, `quantityReturned`, `restocked`)
                    SELECT `id`, `returnId`, `transactionItemId`, `productId`, `productName`, `unitPrice`, CAST(`quantityReturned` AS REAL), `restocked`
                    FROM `return_items`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `return_items`")
                db.execSQL("ALTER TABLE `return_items_new` RENAME TO `return_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_return_items_returnId` ON `return_items` (`returnId`)")
            }
        }

    val ALL: Array<Migration> =
        arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
        )
}