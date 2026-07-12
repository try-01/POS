package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data kasir/staff. Pemakaian fitur ini OPSIONAL — aplikasi tetap bisa dipakai
 * tanpa memilih kasir/shift sama sekali (checkout `cashierId` boleh null).
 */
@Entity(tableName = "cashiers")
data class CashierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinHash: String? = null,   // null = tanpa PIN. Di-hash, bukan plain text.
    val active: Boolean = true,     // soft-delete, konsisten dgn ProductEntity
    val createdAt: Long = System.currentTimeMillis()
)