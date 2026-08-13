package com.example.smartpesa.data.local.dao

import androidx.room.*
import com.example.smartpesa.data.local.entity.CategoryRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<CategoryRule>)

    @Update
    suspend fun update(rule: CategoryRule)

    @Delete
    suspend fun delete(rule: CategoryRule)

    @Query("SELECT * FROM category_rules WHERE enabled = 1 ORDER BY priority ASC")
    fun getAllEnabled(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules ORDER BY priority ASC")
    fun getAll(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules WHERE id = :id")
    fun getById(id: Long): Flow<CategoryRule?>

    @Query("SELECT * FROM category_rules WHERE categoryId = :categoryId")
    fun getByCategoryId(categoryId: Long): Flow<List<CategoryRule>>

    @Query("DELETE FROM category_rules")
    suspend fun deleteAll()

    @Query("SELECT * FROM category_rules WHERE enabled = 1 ORDER BY priority ASC")
    suspend fun getAllEnabledSync(): List<CategoryRule>
}
