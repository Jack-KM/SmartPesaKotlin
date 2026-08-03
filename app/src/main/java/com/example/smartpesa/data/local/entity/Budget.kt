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

    // Budget limit amount
    val amount: Double,

    // Budget period
    val period: BudgetPeriod,

    // Start date of the budget
    val startDate: LocalDateTime
)

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY
}
