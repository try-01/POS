package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM shifts WHERE endedAt IS NULL ORDER BY startedAt ASC")
    fun observeOpenShifts(): Flow<List<ShiftEntity>>

    @Query(
        """
        SELECT * FROM shifts
        WHERE endedAt >= :start AND endedAt < :end
        ORDER BY endedAt DESC
        """,
    )
    fun observeClosedShiftsBetween(
        start: Long,
        end: Long,
    ): Flow<List<ShiftEntity>>

    @Insert
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): ShiftEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM shifts WHERE cashierId = :cashierId AND endedAt IS NULL)")
    suspend fun hasOpenShiftForCashier(cashierId: Long): Boolean

    /**
     * Cegah race condition "double-open shift" untuk kasir yang sama.
     * Check + insert dijalankan dalam SATU transaksi Room (pola sama dengan
     * TransactionDao.checkout) sehingga aman dari TOCTOU walau dipanggil
     * bersamaan dari 2 coroutine/tap cepat.
     * @return id shift baru, atau -1L jika kasir tsb sudah punya shift terbuka.
     */
    @Transaction
    suspend fun insertIfNoOpenShift(shift: ShiftEntity): Long {
        if (hasOpenShiftForCashier(shift.cashierId)) return -1L
        return insert(shift)
    }

    /**
     * Cegah race condition "double-close shift". Re-check endedAt dilakukan
     * DI DALAM transaksi yang sama dengan update, sehingga dua panggilan
     * bersamaan terhadap shift yang sama tidak akan sama-sama lolos.
     * @return ShiftEntity hasil update, atau null jika shift tidak ditemukan
     * atau sudah ditutup sebelumnya.
     */
    @Transaction
    suspend fun endIfOpen(
        id: Long,
        endingCashExpected: Long,
        endingCashActual: Long,
        endedAt: Long,
        note: String,
    ): ShiftEntity? {
        val current = getById(id) ?: return null
        if (current.endedAt != null) return null
        val updated =
            current.copy(
                endingCashExpected = endingCashExpected,
                endingCashActual = endingCashActual,
                endedAt = endedAt,
                note = note,
            )
        update(updated)
        return updated
    }

    // Uang tunai FISIK yang benar-benar masuk laci, bukan nilai nominal
    // transaksi (total). Rumus: paidAmount - MAX(change, 0).
    //   - change >= 0 (kembalian diberikan)      -> hasil = total (sama seperti dulu)
    //   - change <  0 (kurang bayar, "Tetap Lanjutkan") -> hasil = paidAmount (uang fisik aktual)
    // Wajib pakai ini utk expectedCashInDrawer, BUKAN sum(total), agar
    // rekonsiliasi kas saat tutup shift akurat terhadap kasus nego/pembulatan.
    @Query(
        """
        SELECT COALESCE(SUM(paidAmount - MAX(change, 0)), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'CASH' AND status = 'COMPLETED'
        """,
    )
    suspend fun cashRevenueForShift(shiftId: Long): Long

    // Disamakan dengan cashRevenueForShift: uang QRIS yang benar-benar
    // diterima, bukan nilai nominal transaksi (total). Menangani kasus
    // nego harga di lapangan (paid < total via QRIS). Keterbatasan yang
    // diketahui: untuk kasus "tip" (paid > total, kelebihan sengaja TIDAK
    // dikembalikan), formula ini tetap meng-clamp ke `total` karena sistem
    // belum punya field untuk membedakan "kembalian diberikan" vs "kembalian
    // jadi tip" — sama seperti keterbatasan pada cashRevenueForShift.
    @Query(
        """
        SELECT COALESCE(SUM(paidAmount - MAX(change, 0)), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'QRIS' AND status = 'COMPLETED'
        """,
    )
    suspend fun qrisRevenueForShift(shiftId: Long): Long

    // Uang TUNAI FISIK yang KELUAR dari laci sebagai kembalian untuk
    // transaksi NON-TUNAI (QRIS) — skenario nyata di lapangan: pembeli
    // bayar lebih via QRIS (mis. tidak bawa cash) lalu meminta kembaliannya
    // dalam bentuk uang tunai. Transaksi QRIS TIDAK PERNAH menambah kas laci
    // (uangnya digital), tapi BISA menguranginya jika kembaliannya fisik.
    // WAJIB dikurangkan dari expectedCashInDrawer, TERPISAH dari
    // qrisRevenueForShift (yang murni soal pendapatan/P&L, bukan pergerakan
    // kas fisik) — dua konsep ini sengaja tidak digabung.
    @Query(
        """
        SELECT COALESCE(SUM(changeGiven), 0) FROM transactions
        WHERE shiftId = :shiftId AND paymentMethod = 'QRIS' AND status = 'COMPLETED'
          AND changeGivenInCash = 1
        """,
    )
    suspend fun qrisCashChangeOutForShift(shiftId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(ti.unitCost * ti.quantity), 0)
        FROM transaction_items ti
        INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.shiftId = :shiftId AND t.status = 'COMPLETED'
        """,
    )
    suspend fun totalCostForShift(shiftId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(refundAmount), 0) FROM returns
        WHERE shiftId = :shiftId AND refundMethod = 'CASH'
        """,
    )
    suspend fun cashRefundsForShift(shiftId: Long): Long
}