package com.example.smartpesa.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Account
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.AccountRepository
import com.example.smartpesa.data.repository.TransactionRepository
import com.example.smartpesa.util.balanceImpactFor
import com.example.smartpesa.util.isLinkedToAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class ReconcilePreview(
    val currentBalance: Double,
    val actualBalance: Double,
    val difference: Double,
    val adjustmentType: TransactionType
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val accountId: Long = savedStateHandle["accountId"] ?: 0L

    val account: StateFlow<Account?> = accountRepository.getAccountById(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val transactions: StateFlow<List<Transaction>> = combine(account, transactionRepository.getAllTransactions()) { accountValue, allTransactions ->
        accountValue?.let { currentAccount ->
            allTransactions.filter { !it.isWorkTransaction && it.isLinkedToAccount(currentAccount.name) }
        } ?: emptyList()
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentBalance: StateFlow<Double> = combine(account, transactions) { accountValue, accountTransactions ->
        if (accountValue == null) 0.0 else computeCurrentBalance(accountValue, accountTransactions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun saveOpeningBalance(value: Double) {
        val currentAccount = account.value ?: return
        viewModelScope.launch {
            accountRepository.updateAccount(
                currentAccount.copy(
                    openingBalance = value,
                    updatedAt = LocalDateTime.now()
                )
            )
        }
    }

    fun buildReconcilePreview(actualBalance: Double): ReconcilePreview? {
        val currentAccount = account.value ?: return null
        val current = computeCurrentBalance(currentAccount, transactions.value)
        val difference = actualBalance - current
        return ReconcilePreview(
            currentBalance = current,
            actualBalance = actualBalance,
            difference = difference,
            adjustmentType = if (difference >= 0) TransactionType.INCOME else TransactionType.EXPENSE
        )
    }

    fun reconcile(actualBalance: Double) {
        val currentAccount = account.value ?: return
        val preview = buildReconcilePreview(actualBalance) ?: return
        if (kotlin.math.abs(preview.difference) < 0.01) return

        viewModelScope.launch {
            transactionRepository.insertTransaction(
                Transaction(
                    amount = kotlin.math.abs(preview.difference),
                    feeAmount = 0.0,
                    description = "Balance reconciliation adjustment",
                    type = preview.adjustmentType,
                    timestamp = LocalDateTime.now(),
                    categoryId = null,
                    category = "Balance Adjustment",
                    counterparty = currentAccount.name,
                    accountName = currentAccount.name,
                    source = "Manual"
                )
            )
        }
    }

    private fun computeCurrentBalance(accountValue: Account, accountTransactions: List<Transaction>): Double {
        return accountValue.openingBalance + accountTransactions.sumOf { it.balanceImpactFor(accountValue.name) }
    }
}
