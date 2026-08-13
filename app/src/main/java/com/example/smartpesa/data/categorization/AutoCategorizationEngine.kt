package com.example.smartpesa.data.categorization

import android.util.Log
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main engine for automatic transaction categorization
 *
 * Implements a priority-based categorization system:
 * 1. User-defined rules (highest priority)
 * 2. Historical learning (merchant → category mapping)
 * 3. Merchant name matching
 * 4. Keyword matching
 * 5. Transaction metadata
 * 6. Default fallback
 */
@Singleton
class AutoCategorizationEngine @Inject constructor(
    private val categoryRuleEngine: CategoryRuleEngine,
    private val payeeCategoryRuleService: PayeeCategoryRuleService,
    private val historicalLearningService: HistoricalLearningService,
    private val keywordMatcher: KeywordMatcher,
    private val categoryRepository: CategoryRepository
) {

    companion object {
        private const val TAG = "AutoCategorizationEngine"
    }

    /**
     * Suggest a category for a transaction
     * Returns null if no suggestion can be made
     */
    suspend fun suggestCategory(transaction: Transaction): CategorySuggestion? {
        Log.d(TAG, "Suggesting category for transaction: ${transaction.description}")

        // Priority 1: User-defined rules
        val ruleMatch = categoryRuleEngine.findMatchingRule(transaction)
        if (ruleMatch != null) {
            Log.d(TAG, "Matched user rule: ${ruleMatch.id}")
            return CategorySuggestion(
                categoryId = ruleMatch.categoryId,
                confidence = ConfidenceLevel.HIGH,
                source = SuggestionSource.USER_RULE
            )
        }

        // Priority 2: Payee-based learned rules (first-shot, most-recent-wins)
        val payeeRuleMatch = payeeCategoryRuleService.suggestCategory(transaction)
        if (payeeRuleMatch != null) {
            Log.d(TAG, "Matched payee rule: categoryId=${payeeRuleMatch.first}")
            return CategorySuggestion(
                categoryId = payeeRuleMatch.first,
                confidence = payeeRuleMatch.second,
                source = SuggestionSource.PAYEE_RULE
            )
        }

        // Priority 3: Historical learning (frequency-based)
        val historicalMatch = historicalLearningService.suggestCategory(transaction)
        if (historicalMatch != null) {
            Log.d(TAG, "Matched historical data: categoryId=${historicalMatch.first}, confidence=${historicalMatch.second}")
            return CategorySuggestion(
                categoryId = historicalMatch.first,
                confidence = historicalMatch.second,
                source = SuggestionSource.LEARNED_HISTORY
            )
        }

        // Priority 4 & 5: Keyword matching
        // Get available categories to match against
        val categories = categoryRepository.getAllCategories().first()
        val categoryNames = categories.map { it.name }

        val keywordMatch = keywordMatcher.matchKeywords(transaction, categoryNames)
        if (keywordMatch != null) {
            val matchingCategory = categories.find { it.name == keywordMatch.first }
            if (matchingCategory != null) {
                Log.d(TAG, "Matched keyword: category=${matchingCategory.name}")
                return CategorySuggestion(
                    categoryId = matchingCategory.id,
                    confidence = keywordMatch.second,
                    source = SuggestionSource.KEYWORD_MATCH
                )
            }
        }

        // Priority 6: Transaction metadata (M-Pesa type, transaction type)
        val metadataMatch = matchByMetadata(transaction, categories)
        if (metadataMatch != null) {
            Log.d(TAG, "Matched by metadata: category=${metadataMatch.first.name}")
            return CategorySuggestion(
                categoryId = metadataMatch.first.id,
                confidence = metadataMatch.second,
                source = SuggestionSource.METADATA
            )
        }

        // No match found
        Log.d(TAG, "No category suggestion found")
        return null
    }

    /**
     * Match transaction by metadata (M-Pesa type, transaction type, etc.)
     */
    private fun matchByMetadata(
        transaction: Transaction,
        categories: List<com.example.smartpesa.data.local.entity.Category>
    ): Pair<com.example.smartpesa.data.local.entity.Category, ConfidenceLevel>? {

        // Match based on M-Pesa message patterns
        val mpesaMessage = transaction.mpesaMessage?.uppercase() ?: ""

        // Airtime
        if (mpesaMessage.contains("AIRTIME") || transaction.description.contains("airtime", ignoreCase = true)) {
            val airtimeCategory = categories.find {
                it.name.equals("Airtime", ignoreCase = true) &&
                it.type == TransactionType.EXPENSE
            }
            if (airtimeCategory != null) {
                return Pair(airtimeCategory, ConfidenceLevel.HIGH)
            }
        }

        // Electricity/KPLC
        if (mpesaMessage.contains("KPLC") || transaction.description.contains("KPLC", ignoreCase = true)) {
            val utilityCategory = categories.find {
                (it.name.equals("Utilities", ignoreCase = true) ||
                 it.name.equals("Bills", ignoreCase = true)) &&
                it.type == TransactionType.EXPENSE
            }
            if (utilityCategory != null) {
                return Pair(utilityCategory, ConfidenceLevel.HIGH)
            }
        }

        // Fuliza repayment
        if (mpesaMessage.contains("FULIZA") || transaction.description.contains("Fuliza", ignoreCase = true)) {
            val debtCategory = categories.find {
                (it.name.equals("Debt", ignoreCase = true) ||
                 it.name.equals("Loan", ignoreCase = true)) &&
                it.type == TransactionType.EXPENSE
            }
            if (debtCategory != null) {
                return Pair(debtCategory, ConfidenceLevel.HIGH)
            }
        }

        // Withdrawal
        if (transaction.description.contains("Withdrawal", ignoreCase = true) ||
            transaction.description.contains("Withdraw", ignoreCase = true)) {
            val cashCategory = categories.find {
                it.name.equals("Cash Withdrawal", ignoreCase = true) &&
                it.type == TransactionType.EXPENSE
            }
            if (cashCategory != null) {
                return Pair(cashCategory, ConfidenceLevel.MEDIUM)
            }
        }

        // Salary (if transaction is income and amount is large)
        if (transaction.type == TransactionType.INCOME && transaction.amount >= 10000) {
            val salaryCategory = categories.find {
                it.name.equals("Salary", ignoreCase = true) &&
                it.type == TransactionType.INCOME
            }
            if (salaryCategory != null) {
                return Pair(salaryCategory, ConfidenceLevel.MEDIUM)
            }
        }

        return null
    }

    /**
     * Record a transaction categorization for learning
     * Call this after a transaction is categorized (either automatically or manually)
     */
    suspend fun recordCategorization(
        transaction: Transaction,
        categoryId: Long,
        wasUserCorrection: Boolean = false
    ) {
        // Record in payee-based rules (most-recent-wins)
        payeeCategoryRuleService.recordCategorization(
            transaction = transaction,
            categoryId = categoryId
        )

        // Also record in historical learning (frequency-based)
        historicalLearningService.recordTransaction(
            transaction = transaction,
            categoryId = categoryId,
            wasCorrection = wasUserCorrection
        )
    }
}
