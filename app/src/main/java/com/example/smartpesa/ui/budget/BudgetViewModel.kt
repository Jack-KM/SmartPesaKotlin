package com.example.smartpesa.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Budget
import com.example.smartpesa.data.local.entity.BudgetPeriod
import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.repository.BudgetRepository
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

/**
 * ViewModel for Budget screen
 * Computes budget progress and generates data-driven insights from real transaction data
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: com.example.smartpesa.data.repository.CategoryRepository
) : ViewModel() {

    // Dialog state for add/edit budget
    private val _showAddBudgetDialog = MutableStateFlow(false)
    val showAddBudgetDialog: StateFlow<Boolean> = _showAddBudgetDialog.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _budgetAmount = MutableStateFlow("")
    val budgetAmount: StateFlow<String> = _budgetAmount.asStateFlow()

    // All categories for budget selection
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current month budgets with progress
    val budgetProgress: StateFlow<List<BudgetProgress>> = combine(
        budgetRepository.getAllBudgets(),
        categoryRepository.getAllCategories()
    ) { budgets, categories ->
        budgets to categories
    }
    .flatMapLatest { (budgets, categories) ->
        val categoryMap = categories.associateBy { it.id }
        val monthlyBudgets = budgets.filter { it.period == BudgetPeriod.MONTHLY }

        if (monthlyBudgets.isEmpty()) {
            flowOf(emptyList())
        } else {
            // For each budget, compute progress with enriched category data
            val progressFlows = monthlyBudgets.map { budget ->
                computeBudgetProgress(budget, categoryMap[budget.categoryId])
            }
            combine(progressFlows) { progressArray ->
                progressArray.filterNotNull().sortedBy { it.categoryName }
            }
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Budget insights generated from real data
    val budgetInsights: StateFlow<List<BudgetInsight>> = budgetProgress
        .map { progressList ->
            generateInsights(progressList)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Compute budget progress for a single budget
     */
    private fun computeBudgetProgress(budget: Budget, category: Category?): Flow<BudgetProgress?> {
        val currentMonth = YearMonth.now()
        val monthStart = currentMonth.atDay(1).atStartOfDay()
        val monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59)

        return transactionRepository.getTotalByCategoryAndDateRange(
            categoryId = budget.categoryId,
            startDate = monthStart,
            endDate = monthEnd
        ).map { spent ->
            val spentAmount = spent ?: 0.0
            val percentage = if (budget.amount > 0) {
                (spentAmount / budget.amount * 100).toInt()
            } else {
                0
            }

            BudgetProgress(
                budgetId = budget.id,
                categoryId = budget.categoryId,
                categoryName = category?.name ?: "Unknown Category",
                budgeted = budget.amount,
                spent = spentAmount,
                percentage = percentage,
                isOverBudget = spentAmount > budget.amount
            )
        }
    }

    /**
     * Generate data-driven insights from budget progress
     */
    private fun generateInsights(progressList: List<BudgetProgress>): List<BudgetInsight> {
        val insights = mutableListOf<BudgetInsight>()
        val currentMonth = YearMonth.now()
        val daysInMonth = currentMonth.lengthOfMonth()
        val currentDay = LocalDateTime.now().dayOfMonth
        val daysRemaining = daysInMonth - currentDay

        progressList.forEach { progress ->
            // Insight 1: High usage with days remaining
            if (progress.percentage >= 70 && daysRemaining > 0) {
                val message = when {
                    progress.percentage >= 100 -> {
                        "You've exceeded your ${progress.categoryName} budget by ${progress.percentage - 100}% with $daysRemaining days left in the month"
                    }
                    progress.percentage >= 90 -> {
                        "You've spent ${progress.percentage}% of your ${progress.categoryName} budget with $daysRemaining days left in the month"
                    }
                    else -> {
                        "You've spent ${progress.percentage}% of your ${progress.categoryName} budget"
                    }
                }
                insights.add(
                    BudgetInsight(
                        categoryId = progress.categoryId,
                        message = message,
                        severity = when {
                            progress.percentage >= 100 -> InsightSeverity.HIGH
                            progress.percentage >= 90 -> InsightSeverity.MEDIUM
                            else -> InsightSeverity.LOW
                        }
                    )
                )
            }

            // Insight 2: On track / good progress
            if (progress.percentage < 70 && progress.spent > 0 && daysRemaining > 0) {
                val expectedPercentage = ((currentDay.toDouble() / daysInMonth) * 100).toInt()
                if (progress.percentage <= expectedPercentage) {
                    insights.add(
                        BudgetInsight(
                            categoryId = progress.categoryId,
                            message = "Your ${progress.categoryName} spending is on track (${progress.percentage}% used)",
                            severity = InsightSeverity.LOW
                        )
                    )
                }
            }
        }

        return insights
    }

    /**
     * Show add budget dialog
     */
    fun showAddBudgetDialog(category: Category? = null) {
        _selectedCategory.value = category
        _budgetAmount.value = ""
        _showAddBudgetDialog.value = true
    }

    /**
     * Hide add budget dialog
     */
    fun hideAddBudgetDialog() {
        _showAddBudgetDialog.value = false
        _selectedCategory.value = null
        _budgetAmount.value = ""
    }

    /**
     * Update selected category
     */
    fun onCategorySelected(category: Category) {
        _selectedCategory.value = category
    }

    /**
     * Update budget amount
     */
    fun onBudgetAmountChanged(amount: String) {
        _budgetAmount.value = amount
    }

    /**
     * Save budget
     */
    fun saveBudget() {
        val category = _selectedCategory.value ?: return
        val amount = _budgetAmount.value.toDoubleOrNull() ?: return

        if (amount <= 0) return

        viewModelScope.launch {
            val budget = Budget(
                categoryId = category.id,
                amount = amount,
                period = BudgetPeriod.MONTHLY,
                startDate = YearMonth.now().atDay(1).atStartOfDay()
            )
            budgetRepository.insertBudget(budget)
            hideAddBudgetDialog()
        }
    }
}

/**
 * Budget progress for a category
 */
data class BudgetProgress(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val budgeted: Double,
    val spent: Double,
    val percentage: Int,
    val isOverBudget: Boolean
)

/**
 * Budget insight with data-driven message
 */
data class BudgetInsight(
    val categoryId: Long,
    val message: String,
    val severity: InsightSeverity
)

enum class InsightSeverity {
    LOW,    // Green - good/on track
    MEDIUM, // Amber - warning
    HIGH    // Red - over budget
}
