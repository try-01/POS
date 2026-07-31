package com.pos.offline.util
import android.content.Context
import android.net.Uri
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import java.io.IOException
import kotlin.math.round
sealed class ExcelOutcome {
    object Success : ExcelOutcome()
    data class Error(
        val throwable: Throwable,
    ) : ExcelOutcome()
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
    fun suggestedExportFileName(): String = "produk_${System.currentTimeMillis()}.xlsx"
    suspend fun exportProducts(
        context: Context,
        products: List<ProductEntity>,
        destinationUri: Uri,
    ): ExcelOutcome =
        withContext(Dispatchers.IO) {
            try {
                // SXSSFWorkbook implements AutoCloseable.
                // Di POI 5.3.0+, workbook.close() (dipanggil otomatis oleh .use)
                // akan menutup stream SEKALIGUS menghapus temporary files dari disk.
                SXSSFWorkbook(100).use { workbook ->
                    val sheet = workbook.createSheet("Produk")
                    val header = sheet.createRow(0)
                    listOf("SKU", "Barcode", "Nama", "Kategori", "Harga Jual", "Modal", "Stok")
                        .forEachIndexed { i, title ->
                            header.createCell(i).setCellValue(title)
                            sheet.setColumnWidth(i, 4000)
                        }

                    products.forEachIndexed { idx, p ->
                        val row = sheet.createRow(idx + 1)
                        row.createCell(0).setCellValue(p.sku)
                        row.createCell(1).setCellValue(p.barcode ?: "")
                        row.createCell(2).setCellValue(p.name)
                        row.createCell(3).setCellValue(p.category)
                        row.createCell(4).setCellValue(p.price.toDouble())
                        row.createCell(5).setCellValue(p.cost.toDouble())
                        row.createCell(6).setCellValue(p.stock)
                    }

                    val result = context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        workbook.write(outputStream)
                        ExcelOutcome.Success
                    } ?: ExcelOutcome.Error(IOException("Tidak bisa membuka output stream"))
                    
                    // Baris workbook.dispose() telah dihapus di sini karena .use sudah sepenuhnya menanganinya
                    result
                }
            } catch (e: Exception) {
                ExcelOutcome.Error(e)
            }
    }
    private fun parseFlexibleNumber(raw: String): Double? {
        val cleaned = raw.trim().filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
        if (cleaned.isBlank() || cleaned == "-") return null
        val negative = cleaned.startsWith("-")
        val body = cleaned.removePrefix("-")
        if (body.isBlank()) return null
        val hasDot = body.contains('.')
        val hasComma = body.contains(',')
        val normalized: String =
            when {
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
                    val lastDot = body.lastIndexOf('.')
                    val digitsAfter = body.length - lastDot - 1
                    val dotCount = body.count { it == '.' }
                    if (dotCount > 1 || digitsAfter == 3) {
                        body.replace(".", "") 
                    } else {
                        body 
                    }
                }
                hasComma -> {
                    val lastComma = body.lastIndexOf(',')
                    val digitsAfter = body.length - lastComma - 1
                    val commaCount = body.count { it == ',' }
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
    suspend fun importProducts(
        context: Context,
        sourceUri: Uri,
    ): ExcelImportResult =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                WorkbookFactory.create(input).use { workbook ->
                    val sheet = workbook.getSheetAt(0)
                    val fmt = DataFormatter()
                    val rows = mutableListOf<ImportedProductRow>()
                    val errors = mutableListOf<String>()
                    fun parseCurrency(
                        s: String,
                        field: String,
                    ): Long {
                        val value = parseFlexibleNumber(s) ?: error("$field tidak valid: \"$s\"")
                        require(value >= 0) { "$field bernilai negatif" }
                        return round(value).toLong()
                    }
                    fun parseQty(
                        s: String,
                        field: String,
                    ): Double {
                        val value = parseFlexibleNumber(s) ?: error("$field tidak valid: \"$s\"")
                        require(value >= 0) { "$field bernilai negatif" }
                        return value
                    }
                    for (i in 1..sheet.lastRowNum) {
                        val row = sheet.getRow(i) ?: continue
                        fun cell(c: Int) = fmt.formatCellValue(row.getCell(c)).trim()
                        val allBlank = (0..6).all { cell(it).isBlank() }
                        if (allBlank) continue
                        runCatching {
                            ImportedProductRow(
                                sku = cell(0).also { require(it.isNotBlank()) { "SKU kosong" } },
                                barcode = cell(1).ifBlank { null },
                                name = cell(2).also { require(it.isNotBlank()) { "nama kosong" } },
                                category = cell(3).ifBlank { null },
                                price = parseCurrency(cell(4), "harga"),
                                cost = parseCurrency(cell(5), "modal"),
                                stock = parseQty(cell(6), "stok"),
                            )
                        }.onSuccess { rows.add(it) }
                            .onFailure { errors.add("Baris ${i + 1}: ${it.message}") }
                    }
                    ExcelImportResult(rows, errors)
                }
            } ?: ExcelImportResult(emptyList(), listOf("File tidak bisa dibuka"))
        }
}
