package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.CashierDao
import com.pos.offline.data.local.entity.CashierEntity
import kotlinx.coroutines.flow.Flow

class CashierRepository(private val cashierDao: CashierDao) {

    val activeCashiers: Flow<List<CashierEntity>> = cashierDao.observeActive()
    val allCashiers: Flow<List<CashierEntity>> = cashierDao.observeAll()

    suspend fun getById(id: Long): CashierEntity? = cashierDao.getById(id)

    /** Insert atau update (upsert) tergantung ada/tidak-nya id. */
    suspend fun save(cashier: CashierEntity): Long = cashierDao.upsert(cashier)

    suspend fun setActive(id: Long, active: Boolean) = cashierDao.setActive(id, active)
}