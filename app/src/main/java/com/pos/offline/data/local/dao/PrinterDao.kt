package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pos.offline.data.local.entity.PrinterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterDao {

    @Query("SELECT * FROM printers ORDER BY priority ASC")
    fun observeAll(): Flow<List<PrinterEntity>>

    /** Versi non-Flow, dipakai PrintCoordinator (Batch H6) untuk iterasi
     *  fallback printer cadangan di tengah proses cetak. */
    @Query("SELECT * FROM printers ORDER BY priority ASC")
    suspend fun getAllOrderedByPriority(): List<PrinterEntity>

    @Query("SELECT * FROM printers WHERE id = :id")
    suspend fun getById(id: Long): PrinterEntity?

    @Query("SELECT * FROM printers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): PrinterEntity?

    @Insert
    suspend fun insert(printer: PrinterEntity): Long

    @Update
    suspend fun update(printer: PrinterEntity)

    /** Hard delete -- printer tidak direferensikan data transaksi historis apa pun. */
    @Delete
    suspend fun delete(printer: PrinterEntity)

    /** Dipanggil setelah insert/update printer dengan isDefault=true, supaya
     *  cuma ada 1 default aktif dalam satu waktu. */
    @Query("UPDATE printers SET isDefault = 0 WHERE id != :exceptId")
    suspend fun clearDefaultExcept(exceptId: Long)

    @Query("UPDATE printers SET isDefault = 0")
    suspend fun clearAllDefault()
}