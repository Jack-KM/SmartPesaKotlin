package com.example.smartpesa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Learned payee → category mapping
 *
 * Records which category was most recently assigned to a given payee.
 * Next transaction from the same payee auto-applies that category.
 * Most-recent-wins: updating overwrites the previous categoryId.
 */
@Entity(
    tableName = "payee_category_rules",
    indices = [Index("categoryId")]
)
data class PayeeCategoryRule(
    @PrimaryKey
    val payeeKey: String,

    val categoryId: Long,

    val timesUsed: Int = 1,

    val lastUsedAt: Long = System.currentTimeMillis()
)
