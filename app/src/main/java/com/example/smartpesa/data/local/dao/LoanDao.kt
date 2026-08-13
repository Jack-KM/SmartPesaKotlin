package com.example.smartpesa.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartpesa.data.local.entity.Loan
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan): Long

    @Update
    suspend fun update(loan: Loan)

    @Delete
    suspend fun delete(loan: Loan)

    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAll(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    fun getById(id: Long): Flow<Loan?>

    @Query("DELETE FROM loans")
    suspend fun deleteAll()
}
