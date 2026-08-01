package com.pos.offline.util

import android.content.Context
import android.net.Uri
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.round

sealed class ExcelOutcome {
    object Success : ExcelOutcome()
    data class Error(val throwable: Throwable) : ExcelOutcome()
}

data class ImportedProductRow(
    val sku: String,
    val barcode: String?,
    val name: String,
    val category: String?,
    val price: Long,
    val cost: Long,
    val stock: Double,
)

data class ExcelImportResult(
    val rows: List<ImportedProductRow>,
    val errors: List<String>,
)

object ExcelManager {

    /**
     * Batas maksimum baris yang bisa diimport.
     * Mencegah OOM jika user import file sangat besar.
     * 50.000 produk sudah sangat banyak untuk POS app.
     */
    private const val MAX_IMPORT_ROWS = 50_000

    /**
     * Batas maksimum ukuran file import (50 MB).
     * File Excel normal untuk data produk biasanya < 5 MB.
     */
    private const val MAX_IMPORT_FILE_SIZE = 50L * 1024 * 1024

    /**
     * Header kolom yang diharapkan.
     */
    private val EXPECTED_HEADERS = listOf(
        "SKU", "Barcode", "Nama", "Kategori",
        "Harga Jual", "Modal", "Stok"
    )

    private const val REQUIRED_COLUMN_COUNT = 7

    fun suggestedExportFileName(): String =
        "produk_${System.currentTimeMillis()}.xlsx"

    // =============================================================
    //  EXPORT
    // =============================================================

    suspend fun exportProducts(
        context: Context,
        products: List<ProductEntity>,
        destinationUri: Uri,
    ): ExcelOutcome = withContext(Dispatchers.IO) {
        var outputStream: OutputStream? = null

        try {
            outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext ExcelOutcome.Error(
                    IOException("Tidak bisa membuka output stream")
                )

            writeWorkbook(outputStream, products)

            ExcelOutcome.Success
        } catch (e: Exception) {
            ExcelOutcome.Error(e)
        } finally {
            // Pastikan stream SELALU ditutup,
            // bahkan jika Workbook.close() gagal
            try {
                outputStream?.close()
            } catch (_: Exception) {
                // Abaikan error saat close
            }
        }
    }

    /**
     * Menulis data produk ke workbook.
     * Menggunakan FastExcel streaming writer → hemat memory.
     *
     * FastExcel Workbook TIDAK membuat temp file di disk
     * (berbeda dengan SXSSFWorkbook POI yang buat temp file).
     * Data langsung di-flush ke OutputStream.
     */
    private fun writeWorkbook(
        outputStream: OutputStream,
        products: List<ProductEntity>,
    ) {
        // Workbook(os, appName, version) → streaming, langsung tulis ke os
        // .use{} memastikan workbook.close() dipanggil → flush & finalize
        Workbook(outputStream, "POS Offline", "1.0").use { wb ->
            val ws = wb.newWorksheet("Produk")

            // Header
            EXPECTED_HEADERS.forEachIndexed { col, title ->
                ws.value(0, col, title)
                ws.width(col, 20.0)
            }

            // Data
            products.forEachIndexed { idx, p ->
                val row = idx + 1
                ws.value(row, 0, p.sku)
                ws.value(row, 1, p.barcode ?: "")
                ws.value(row, 2, p.name)
                ws.value(row, 3, p.category)
                ws.value(row, 4, p.price)
                ws.value(row, 5, p.cost)
                ws.value(row, 6, p.stock)
            }
        }
    }

    // =============================================================
    //  IMPORT
    // =============================================================

    suspend fun importProducts(
        context: Context,
        sourceUri: Uri,
    ): ExcelImportResult = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null

