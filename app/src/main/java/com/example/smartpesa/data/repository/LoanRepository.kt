package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.LoanDao
import com.example.smartpesa.data.local.entity.Loan
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao
) {

    fun getAllLoans(): Flow<List<Loan>> = loanDao.getAll()

    fun getLoanById(id: Long): Flow<Loan?> = loanDao.getById(id)

    suspend fun insertLoan(loan: Loan): Long = loanDao.insert(loan)

    suspend fun updateLoan(loan: Loan) = loanDao.update(loan)

    suspend fun deleteLoan(loan: Loan) = loanDao.delete(loan)

    suspend fun deleteAllLoans() = loanDao.deleteAll()
}
