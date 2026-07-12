package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sesi kerja kasir (buka s.d. tutup shift).
 *
 * [endingCashExpected] dihitung sistem (kas awal + total penjualan TUNAI
 * selama shift — penjualan QRIS TIDAK dihitung karena uangnya masuk rekening,
 * bukan ke laci fisik). [endingCashActual] diinput manual kasir dari hasil
 * hitung fisik laci — selisih keduanya membantu mendeteksi kekurangan/
 * kelebihan kas per shift.
 *
 * Sengaja TIDAK memakai @ForeignKey ke [CashierEntity]: [cashierName] adalah
 * snapshot (sama prinsip dgn productName di TransactionItemEntity) agar
 * riwayat shift tetap utuh walau data kasir suatu saat dihapus/diubah.
 */
@Entity(
    tableName = "shifts",
    indices = [Index(value = ["cashierId"]), Index(value = ["endedAt"])]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashierId: Long,
    val cashierName: String,
    val startingCash: Long,
    val startedAt: Long,
    val endingCashExpected: Long? = null,
    val endingCashActual: Long? = null,
    val endedAt: Long? = null,      // null = shift masih berjalan
    val note: String = ""
) {
    val isOpen: Boolean get() = endedAt == null

    /** Positif = kas lebih dari seharusnya, negatif = kas kurang (mis. ada kebocoran). */
    val cashDifference: Long?
        get() = if (endingCashActual != null && endingCashExpected != null)
            endingCashActual - endingCashExpected else null
}