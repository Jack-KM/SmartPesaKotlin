package com.example.smartpesa.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionCost
import com.example.smartpesa.data.repository.TransactionCostRepository
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val transactionCostRepository: TransactionCostRepository
) : ViewModel() {

    private val transactionId: Long = savedStateHandle["transactionId"] ?: 0L

    val transaction: StateFlow<Transaction?> = transactionRepository
        .getTransactionById(transactionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val costs: StateFlow<List<TransactionCost>> = transactionCostRepository
        .getCostsByTransactionId(transactionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun deleteTransaction() {
        transaction.value?.let { transactionRepository.deleteTransaction(it) }
    }
}
