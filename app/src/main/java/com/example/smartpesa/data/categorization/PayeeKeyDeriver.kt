package com.example.smartpesa.data.categorization

import com.example.smartpesa.data.local.entity.Transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Derives normalized payee keys from transactions for auto-categorization
 *
 * Strategy: Use normalized counterparty name as key for most transactions.
 * Withdrawals return null (should use fixed category by type, not learned).
 */
@Singleton
class PayeeKeyDeriver @Inject constructor(
    private val merchantNormalizer: MerchantNormalizer
) {

    /**
     * Derive a payee key from a transaction
     * Returns null for withdrawals (these should use a fixed category, not learned mapping)
     */
    fun derivePayeeKey(transaction: Transaction): String? {
        // Withdrawals should not use learned categorization
        if (isWithdrawal(transaction)) {
            return null
        }

        // Extract counterparty name
        val counterpartyName = extractCounterpartyName(transaction) ?: return null

        // Normalize and return
        val normalized = merchantNormalizer.normalize(counterpartyName)
        return normalized.takeIf { it.isNotEmpty() }
    }

    /**
     * Check if transaction is a withdrawal
     */
    private fun isWithdrawal(transaction: Transaction): Boolean {
        return transaction.description.startsWith("Withdrawal from", ignoreCase = true) ||
               transaction.description.contains("Withdraw", ignoreCase = true)
    }

    /**
     * Extract counterparty name from transaction
     * Tries counterparty field first, falls back to parsing description
     */
    private fun extractCounterpartyName(transaction: Transaction): String? {
        // Use counterparty field if populated with a real payee, not just the funding account.
        if (transaction.counterparty.isNotEmpty() && !isAccountName(transaction.counterparty)) {
            return transaction.counterparty
        }

        return merchantNormalizer.extractMerchantName(transaction.description)
            ?: transaction.description.lineSequence().firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun isAccountName(value: String): Boolean {
        return when (value.trim().uppercase()) {
            "M-PESA", "MPESA", "CASH", "AIRTEL MONEY" -> true
            else -> false
        }
    }
}
