package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.BudgetDao
import com.example.smartpesa.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Budget data operations
 * Provides a clean API for the UI layer
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {

    fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAll()
    }

    fun getBudgetById(id: Long): Flow<Budget?> {
        return budgetDao.getById(id)
    }

    fun getBudgetsByCategory(categoryId: Long): Flow<List<Budget>> {
        return budgetDao.getByCategory(categoryId)
    }

    suspend fun countByAccountName(accountName: String): Int {
        return budgetDao.countByAccountName(accountName)
    }

    suspend fun reassignAccountName(oldName: String, newName: String) {
        budgetDao.reassignAccountName(oldName, newName)
    }

    suspend fun deleteByAccountName(accountName: String) {
        budgetDao.deleteByAccountName(accountName)
    }

    suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insert(budget)
    }

    suspend fun insertBudgets(budgets: List<Budget>) {
        budgetDao.insertAll(budgets)
    }

    suspend fun updateBudget(budget: Budget) {
        budgetDao.update(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.delete(budget)
    }

    suspend fun deleteAllBudgets() {
        budgetDao.deleteAll()
    }
}
