package com.example.smartpesa.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartpesa.data.local.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, createdAt ASC")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getById(id: Long): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Account?

    @Query("SELECT COUNT(*) FROM transactions WHERE accountName = :name")
    suspend fun countTransactionsByName(name: String): Int

    @Query("SELECT COUNT(*) FROM budgets WHERE accountName = :name")
    suspend fun countBudgetsByName(name: String): Int

    @Query("UPDATE transactions SET accountName = :newName WHERE accountName = :oldName")
    suspend fun reassignTransactions(oldName: String, newName: String)

    @Query("UPDATE budgets SET accountName = :newName WHERE accountName = :oldName")
    suspend fun reassignBudgets(oldName: String, newName: String)

    @Query("DELETE FROM transactions WHERE accountName = :name")
    suspend fun deleteTransactionsByName(name: String)

    @Query("DELETE FROM budgets WHERE accountName = :name")
    suspend fun deleteBudgetsByName(name: String)

    @Query("UPDATE accounts SET isDefault = CASE WHEN id = :defaultAccountId THEN 1 ELSE 0 END")
    suspend fun setDefaultAccount(defaultAccountId: Long)

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC LIMIT 1")
    suspend fun getOldestAccount(): Account?

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}
