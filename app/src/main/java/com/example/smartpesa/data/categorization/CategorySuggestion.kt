package com.example.smartpesa.data.categorization

/**
 * Represents a category suggestion for a transaction
 */
data class CategorySuggestion(
    val categoryId: Long?,
    val confidence: ConfidenceLevel,
    val source: SuggestionSource
)

/**
 * Confidence level for category suggestions
 */
enum class ConfidenceLevel {
    /**
     * High confidence - strong historical relationship or explicit user rule
     * UI should auto-assign this category
     */
    HIGH,

    /**
     * Medium confidence - some historical evidence or keyword match
     * UI should suggest but allow easy override
     */
    MEDIUM,

    /**
     * Low confidence - weak signals or no strong match
     * UI should show as tentative suggestion
     */
    LOW
}

/**
 * Source of the category suggestion
 */
enum class SuggestionSource {
    /** User-created explicit rule */
    USER_RULE,

    /** Learned from payee-based categorization (first-shot, most-recent-wins) */
    PAYEE_RULE,

    /** Learned from historical transactions */
    LEARNED_HISTORY,

    /** Matched merchant name */
    MERCHANT_MATCH,

    /** Keyword/metadata match */
    KEYWORD_MATCH,

    /** Transaction metadata (type, etc.) */
    METADATA,

    /** Default/fallback suggestion */
    DEFAULT
}
