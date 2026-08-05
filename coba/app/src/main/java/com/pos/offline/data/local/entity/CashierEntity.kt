package com.pos.offline.data.local.entity
import androidx.room3.Entity
import androidx.room3.PrimaryKey
@Entity(tableName = "cashiers")
data class CashierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinHash: String? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
