package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pos.offline.data.local.entity.CashierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashierDao {

    @Query("SELECT * FROM cashiers WHERE active = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<CashierEntity>>

    @Query("SELECT * FROM cashiers ORDER BY name ASC")
    fun observeAll(): Flow<List<CashierEntity>>

    @Query("SELECT * FROM cashiers WHERE id = :id")
    suspend fun getById(id: Long): CashierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cashier: CashierEntity): Long

    @Query("UPDATE cashiers SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}