package com.example.smartpesa.data.categorization

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Normalizes merchant names and transaction descriptions for consistent matching
 *
 * Handles:
 * - Case normalization
 * - Whitespace normalization
 * - M-Pesa reference codes
 * - Phone numbers
 * - Transaction IDs
 * - Common punctuation
 * - Business name suffixes (LTD, INC, etc.)
 */
@Singleton
class MerchantNormalizer @Inject constructor() {

    companion object {
        // Patterns to remove during normalization
        private val MPESA_CODE_PATTERN = Regex("""\b(?=[A-Z0-9]*\d)[A-Z0-9]{10}\b""")
        private val PHONE_PATTERN = Regex("""\b\d{4}\*?\*?\*?\d{3}\b""")
        private val PHONE_FULL_PATTERN = Regex("""\b\d{10,12}\b""")
        private val PAYBILL_PATTERN = Regex("""(?i)MPESA PAYBILL \d+""")
        private val TILL_PATTERN = Regex("""(?i)MPESA TILL \d+""")
        private val BUSINESS_SUFFIXES = listOf(
            "LIMITED", "LTD", "INC", "INCORPORATED", "LLC", "PLC",
            "COMPANY", "CORP", "CORPORATION"
        )
        private val PUNCTUATION = Regex("""[.,;:\-_/\\]+""")
    }

    /**
     * Normalize a merchant name or transaction description
     * Returns a normalized string suitable for matching
     */
    fun normalize(text: String): String {
        var normalized = text.trim()

        // Remove M-Pesa codes
        normalized = MPESA_CODE_PATTERN.replace(normalized, "")

        // Remove phone numbers (both masked and full)
        normalized = PHONE_PATTERN.replace(normalized, "")
        normalized = PHONE_FULL_PATTERN.replace(normalized, "")

        // Remove M-Pesa paybill/till patterns
        normalized = PAYBILL_PATTERN.replace(normalized, "")
        normalized = TILL_PATTERN.replace(normalized, "")

        // Convert to uppercase
        normalized = normalized.uppercase()

        // Remove punctuation
        normalized = PUNCTUATION.replace(normalized, " ")

        // Remove business suffixes
        for (suffix in BUSINESS_SUFFIXES) {
            normalized = normalized.replace(Regex("""\b$suffix\b"""), "")
        }

        // Normalize whitespace
        normalized = normalized.replace(Regex("""\s+"""), " ").trim()

        return normalized
    }

    /**
     * Extract merchant name from a transaction description
     * Handles common patterns like "Sent to X", "Paid to X", etc.
     */
    fun extractMerchantName(description: String): String? {
        val patterns = listOf(
            Regex("""(?i)sent to (.+)"""),
            Regex("""(?i)paid to (.+)"""),
            Regex("""(?i)received from (.+)"""),
            Regex("""(?i)withdrawal from (.+)"""),
            Regex("""(?i)deposit to (.+)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(description)
            if (match != null) {
                return match.groupValues.getOrNull(1)?.trim()
            }
        }

        return null
    }

    /**
     * Check if two normalized merchant names are similar enough to be considered the same
     * Uses simple contains/prefix logic
     */
    fun areSimilar(normalized1: String, normalized2: String): Boolean {
        if (normalized1 == normalized2) return true
        if (normalized1.isEmpty() || normalized2.isEmpty()) return false

        // Check if one contains the other
        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) {
            return true
        }

        // Check if they share a common prefix (at least 5 characters)
        val minLength = minOf(normalized1.length, normalized2.length)
        if (minLength >= 5) {
            val commonPrefixLength = normalized1.commonPrefixWith(normalized2).length
            if (commonPrefixLength >= 5) {
                return true
            }
        }

        return false
    }
}
