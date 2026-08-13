package com.example.smartpesa.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartpesa.data.local.entity.Fuliza
import kotlinx.coroutines.flow.Flow

@Dao
interface FulizaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fuliza: Fuliza): Long

    @Update
    suspend fun update(fuliza: Fuliza)

    @Delete
    suspend fun delete(fuliza: Fuliza)

    @Query("SELECT * FROM fuliza ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<Fuliza>>

    @Query("SELECT * FROM fuliza WHERE id = :id")
    fun getById(id: Long): Flow<Fuliza?>

    @Query("DELETE FROM fuliza")
    suspend fun deleteAll()
}
