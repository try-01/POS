package com.pos.offline.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "cashiers")
data class CashierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinHash: String? = null, 
    val active: Boolean = true, 
    val createdAt: Long = System.currentTimeMillis(),
)
