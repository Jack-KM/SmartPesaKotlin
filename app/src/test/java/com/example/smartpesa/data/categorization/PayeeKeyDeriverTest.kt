package com.example.smartpesa.data.categorization

import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for PayeeKeyDeriver
 * Tests payee key derivation for different transaction types
 */
class PayeeKeyDeriverTest {

    private lateinit var merchantNormalizer: MerchantNormalizer
    private lateinit var payeeKeyDeriver: PayeeKeyDeriver

    @Before
    fun setup() {
        merchantNormalizer = MerchantNormalizer()
        payeeKeyDeriver = PayeeKeyDeriver(merchantNormalizer)
    }

    @Test
    fun `derivePayeeKey for P2P send transaction returns normalized name`() {
        val transaction = Transaction(
            id = 1,
            amount = 50.0,
            description = "Sent to TORY RUKWARO",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "TORY RUKWARO",
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        assertEquals("TORY RUKWARO", result)
    }

    @Test
    fun `derivePayeeKey for P2P receive transaction returns normalized name`() {
        val transaction = Transaction(
            id = 1,
            amount = 2000.0,
            description = "Received from BRIAN MBOGO",
            type = TransactionType.INCOME,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "BRIAN MBOGO",
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        assertEquals("BRIAN MBOGO", result)
    }

    @Test
    fun `derivePayeeKey for paybill transaction returns normalized business name`() {
        val transaction = Transaction(
            id = 1,
            amount = 70.0,
            description = "Paid to CASCADE INDUSTRIES LTD",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "CASCADE INDUSTRIES LTD",
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        // MerchantNormalizer removes "LTD" suffix
        assertEquals("CASCADE INDUSTRIES", result)
    }

    @Test
    fun `derivePayeeKey for deposit transaction returns normalized agent name`() {
        val transaction = Transaction(
            id = 1,
            amount = 12000.0,
            description = "Deposit to UNIPROS PIPELINE",
            type = TransactionType.INCOME,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "UNIPROS PIPELINE",
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        assertEquals("UNIPROS PIPELINE", result)
    }

    @Test
    fun `derivePayeeKey for withdrawal transaction returns null`() {
        val transaction = Transaction(
            id = 1,
            amount = 18310.0,
            description = "Withdrawal from Unipros Agriculture",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "Unipros Agriculture",
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNull(result)
    }

    @Test
    fun `derivePayeeKey for airtime transaction returns normalized name`() {
        val transaction = Transaction(
            id = 1,
            amount = 5.0,
            description = "Airtime purchase",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "Airtime",
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        assertEquals("AIRTIME", result)
    }

    @Test
    fun `derivePayeeKey for KPLC token purchase returns normalized name`() {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Token purchase (KPLC)",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "KPLC",
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        assertEquals("KPLC", result)
    }

    @Test
    fun `derivePayeeKey extracts from description when counterparty is empty`() {
        val transaction = Transaction(
            id = 1,
            amount = 50.0,
            description = "Sent to JOHN DOE",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "", // Empty counterparty
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        assertEquals("JOHN DOE", result)
    }

    @Test
    fun `derivePayeeKey returns null when no counterparty can be extracted`() {
        val transaction = Transaction(
            id = 1,
            amount = 50.0,
            description = "Unknown transaction",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "",
            source = "Manual"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNull(result)
    }

    @Test
    fun `derivePayeeKey normalizes counterparty name removing phone numbers`() {
        val transaction = Transaction(
            id = 1,
            amount = 50.0,
            description = "Sent to JANE SMITH",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "JANE SMITH 0712345678", // Phone number included
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        // MerchantNormalizer removes phone numbers
        assertEquals("JANE SMITH", result)
    }

    @Test
    fun `derivePayeeKey normalizes business name removing punctuation`() {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Paid to ABC CO., LTD.",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.now(),
            categoryId = null,
            counterparty = "ABC CO., LTD.",
            source = "M-Pesa SMS"
        )

        val result = payeeKeyDeriver.derivePayeeKey(transaction)

        assertNotNull(result)
        // MerchantNormalizer removes punctuation and business suffixes
        assertEquals("ABC CO", result)
    }
}
