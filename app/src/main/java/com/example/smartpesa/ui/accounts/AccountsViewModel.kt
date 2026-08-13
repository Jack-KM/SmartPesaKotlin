package com.example.smartpesa.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Account
import com.example.smartpesa.data.local.entity.AccountType
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.preferences.UserPreferencesRepository
import com.example.smartpesa.data.repository.AccountRepository
import com.example.smartpesa.data.repository.TransactionRepository
import com.example.smartpesa.util.balanceImpactFor
import com.example.smartpesa.util.isLinkedToAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactionCounts: StateFlow<Map<String, Int>> = combine(accounts, transactionRepository.getAllTransactions()) { accountList, transactions ->
        accountList.associate { account ->
            account.name to transactions.count { !it.isWorkTransaction && it.isLinkedToAccount(account.name) }
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val accountBalances: StateFlow<Map<String, Double>> = combine(accounts, transactionRepository.getAllTransactions()) { accountList, transactions ->
        accountList.associate { account ->
            account.name to transactions.filterNot { it.isWorkTransaction }.sumOf { transaction -> transaction.balanceImpactFor(account.name) }
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun makeDefault(account: Account) {
        viewModelScope.launch {
            accountRepository.setDefaultAccount(account.id)
        }
    }

    suspend fun saveAccount(
        existingAccount: Account?,
        name: String,
        type: AccountType,
        currencyCode: String,
        openingBalanceText: String,
        phoneNumber: String
    ): String? {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return "Account name is required"
        if (trimmedName.length > 60) return "Account name is too long"

        val duplicate = accounts.value.any {
            it.name.equals(trimmedName, ignoreCase = true) && it.id != existingAccount?.id
        }
        if (duplicate) return "Account name already exists"

        val openingBalance = when {
            openingBalanceText.trim().isBlank() -> 0.0
            openingBalanceText.trim().toDoubleOrNull() != null -> openingBalanceText.trim().toDouble()
            else -> return "Opening balance must be a number"
        }

        if (type == AccountType.MPESA && phoneNumber.trim().isNotBlank() && !isValidMpesaPhone(phoneNumber.trim())) {
            return "Phone must look like 07XXXXXXXX or +2547XXXXXXXX"
        }

        val selectedCurrency = currencyCode.trim().ifBlank { userPreferencesRepository.currencyPreference.first() }
        val normalizedPhone = phoneNumber.trim().takeIf { it.isNotBlank() }?.normalizeKenyanPhone()
        val now = LocalDateTime.now()

        if (existingAccount == null) {
            val shouldBeDefault = accounts.value.isEmpty()
            accountRepository.insertAccount(
                Account(
                    name = trimmedName,
                    type = type,
                    currencyCode = selectedCurrency,
                    openingBalance = openingBalance,
                    phoneNumber = normalizedPhone,
                    isDefault = shouldBeDefault,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            if (!existingAccount.name.equals(trimmedName, ignoreCase = true)) {
                accountRepository.reassignTransactions(existingAccount.name, trimmedName)
                accountRepository.reassignBudgets(existingAccount.name, trimmedName)
            }
            accountRepository.updateAccount(
                existingAccount.copy(
                    name = trimmedName,
                    type = type,
                    currencyCode = selectedCurrency,
                    openingBalance = openingBalance,
                    phoneNumber = normalizedPhone,
                    updatedAt = now
                )
            )
        }

        return null
    }

    suspend fun getDeleteInfo(account: Account): DeleteInfo {
        val transactionCount = accountRepository.countTransactionsByName(account.name)
        val budgetCount = accountRepository.countBudgetsByName(account.name)
        val remainingAccounts = accounts.value.filterNot { it.id == account.id }
        val defaultCandidate = if (account.isDefault) remainingAccounts.minByOrNull { it.createdAt } else null
        return DeleteInfo(
            transactionCount = transactionCount,
            budgetCount = budgetCount,
            remainingAccounts = remainingAccounts,
            nextDefaultAccount = defaultCandidate
        )
    }

    suspend fun deleteAccount(
        account: Account,
        deleteTransactions: Boolean,
        deleteBudgets: Boolean,
        moveToAccountName: String? = null
    ) {
        if (moveToAccountName != null) {
            accountRepository.reassignTransactions(account.name, moveToAccountName)
            accountRepository.reassignBudgets(account.name, moveToAccountName)
        } else {
            if (deleteTransactions) accountRepository.deleteTransactionsByName(account.name)
            if (deleteBudgets) accountRepository.deleteBudgetsByName(account.name)
        }

        accountRepository.deleteAccount(account)

        if (account.isDefault) {
            val nextDefault = accounts.value
                .filterNot { it.id == account.id }
                .minByOrNull { it.createdAt }
            if (nextDefault != null) {
                accountRepository.setDefaultAccount(nextDefault.id)
            }
        }
    }

    suspend fun restoreAccount(account: Account) {
        val restoredId = accountRepository.insertAccount(
            account.copy(
                id = 0,
                currencyCode = account.currencyCode,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        if (account.isDefault) {
            accountRepository.setDefaultAccount(restoredId)
        }
    }

    private fun isValidMpesaPhone(value: String): Boolean {
        val normalized = value.normalizeKenyanPhone()
        return normalized.matches(Regex("^07\\d{8}$"))
    }

    private fun String.normalizeKenyanPhone(): String {
        val digits = trim().replace(" ", "")
        return when {
            digits.startsWith("+254") -> "0${digits.removePrefix("+254")}".take(10)
            digits.startsWith("254") -> "0${digits.removePrefix("254")}".take(10)
            else -> digits
        }
    }
}

data class DeleteInfo(
    val transactionCount: Int,
    val budgetCount: Int,
    val remainingAccounts: List<Account>,
    val nextDefaultAccount: Account?
)
