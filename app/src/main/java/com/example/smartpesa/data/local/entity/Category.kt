package com.example.smartpesa.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Category entity for organizing transactions
 * Supports hierarchical structure with parent/sub-categories
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentCategoryId")]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val type: TransactionType,

    // Hex color code (e.g., "#FF5722")
    val color: String = "#6200EE",

    // Material Icon name (e.g., "Restaurant", "DirectionsCar")
    val icon: String? = null,

    // Parent category ID for sub-categories (null for main categories)
    val parentCategoryId: Long? = null
) {
    /**
     * Check if this is a main category (no parent)
     */
    fun isMainCategory(): Boolean = parentCategoryId == null

    /**
     * Check if this is a sub-category (has parent)
     */
    fun isSubCategory(): Boolean = parentCategoryId != null
}
