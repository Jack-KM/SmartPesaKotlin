package com.example.smartpesa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Tracks historical merchant → category relationships learned from user behavior
 *
 * Used to suggest categories based on previously categorized transactions
 */
@Entity(
    tableName = "merchant_category_history",
    indices = [
        Index("normalizedMerchant"),
        Index("categoryId"),
        Index(value = ["normalizedMerchant", "categoryId"], unique = true)
    ]
)
data class MerchantCategoryHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Normalized merchant name (from MerchantNormalizer) */
    val normalizedMerchant: String,

    /** Category ID that this merchant has been assigned to */
    val categoryId: Long,

    /** Number of times this merchant → category mapping has occurred */
    val occurrenceCount: Int = 1,

    /** Number of times user corrected from a different category to this one */
    val correctionCount: Int = 0,

    /** Timestamp of the most recent transaction with this mapping */
    val lastUsedAt: LocalDateTime = LocalDateTime.now(),

    /** Timestamp when this history entry was first created */
    val createdAt: LocalDateTime = LocalDateTime.now()
)
