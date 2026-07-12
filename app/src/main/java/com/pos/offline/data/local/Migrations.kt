package com.pos.offline.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Kumpulan migrasi database versi-ke-versi yang EKSPLISIT (manual).
 *
 * Mengapa migrasi manual?
 *  - Menjaga data pengguna saat aplikasi di-update (tidak dihapus/dibuat ulang).
 *  - Lebih dapat diandalkan daripada `fallbackToDestructiveMigration` yang
 *    diam-diam menghapus seluruh tabel — berbahaya di produksi.
 *  - Setiap langkah dapat diuji (lihat catatan schema JSON di app/schemas).
 *
 * Setiap [Migration] wajib menulis SQL sehingga skema DB lama (di perangkat)
 * menjadi IDENTIK dengan skema yang dibangkitkan Room dari entity versi baru.
 * Jika tidak identik, Room melempar `IllegalStateException` saat runtime —
 * ini sengaja, untuk mencegah korupsi data diam-diam.
 */
object Migrations {

    /**
     * v1 → v2: tambah kolom `cost` (harga modal/beli) pada tabel `products`.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE products ADD COLUMN cost INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * v2 → v3: fondasi fitur Kasir/Shift/Metode Bayar.
     *
     *  1) Tabel `transactions` mendapat 4 kolom baru — semua ADD COLUMN bersifat
     *     aditif (tidak mengubah/menghapus baris lama). Transaksi historis
     *     otomatis terisi `paymentMethod='CASH'`, `cashierName=''`,
     *     `cashierId=NULL`, `shiftId=NULL` — konsisten dgn asumsi bahwa
     *     transaksi lama (sebelum fitur ini ada) dianggap tunai tanpa kasir
     *     tercatat.
     *  2) Tabel baru `cashiers` & `shifts` dibuat dari nol (belum ada baris
     *     apa pun sebelumnya, jadi tidak perlu strategi migrasi data).
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ---- Kolom baru di transactions ----
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

            // ---- Tabel baru: cashiers ----
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

            // ---- Tabel baru: shifts ----
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

    /** Daftar semua migrasi yang terdaftar pada [androidx.room.RoomDatabase]. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}