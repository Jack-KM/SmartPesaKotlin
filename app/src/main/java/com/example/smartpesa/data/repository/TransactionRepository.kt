package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.TransactionDao
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Transaction data operations
 * Provides a clean API for the UI layer
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAll()
    }

    fun getTransactionById(id: Long): Flow<Transaction?> {
        return transactionDao.getById(id)
    }

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getByType(type)
    }

    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getByCategory(categoryId)
    }

    fun getTransactionsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<Transaction>> {
        return transactionDao.getByDateRange(startDate, endDate)
    }

    fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<Double?> {
        return transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)
    }

    fun getTotalByCategoryAndDateRange(
        categoryId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<Double?> {
        return transactionDao.getTotalByCategoryAndDateRange(categoryId, startDate, endDate)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insert(transaction)
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAll()
    }

    // Duplicate detection for M-Pesa SMS processing
    suspend fun getByMpesaCode(mpesaCode: String): Transaction? {
        return transactionDao.getByMpesaCode(mpesaCode)
    }
}
