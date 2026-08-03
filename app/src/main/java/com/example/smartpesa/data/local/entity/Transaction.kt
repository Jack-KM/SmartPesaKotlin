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

    // Source of the transaction (e.g., "M-Pesa SMS", "Manual")
    val source: String = "Manual",

    // M-Pesa transaction reference code (e.g., UG9QXAXODW) - used for deduplication
    val mpesaCode: String? = null,

    // Original SMS body for debugging/verification (M-Pesa transactions)
    val originalSmsBody: String? = null
)

enum class TransactionType {
    INCOME,
    EXPENSE
}
