package com.example.smartpesa.data.mpesa

/**
 * Parsed M-Pesa transaction data from SMS
 */
data class ParsedTransaction(
    /** Transaction type */
    val type: TransactionType,

    /** Transaction amount in KES */
    val amount: Double,

    /** Transaction fee/cost (nullable - not all transactions have fees) */
    val feeAmount: Double? = null,

    /** Name of the other party (person/business) */
    val counterpartyName: String? = null,

    /** Phone number of the other party (if present, may be masked) */
    val counterpartyPhone: String? = null,

    /** M-Pesa transaction reference code (e.g., UG9QXAXODW) */
    val mpesaCode: String,

    /** M-Pesa balance after transaction (if present in SMS) */
    val balance: Double? = null,

    /** Original SMS body for audit/debugging */
    val rawSmsBody: String,

    /** SMS timestamp in milliseconds */
    val timestamp: Long
)
