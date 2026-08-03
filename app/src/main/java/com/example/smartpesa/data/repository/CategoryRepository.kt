package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.CategoryDao
import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Category data operations
 * Provides a clean API for the UI layer
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAll()
    }

    fun getCategoryById(id: Long): Flow<Category?> {
        return categoryDao.getById(id)
    }

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getByType(type)
    }

    fun getCategoryByName(name: String): Flow<Category?> {
        return categoryDao.getByName(name)
    }

    fun getMainCategories(): Flow<List<Category>> {
        return categoryDao.getMainCategories()
    }

    fun getSubCategories(parentId: Long): Flow<List<Category>> {
        return categoryDao.getSubCategories(parentId)
    }

    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insert(category)
    }

    suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertAll(categories)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun deleteAllCategories() {
        categoryDao.deleteAll()
    }
}
