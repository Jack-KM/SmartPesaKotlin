package com.example.smartpesa.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class LoanType {
    BORROWED,
    LENT
}

data class LoanPayment(
    val amount: Double,
    val timestamp: LocalDateTime,
    val transactionId: Long? = null,
    val note: String = ""
)

data class FulizaRepayment(
    val amount: Double,
    val timestamp: LocalDateTime,
    val transactionId: Long? = null
)

data class FulizaAccessCharge(
    val amount: Double,
    val timestamp: LocalDateTime,
    val transactionId: Long? = null
)

enum class TransactionCostType {
    FEE,
    CHARGE,
    INTEREST,
    OTHER
}

enum class TransactionProvider {
    MPESA,
    BANK,
    ATM,
    CARD
}

@Entity(
    tableName = "transaction_costs",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId")]
)
data class TransactionCost(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val costAmount: Double,
    val costType: TransactionCostType,
    val provider: TransactionProvider
)

@Entity(tableName = "fuliza")
data class Fuliza(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // The amount currently owed (outstanding) — set directly from SMS "outstanding amount is Ksh X"
    val currentBalance: Double = 0.0,
    val availableLimit: Double = 0.0,
    // Cumulative access fees charged so far
    val totalAccessFees: Double = 0.0,
    // Due date string as received from SMS e.g. "10/09/26"
    val dueDate: String? = null,
    val accessCharges: List<FulizaAccessCharge> = emptyList(),
    val repaymentHistory: List<FulizaRepayment> = emptyList(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val interestRate: Double,
    val type: LoanType,
    val counterparty: String,
    val startDate: LocalDateTime,
    val dueDate: LocalDateTime,
    val remainingBalance: Double,
    val payments: List<LoanPayment> = emptyList()
)
