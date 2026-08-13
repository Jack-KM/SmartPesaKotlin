package com.example.smartpesa.data.mpesa

/**
 * Parsed M-Pesa transaction data from SMS
 */
data class ParsedTransaction(
    val type: TransactionType,
    val amount: Double,
    val feeAmount: Double? = null,

    // Fuliza-specific: access fee charged for this draw, total outstanding, due date string
    val fulizaAccessFee: Double? = null,
    val fulizaOutstanding: Double? = null,
    val fulizaDueDate: String? = null,

    val counterpartyName: String? = null,
    val counterpartyPhone: String? = null,
    val mpesaCode: String,
    val balance: Double? = null,
    val rawSmsBody: String,
    val timestamp: Long
)
