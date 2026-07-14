package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pos.offline.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    @Query("SELECT * FROM shifts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpenShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenShift(): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Insert
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): ShiftEntity?

    /**
     * BATCH B: cek apakah kasir tertentu masih punya shift berjalan
     * (`endedAt IS NULL`) — dipakai untuk MEMBLOKIR nonaktifkan kasir yang
     * shift-nya belum ditutup (mencegah shift "menggantung" tanpa rekonsiliasi
     * kas). Tidak peduli shift itu "yang ditunjuk aktif" versi UI (terbaru)
     * atau tertinggal terbuka dari sesi sebelumnya — keduanya tetap harus
     * ditutup dulu sebelum kasirnya dinonaktifkan.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM shifts WHERE cashierId = :cashierId AND endedAt IS NULL)")
    suspend fun hasOpenShiftForCashier(cashierId: Long): Boolean

    /**
     * BATCH D: exclude status VOID — transaksi yang dibatalkan tidak boleh
     * ikut dihitung dalam rekonsiliasi kas laci fisik.
     */
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'CASH' AND status = 'COMPLETED'
        """
    )
    suspend fun cashRevenueForShift(shiftId: Long): Long

    /**
     * BATCH 3C: total penjualan QRIS selama shift — uangnya masuk rekening
     * bank, BUKAN ke laci fisik, jadi sengaja dipisah dari cashRevenueForShift.
     * BATCH D: exclude status VOID.
     */
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'QRIS' AND status = 'COMPLETED'
        """
    )
    suspend fun qrisRevenueForShift(shiftId: Long): Long

    /**
     * BATCH 3C: total modal (Σ unitCost × qty) seluruh item yang terjual
     * dalam shift ini — dasar kalkulasi Laba Kotor. Join ke `transactions`
     * untuk memfilter berdasarkan `shiftId` (kolom itu ada di header, bukan
     * di `transaction_items`).
     * BATCH D: exclude status VOID — modal barang dari transaksi yang
     * dibatalkan tidak boleh ikut mengurangi Laba Kotor.
     */
    @Query(
        """
        SELECT COALESCE(SUM(ti.unitCost * ti.quantity), 0)
        FROM transaction_items ti
        INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.shiftId = :shiftId AND t.status = 'COMPLETED'
        """
    )
    suspend fun totalCostForShift(shiftId: Long): Long
}