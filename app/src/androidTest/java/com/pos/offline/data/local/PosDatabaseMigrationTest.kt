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
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PosDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesDataAndAddsCostColumn() {
        val v1: SupportSQLiteDatabase = helper.createDatabase(testDbName, 1)

        v1.execSQL(
            """
            INSERT INTO products
                (id, name, sku, price, stock, active, createdAt, updatedAt)
            VALUES
                (1, 'Kopi Test', 'SKU-TEST', 8000, 10, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )
        v1.close()

        val v2: SupportSQLiteDatabase =
            helper.runMigrationsAndValidate(testDbName, 2, true, Migrations.MIGRATION_1_2)

        v2.query("SELECT name, price, stock, cost FROM products WHERE id = 1").use { cursor: Cursor ->
            assertTrue("Baris id=1 harus tetap ada setelah migrasi", cursor.moveToFirst())

            assertEquals("Kopi Test", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(8000L, cursor.getLong(cursor.getColumnIndexOrThrow("price")))
            assertEquals(10, cursor.getInt(cursor.getColumnIndexOrThrow("stock")))

            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("cost")))
        }
        v2.close()
    }
}
