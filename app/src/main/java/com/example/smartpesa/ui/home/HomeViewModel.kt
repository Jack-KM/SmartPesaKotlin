package com.example.smartpesa.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.preferences.UserPreferencesRepository
import com.example.smartpesa.data.repository.TransactionRepository
import com.example.smartpesa.util.balanceImpactFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val previousMonth = currentMonth.minusMonths(1)

    private val currentMonthStart: LocalDateTime = currentMonth.atDay(1).atStartOfDay()
    private val currentMonthEnd: LocalDateTime = currentMonth.atEndOfMonth().atTime(23, 59, 59)
    private val previousMonthStart: LocalDateTime = previousMonth.atDay(1).atStartOfDay()
    private val previousMonthEnd: LocalDateTime = previousMonth.atEndOfMonth().atTime(23, 59, 59)

    private val currentMonthTransactions: Flow<List<Transaction>> =
        transactionRepository.getTransactionsByDateRange(currentMonthStart, currentMonthEnd)

    private val previousMonthTransactions: Flow<List<Transaction>> =
        transactionRepository.getTransactionsByDateRange(previousMonthStart, previousMonthEnd)

    val displayName: StateFlow<String?> = userPreferencesRepository.displayName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val monthlyOverview: StateFlow<MonthlyOverview> = currentMonthTransactions
        .map(::buildMonthlyOverview)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyOverview(0.0, 0.0, 0, 0.0)
        )

    val previousMonthlyOverview: StateFlow<MonthlyOverview> = previousMonthTransactions
        .map(::buildMonthlyOverview)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyOverview(0.0, 0.0, 0, 0.0)
        )

    val recentTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getAllTransactions()
        .map { transactions -> transactions.filterNot { it.isWorkTransaction }.take(7) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val spendingCategories: StateFlow<List<SpendingCategorySummary>> = currentMonthTransactions
        .map { transactions ->
            transactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE && !it.isWorkTransaction }
                .groupBy { it.category.ifBlank { "Uncategorized" } }
                .mapValues { (_, categoryTransactions) -> categoryTransactions.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { (category, amount) -> SpendingCategorySummary(category = category, amount = amount) }
                .toList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val accountBalances: StateFlow<Map<String, Double>> = transactionRepository.getAllTransactions()
        .map { transactions ->
            listOf("M-Pesa", "Cash", "Airtel Money").associateWith { account ->
                transactions.filterNot { it.isWorkTransaction }.sumOf { transaction -> transaction.balanceImpactFor(account) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    val totalBalance: StateFlow<Double> = accountBalances
        .map { balances -> balances.values.sum() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val dailyTip: StateFlow<String> = MutableStateFlow(randomDailyTip())

    fun saveDisplayName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDisplayName(name)
        }
    }

    fun getCurrentMonthName(): String = currentMonth.formatForDisplay()

    fun getPreviousMonthName(): String = previousMonth.formatForDisplay()

    private fun buildMonthlyOverview(transactions: List<Transaction>): MonthlyOverview {
        val personalTransactions = transactions.filterNot { it.isWorkTransaction }
        val spent = transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isWorkTransaction }
            .sumOf { it.amount }

        val received = personalTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        return MonthlyOverview(
            totalSpent = spent,
            totalReceived = received,
            transactionCount = personalTransactions.size,
            net = received - spent
        )
    }

    private fun randomDailyTip(): String = dailyTips.random()

    private fun YearMonth.formatForDisplay(): String {
        val month = month.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$month $year"
    }

    private companion object {
        val dailyTips = listOf(
            "Move spare money from cash into savings before the day ends.",
            "Check top spending category first. Small repeats grow fast.",
            "Record cash purchases right away to keep balance honest.",
            "Set one weekly spending limit for your highest category.",
            "Review income and expense once daily for cleaner month totals.",
            "Keep transfer notes short so future searches stay easy."
        )
    }
}

data class SpendingCategorySummary(
    val category: String,
    val amount: Double
)
