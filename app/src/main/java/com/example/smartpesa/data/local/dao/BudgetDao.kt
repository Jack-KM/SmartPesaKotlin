package com.example.smartpesa.data.local.dao

import androidx.room.*
import com.example.smartpesa.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>)

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets ORDER BY startDate DESC")
    fun getAll(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun getById(id: Long): Flow<Budget?>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId ORDER BY startDate DESC")
    fun getByCategory(categoryId: Long): Flow<List<Budget>>

    @Query("SELECT COUNT(*) FROM budgets WHERE accountName = :accountName")
    suspend fun countByAccountName(accountName: String): Int

    @Query("UPDATE budgets SET accountName = :newName WHERE accountName = :oldName")
    suspend fun reassignAccountName(oldName: String, newName: String)

    @Query("DELETE FROM budgets WHERE accountName = :accountName")
    suspend fun deleteByAccountName(accountName: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
