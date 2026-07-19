package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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

    val cashDifference: Long?
        get() = if (endingCashActual != null && endingCashExpected != null)
            endingCashActual - endingCashExpected else null
}