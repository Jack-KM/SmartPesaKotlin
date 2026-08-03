package com.example.smartpesa.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

/**
 * ViewModel for Home screen
 * Provides monthly overview and recent transactions from Room database
 *
 * Uses StateFlow with stateIn + WhileSubscribed instead of collectAsState directly because:
 * - StateFlow is a hot flow shared among multiple collectors (efficient)
 * - WhileSubscribed(5000) keeps upstream active for 5 seconds after last collector unsubscribes
 * - Prevents unnecessary DB re-queries when navigating away and back quickly
 * - Caches last value so new collectors get it immediately without recomputation
 * - collectAsState on cold Flow would restart query on every recomposition
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // Current month boundaries for filtering
    private val currentMonth = YearMonth.now()
    private val monthStart: LocalDateTime = currentMonth.atDay(1).atStartOfDay()
    private val monthEnd: LocalDateTime = currentMonth.atEndOfMonth().atTime(23, 59, 59)

    /**
     * All transactions for the current calendar month
     * Used to compute monthly overview
     */
    private val monthTransactionsFlow: Flow<List<Transaction>> =
        transactionRepository.getTransactionsByDateRange(monthStart, monthEnd)

    /**
     * Monthly overview: spent, received, count, net for current month
     * Computed from current month's transactions
     */
    val monthlyOverview: StateFlow<MonthlyOverview> = monthTransactionsFlow
        .map { transactions ->
            val spent = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val received = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            MonthlyOverview(
                totalSpent = spent,
                totalReceived = received,
                transactionCount = transactions.size,
                net = received - spent
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep active 5s after last collector
            initialValue = MonthlyOverview(0.0, 0.0, 0, 0.0)
        )

    /**
     * Recent transactions (last 15, newest first)
     * Displayed in the Home screen list
     */
    val recentTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getAllTransactions()
            .map { transactions ->
                transactions.take(15) // Already sorted newest first by DAO
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Loading state - true until first data arrives
     */
    val isLoading: StateFlow<Boolean> = recentTransactions
        .map { false } // Once we have data, loading is done
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    /**
     * Get current month name for display (e.g., "August 2026")
     */
    fun getCurrentMonthName(): String {
        val month = currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val year = currentMonth.year
        return "$month $year"
    }
}
