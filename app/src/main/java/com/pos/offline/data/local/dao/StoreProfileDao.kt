package com.pos.offline.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pos.offline.data.local.entity.StoreProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreProfileDao {

    @Query("SELECT * FROM store_profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<StoreProfileEntity?>

    @Query("SELECT * FROM store_profile WHERE id = 1 LIMIT 1")
    suspend fun get(): StoreProfileEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultIfAbsent(profile: StoreProfileEntity = StoreProfileEntity())

    @Update
    suspend fun update(profile: StoreProfileEntity)
}