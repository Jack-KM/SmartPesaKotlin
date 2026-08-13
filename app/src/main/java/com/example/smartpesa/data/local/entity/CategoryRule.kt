package com.example.smartpesa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-defined rule for automatic transaction categorization
 *
 * Rules are evaluated in priority order (lower priority value = higher priority)
 * If multiple rules match a transaction, the highest priority rule wins
 */
@Entity(tableName = "category_rules")
data class CategoryRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Target category ID for matching transactions */
    val categoryId: Long,

    /** Rule priority (lower value = higher priority) */
    val priority: Int = 100,

    /** Whether this rule is enabled */
    val enabled: Boolean = true,

    // Condition fields (null means "don't check this field")

    /** Match if merchant/description contains this text (case-insensitive) */
    val merchantContains: String? = null,

    /** Match if description contains this text (case-insensitive) */
    val descriptionContains: String? = null,

    /** Match if counterparty equals this (case-insensitive) */
    val counterpartyEquals: String? = null,

    /** Match if transaction type equals this */
    val transactionTypeEquals: TransactionType? = null,

    /** Match if amount is greater than this */
    val amountGreaterThan: Double? = null,

    /** Match if amount is less than this */
    val amountLessThan: Double? = null,

    /** Match if account name equals this */
    val accountNameEquals: String? = null,

    /** Match if M-Pesa transaction type contains this */
    val mpesaTypeContains: String? = null
)
