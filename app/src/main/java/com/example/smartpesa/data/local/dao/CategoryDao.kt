package com.example.smartpesa.data.local.dao

import androidx.room.*
import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getById(id: Long): Flow<Category?>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getByType(type: TransactionType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    fun getByName(name: String): Flow<Category?>

    @Query("SELECT * FROM categories WHERE parentCategoryId IS NULL ORDER BY name ASC")
    fun getMainCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentCategoryId = :parentId ORDER BY name ASC")
    fun getSubCategories(parentId: Long): Flow<List<Category>>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
