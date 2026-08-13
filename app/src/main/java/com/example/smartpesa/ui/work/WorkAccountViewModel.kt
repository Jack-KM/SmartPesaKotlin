package com.example.smartpesa.ui.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for work account screen
 * Manages work transactions and balance tracking
 */
@HiltViewModel
class WorkAccountViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val workData = combine(
        transactionRepository.getWorkTransactions(),
        transactionRepository.getWorkBalance()
    ) { transactions, balance -> transactions to (balance ?: 0.0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Transaction>() to 0.0)

    val workTransactions: StateFlow<List<Transaction>> = workData
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workBalance: StateFlow<Double> = workData
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val isLoading: StateFlow<Boolean> = workTransactions
        .map { false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /**
     * Toggle work status of a transaction
     */
    suspend fun toggleWorkStatus(transaction: Transaction) {
        val updated = transaction.copy(isWorkTransaction = !transaction.isWorkTransaction)
        transactionRepository.updateTransaction(updated)
    }

    /**
     * Settle work balance by creating a transfer transaction
     * Positive balance = company owes you (create income transaction)
     * Negative balance = you owe company (create expense transaction)
     */
    suspend fun settleBalance(): Boolean {
        val balance = workBalance.value

        if (balance == 0.0) {
            return false // Nothing to settle
        }

        val settlementTransaction = Transaction(
            amount = kotlin.math.abs(balance),
            feeAmount = 0.0,
            description = if (balance > 0) {
                "Work balance settlement - Company payment"
            } else {
                "Work balance settlement - Personal reimbursement"
            },
            type = if (balance > 0) TransactionType.INCOME else TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            category = "Work Settlement",
            counterparty = "Work Account",
            source = "Manual",
            isWorkTransaction = false // Settlement goes to personal account
        )

        transactionRepository.insertTransaction(settlementTransaction)

        // Create offsetting work transaction to zero out work balance
        val offsetTransaction = Transaction(
            amount = kotlin.math.abs(balance),
            feeAmount = 0.0,
            description = if (balance > 0) {
                "Balance settlement - Paid to personal account"
            } else {
                "Balance settlement - Received from personal account"
            },
            type = if (balance > 0) TransactionType.EXPENSE else TransactionType.INCOME,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            category = "Work Settlement",
            counterparty = "Personal Account",
            source = "Manual",
            isWorkTransaction = true // This stays in work account
        )

        transactionRepository.insertTransaction(offsetTransaction)

        return true
    }

    /**
     * Get balance status description
     */
    fun getBalanceStatus(): String {
        return when {
            workBalance.value > 0 -> "Company owes you"
            workBalance.value < 0 -> "You owe company"
            else -> "Balanced"
        }
    }

    /**
     * Check if settlement is needed
     */
    fun needsSettlement(): Boolean {
        return workBalance.value != 0.0
    }
}
