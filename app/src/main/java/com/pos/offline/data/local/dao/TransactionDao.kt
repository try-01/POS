package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.pos.offline.data.local.entity.TransactionEntity
import com.pos.offline.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Akses data transaksi (riwayat & laporan). Header + detail ditulis bersamaan
 * dalam satu [Transaction] (konsistensi penuh, atau rollback seluruhnya).
 */
@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    /**
     * Ambil transaksi pada rentang hari tertentu (untuk laporan harian).
     * SENGAJA tidak difilter status — transaksi VOID tetap harus tampil di
     * daftar riwayat (dengan badge "Dibatalkan"); pengecualian dari
     * agregat pendapatan dilakukan di layer ViewModel ([ReportViewModel.aggregate]),
     * bukan di query ini.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE createdAt >= :startOfDay AND createdAt < :endOfDay
        ORDER BY createdAt DESC
        """
    )
    fun observeByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    /**
     * COALESCE menjaga hasil tetap 0 ketika belum ada transaksi (tidak null).
     * BATCH D: exclude status VOID — pendapatan transaksi yang dibatalkan
     * tidak boleh ikut terhitung.
     */
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE createdAt >= :startOfDay AND createdAt < :endOfDay AND status = 'COMPLETED'
        """
    )
    fun observeDailyRevenue(startOfDay: Long, endOfDay: Long): Flow<Long>

    @Query("SELECT * FROM transactions WHERE id = :invoiceId")
    suspend fun getById(invoiceId: String): TransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :invoiceId")
    suspend fun getItems(invoiceId: String): List<TransactionItemEntity>

    /** Semua transaksi dalam satu sesi shift — dasar Laporan Tutup Shift. */
    @Query("SELECT * FROM transactions WHERE shiftId = :shiftId ORDER BY createdAt ASC")
    fun observeByShift(shiftId: Long): Flow<List<TransactionEntity>>

    /**
     * BATCH D: ubah status transaksi menjadi VOID (soft-delete) + catat
     * jejak audit waktu & alasan (opsional). Tidak pernah menghapus baris —
     * nomor struk tetap ada di riwayat untuk audit.
     */
    @Query(
        """
        UPDATE transactions
        SET status = :status, voidedAt = :voidedAt, voidReason = :reason
        WHERE id = :id
        """
    )
    suspend fun setStatus(id: String, status: String, voidedAt: Long?, reason: String?)

    /** Tulis header + semua item dalam satu transaksi DB. */
    @Transaction
    suspend fun checkout(tx: TransactionEntity, items: List<TransactionItemEntity>) {
        insertTransaction(tx)
        insertItems(items)
    }
}