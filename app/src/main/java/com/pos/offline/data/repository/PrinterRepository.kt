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

    /** Update kolom priority saja secara atomik (lihat catatan di PrinterDao). */
    suspend fun updatePriority(id: Long, priority: Int) = printerDao.updatePriority(id, priority)

    /** Menaikkan fail-streak status-query printer secara atomik, mengembalikan nilai terbaru. */
    suspend fun incrementStatusQueryFailStreak(id: Long): Int =
        printerDao.incrementAndGetStatusQueryFailStreak(id)

    /** Reset fail-streak ke 0 (dipanggil saat status-query berhasil merespons). */
    suspend fun resetStatusQueryFailStreak(id: Long) = printerDao.resetStatusQueryFailStreak(id)

    /** Matikan fitur deteksi status kertas untuk printer ini setelah gagal berulang kali. */
    suspend fun disableStatusQuery(id: Long) = printerDao.disableStatusQuery(id)
}
