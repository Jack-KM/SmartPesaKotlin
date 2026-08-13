package com.example.smartpesa.ui.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Budget
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.repository.BudgetRepository
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BudgetDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val budgetId: Long = savedStateHandle["budgetId"] ?: 0L

    val budget: StateFlow<Budget?> = budgetRepository
        .getBudgetById(budgetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val transactions: StateFlow<List<Transaction>> = budget
        .combine(transactionRepository.getAllTransactions()) { budgetValue, allTransactions ->
            val categoryId = budgetValue?.categoryId
            if (categoryId == null) emptyList() else allTransactions.filter { !it.isWorkTransaction && (it.categoryId == categoryId || it.category == budgetValue.category) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun deleteBudget() {
        budget.value?.let { budgetRepository.deleteBudget(it) }
    }
}
