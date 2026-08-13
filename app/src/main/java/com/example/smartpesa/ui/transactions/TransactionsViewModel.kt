package com.example.smartpesa.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for Transactions screen
 * Provides full transaction list with search and filter capabilities
 *
 * Uses Flow.combine() to merge base list + search + filter efficiently
 * without re-querying database on every keystroke
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter state
    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    private val accountFilter: StateFlow<String?> = savedStateHandle.getStateFlow("account", "")
        .map { it.trim().takeIf(String::isNotBlank) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = savedStateHandle.get<String>("account")?.trim()?.takeIf(String::isNotBlank)
        )

    val selectedAccount: StateFlow<String?> = accountFilter

    // Base transaction list from Room (all transactions, newest first)
    private val allTransactions: Flow<List<Transaction>> =
        transactionRepository.getPersonalTransactions()

    /**
     * Filtered and searched transactions
     * Combines base list + search query + filter using Flow.combine()
     * Updates automatically when any source changes
     */
    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(
            allTransactions,
            searchQuery,
            selectedFilter,
            accountFilter
        ) { transactions, query, filter, account ->
            var filtered = transactions

            if (!account.isNullOrBlank()) {
                filtered = filtered.filter { transaction ->
                    transaction.matchesAccount(account)
                }
            }

            // Apply type filter
            if (filter != TransactionFilter.ALL) {
                filtered = filtered.filter { transaction ->
                    filter.matches(transaction.description)
                }
            }

            // Apply search query (counterparty name or mpesaCode)
            if (query.isNotBlank()) {
                filtered = filtered.filter { transaction ->
                    transaction.description.contains(query, ignoreCase = true) ||
                            transaction.mpesaCode?.contains(query, ignoreCase = true) == true
                }
            }

            filtered
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Loading state - true until first data arrives
     */
    val isLoading: StateFlow<Boolean> = filteredTransactions
        .map { false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    /**
     * Update search query
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Update selected filter
     */
    fun onFilterSelected(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    /**
     * Clear search and filters
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedFilter.value = TransactionFilter.ALL
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionRepository.deleteTransaction(transaction)
    }

    private fun Transaction.matchesAccount(account: String): Boolean {
        val counterpartyText = counterparty.trim()
        if (counterpartyText.isBlank()) return false

        val parts = counterpartyText.split("→", limit = 2).map { it.trim() }
        return when {
            parts.size == 2 -> parts.any { it.equals(account, ignoreCase = true) }
            else -> counterpartyText.equals(account, ignoreCase = true) || counterpartyText.contains(account, ignoreCase = true)
        }
    }
}
