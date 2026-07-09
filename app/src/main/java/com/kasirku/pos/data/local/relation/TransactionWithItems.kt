package com.kasirku.pos.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity

/**
 * Gabungan header transaksi + daftar itemnya dalam satu objek,
 * dipakai untuk menampilkan Riwayat Transaksi & detail struk tanpa query N+1
 * (Room otomatis menjalankan satu query tambahan yang efisien untuk semua parent sekaligus).
 */
data class TransactionWithItems(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItemEntity>
)
