package com.example.smartpesa.data.categorization

import android.util.Log
import com.example.smartpesa.data.local.entity.PayeeCategoryRule
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.repository.PayeeCategoryRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing payee-based category rules
 *
 * Implements first-shot learning with most-recent-wins:
 * - First categorization of a payee creates a rule
 * - Subsequent categorizations update the rule (overwrite categoryId)
 * - Lookup is instant (no minimum occurrence threshold)
 */
@Singleton
class PayeeCategoryRuleService @Inject constructor(
    private val payeeCategoryRuleRepository: PayeeCategoryRuleRepository,
    private val payeeKeyDeriver: PayeeKeyDeriver
) {

    companion object {
        private const val TAG = "PayeeCategoryRuleService"
    }

    /**
     * Suggest a category for a transaction based on payee rules
     * Returns null if no rule exists for this payee
     */
    suspend fun suggestCategory(transaction: Transaction): Pair<Long, ConfidenceLevel>? {
        val payeeKey = payeeKeyDeriver.derivePayeeKey(transaction) ?: return null

        val rule = payeeCategoryRuleRepository.getByPayeeKey(payeeKey) ?: return null

        Log.d(TAG, "Found payee rule for '$payeeKey': categoryId=${rule.categoryId}, timesUsed=${rule.timesUsed}")

        // Payee rules are high confidence - they're explicit user choices
        return Pair(rule.categoryId, ConfidenceLevel.HIGH)
    }

    /**
     * Record a categorization by upserting a payee rule
     * Most-recent-wins: overwrites previous categoryId for this payee
     */
    suspend fun recordCategorization(
        transaction: Transaction,
        categoryId: Long
    ) {
        val payeeKey = payeeKeyDeriver.derivePayeeKey(transaction)

        if (payeeKey.isNullOrEmpty()) {
            Log.d(TAG, "No payee key derived, skipping rule creation")
            return
        }

        val existing = payeeCategoryRuleRepository.getByPayeeKey(payeeKey)

        val rule = if (existing != null) {
            // Update existing rule - most recent wins
            existing.copy(
                categoryId = categoryId,
                timesUsed = existing.timesUsed + 1,
                lastUsedAt = System.currentTimeMillis()
            )
        } else {
            // Create new rule
            PayeeCategoryRule(
                payeeKey = payeeKey,
                categoryId = categoryId,
                timesUsed = 1,
                lastUsedAt = System.currentTimeMillis()
            )
        }

        payeeCategoryRuleRepository.upsertRule(rule)
        Log.d(TAG, "Upserted payee rule for '$payeeKey': categoryId=$categoryId")
    }
}
