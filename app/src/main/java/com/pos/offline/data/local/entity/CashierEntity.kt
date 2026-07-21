package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cashiers")
data class CashierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinHash: String? = null, // null = tanpa PIN. Di-hash, bukan plain text.
    val active: Boolean = true, // soft-delete, konsisten dgn ProductEntity
    val createdAt: Long = System.currentTimeMillis(),
)
