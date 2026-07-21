package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.PrinterDao
import com.pos.offline.data.local.entity.PrinterEntity
import kotlinx.coroutines.flow.Flow

class PrinterRepository(
    private val printerDao: PrinterDao,
) {
    val allPrinters: Flow<List<PrinterEntity>> = printerDao.observeAll()

    suspend fun getById(id: Long): PrinterEntity? = printerDao.getById(id)

    suspend fun getDefault(): PrinterEntity? = printerDao.getDefault()

    suspend fun getAllOrderedByPriority(): List<PrinterEntity> = printerDao.getAllOrderedByPriority()

    suspend fun add(printer: PrinterEntity): Long {
        val id = printerDao.insert(printer)
        if (printer.isDefault) {
            printerDao.clearDefaultExcept(id)
        }
        return id
    }

    suspend fun update(printer: PrinterEntity) {
        printerDao.update(printer)
        if (printer.isDefault) {
            printerDao.clearDefaultExcept(printer.id)
        }
    }

    suspend fun delete(printer: PrinterEntity) = printerDao.delete(printer)

    suspend fun setAsDefault(printer: PrinterEntity) {
        printerDao.clearAllDefault()
        printerDao.update(printer.copy(isDefault = true))
    }
}
