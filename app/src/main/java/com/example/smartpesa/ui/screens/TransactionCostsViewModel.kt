package com.example.smartpesa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class TransactionCostsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    fun selectPreviousMonth() { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun selectNextMonth() { _selectedMonth.value = _selectedMonth.value.plusMonths(1) }

    val uiState: StateFlow<TransactionCostsUiState> = combine(
        transactionRepository.getAllTransactions(),
        _selectedMonth
    ) { transactions, month ->
        // All transactions that have a fee
        val withFees = transactions.filter { it.feeAmount > 0.0 && !it.isWorkTransaction }

        val monthEntries = withFees
            .filter { YearMonth.from(it.timestamp.toLocalDate()) == month }
            .sortedByDescending { it.timestamp }
            .map { tx ->
                FeeEntry(
                    id = tx.id,
                    date = tx.timestamp.toLocalDate(),
                    time = tx.timestamp.toLocalTime().toString().take(5),
                    title = tx.description.lineSequence().firstOrNull()?.trim()
                        .orEmpty().ifBlank { tx.category.ifBlank { "Transaction" } },
                    category = tx.category.ifBlank { "Uncategorized" },
                    type = tx.type.name,
                    feeAmount = tx.feeAmount
                )
            }

        val grouped = monthEntries
            .groupBy { it.date }
            .map { (date, items) ->
                FeeDayGroup(date, items.sumOf { it.feeAmount }, items)
            }
            .sortedByDescending { it.date }

        TransactionCostsUiState(
            hasAnyFees = withFees.isNotEmpty(),
            dayGroups = grouped,
            monthTotal = monthEntries.sumOf { it.feeAmount }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionCostsUiState()
    )
}

data class TransactionCostsUiState(
    val hasAnyFees: Boolean = false,
    val dayGroups: List<FeeDayGroup> = emptyList(),
    val monthTotal: Double = 0.0
)

data class FeeDayGroup(
    val date: LocalDate,
    val totalFee: Double,
    val items: List<FeeEntry>
)

data class FeeEntry(
    val id: Long,
    val date: LocalDate,
    val time: String,
    val title: String,
    val category: String,
    val type: String,
    val feeAmount: Double
)
