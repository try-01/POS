package com.pos.offline.data.repository
import com.pos.offline.data.local.dao.StoreProfileDao
import com.pos.offline.data.local.entity.StoreProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StoreProfileRepository(
    private val storeProfileDao: StoreProfileDao,
) {
    val profile: Flow<StoreProfileEntity> =
        storeProfileDao
            .observe()
            .map { it ?: StoreProfileEntity() }

    suspend fun get(): StoreProfileEntity = storeProfileDao.get() ?: StoreProfileEntity()

    suspend fun ensureInitialized() = storeProfileDao.insertDefaultIfAbsent()

    suspend fun save(profile: StoreProfileEntity) {
        val singletonProfile = profile.copy(id = 1L)
        storeProfileDao.insertDefaultIfAbsent(singletonProfile)
        storeProfileDao.update(singletonProfile)
    }
}
