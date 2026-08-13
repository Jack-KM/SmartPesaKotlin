package com.example.smartpesa.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Loan
import com.example.smartpesa.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LoansViewModel @Inject constructor(
    loanRepository: LoanRepository
) : ViewModel() {
    val loans: StateFlow<List<Loan>> = loanRepository
        .getAllLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
