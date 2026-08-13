package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.FulizaDao
import com.example.smartpesa.data.local.entity.Fuliza
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FulizaRepository @Inject constructor(
    private val fulizaDao: FulizaDao
) {

    fun getAllFuliza(): Flow<List<Fuliza>> = fulizaDao.getAll()

    fun getFulizaById(id: Long): Flow<Fuliza?> = fulizaDao.getById(id)

    suspend fun insertFuliza(fuliza: Fuliza): Long = fulizaDao.insert(fuliza)

    suspend fun updateFuliza(fuliza: Fuliza) = fulizaDao.update(fuliza)

    suspend fun deleteFuliza(fuliza: Fuliza) = fulizaDao.delete(fuliza)

    suspend fun deleteAllFuliza() = fulizaDao.deleteAll()
}
