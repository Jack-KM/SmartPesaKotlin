package com.example.smartpesa.data.categorization

import com.example.smartpesa.data.local.entity.Transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Matches transactions to categories based on keyword patterns
 */
@Singleton
class KeywordMatcher @Inject constructor(
    private val merchantNormalizer: MerchantNormalizer
) {

    companion object {
        // Keyword mappings for common categories
        // These are configurable and can be extended
        private val KEYWORD_MAPPINGS = mapOf(
            "Transport" to listOf(
                "uber", "bolt", "matatu", "bus", "fuel", "petrol",
                "shell", "taxi", "transport", "parking", "toll"
            ),
            "Groceries" to listOf(
                "naivas", "quickmart", "carrefour", "food", "supermarket",
                "grocery", "tuskys", "chandarana"
            ),
            "Utilities" to listOf(
                "kplc", "electricity", "token", "water", "nairobi water",
                "sewerage"
            ),
            "Entertainment" to listOf(
                "netflix", "showmax", "spotify", "cinema", "movie",
                "game", "entertainment", "dstv", "gotv"
            ),
            "Airtime" to listOf(
                "airtime", "safaricom", "airtel"
            ),
            "Restaurant" to listOf(
                "restaurant", "cafe", "kfc", "pizza", "burger",
                "java", "artcaffe", "dinner", "lunch"
            ),
            "Health" to listOf(
                "pharmacy", "hospital", "clinic", "doctor", "medical",
                "medicine", "health"
            ),
            "Education" to listOf(
                "school", "tuition", "fees", "books", "university",
                "college", "education"
            )
        )
    }

    /**
     * Find matching category based on keywords in transaction
     * Returns category name and confidence level
     */
    fun matchKeywords(transaction: Transaction, availableCategories: List<String>): Pair<String, ConfidenceLevel>? {
        val normalizedDescription = merchantNormalizer.normalize(transaction.description)
        val normalizedCounterparty = merchantNormalizer.normalize(transaction.counterparty)
        val normalizedText = "$normalizedDescription $normalizedCounterparty"

        // Try to match keywords
        for ((categoryName, keywords) in KEYWORD_MAPPINGS) {
            // Check if this category exists in available categories
            val matchingCategory = availableCategories.find {
                merchantNormalizer.normalize(it) == merchantNormalizer.normalize(categoryName)
            }

            if (matchingCategory != null) {
                // Check if any keyword matches
                for (keyword in keywords) {
                    val normalizedKeyword = merchantNormalizer.normalize(keyword)
                    if (normalizedText.contains(normalizedKeyword)) {
                        // Found a match
                        return Pair(matchingCategory, ConfidenceLevel.MEDIUM)
                    }
                }
            }
        }

        return null
    }

    /**
     * Get keyword weight for a specific keyword
     * Higher weight = more specific/reliable keyword
     */
    private fun getKeywordWeight(keyword: String): Double {
        // More specific keywords get higher weight
        // This is a simple implementation - could be enhanced with TF-IDF or similar
        return when {
            keyword.length >= 10 -> 1.0
            keyword.length >= 6 -> 0.8
            else -> 0.6
        }
    }
}
