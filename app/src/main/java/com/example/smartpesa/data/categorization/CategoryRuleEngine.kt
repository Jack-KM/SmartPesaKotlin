package com.example.smartpesa.data.categorization

import com.example.smartpesa.data.local.entity.CategoryRule
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.CategoryRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates user-defined category rules against transactions
 */
@Singleton
class CategoryRuleEngine @Inject constructor(
    private val categoryRuleRepository: CategoryRuleRepository,
    private val merchantNormalizer: MerchantNormalizer
) {

    /**
     * Find the first matching rule for a transaction
     * Rules are evaluated in priority order (lower priority value = higher priority)
     * Returns null if no rules match
     */
    suspend fun findMatchingRule(transaction: Transaction): CategoryRule? {
        val enabledRules = categoryRuleRepository.getAllEnabledRulesSync()

        for (rule in enabledRules) {
            if (matches(rule, transaction)) {
                return rule
            }
        }

        return null
    }

    /**
     * Check if a rule matches a transaction
     * All non-null conditions must match for the rule to match
     */
    private fun matches(rule: CategoryRule, transaction: Transaction): Boolean {
        // Check merchant contains
        if (rule.merchantContains != null) {
            val merchantName = merchantNormalizer.extractMerchantName(transaction.description)
                ?: transaction.counterparty.takeIf { it.isNotEmpty() }
                ?: transaction.description

            val normalizedMerchant = merchantNormalizer.normalize(merchantName)
            val normalizedPattern = merchantNormalizer.normalize(rule.merchantContains)

            if (!normalizedMerchant.contains(normalizedPattern)) {
                return false
            }
        }

        // Check description contains
        if (rule.descriptionContains != null) {
            val normalizedDescription = merchantNormalizer.normalize(transaction.description)
            val normalizedPattern = merchantNormalizer.normalize(rule.descriptionContains)

            if (!normalizedDescription.contains(normalizedPattern)) {
                return false
            }
        }

        // Check counterparty equals
        if (rule.counterpartyEquals != null) {
            val normalizedCounterparty = merchantNormalizer.normalize(transaction.counterparty)
            val normalizedPattern = merchantNormalizer.normalize(rule.counterpartyEquals)

            if (normalizedCounterparty != normalizedPattern) {
                return false
            }
        }

        // Check transaction type equals
        if (rule.transactionTypeEquals != null) {
            if (transaction.type != rule.transactionTypeEquals) {
                return false
            }
        }

        // Check amount greater than
        if (rule.amountGreaterThan != null) {
            if (transaction.amount <= rule.amountGreaterThan) {
                return false
            }
        }

        // Check amount less than
        if (rule.amountLessThan != null) {
            if (transaction.amount >= rule.amountLessThan) {
                return false
            }
        }

        // Check account name equals
        if (rule.accountNameEquals != null) {
            if (transaction.accountName != rule.accountNameEquals) {
                return false
            }
        }

        // Check M-Pesa type contains
        if (rule.mpesaTypeContains != null) {
            val mpesaMessage = transaction.mpesaMessage ?: ""
            if (!mpesaMessage.contains(rule.mpesaTypeContains, ignoreCase = true)) {
                return false
            }
        }

        // All conditions matched
        return true
    }
}
