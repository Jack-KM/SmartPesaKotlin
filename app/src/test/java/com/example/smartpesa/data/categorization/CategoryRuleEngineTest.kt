package com.example.smartpesa.data.categorization

import com.example.smartpesa.data.local.entity.CategoryRule
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.CategoryRuleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class CategoryRuleEngineTest {

    private lateinit var categoryRuleRepository: CategoryRuleRepository
    private lateinit var merchantNormalizer: MerchantNormalizer
    private lateinit var ruleEngine: CategoryRuleEngine

    @Before
    fun setup() {
        categoryRuleRepository = mockk()
        merchantNormalizer = MerchantNormalizer()
        ruleEngine = CategoryRuleEngine(categoryRuleRepository, merchantNormalizer)
    }

    @Test
    fun `findMatchingRule returns null when no rules exist`() = runTest {
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns emptyList()

        val transaction = createTestTransaction(description = "Sent to NAIVAS")
        val result = ruleEngine.findMatchingRule(transaction)

        assertNull(result)
    }

    @Test
    fun `findMatchingRule matches merchant contains condition`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            merchantContains = "NAIVAS"
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(description = "Sent to NAIVAS SUPERMARKET")
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    @Test
    fun `findMatchingRule matches description contains condition`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            descriptionContains = "GROCERY"
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(description = "Grocery shopping at store")
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    @Test
    fun `findMatchingRule matches counterparty equals condition`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            counterpartyEquals = "JOHN DOE"
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(
            description = "Sent to someone",
            counterparty = "John Doe"
        )
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    @Test
    fun `findMatchingRule matches transaction type condition`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            transactionTypeEquals = TransactionType.INCOME
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(type = TransactionType.INCOME)
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    @Test
    fun `findMatchingRule matches amount greater than condition`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            amountGreaterThan = 1000.0
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(amount = 1500.0)
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    @Test
    fun `findMatchingRule does not match when amount is not greater than threshold`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            amountGreaterThan = 1000.0
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(amount = 900.0)
        val result = ruleEngine.findMatchingRule(transaction)

        assertNull(result)
    }

    @Test
    fun `findMatchingRule matches amount less than condition`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            amountLessThan = 1000.0
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(amount = 500.0)
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    @Test
    fun `findMatchingRule matches multiple conditions (AND logic)`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            merchantContains = "NAIVAS",
            amountGreaterThan = 500.0
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(
            description = "Sent to NAIVAS",
            amount = 1000.0
        )
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    @Test
    fun `findMatchingRule does not match when one condition fails`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            merchantContains = "NAIVAS",
            amountGreaterThan = 500.0
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(
            description = "Sent to NAIVAS",
            amount = 300.0  // Fails the amount condition
        )
        val result = ruleEngine.findMatchingRule(transaction)

        assertNull(result)
    }

    @Test
    fun `findMatchingRule returns first matching rule based on priority`() = runTest {
        val rule1 = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            merchantContains = "NAIVAS"
        )
        val rule2 = CategoryRule(
            id = 2,
            categoryId = 200,
            priority = 2,
            enabled = true,
            merchantContains = "NAIVAS"
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule1, rule2)

        val transaction = createTestTransaction(description = "Sent to NAIVAS")
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)  // Should match rule1 (higher priority)
    }

    @Test
    fun `findMatchingRule skips disabled rules`() = runTest {
        val rule1 = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = false,
            merchantContains = "NAIVAS"
        )
        val rule2 = CategoryRule(
            id = 2,
            categoryId = 200,
            priority = 2,
            enabled = true,
            merchantContains = "NAIVAS"
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule2)

        val transaction = createTestTransaction(description = "Sent to NAIVAS")
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(200L, result?.categoryId)  // Should match rule2 (rule1 is disabled)
    }

    @Test
    fun `findMatchingRule handles case-insensitive matching`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            merchantContains = "naivas"
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(description = "Sent to NAIVAS SUPERMARKET")
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    @Test
    fun `findMatchingRule matches M-Pesa type contains condition`() = runTest {
        val rule = CategoryRule(
            id = 1,
            categoryId = 100,
            priority = 1,
            enabled = true,
            mpesaTypeContains = "AIRTIME"
        )
        coEvery { categoryRuleRepository.getAllEnabledRulesSync() } returns listOf(rule)

        val transaction = createTestTransaction(
            description = "Airtime purchase",
            mpesaMessage = "You bought Ksh5.00 of airtime"
        )
        val result = ruleEngine.findMatchingRule(transaction)

        assertNotNull(result)
        assertEquals(100L, result?.categoryId)
    }

    private fun createTestTransaction(
        description: String = "Test transaction",
        amount: Double = 100.0,
        type: TransactionType = TransactionType.EXPENSE,
        counterparty: String = "",
        mpesaMessage: String? = null,
        accountName: String? = null
    ): Transaction {
        return Transaction(
            id = 1,
            amount = amount,
            description = description,
            type = type,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = counterparty,
            mpesaMessage = mpesaMessage,
            accountName = accountName
        )
    }
}
