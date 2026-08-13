package com.example.smartpesa.data.local.entity

const val LOAN_RECEIVED_CATEGORY = "Loans Received"
const val LOAN_GIVEN_CATEGORY = "Loans Given"
const val LOAN_INTEREST_CATEGORY = "Interest on Loans"

fun String.isLoanCategory(): Boolean = when (trim()) {
    LOAN_RECEIVED_CATEGORY,
    LOAN_GIVEN_CATEGORY,
    LOAN_INTEREST_CATEGORY -> true
    else -> false
}

fun loanTypeForCategory(category: String): LoanType? = when (category.trim()) {
    LOAN_RECEIVED_CATEGORY -> LoanType.BORROWED
    LOAN_GIVEN_CATEGORY -> LoanType.LENT
    else -> null
}

