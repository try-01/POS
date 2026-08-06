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

@RunWith(AndroidJUnit4::class)
class PosDatabaseMigrationTest {
    private val testDbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PosDatabase::class.java,
        )

    @Test
    fun migrateAll_1To18_preservesDataAndAppliesAllSchemaChanges() {
        // 1. Buat database mentah di Versi 1
        val v1: SupportSQLiteDatabase = helper.createDatabase(testDbName, 1)
        v1.execSQL(
            """
            INSERT INTO products
                (id, name, sku, price, stock, active, createdAt, updatedAt)
            VALUES
                (1, 'Kopi Test', 'SKU-TEST', 8000, 10, 1, 1700000000000, 1700000000000)
            """.trimIndent(),
        )
        v1.close()

        // 2. Jalankan SELURUH migrasi sekaligus (Versi 1 ke 18) menggunakan array Migrations.ALL
        val v18: SupportSQLiteDatabase =
            helper.runMigrationsAndValidate(
                testDbName,
                18, // Target versi database terbaru
                true,
                *Migrations.ALL
            )

        // 3. Validasi apakah data awal tetap aman dan tabel memiliki skema kolom baru
        v18.query("SELECT * FROM products WHERE id = 1").use { cursor: Cursor ->
            assertTrue("Baris id=1 harus tetap ada setelah migrasi ke V18", cursor.moveToFirst())

            // Data Lama (Harus tidak berubah)
            assertEquals("Kopi Test", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(8000L, cursor.getLong(cursor.getColumnIndexOrThrow("price")))
            
            // Di MIGRATION_14_15, tipe data stock berubah menjadi REAL (Double)
            assertEquals(10.0, cursor.getDouble(cursor.getColumnIndexOrThrow("stock")), 0.001)

            // Kolom baru dari MIGRATION_1_2 (Cost)
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("cost")))

            // Kolom baru dari MIGRATION_8_9 (Barcode)
            assertTrue("Barcode harus null secara default", cursor.isNull(cursor.getColumnIndexOrThrow("barcode")))

            // Kolom baru dari MIGRATION_9_10 (Category)
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("category")))

            // Kolom baru dari MIGRATION_15_16 (Damaged Stock)
            assertEquals(0.0, cursor.getDouble(cursor.getColumnIndexOrThrow("damagedStock")), 0.001)
        }
        v18.close()
    }
}
