package com.pos.offline.util

import android.content.Context
import android.net.Uri
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
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

    private const val MAX_IMPORT_ROWS = 50_000
    private const val MAX_IMPORT_FILE_SIZE = 50L * 1024 * 1024

    private val HEADERS = listOf(
        "SKU", "Barcode", "Nama Produk", "Kategori",
        "Harga Jual", "Modal", "Stok"
    )

    // Lebar kolom (karakter)
    private val COLUMN_WIDTHS = listOf(
        15.0,  // SKU
        18.0,  // Barcode
        30.0,  // Nama Produk
        20.0,  // Kategori
        18.0,  // Harga Jual
        18.0,  // Modal
        12.0   // Stok
    )

    private const val REQUIRED_COLUMN_COUNT = 7

    fun suggestedExportFileName(): String =
        "produk_${System.currentTimeMillis()}.xlsx"

    // =========================================================
    //  EXPORT
    // =========================================================

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
            try { outputStream?.close() } catch (_: Exception) { }
        }
    }

    private fun writeWorkbook(
        outputStream: OutputStream,
        products: List<ProductEntity>,
    ) {
        Workbook(outputStream, "POS Offline", "1.0").use { wb ->
            val ws = wb.newWorksheet("Produk")

            // Setup lebar kolom
            COLUMN_WIDTHS.forEachIndexed { col, width ->
                ws.width(col, width)
            }

            // Tulis header dengan formatting
            writeHeader(wb, ws)

            // Tulis data
            products.forEachIndexed { idx, p ->
                writeDataRow(wb, ws, idx + 1, p)
            }
        }
    }

    private fun writeHeader(wb: Workbook, ws: Worksheet) {
        // Style header: bold, background biru, teks putih
        val headerStyle = wb.style()
            .bold()
            .fontSize(11)
            .fontColor("FFFFFF")
            .fillColor("2E75B6")
            .horizontalAlignment("center")
            .verticalAlignment("center")
            .set()

        ws.rowHeight(0, 22.0)

        HEADERS.forEachIndexed { col, title ->
            ws.value(0, col, title)
            ws.style(0, col).style(headerStyle).set()
        }
    }

    private fun writeDataRow(
        wb: Workbook,
        ws: Worksheet,
        rowIndex: Int,
        p: ProductEntity,
    ) {
        // Style alternating row (zebra)
        val isEven = rowIndex % 2 == 0
        val bgColor = if (isEven) "F2F2F2" else "FFFFFF"

        val textStyle = wb.style()
            .fontSize(10)
            .fillColor(bgColor)
            .verticalAlignment("center")
            .set()

        val numberStyle = wb.style()
            .fontSize(10)
            .fillColor(bgColor)
            .format("#,##0")
            .horizontalAlignment("right")
            .verticalAlignment("center")
            .set()

        val decimalStyle = wb.style()
            .fontSize(10)
            .fillColor(bgColor)
            .format("#,##0.##")
            .horizontalAlignment("right")
            .verticalAlignment("center")
            .set()

        ws.rowHeight(rowIndex, 18.0)

        // Isi data dengan style
        ws.value(rowIndex, 0, p.sku)
        ws.style(rowIndex, 0).style(textStyle).set()

        ws.value(rowIndex, 1, p.barcode ?: "")
        ws.style(rowIndex, 1).style(textStyle).set()

        ws.value(rowIndex, 2, p.name)
        ws.style(rowIndex, 2).style(textStyle).set()

        ws.value(rowIndex, 3, p.category ?: "")
        ws.style(rowIndex, 3).style(textStyle).set()

        // Harga & modal sebagai angka (bukan string)
        // Agar Excel bisa sort/filter/sum
        ws.value(rowIndex, 4, p.price.toDouble())
        ws.style(rowIndex, 4).style(numberStyle).set()

        ws.value(rowIndex, 5, p.cost.toDouble())
        ws.style(rowIndex, 5).style(numberStyle).set()

        // Stok bisa desimal
        ws.value(rowIndex, 6, p.stock)
        ws.style(rowIndex, 6).style(decimalStyle).set()
    }

    // =========================================================
    //  IMPORT
    // =========================================================

    suspend fun importProducts(
        context: Context,
        sourceUri: Uri,
    ): ExcelImportResult = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
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
            try { inputStream?.close() } catch (_: Exception) { }
        }
    }

    private suspend fun readWorkbook(
        inputStream: InputStream,
    ): ExcelImportResult {
        val rows = ArrayList<ImportedProductRow>(256)
        val errors = mutableListOf<String>()

        ReadableWorkbook(inputStream).use { wb ->
            val sheet = wb.firstSheet

            sheet.openStream().use { rowStream ->
                var rowIndex = 0
                val iterator = rowStream.iterator()

                while (iterator.hasNext()) {
                    withContext(Dispatchers.IO) { ensureActive() }

                    val row = iterator.next()

                    if (rowIndex == 0) {
                        val headerError = validateHeader(row)
                        if (headerError != null) {
                            return ExcelImportResult(
                                emptyList(),
                                listOf(headerError)
                            )
                        }
                        rowIndex++
                        continue
                    }

                    if (rows.size >= MAX_IMPORT_ROWS) {
                        errors.add(
                            "Import dibatasi $MAX_IMPORT_ROWS baris. " +
                            "Sisa baris diabaikan."
                        )
                        break
                    }

                    val parsed = parseRow(row, rowIndex)
                    if (parsed != null) {
                        parsed.first?.let { rows.add(it) }
                        parsed.second?.let { errors.add(it) }
                    }

                    rowIndex++
                }
            }
        }

        rows.trimToSize()
        return ExcelImportResult(rows, errors)
    }

    private fun validateHeader(
        row: org.dhatim.fastexcel.reader.Row,
    ): String? {
        if (row.cellCount < REQUIRED_COLUMN_COUNT) {
            return "Format file tidak valid: " +
                   "butuh $REQUIRED_COLUMN_COUNT kolom, " +
                   "ditemukan ${row.cellCount} kolom. " +
                   "Kolom: ${HEADERS.joinToString(", ")}"
        }
        return null
    }

    private fun parseRow(
        row: org.dhatim.fastexcel.reader.Row,
        rowIndex: Int,
    ): Pair<ImportedProductRow?, String?>? {
        fun cell(c: Int): String =
            row.getCellAsString(c).orElse("").trim()

        val allBlank = (0 until REQUIRED_COLUMN_COUNT).all {
            cell(it).isBlank()
        }
        if (allBlank) return null

        return try {
            Pair(
                ImportedProductRow(
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
                ),
                null
            )
        } catch (e: Exception) {
            Pair(null, "Baris ${rowIndex + 1}: ${e.message}")
        }
    }

    // =========================================================
    //  NUMBER PARSING
    // =========================================================

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

        val normalized = when {
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
                val digitsAfter = body.length - body.lastIndexOf('.') - 1
                if (dotCount > 1 || digitsAfter == 3) {
                    body.replace(".", "")
                } else body
            }
            hasComma -> {
                val commaCount = body.count { it == ',' }
                val digitsAfter = body.length - body.lastIndexOf(',') - 1
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

    // =========================================================
    //  UTILITY
    // =========================================================

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?.use { it.length } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }
}