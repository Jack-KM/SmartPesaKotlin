package com.example.smartpesa.data.local.dao

import androidx.room.*
import com.example.smartpesa.data.local.entity.MerchantCategoryHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: MerchantCategoryHistory): Long

    @Update
    suspend fun update(history: MerchantCategoryHistory)

    @Delete
    suspend fun delete(history: MerchantCategoryHistory)

    @Query("SELECT * FROM merchant_category_history WHERE normalizedMerchant = :normalizedMerchant ORDER BY occurrenceCount DESC")
    suspend fun getByMerchant(normalizedMerchant: String): List<MerchantCategoryHistory>

    @Query("SELECT * FROM merchant_category_history WHERE normalizedMerchant = :normalizedMerchant AND categoryId = :categoryId")
    suspend fun getByMerchantAndCategory(normalizedMerchant: String, categoryId: Long): MerchantCategoryHistory?

    @Query("SELECT * FROM merchant_category_history WHERE categoryId = :categoryId")
    fun getByCategoryId(categoryId: Long): Flow<List<MerchantCategoryHistory>>

    @Query("SELECT * FROM merchant_category_history ORDER BY lastUsedAt DESC")
    fun getAll(): Flow<List<MerchantCategoryHistory>>

    @Query("DELETE FROM merchant_category_history WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)

    @Query("DELETE FROM merchant_category_history")
    suspend fun deleteAll()
}