        try {
            // --- Validasi ukuran file ---
            val fileSize = getFileSize(context, sourceUri)
            if (fileSize > MAX_IMPORT_FILE_SIZE) {
                val maxMb = MAX_IMPORT_FILE_SIZE / (1024 * 1024)
                return@withContext ExcelImportResult(
                    emptyList(),
                    listOf("File terlalu besar (maks ${maxMb} MB)")
                )
            }

            inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext ExcelImportResult(
                    emptyList(),
                    listOf("File tidak bisa dibuka")
                )

            readWorkbook(inputStream)
        } catch (e: Exception) {
            ExcelImportResult(
                emptyList(),
                listOf("Error membaca file: ${e.message}")
            )
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Membaca workbook secara streaming (row by row).
     *
     * FastExcel ReadableWorkbook menggunakan streaming XML parser,
     * sehingga TIDAK memuat seluruh file ke RAM.
     *
     * Namun hasil (List<ImportedProductRow>) tetap di memory.
     * Dibatasi MAX_IMPORT_ROWS untuk mencegah OOM.
     */
    private suspend fun readWorkbook(
        inputStream: InputStream,
    ): ExcelImportResult {
        val rows = ArrayList<ImportedProductRow>(256)
        val errors = mutableListOf<String>()

        ReadableWorkbook(inputStream).use { wb ->
            val sheet = wb.firstSheet

            // Gunakan openStream() untuk streaming read
            sheet.openStream().use { rowStream ->
                var rowIndex = 0
                val iterator = rowStream.iterator()

                while (iterator.hasNext()) {
                    // Dukung coroutine cancellation
                    withContext(Dispatchers.IO) { ensureActive() }

                    val row = iterator.next()

                    // Baris pertama = header, validasi lalu skip
                    if (rowIndex == 0) {
                        val headerError = validateHeader(row)
                        if (headerError != null) {
                            return ExcelImportResult(emptyList(), listOf(headerError))
                        }
                        rowIndex++
                        continue
                    }

                    // Cek batas maksimum
                    if (rows.size >= MAX_IMPORT_ROWS) {
                        errors.add(
                            "Import dibatasi maksimal $MAX_IMPORT_ROWS baris. " +
                            "Sisa baris diabaikan."
                        )
                        break
                    }

                    // Parse row
                    val parsed = parseRow(row, rowIndex)
                    if (parsed != null) {
                        parsed.first?.let { rows.add(it) }
                        parsed.second?.let { errors.add(it) }
                    }

                    rowIndex++
                }
            }
        }

        // Trim ArrayList ke ukuran aktual → lepas memory berlebih
        rows.trimToSize()

        return ExcelImportResult(rows, errors)
    }

    /**
     * Validasi header row.
     * Return null jika valid, error message jika tidak.
     */
    private fun validateHeader(
        row: org.dhatim.fastexcel.reader.Row,
    ): String? {
        val cellCount = row.cellCount
        if (cellCount < REQUIRED_COLUMN_COUNT) {
            return "Format file tidak valid: " +
                   "dibutuhkan minimal $REQUIRED_COLUMN_COUNT kolom, " +
                   "ditemukan $cellCount kolom. " +
                   "Kolom yang diharapkan: ${EXPECTED_HEADERS.joinToString(", ")}"
        }
        return null
    }

    /**
     * Parse satu baris data.
     * Return Pair(data, null) jika sukses.
     * Return Pair(null, errorMessage) jika gagal.
     * Return null jika baris kosong (skip).
     */
    private fun parseRow(
        row: org.dhatim.fastexcel.reader.Row,
        rowIndex: Int,
    ): Pair<ImportedProductRow?, String?>? {
        fun cell(c: Int): String =
            row.getCellAsString(c).orElse("").trim()

        // Skip baris kosong
        val allBlank = (0 until REQUIRED_COLUMN_COUNT).all { cell(it).isBlank() }
        if (allBlank) return null

        return try {
            val product = ImportedProductRow(
                sku = cell(0).also {
                    require(it.isNotBlank()) { "SKU kosong" }
                },
                barcode = cell(1).ifBlank { null },
                name = cell(2).also {
                    require(it.isNotBlank()) { "Nama kosong" }
                },
                category = cell(3).ifBlank { null },
                price = parseCurrency(cell(4), "Harga"),
                cost = parseCurrency(cell(5), "Modal"),
                stock = parseQty(cell(6), "Stok"),
            )
            Pair(product, null)
        } catch (e: Exception) {
            Pair(null, "Baris ${rowIndex + 1}: ${e.message}")
        }
    }

    // =============================================================
    //  NUMBER PARSING
    // =============================================================

    private fun parseCurrency(s: String, field: String): Long {
        val value = parseFlexibleNumber(s)
            ?: error("$field tidak valid: \"$s\"")
        require(value >= 0) { "$field bernilai negatif" }
        return round(value).toLong()
    }

    private fun parseQty(s: String, field: String): Double {
        val value = parseFlexibleNumber(s)
            ?: error("$field tidak valid: \"$s\"")
        require(value >= 0) { "$field bernilai negatif" }
        return value
    }

    /**
     * Parse angka dengan format fleksibel.
     * Support: 1000, 1.000, 1,000, 1.000,50, 1,000.50, dll.
     */
    private fun parseFlexibleNumber(raw: String): Double? {
        val cleaned = raw.trim().filter {
            it.isDigit() || it == '.' || it == ',' || it == '-'
        }
        if (cleaned.isBlank() || cleaned == "-") return null

        val negative = cleaned.startsWith("-")
        val body = cleaned.removePrefix("-")
        if (body.isBlank()) return null

        val hasDot = body.contains('.')
        val hasComma = body.contains(',')

        val normalized: String = when {
            hasDot && hasComma -> {
                val lastDot = body.lastIndexOf('.')
                val lastComma = body.lastIndexOf(',')
                if (lastComma > lastDot) {
                    body.replace(".", "").replace(',', '.')
                } else {
                    body.replace(",", "")
                }
            }
            hasDot -> {
                val dotCount = body.count { it == '.' }
                val lastDot = body.lastIndexOf('.')
                val digitsAfter = body.length - lastDot - 1
                if (dotCount > 1 || digitsAfter == 3) {
                    body.replace(".", "")
                } else {
                    body
                }
            }
            hasComma -> {
                val commaCount = body.count { it == ',' }
                val lastComma = body.lastIndexOf(',')
                val digitsAfter = body.length - lastComma - 1
                if (commaCount > 1 || digitsAfter == 3) {
                    body.replace(",", "")
                } else {
                    body.replace(',', '.')
                }
            }
            else -> body
        }

        val value = normalized.toDoubleOrNull() ?: return null
        return if (negative) -value else value
    }

    // =============================================================
    //  UTILITY
    // =============================================================

    /**
     * Mendapatkan ukuran file dari Uri.
     * Return -1 jika tidak bisa ditentukan.
     */
    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }
}