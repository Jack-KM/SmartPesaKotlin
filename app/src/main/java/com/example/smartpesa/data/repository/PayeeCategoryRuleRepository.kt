package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.PayeeCategoryRuleDao
import com.example.smartpesa.data.local.entity.PayeeCategoryRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayeeCategoryRuleRepository @Inject constructor(
    private val payeeCategoryRuleDao: PayeeCategoryRuleDao
) {

    suspend fun upsertRule(rule: PayeeCategoryRule) {
        payeeCategoryRuleDao.upsert(rule)
    }

    suspend fun getByPayeeKey(payeeKey: String): PayeeCategoryRule? {
        return payeeCategoryRuleDao.getByPayeeKey(payeeKey)
    }

    fun getByCategoryId(categoryId: Long): Flow<List<PayeeCategoryRule>> {
        return payeeCategoryRuleDao.getByCategoryId(categoryId)
    }

    fun getAll(): Flow<List<PayeeCategoryRule>> {
        return payeeCategoryRuleDao.getAll()
    }

    suspend fun deleteByCategoryId(categoryId: Long) {
        payeeCategoryRuleDao.deleteByCategoryId(categoryId)
    }

    suspend fun deleteByPayeeKey(payeeKey: String) {
        payeeCategoryRuleDao.deleteByPayeeKey(payeeKey)
    }

    suspend fun deleteAll() {
        payeeCategoryRuleDao.deleteAll()
    }
}
