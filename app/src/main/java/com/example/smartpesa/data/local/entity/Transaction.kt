package com.example.smartpesa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Transaction entity representing a financial transaction
 * Fee amount is tracked as metadata, NOT as a budgetable category
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val amount: Double,

    // M-Pesa transaction fee - tracked but not budgeted
    val feeAmount: Double = 0.0,

    val description: String,

    val type: TransactionType,

    val timestamp: LocalDateTime,

    // Foreign key to Category
    val categoryId: Long?,

    // Display category name for details and validation rules
    val category: String = "",

    // Counterparty involved in transaction, if known
    val counterparty: String = "",

    // Account name tied to this transaction
    val accountName: String? = null,

    // Recurring transaction metadata
    val isRecurring: Boolean = false,

    val recurringConfig: RecurringConfig? = null,

    // Links to related balances
    val relatedLoanId: Long? = null,
    val relatedFulizaId: Long? = null,

    // Source of the transaction (e.g., "M-Pesa SMS", "Manual")
    val source: String = "Manual",

    // Full M-Pesa message body stored separately from description
    val mpesaMessage: String? = null,

    // M-Pesa transaction reference code (e.g., UG9QXAXODW) - used for deduplication
    val mpesaCode: String? = null,

    // Original SMS body for debugging/verification (M-Pesa transactions)
    val originalSmsBody: String? = null,

    // Work transaction flag - separates business/work transactions from personal
    val isWorkTransaction: Boolean = false,

    // Auto-categorization flag - indicates if categoryId was set by auto-categorization
    val isAutoCategorized: Boolean = false
)

data class RecurringConfig(
    val frequency: String = "monthly",
    val interval: Int = 1,
    val nextRunAt: LocalDateTime? = null
)

enum class TransactionType {
    INCOME,
    EXPENSE
}
