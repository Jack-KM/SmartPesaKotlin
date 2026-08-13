package com.example.smartpesa.data.categorization

import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class KeywordMatcherTest {

    private lateinit var merchantNormalizer: MerchantNormalizer
    private lateinit var keywordMatcher: KeywordMatcher

    @Before
    fun setup() {
        merchantNormalizer = MerchantNormalizer()
        keywordMatcher = KeywordMatcher(merchantNormalizer)
    }

    @Test
    fun `matchKeywords returns null when no keywords match`() {
        val transaction = createTestTransaction(description = "Unknown merchant")
        val categories = listOf("Transport", "Groceries", "Entertainment")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNull(result)
    }

    @Test
    fun `matchKeywords matches transport keywords`() {
        val transaction = createTestTransaction(description = "Paid to UBER")
        val categories = listOf("Transport", "Groceries", "Entertainment")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Transport", result?.first)
        assertEquals(ConfidenceLevel.MEDIUM, result?.second)
    }

    @Test
    fun `matchKeywords matches groceries keywords`() {
        val transaction = createTestTransaction(description = "Sent to NAIVAS SUPERMARKET")
        val categories = listOf("Transport", "Groceries", "Entertainment")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Groceries", result?.first)
        assertEquals(ConfidenceLevel.MEDIUM, result?.second)
    }

    @Test
    fun `matchKeywords matches entertainment keywords`() {
        val transaction = createTestTransaction(description = "Netflix subscription")
        val categories = listOf("Transport", "Groceries", "Entertainment")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Entertainment", result?.first)
        assertEquals(ConfidenceLevel.MEDIUM, result?.second)
    }

    @Test
    fun `matchKeywords matches airtime keywords`() {
        val transaction = createTestTransaction(description = "Airtime purchase")
        val categories = listOf("Airtime", "Transport", "Groceries")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Airtime", result?.first)
    }

    @Test
    fun `matchKeywords handles case-insensitive matching`() {
        val transaction = createTestTransaction(description = "uber ride")
        val categories = listOf("TRANSPORT", "Groceries")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("TRANSPORT", result?.first)
    }

    @Test
    fun `matchKeywords searches both description and counterparty`() {
        val transaction = createTestTransaction(
            description = "Sent to merchant",
            counterparty = "UBER"
        )
        val categories = listOf("Transport", "Groceries")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Transport", result?.first)
    }

    @Test
    fun `matchKeywords returns null when category not in available list`() {
        val transaction = createTestTransaction(description = "UBER ride")
        val categories = listOf("Groceries", "Entertainment")  // No Transport

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNull(result)
    }

    @Test
    fun `matchKeywords matches utilities keywords`() {
        val transaction = createTestTransaction(description = "KPLC token purchase")
        val categories = listOf("Utilities", "Transport")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Utilities", result?.first)
    }

    @Test
    fun `matchKeywords matches restaurant keywords`() {
        val transaction = createTestTransaction(description = "Java House coffee")
        val categories = listOf("Restaurant", "Groceries")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Restaurant", result?.first)
    }

    @Test
    fun `matchKeywords matches health keywords`() {
        val transaction = createTestTransaction(description = "Pharmacy purchase")
        val categories = listOf("Health", "Groceries")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Health", result?.first)
    }

    @Test
    fun `matchKeywords matches education keywords`() {
        val transaction = createTestTransaction(description = "School fees payment")
        val categories = listOf("Education", "Other")

        val result = keywordMatcher.matchKeywords(transaction, categories)

        assertNotNull(result)
        assertEquals("Education", result?.first)
    }

    private fun createTestTransaction(
        description: String = "Test transaction",
        counterparty: String = ""
    ): Transaction {
        return Transaction(
            id = 1,
            amount = 100.0,
            description = description,
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = counterparty
        )
    }
}
