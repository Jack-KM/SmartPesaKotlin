package com.example.smartpesa.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Budget entity for setting spending/income limits
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Foreign key to Category
    val categoryId: Long,

    // Optional account linkage for account-specific budgets
    val accountName: String? = null,

    // Budget limit amount
    val limit: Double,

    // Budget period
    val period: BudgetPeriod,

    // Start date of the budget
    val startDate: LocalDateTime,

    // Optional end date for custom budgets
    val endDate: LocalDateTime? = null,

    // Stored progress values for quick rendering
    val spent: Double = 0.0,
    val remaining: Double = 0.0,

    // Display label used when category relation is unavailable
    val category: String = ""
){
    val amount: Double
        get() = limit

    val isCustomPeriod: Boolean
        get() = period == BudgetPeriod.CUSTOM
}

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    CUSTOM
}
