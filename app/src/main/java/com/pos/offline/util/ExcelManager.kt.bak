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
    val stock: Int,
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
            val workbook = SXSSFWorkbook(100)
            try {
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
                    row.createCell(6).setCellValue(p.stock.toDouble())
                }

                context.contentResolver.openOutputStream(destinationUri)?.use { workbook.write(it) }
                    ?: return@withContext ExcelOutcome.Error(IOException("Tidak bisa membuka output stream"))

                ExcelOutcome.Success
            } catch (e: Exception) {
                ExcelOutcome.Error(e)
            } finally {
                workbook.dispose()
                workbook.close()
            }
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
                        val t = s.trim()
                        require(!t.startsWith("-")) { "$field bernilai negatif" }
                        return t.filter { it.isDigit() }.toLongOrNull()
                            ?: error("$field tidak valid: \"$s\"")
                    }

                    fun parseQty(
                        s: String,
                        field: String,
                    ): Int {
                        val t = s.trim()
                        require(!t.startsWith("-")) { "$field bernilai negatif" }
                        return t.filter { it.isDigit() }.toIntOrNull()
                            ?: error("$field tidak valid: \"$s\"")
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
