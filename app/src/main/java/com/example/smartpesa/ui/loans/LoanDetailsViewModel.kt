package com.example.smartpesa.ui.loans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Loan
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.repository.LoanRepository
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loanRepository: LoanRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val loanId: Long = savedStateHandle["loanId"] ?: 0L

    val loan: StateFlow<Loan?> = loanRepository
        .getLoanById(loanId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val relatedTransactions: StateFlow<List<Transaction>> = transactionRepository
        .getTransactionsByRelatedLoanId(loanId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun deleteLoan(): Boolean {
        val currentLoan = loan.value ?: return false
        val linkedTransactions = transactionRepository.getTransactionsByRelatedLoanId(currentLoan.id).first()
        if (currentLoan.remainingBalance > 0.0 || currentLoan.payments.isNotEmpty() || linkedTransactions.isNotEmpty()) {
            return false
        }
        loanRepository.deleteLoan(currentLoan)
        return true
    }

    suspend fun markAsPaid(): Boolean {
        val currentLoan = loan.value ?: return false
        loanRepository.updateLoan(currentLoan.copy(remainingBalance = 0.0))
        return true
    }
}
