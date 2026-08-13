package com.example.smartpesa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Fuliza
import com.example.smartpesa.data.local.entity.FulizaRepayment
import com.example.smartpesa.data.repository.FulizaRepository
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FulizaViewModel @Inject constructor(
    private val fulizaRepository: FulizaRepository,
    transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<FulizaUiState> = combine(
        fulizaRepository.getAllFuliza(),
        transactionRepository.getAllTransactions()
    ) { records, transactions ->
            val active = records.maxByOrNull { it.updatedAt }
            if (active == null) {
                FulizaUiState()
            } else {
                val transactionsById = transactions.associateBy { it.id }
                FulizaUiState(
                    hasData = true,
                    outstandingBalance = active.currentBalance,
                    totalAccessFees = active.totalAccessFees,
                    dueDate = active.dueDate,
                    isPaidOff = active.currentBalance == 0.0,
                    accessCharges = active.accessCharges
                        .sortedByDescending { it.timestamp }
                        .map { charge ->
                            val tx = charge.transactionId?.let(transactionsById::get)
                            FulizaAccessFeeEntry(
                                amount = charge.amount,
                                timestamp = charge.timestamp,
                                transactionId = charge.transactionId,
                                title = tx?.description?.lineSequence()?.firstOrNull()?.trim()
                                    .orEmpty().ifBlank { "Fuliza transaction" }
                            )
                        },
                    repaymentHistory = active.repaymentHistory
                        .sortedByDescending { it.timestamp }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FulizaUiState()
        )
}

data class FulizaUiState(
    val hasData: Boolean = false,
    val outstandingBalance: Double = 0.0,
    val totalAccessFees: Double = 0.0,
    val dueDate: String? = null,
    val isPaidOff: Boolean = false,
    val accessCharges: List<FulizaAccessFeeEntry> = emptyList(),
    val repaymentHistory: List<FulizaRepayment> = emptyList()
)

data class FulizaAccessFeeEntry(
    val amount: Double,
    val timestamp: java.time.LocalDateTime,
    val transactionId: Long?,
    val title: String
)
