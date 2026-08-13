package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.CategoryRuleDao
import com.example.smartpesa.data.local.entity.CategoryRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRuleRepository @Inject constructor(
    private val categoryRuleDao: CategoryRuleDao
) {

    fun getAllEnabledRules(): Flow<List<CategoryRule>> {
        return categoryRuleDao.getAllEnabled()
    }

    fun getAllRules(): Flow<List<CategoryRule>> {
        return categoryRuleDao.getAll()
    }

    fun getRuleById(id: Long): Flow<CategoryRule?> {
        return categoryRuleDao.getById(id)
    }

    fun getRulesByCategoryId(categoryId: Long): Flow<List<CategoryRule>> {
        return categoryRuleDao.getByCategoryId(categoryId)
    }

    suspend fun insertRule(rule: CategoryRule): Long {
        return categoryRuleDao.insert(rule)
    }

    suspend fun insertRules(rules: List<CategoryRule>) {
        categoryRuleDao.insertAll(rules)
    }

    suspend fun updateRule(rule: CategoryRule) {
        categoryRuleDao.update(rule)
    }

    suspend fun deleteRule(rule: CategoryRule) {
        categoryRuleDao.delete(rule)
    }

    suspend fun deleteAllRules() {
        categoryRuleDao.deleteAll()
    }

    suspend fun getAllEnabledRulesSync(): List<CategoryRule> {
        return categoryRuleDao.getAllEnabledSync()
    }
}
