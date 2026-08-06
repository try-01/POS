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
    fun migrate14To18_preservesDataAndAppliesRecentSchemaChanges() {
        // 1. Buat database mentah langsung di Versi 14
        val v14: SupportSQLiteDatabase = helper.createDatabase(testDbName, 14)
        
        // Insert data simulasi dengan skema V14 (stock masih INTEGER di versi ini)
        v14.execSQL(
            """
            INSERT INTO products
                (id, name, sku, barcode, price, cost, stock, active, createdAt, updatedAt, category)
            VALUES
                (1, 'Produk V14', 'SKU-14', '12345', 10000, 5000, 50, 1, 1000, 1000, 'Kategori Test')
            """.trimIndent()
        )
        v14.close()

        // 2. Jalankan HANYA migrasi 14 ke 18 secara berurutan
        val v18: SupportSQLiteDatabase =
            helper.runMigrationsAndValidate(
                testDbName,
                18,
                true,
                Migrations.MIGRATION_14_15,
                Migrations.MIGRATION_15_16,
                Migrations.MIGRATION_16_17,
                Migrations.MIGRATION_17_18
            )

        // 3. Validasi apakah data awal tetap aman dan tabel memiliki skema baru
        v18.query("SELECT * FROM products WHERE id = 1").use { cursor: Cursor ->
            assertTrue("Baris id=1 harus tetap ada setelah migrasi", cursor.moveToFirst())

            // Data Lama (Harus tidak berubah)
            assertEquals("Produk V14", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(10000L, cursor.getLong(cursor.getColumnIndexOrThrow("price")))
            
            // Di MIGRATION_14_15, tipe data stock berubah menjadi REAL (Double)
            assertEquals(50.0, cursor.getDouble(cursor.getColumnIndexOrThrow("stock")), 0.001)

            // Kolom baru dari MIGRATION_15_16 (Damaged Stock)
            assertEquals(0.0, cursor.getDouble(cursor.getColumnIndexOrThrow("damagedStock")), 0.001)
        }
        
        // 4. Validasi kolom baru di tabel transactions (MIGRATION_16_17)
        v18.query("PRAGMA table_info(transactions)").use { cursor ->
            var hasIsWarrantyExchange = false
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (name == "isWarrantyExchange") hasIsWarrantyExchange = true
            }
            assertTrue("Kolom isWarrantyExchange harus ada di tabel transactions", hasIsWarrantyExchange)
        }
        
        // 5. Validasi kolom baru di tabel returns (MIGRATION_17_18)
        v18.query("PRAGMA table_info(returns)").use { cursor ->
            var hasIsWarrantyExchange = false
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (name == "isWarrantyExchange") hasIsWarrantyExchange = true
            }
            assertTrue("Kolom isWarrantyExchange harus ada di tabel returns", hasIsWarrantyExchange)
        }

        v18.close()
    }
}
