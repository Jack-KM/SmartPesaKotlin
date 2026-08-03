package com.example.smartpesa.ui.home

/**
 * Monthly overview data for the Home screen
 * Represents aggregated transaction data for the current calendar month
 */
data class MonthlyOverview(
    /** Total amount spent (EXPENSE transactions) this month */
    val totalSpent: Double,

    /** Total amount received (INCOME transactions) this month */
    val totalReceived: Double,

    /** Number of transactions this month */
    val transactionCount: Int,

    /** Net amount (received - spent) */
    val net: Double
)
