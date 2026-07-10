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
     *
     * `ALTER TABLE ... ADD COLUMN` bersifat tambahan (additive) sehingga:
     *  - Cepat & aman: SQLite hanya menambah metadata kolom + nilai default.
     *  - Tidak mengubah/menghapus baris yang sudah ada → stok & riwayat aman.
     *
     * `INTEGER NOT NULL DEFAULT 0` HARUS sama persis dengan anotasi entity
     * (`@ColumnInfo(defaultValue = "0")`) agar validasi skema Room lolos.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE products ADD COLUMN cost INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /** Daftar semua migrasi yang terdaftar pada [androidx.room.RoomDatabase]. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
