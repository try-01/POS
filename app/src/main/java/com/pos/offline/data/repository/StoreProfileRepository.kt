package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.StoreProfileDao
import com.pos.offline.data.local.entity.StoreProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StoreProfileRepository(private val storeProfileDao: StoreProfileDao) {

    /** Selalu mengembalikan profil non-null -- fallback ke default kosong
     *  kalau baris singleton entah kenapa belum ada (edge-case aman). */
    val profile: Flow<StoreProfileEntity> = storeProfileDao.observe()
        .map { it ?: StoreProfileEntity() }

    suspend fun get(): StoreProfileEntity = storeProfileDao.get() ?: StoreProfileEntity()

    /** Safety-net, opsional dipanggil manual kalau ada skenario butuh
     *  memastikan baris singleton sudah ada sebelum dibaca. Dalam kondisi
     *  normal, baris ini sudah dijamin ada lewat migrasi/seed database. */
    suspend fun ensureInitialized() = storeProfileDao.insertDefaultIfAbsent()

    suspend fun save(profile: StoreProfileEntity) {
        storeProfileDao.insertDefaultIfAbsent(profile) // no-op kalau baris id=1 sudah ada
        storeProfileDao.update(profile)
    }
}