package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.MerchantCategoryHistoryDao
import com.example.smartpesa.data.local.entity.MerchantCategoryHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantCategoryHistoryRepository @Inject constructor(
    private val historyDao: MerchantCategoryHistoryDao
) {

    suspend fun getByMerchant(normalizedMerchant: String): List<MerchantCategoryHistory> {
        return historyDao.getByMerchant(normalizedMerchant)
    }

    suspend fun getByMerchantAndCategory(
        normalizedMerchant: String,
        categoryId: Long
    ): MerchantCategoryHistory? {
        return historyDao.getByMerchantAndCategory(normalizedMerchant, categoryId)
    }

    fun getByCategoryId(categoryId: Long): Flow<List<MerchantCategoryHistory>> {
        return historyDao.getByCategoryId(categoryId)
    }

    fun getAll(): Flow<List<MerchantCategoryHistory>> {
        return historyDao.getAll()
    }

    suspend fun insert(history: MerchantCategoryHistory): Long {
        return historyDao.insert(history)
    }

    suspend fun update(history: MerchantCategoryHistory) {
        historyDao.update(history)
    }

    suspend fun delete(history: MerchantCategoryHistory) {
        historyDao.delete(history)
    }

    suspend fun deleteByCategoryId(categoryId: Long) {
        historyDao.deleteByCategoryId(categoryId)
    }

    suspend fun deleteAll() {
        historyDao.deleteAll()
    }
}
