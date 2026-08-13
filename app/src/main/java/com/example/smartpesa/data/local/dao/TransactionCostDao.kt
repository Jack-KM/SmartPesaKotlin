package com.example.smartpesa.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartpesa.data.local.entity.TransactionCost
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionCostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cost: TransactionCost): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(costs: List<TransactionCost>)

    @Update
    suspend fun update(cost: TransactionCost)

    @Delete
    suspend fun delete(cost: TransactionCost)

    @Query("SELECT * FROM transaction_costs ORDER BY id DESC")
    fun getAll(): Flow<List<TransactionCost>>

    @Query("SELECT * FROM transaction_costs WHERE transactionId = :transactionId ORDER BY id DESC")
    fun getByTransactionId(transactionId: Long): Flow<List<TransactionCost>>

    @Query("DELETE FROM transaction_costs WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Long)

    @Query("DELETE FROM transaction_costs")
    suspend fun deleteAll()
}
