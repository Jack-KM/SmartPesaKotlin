package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.TransactionCostDao
import com.example.smartpesa.data.local.entity.TransactionCost
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionCostRepository @Inject constructor(
    private val transactionCostDao: TransactionCostDao
) {

    fun getAllCosts(): Flow<List<TransactionCost>> = transactionCostDao.getAll()

    fun getCostsByTransactionId(transactionId: Long): Flow<List<TransactionCost>> =
        transactionCostDao.getByTransactionId(transactionId)

    suspend fun insertCost(cost: TransactionCost): Long = transactionCostDao.insert(cost)

    suspend fun insertCosts(costs: List<TransactionCost>) = transactionCostDao.insertAll(costs)

    suspend fun deleteCostsByTransactionId(transactionId: Long) =
        transactionCostDao.deleteByTransactionId(transactionId)

    suspend fun updateCost(cost: TransactionCost) = transactionCostDao.update(cost)

    suspend fun deleteCost(cost: TransactionCost) = transactionCostDao.delete(cost)

    suspend fun deleteAllCosts() = transactionCostDao.deleteAll()
}
