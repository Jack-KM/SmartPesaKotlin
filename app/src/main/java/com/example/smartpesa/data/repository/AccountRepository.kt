package com.example.smartpesa.data.repository

import com.example.smartpesa.data.local.dao.AccountDao
import com.example.smartpesa.data.local.entity.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAll()

    fun getAccountById(id: Long): Flow<Account?> = accountDao.getById(id)

    suspend fun getAccountByName(name: String): Account? = accountDao.getByName(name)

    suspend fun insertAccount(account: Account): Long = accountDao.insert(account)

    suspend fun updateAccount(account: Account) = accountDao.update(account)

    suspend fun deleteAccount(account: Account) = accountDao.delete(account)

    suspend fun countTransactionsByName(name: String): Int = accountDao.countTransactionsByName(name)

    suspend fun countBudgetsByName(name: String): Int = accountDao.countBudgetsByName(name)

    suspend fun reassignTransactions(oldName: String, newName: String) = accountDao.reassignTransactions(oldName, newName)

    suspend fun reassignBudgets(oldName: String, newName: String) = accountDao.reassignBudgets(oldName, newName)

    suspend fun deleteTransactionsByName(name: String) = accountDao.deleteTransactionsByName(name)

    suspend fun deleteBudgetsByName(name: String) = accountDao.deleteBudgetsByName(name)

    suspend fun setDefaultAccount(defaultAccountId: Long) = accountDao.setDefaultAccount(defaultAccountId)

    suspend fun getOldestAccount(): Account? = accountDao.getOldestAccount()

    suspend fun deleteAllAccounts() = accountDao.deleteAll()
}
