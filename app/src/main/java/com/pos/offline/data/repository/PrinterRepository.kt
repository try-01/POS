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
    suspend fun add(printer: PrinterEntity): Long = printerDao.insertAndSyncDefault(printer)
    suspend fun update(printer: PrinterEntity) = printerDao.updateAndSyncDefault(printer)
    suspend fun delete(printer: PrinterEntity) = printerDao.delete(printer)
    suspend fun setAsDefault(printer: PrinterEntity) = printerDao.setAsDefault(printer)
    suspend fun updatePriority(id: Long, priority: Int) = printerDao.updatePriority(id, priority)
    suspend fun incrementStatusQueryFailStreak(id: Long): Int =
        printerDao.incrementAndGetStatusQueryFailStreak(id)
    suspend fun resetStatusQueryFailStreak(id: Long) = printerDao.resetStatusQueryFailStreak(id)
    suspend fun disableStatusQuery(id: Long) = printerDao.disableStatusQuery(id)
}
