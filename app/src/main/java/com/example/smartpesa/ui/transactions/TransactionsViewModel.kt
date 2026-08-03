package com.example.smartpesa.ui.transactions

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
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter state
    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    // Base transaction list from Room (all transactions, newest first)
    private val allTransactions: Flow<List<Transaction>> =
        transactionRepository.getAllTransactions()

    /**
     * Filtered and searched transactions
     * Combines base list + search query + filter using Flow.combine()
     * Updates automatically when any source changes
     */
    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(
            allTransactions,
            searchQuery,
            selectedFilter
        ) { transactions, query, filter ->
            var filtered = transactions

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
}
