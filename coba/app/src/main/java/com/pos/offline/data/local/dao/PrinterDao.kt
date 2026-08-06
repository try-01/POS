package com.pos.offline.data.local.dao
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pos.offline.data.local.entity.PrinterEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface PrinterDao {
    @Query("SELECT * FROM printers ORDER BY priority ASC")
    fun observeAll(): Flow<List<PrinterEntity>>
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
    @Delete
    suspend fun delete(printer: PrinterEntity)
    @Query("UPDATE printers SET isDefault = 0 WHERE id != :exceptId")
    suspend fun clearDefaultExcept(exceptId: Long)
    @Query("UPDATE printers SET isDefault = 0")
    suspend fun clearAllDefault()
    @Transaction
    suspend fun insertAndSyncDefault(printer: PrinterEntity): Long {
        val id = insert(printer)
        if (printer.isDefault) {
            clearDefaultExcept(id)
        }
        return id
    }
    @Transaction
    suspend fun updateAndSyncDefault(printer: PrinterEntity) {
        update(printer)
        if (printer.isDefault) {
            clearDefaultExcept(printer.id)
        }
    }
    @Transaction
    suspend fun setAsDefault(printer: PrinterEntity) {
        clearAllDefault()
        update(printer.copy(isDefault = true))
    }
    @Query("UPDATE printers SET statusQueryFailStreak = statusQueryFailStreak + 1 WHERE id = :id")
    suspend fun incrementStatusQueryFailStreakRaw(id: Long)
    @Query("SELECT statusQueryFailStreak FROM printers WHERE id = :id")
    suspend fun getStatusQueryFailStreak(id: Long): Int?
    @Transaction
    suspend fun incrementAndGetStatusQueryFailStreak(id: Long): Int {
        incrementStatusQueryFailStreakRaw(id)
        return getStatusQueryFailStreak(id) ?: 0
    }
    @Query("UPDATE printers SET statusQueryFailStreak = 0 WHERE id = :id")
    suspend fun resetStatusQueryFailStreak(id: Long)
    @Query(
        "UPDATE printers SET supportsStatusQuery = 0, statusQueryFailStreak = 0, " +
            "autoDisabledDueToNoResponse = 1 WHERE id = :id",
    )
    suspend fun disableStatusQuery(id: Long)
    @Query("UPDATE printers SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(
        id: Long,
        priority: Int,
    )
}
