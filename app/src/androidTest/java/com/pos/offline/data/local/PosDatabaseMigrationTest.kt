package com.pos.offline.data.local

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uji migrasi database Room v1 → v2 (penambahan kolom `cost` pada tabel `products`).
 *
 * Dua jaminan yang diuji:
 *  1. SKEMA VALID: `runMigrationsAndValidate` membandingkan struktur tabel hasil
 *     migrasi dengan skema v2 (berkas `2.json` yang dibangkitkan Room dari entity).
 *     Bila migrasi menghasilkan skema yang berbeda → tes GAGAL (fail-loud). Inilah
 *     yang mencegah bug diam-diam akibat migrasi salah.
 *  2. DATA TERJAGA: baris yang sudah ada di v1 tetap utuh setelah migrasi, dan
 *     kolom baru `cost` mendapat nilai DEFAULT-nya (0) untuk baris lama.
 *
 * === PRASYARAT BERKAS SKEMA ===
 * Tes ini butuh berkas skema yang diekspor Room di `app/schemas`:
 *   - `.../PosDatabase/1.json`  (skema v1: tanpa kolom `cost`)
 *   - `.../PosDatabase/2.json`  (skema v2: dengan kolom `cost`)
 * `2.json` dibangkitkan otomatis saat build (version sekarang = 2). Berkas `1.json`
 * HARUS ada di repo (hasil build ketika version masih = 1). Bila belum ada,
 * lakukan: set `version = 1` di PosDatabase, build sekali lalu commit `1.json`,
 * kembalikan `version = 2`. Lihat `room { schemaDirectory(...) }` di build.gradle.kts.
 *
 * Menjalankan: `./gradlew :app:connectedDebugAndroidTest` (butuh emulator/perangkat).
 */
@RunWith(AndroidJUnit4::class)
class PosDatabaseMigrationTest {

    private val testDbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PosDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesDataAndAddsCostColumn() {
        // ---- 1) Buat database pada skema v1 (BELUM ada kolom `cost`) ----
        // createDatabase membaca 1.json → DB kosong dengan struktur v1 (tanpa callback seed).
        val v1: SupportSQLiteDatabase = helper.createDatabase(testDbName, 1)

        // Sisipkan satu produk memakai kolom yang ADA di v1 (tanpa `cost`).
        v1.execSQL(
            """
            INSERT INTO products
                (id, name, sku, price, stock, active, createdAt, updatedAt)
            VALUES
                (1, 'Kopi Test', 'SKU-TEST', 8000, 10, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )
        v1.close()

        // ---- 2) Jalankan migrasi v1→v2 + VALIDASI SKEMA ----
        // Argumen `true` = validateDroppedTables (memastikan tak ada tabel terhapus
        // tak terduga). Bila skema hasil tak cocok dengan 2.json → IllegalStateException.
        val v2: SupportSQLiteDatabase =
            helper.runMigrationsAndValidate(testDbName, 2, true, Migrations.MIGRATION_1_2)

        // ---- 3) Pastikan DATA terjaga & kolom baru ber-default ----
        v2.query("SELECT name, price, stock, cost FROM products WHERE id = 1").use { cursor: Cursor ->
            assertTrue("Baris id=1 harus tetap ada setelah migrasi", cursor.moveToFirst())

            // Data lama utuh (bukan direset/dihapus).
            assertEquals("Kopi Test", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(8000L, cursor.getLong(cursor.getColumnIndexOrThrow("price")))
            assertEquals(10, cursor.getInt(cursor.getColumnIndexOrThrow("stock")))

            // Kolom `cost` baru di v2 wajib ber-nilai DEFAULT 0 untuk baris lama
            // (sesuai "ADD COLUMN cost INTEGER NOT NULL DEFAULT 0" pada MIGRATION_1_2).
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("cost")))
        }
        v2.close()
    }
}
