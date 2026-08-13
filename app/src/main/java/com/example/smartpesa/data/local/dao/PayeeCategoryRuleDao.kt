package com.example.smartpesa.data.local.dao

import androidx.room.*
import com.example.smartpesa.data.local.entity.PayeeCategoryRule
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeCategoryRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: PayeeCategoryRule)

    @Query("SELECT * FROM payee_category_rules WHERE payeeKey = :payeeKey")
    suspend fun getByPayeeKey(payeeKey: String): PayeeCategoryRule?

    @Query("SELECT * FROM payee_category_rules WHERE categoryId = :categoryId")
    fun getByCategoryId(categoryId: Long): Flow<List<PayeeCategoryRule>>

    @Query("SELECT * FROM payee_category_rules ORDER BY lastUsedAt DESC")
    fun getAll(): Flow<List<PayeeCategoryRule>>

    @Query("DELETE FROM payee_category_rules WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)

    @Query("DELETE FROM payee_category_rules WHERE payeeKey = :payeeKey")
    suspend fun deleteByPayeeKey(payeeKey: String)

    @Query("DELETE FROM payee_category_rules")
    suspend fun deleteAll()
}
