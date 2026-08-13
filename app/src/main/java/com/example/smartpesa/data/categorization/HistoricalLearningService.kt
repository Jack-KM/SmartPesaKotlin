package com.example.smartpesa.data.categorization

import com.example.smartpesa.data.local.entity.MerchantCategoryHistory
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.repository.MerchantCategoryHistoryRepository
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for learning and querying merchant → category relationships from historical data
 */
@Singleton
class HistoricalLearningService @Inject constructor(
    private val historyRepository: MerchantCategoryHistoryRepository,
    private val merchantNormalizer: MerchantNormalizer
) {

    companion object {
        // Minimum occurrences before considering a relationship reliable
        private const val MIN_OCCURRENCES = 2

        // Confidence thresholds based on occurrence count
        private const val HIGH_CONFIDENCE_THRESHOLD = 5
        private const val MEDIUM_CONFIDENCE_THRESHOLD = 2
    }

    /**
     * Record a transaction's merchant → category relationship
     * Call this after a transaction is categorized (either automatically or by user)
     */
    suspend fun recordTransaction(
        transaction: Transaction,
        categoryId: Long,
        wasCorrection: Boolean = false
    ) {
        val merchantName = extractMerchantName(transaction) ?: return
        val normalizedMerchant = merchantNormalizer.normalize(merchantName)

        if (normalizedMerchant.isEmpty()) return

        // Find or create history entry
        val existing = historyRepository.getByMerchantAndCategory(normalizedMerchant, categoryId)

        if (existing != null) {
            // Update existing entry
            val updated = existing.copy(
                occurrenceCount = existing.occurrenceCount + 1,
                correctionCount = if (wasCorrection) existing.correctionCount + 1 else existing.correctionCount,
                lastUsedAt = LocalDateTime.now()
            )
            historyRepository.update(updated)
        } else {
            // Create new entry
            val newHistory = MerchantCategoryHistory(
                normalizedMerchant = normalizedMerchant,
                categoryId = categoryId,
                occurrenceCount = 1,
                correctionCount = if (wasCorrection) 1 else 0,
                lastUsedAt = LocalDateTime.now(),
                createdAt = LocalDateTime.now()
            )
            historyRepository.insert(newHistory)
        }
    }

    /**
     * Get the most likely category for a merchant based on historical data
     * Returns null if no reliable historical data exists
     */
    suspend fun suggestCategory(transaction: Transaction): Pair<Long, ConfidenceLevel>? {
        val merchantName = extractMerchantName(transaction) ?: return null
        val normalizedMerchant = merchantNormalizer.normalize(merchantName)

        if (normalizedMerchant.isEmpty()) return null

        // Get all history entries for this merchant
        val histories = historyRepository.getByMerchant(normalizedMerchant)

        if (histories.isEmpty()) {
            // Try fuzzy matching
            return findSimilarMerchant(normalizedMerchant)
        }

        // Find the most common category
        val bestMatch = histories.maxByOrNull { it.occurrenceCount } ?: return null

        // Check if it meets minimum threshold
        if (bestMatch.occurrenceCount < MIN_OCCURRENCES) {
            return null
        }

        // Calculate confidence based on occurrence count and correction count
        val confidence = calculateConfidence(bestMatch)

        return Pair(bestMatch.categoryId, confidence)
    }

    /**
     * Find similar merchant using fuzzy matching
     * Returns null if no similar merchant found
     */
    private suspend fun findSimilarMerchant(normalizedMerchant: String): Pair<Long, ConfidenceLevel>? {
        // This is a simple implementation - could be improved with more sophisticated fuzzy matching
        // For now, we skip this to avoid performance issues
        // A future optimization could use a separate index or caching
        return null
    }

    /**
     * Calculate confidence level based on historical data
     */
    private fun calculateConfidence(history: MerchantCategoryHistory): ConfidenceLevel {
        val occurrenceCount = history.occurrenceCount
        val correctionRatio = if (occurrenceCount > 0) {
            history.correctionCount.toDouble() / occurrenceCount.toDouble()
        } else {
            0.0
        }

        // High correction ratio lowers confidence
        if (correctionRatio > 0.3) {
            return ConfidenceLevel.LOW
        }

        return when {
            occurrenceCount >= HIGH_CONFIDENCE_THRESHOLD -> ConfidenceLevel.HIGH
            occurrenceCount >= MEDIUM_CONFIDENCE_THRESHOLD -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
    }

    /**
     * Extract merchant name from transaction
     */
    private fun extractMerchantName(transaction: Transaction): String? {
        // Try to extract from description first
        val fromDescription = merchantNormalizer.extractMerchantName(transaction.description)
        if (fromDescription != null) {
            return fromDescription
        }

        // Fall back to counterparty
        if (transaction.counterparty.isNotEmpty()) {
            return transaction.counterparty
        }

        // Last resort: use description as-is
        return transaction.description.takeIf { it.isNotEmpty() }
    }
}
