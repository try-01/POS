package com.pos.offline.util

import android.content.Context
import android.net.Uri
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.BorderSide
import org.dhatim.fastexcel.BorderStyle
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
        "No",
        "SKU",
        "Barcode",
        "Nama Produk",
        "Kategori",
        "Harga Jual",
        "Modal",
        "Stok",
        "Margin",
        "Nilai Stok"
    )

    private val COLUMN_WIDTHS = listOf(
        6.0, 15.0, 18.0, 32.0, 18.0,
        18.0, 18.0, 12.0, 14.0, 20.0
    )

    private const val REQUIRED_IMPORT_COLUMNS = 7

    private const val COLOR_HEADER_BG = "1F4E79"
    private const val COLOR_HEADER_TEXT = "FFFFFF"
    private const val COLOR_TITLE_BG = "2E75B6"
    private const val COLOR_TITLE_TEXT = "FFFFFF"
    private const val COLOR_ROW_EVEN = "F2F7FC"
    private const val COLOR_ROW_ODD = "FFFFFF"
    private const val COLOR_BORDER = "B4C6E7"
    private const val COLOR_SUMMARY_BG = "D6E4F0"
    private const val COLOR_NEGATIVE = "FF0000"
    private const val COLOR_POSITIVE = "006100"

    fun suggestedExportFileName(): String =
        "produk_${System.currentTimeMillis()}.xlsx"

    // =================================================================
    //  EXPORT
    // =================================================================

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
            try {
                outputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun writeWorkbook(
        outputStream: OutputStream,
        products: List<ProductEntity>,
    ) {
        Workbook(outputStream, "POS Offline", "1.0").use { wb ->
            val ws = wb.newWorksheet("Produk")

            COLUMN_WIDTHS.forEachIndexed { col, width ->
                ws.width(col, width)
            }

            ws.freezePane(0, 2)

            writeTitleRow(ws, products.size)
            writeHeaderRow(ws)

            var totalPrice = 0L
            var totalCost = 0L
            var totalStock = 0.0
            var totalStockValue = 0.0

            products.forEachIndexed { idx, p ->
                val margin = if (p.price > 0) p.price - p.cost else 0L
                val stockValue = p.stock * p.cost
                totalPrice += p.price
                totalCost += p.cost
                totalStock += p.stock
                totalStockValue += stockValue
                writeDataRow(ws, idx + 2, idx + 1, p, margin, stockValue)
            }

            val summaryRow = products.size + 2
            writeSummaryRow(
                ws, summaryRow, products.size,
                totalPrice, totalCost, totalStock, totalStockValue
            )
        }
    }

    // -----------------------------------------------------------------
    //  TITLE ROW
    // -----------------------------------------------------------------

    private fun writeTitleRow(ws: Worksheet, productCount: Int) {
        ws.rowHeight(0, 30.0)
        val title = "Data Produk — Total: $productCount produk"
        ws.value(0, 0, title)

        for (col in HEADERS.indices) {
            ws.style(0, col)
                .bold()
                .fontSize(if (col == 0) 14 else 11)
                .fontColor(COLOR_TITLE_TEXT)
                .fillColor(COLOR_TITLE_BG)
                .horizontalAlignment(if (col == 0) "left" else "center")
                .verticalAlignment("center")
                .set()
        }
    }

    // -----------------------------------------------------------------
    //  HEADER ROW
    // -----------------------------------------------------------------

    private fun writeHeaderRow(ws: Worksheet) {
        ws.rowHeight(1, 24.0)

        HEADERS.forEachIndexed { col, title ->
            ws.value(1, col, title)
            ws.style(1, col)
                .bold()
                .fontSize(11)
                .fontColor(COLOR_HEADER_TEXT)
                .fillColor(COLOR_HEADER_BG)
                .horizontalAlignment("center")
                .verticalAlignment("center")
                .borderStyle(BorderSide.BOTTOM, BorderStyle.MEDIUM)
                .borderColor(BorderSide.BOTTOM, COLOR_BORDER)
                .set()
        }
    }

    // -----------------------------------------------------------------
    //  DATA ROW
    // -----------------------------------------------------------------

    private fun writeDataRow(
        ws: Worksheet,
        rowIndex: Int,
        number: Int,
        p: ProductEntity,
        margin: Long,
        stockValue: Double,
    ) {
        val isEven = number % 2 == 0
        val bgColor = if (isEven) COLOR_ROW_EVEN else COLOR_ROW_ODD
        val marginColor = if (margin >= 0) COLOR_POSITIVE else COLOR_NEGATIVE

        ws.rowHeight(rowIndex, 20.0)

        // Col 0: No
        ws.value(rowIndex, 0, number)
        ws.style(rowIndex, 0)
            .fontSize(10)
            .fillColor(bgColor)
            .horizontalAlignment("center")
            .verticalAlignment("center")
            .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN)
            .borderColor(BorderSide.BOTTOM, COLOR_BORDER)
            .set()

        // Col 1: SKU
        ws.value(rowIndex, 1, p.sku)
        applyTextStyle(ws, rowIndex, 1, bgColor)

        // Col 2: Barcode
        ws.value(rowIndex, 2, p.barcode ?: "")
        applyTextStyle(ws, rowIndex, 2, bgColor)

        // Col 3: Nama Produk (bold)
        ws.value(rowIndex, 3, p.name)
        ws.style(rowIndex, 3)
            .bold()
            .fontSize(10)
            .fillColor(bgColor)
            .verticalAlignment("center")
            .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN)
            .borderColor(BorderSide.BOTTOM, COLOR_BORDER)
            .set()

        // Col 4: Kategori
        ws.value(rowIndex, 4, p.category ?: "")
        applyTextStyle(ws, rowIndex, 4, bgColor)

        // Col 5: Harga Jual
        ws.value(rowIndex, 5, p.price.toDouble())
        applyCurrencyStyle(ws, rowIndex, 5, bgColor)

        // Col 6: Modal
        ws.value(rowIndex, 6, p.cost.toDouble())
        applyCurrencyStyle(ws, rowIndex, 6, bgColor)

        // Col 7: Stok
        ws.value(rowIndex, 7, p.stock)
        ws.style(rowIndex, 7)
            .fontSize(10)
            .fillColor(bgColor)
            .format("#,##0.##")
            .horizontalAlignment("right")
            .verticalAlignment("center")
            .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN)
            .borderColor(BorderSide.BOTTOM, COLOR_BORDER)
            .set()

        // Col 8: Margin
        ws.value(rowIndex, 8, margin.toDouble())
        ws.style(rowIndex, 8)
            .bold()
            .fontSize(10)
            .fillColor(bgColor)
            .fontColor(marginColor)
            .format("#,##0")
            .horizontalAlignment("right")
            .verticalAlignment("center")
            .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN)
            .borderColor(BorderSide.BOTTOM, COLOR_BORDER)
            .set()

        // Col 9: Nilai Stok
        ws.value(rowIndex, 9, stockValue)
        applyCurrencyStyle(ws, rowIndex, 9, bgColor)
    }

    // -----------------------------------------------------------------
    //  STYLE HELPERS
    // -----------------------------------------------------------------

    private fun applyTextStyle(ws: Worksheet, row: Int, col: Int, bgColor: String) {
        ws.style(row, col)
            .fontSize(10)
            .fillColor(bgColor)
            .verticalAlignment("center")
            .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN)
            .borderColor(BorderSide.BOTTOM, COLOR_BORDER)
            .set()
    }

    private fun applyCurrencyStyle(ws: Worksheet, row: Int, col: Int, bgColor: String) {
        ws.style(row, col)
            .fontSize(10)
            .fillColor(bgColor)
            .format("#,##0")
            .horizontalAlignment("right")
            .verticalAlignment("center")
            .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN)
            .borderColor(BorderSide.BOTTOM, COLOR_BORDER)
            .set()
    }

    // -----------------------------------------------------------------
    //  SUMMARY ROW
    // -----------------------------------------------------------------

    private fun writeSummaryRow(
        ws: Worksheet,
        rowIndex: Int,
        productCount: Int,
        totalPrice: Long,
        totalCost: Long,
        totalStock: Double,
        totalStockValue: Double,
    ) {
        val totalMargin = totalPrice - totalCost
        val marginColor = if (totalMargin >= 0) COLOR_POSITIVE else COLOR_NEGATIVE

        ws.rowHeight(rowIndex, 26.0)

        // Kolom kosong 0-2 dengan background
        for (col in 0..2) {
            ws.style(rowIndex, col)
                .fillColor(COLOR_SUMMARY_BG)
                .borderStyle(BorderSide.TOP, BorderStyle.DOUBLE)
                .borderColor(BorderSide.TOP, COLOR_HEADER_BG)
                .set()
        }

        // Col 3: Label TOTAL
        ws.value(rowIndex, 3, "TOTAL ($productCount produk)")
        ws.style(rowIndex, 3)
            .bold()
            .fontSize(11)
            .fillColor(COLOR_SUMMARY_BG)
            .horizontalAlignment("right")
            .verticalAlignment("center")
            .borderStyle(BorderSide.TOP, BorderStyle.DOUBLE)
            .borderColor(BorderSide.TOP, COLOR_HEADER_BG)
            .set()

        // Col 4: Kategori kosong
        ws.style(rowIndex, 4)
            .fillColor(COLOR_SUMMARY_BG)
            .borderStyle(BorderSide.TOP, BorderStyle.DOUBLE)
            .borderColor(BorderSide.TOP, COLOR_HEADER_BG)
            .set()

        // Col 5: Total harga
        ws.value(rowIndex, 5, totalPrice.toDouble())
        applySummaryNumberStyle(ws, rowIndex, 5, "#,##0")

        // Col 6: Total modal
        ws.value(rowIndex, 6, totalCost.toDouble())
        applySummaryNumberStyle(ws, rowIndex, 6, "#,##0")

        // Col 7: Total stok
        ws.value(rowIndex, 7, totalStock)
        applySummaryNumberStyle(ws, rowIndex, 7, "#,##0.##")

        // Col 8: Total margin (warna)
        ws.value(rowIndex, 8, totalMargin.toDouble())
        ws.style(rowIndex, 8)
            .bold()
            .fontSize(11)
            .fillColor(COLOR_SUMMARY_BG)
            .fontColor(marginColor)
            .format("#,##0")
            .horizontalAlignment("right")
            .verticalAlignment("center")
            .borderStyle(BorderSide.TOP, BorderStyle.DOUBLE)
            .borderColor(BorderSide.TOP, COLOR_HEADER_BG)
            .set()

        // Col 9: Total nilai stok
        ws.value(rowIndex, 9, totalStockValue)
        applySummaryNumberStyle(ws, rowIndex, 9, "#,##0")
    }

    private fun applySummaryNumberStyle(
        ws: Worksheet,
        row: Int,
        col: Int,
        numberFormat: String,
    ) {
        ws.style(row, col)
            .bold()
            .fontSize(11)
            .fillColor(COLOR_SUMMARY_BG)
            .format(numberFormat)
            .horizontalAlignment("right")
            .verticalAlignment("center")
            .borderStyle(BorderSide.TOP, BorderStyle.DOUBLE)
            .borderColor(BorderSide.TOP, COLOR_HEADER_BG)
            .set()
    }

    // =================================================================
    //  IMPORT
    // =================================================================

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
            try {
                inputStream?.close()
            } catch (_: Exception) {
            }
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
                var skipOffset = 0
                var headerFound = false
                val iterator = rowStream.iterator()

                while (iterator.hasNext()) {
                    withContext(Dispatchers.IO) { ensureActive() }

                    val row = iterator.next()

                    if (!headerFound) {
                        val detection = detectHeader(row)
                        if (detection != null) {
                            headerFound = true
                            skipOffset = detection
                            rowIndex++
                            continue
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

                    val parsed = parseRow(row, rowIndex, skipOffset)
                    if (parsed != null) {
                        parsed.first?.let { rows.add(it) }
                        parsed.second?.let { errors.add(it) }
                    }

                    rowIndex++
                }

                if (!headerFound) {
                    return ExcelImportResult(
                        emptyList(),
                        listOf(
                            "Header tidak ditemukan. " +
                                    "File harus memiliki kolom: SKU, Barcode, Nama, " +
                                    "Kategori, Harga Jual, Modal, Stok"
                        )
                    )
                }
            }
        }

        rows.trimToSize()
        return ExcelImportResult(rows, errors)
    }

    private fun detectHeader(
        row: org.dhatim.fastexcel.reader.Row,
    ): Int? {
        if (row.cellCount < REQUIRED_IMPORT_COLUMNS) return null

        fun cell(c: Int): String = 
            getSafeCellString(row, c).lowercase()

        val cell0 = cell(0)
        val cell1 = cell(1)
        val cell2 = cell(2)

        if ((cell0 == "no" || cell0 == "no.") && cell1.contains("sku")) {
            return 1
        }

        if (cell0.contains("sku") &&
            (cell2.contains("nama") || cell2.contains("name"))
        ) {
            return 0
        }

        val allCells = (0 until minOf(row.cellCount, 10)).map { cell(it) }
        val headerKeywords = listOf("sku", "nama", "harga", "modal", "stok")
        val matchCount = headerKeywords.count { keyword ->
            allCells.any { it.contains(keyword) }
        }

        if (matchCount >= 3) {
            return if (cell0 == "no" || cell0 == "no." ||
                cell0.contains("nomor") || cell0 == "#"
            ) 1 else 0
        }

        return null
    }

    private fun parseRow(
        row: org.dhatim.fastexcel.reader.Row,
        rowIndex: Int,
        skipOffset: Int,
    ): Pair<ImportedProductRow?, String?>? {

        fun cell(logicalCol: Int): String = 
            getSafeCellString(row, logicalCol + skipOffset)

        val allBlank = (0 until REQUIRED_IMPORT_COLUMNS).all {
            cell(it).isBlank()
        }
        if (allBlank) return null

        val firstCell = cell(0).lowercase()
        if (firstCell.contains("total") || firstCell.contains("jumlah")) {
            return null
        }

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

    // =================================================================
    //  NUMBER PARSING
    // =================================================================

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

    // =================================================================
    //  UTILITY
    // =================================================================

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?.use { it.length } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    // =================================================================
    //  FUNGSI PEMBANTU BACA EXCEL YANG AMAN
    // =================================================================
    
    /**
     * Membaca isi sel Excel dengan aman tanpa mempedulikan tipe datanya 
     * (Angka maupun Teks tidak akan crash).
     */
    private fun getSafeCellString(row: org.dhatim.fastexcel.reader.Row, col: Int): String {
        // 1. Mencegah error jika kolom yang diminta di luar batas
        if (col < 0 || col >= row.cellCount) return ""
        
        // 2. Ambil sel. Jika kosong, kembalikan teks kosong
        val cell = row.getCell(col).orElse(null) ?: return ""
        
        // 3. Coba ambil teks format asli dari Excel (getText). 
        // Jika gagal, ambil nilai mentahnya (getValue) lalu ubah ke String.
        val textValue = cell.text ?: cell.value?.toString() ?: ""
        
        // 4. Jika Excel mengubah angka bulat jadi desimal (contoh SKU "1234" dibaca "1234.0"), 
        // kita buang ".0" di belakangnya agar rapi.
        return if (textValue.endsWith(".0")) {
            textValue.dropLast(2).trim()
        } else {
            textValue.trim()
        }
    }
}