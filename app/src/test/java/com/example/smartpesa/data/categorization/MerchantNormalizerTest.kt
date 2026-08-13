package com.example.smartpesa.data.categorization

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MerchantNormalizerTest {

    private lateinit var normalizer: MerchantNormalizer

    @Before
    fun setup() {
        normalizer = MerchantNormalizer()
    }

    @Test
    fun `normalize removes M-Pesa transaction codes`() {
        val input = "UG9QXAXODW Confirmed. Ksh50.00 sent to NAIVAS"
        val result = normalizer.normalize(input)
        assertFalse(result.contains("UG9QXAXODW"))
    }

    @Test
    fun `normalize removes phone numbers`() {
        val input = "Sent to JOHN DOE 0758625343"
        val result = normalizer.normalize(input)
        assertFalse(result.contains("0758625343"))
        assertTrue(result.contains("JOHN DOE"))
    }

    @Test
    fun `normalize removes masked phone numbers`() {
        val input = "Received from JANE DOE 0725***211"
        val result = normalizer.normalize(input)
        assertFalse(result.contains("0725***211"))
        assertTrue(result.contains("JANE DOE"))
    }

    @Test
    fun `normalize removes M-Pesa paybill patterns`() {
        val input = "MPESA PAYBILL 123456 NAIVAS SUPERMARKET"
        val result = normalizer.normalize(input)
        assertFalse(result.contains("MPESA PAYBILL"))
        assertTrue(result.contains("NAIVAS"))
    }

    @Test
    fun `normalize converts to uppercase`() {
        val input = "naivas supermarket"
        val result = normalizer.normalize(input)
        assertEquals("NAIVAS SUPERMARKET", result)
    }

    @Test
    fun `normalize removes punctuation`() {
        val input = "NAIVAS-LTD."
        val result = normalizer.normalize(input)
        assertFalse(result.contains("-"))
        assertFalse(result.contains("."))
    }

    @Test
    fun `normalize removes business suffixes`() {
        val input = "CASCADE INDUSTRIES LTD"
        val result = normalizer.normalize(input)
        assertEquals("CASCADE INDUSTRIES", result)
    }

    @Test
    fun `normalize handles extra whitespace`() {
        val input = "NAIVAS   SUPERMARKET  "
        val result = normalizer.normalize(input)
        assertEquals("NAIVAS SUPERMARKET", result)
    }

    @Test
    fun `normalize handles multiple variations of same merchant`() {
        val variations = listOf(
            "NAIVAS SUPERMARKET LTD",
            "Naivas Supermarket",
            "NAIVAS LTD",
            "naivas supermarket"
        )

        val normalized = variations.map { normalizer.normalize(it) }

        // All should normalize to same or similar result
        assertTrue(normalized.all { it.contains("NAIVAS") })
        assertTrue(normalized.all { !it.contains("LTD") })
    }

    @Test
    fun `extractMerchantName extracts from sent to pattern`() {
        val description = "Sent to NAIVAS SUPERMARKET"
        val result = normalizer.extractMerchantName(description)
        assertEquals("NAIVAS SUPERMARKET", result)
    }

    @Test
    fun `extractMerchantName extracts from paid to pattern`() {
        val description = "Paid to CARREFOUR"
        val result = normalizer.extractMerchantName(description)
        assertEquals("CARREFOUR", result)
    }

    @Test
    fun `extractMerchantName extracts from received from pattern`() {
        val description = "Received from JOHN DOE"
        val result = normalizer.extractMerchantName(description)
        assertEquals("JOHN DOE", result)
    }

    @Test
    fun `extractMerchantName returns null for unmatched patterns`() {
        val description = "Random transaction description"
        val result = normalizer.extractMerchantName(description)
        assertNull(result)
    }

    @Test
    fun `areSimilar returns true for identical strings`() {
        val result = normalizer.areSimilar("NAIVAS", "NAIVAS")
        assertTrue(result)
    }

    @Test
    fun `areSimilar returns true when one contains the other`() {
        val result = normalizer.areSimilar("NAIVAS SUPERMARKET", "NAIVAS")
        assertTrue(result)
    }

    @Test
    fun `areSimilar returns true for common prefix`() {
        val result = normalizer.areSimilar("NAIVAS SUPERMARKET", "NAIVAS STORE")
        assertTrue(result)
    }

    @Test
    fun `areSimilar returns false for completely different strings`() {
        val result = normalizer.areSimilar("NAIVAS", "UBER")
        assertFalse(result)
    }

    @Test
    fun `areSimilar returns false for empty strings`() {
        val result = normalizer.areSimilar("", "NAIVAS")
        assertFalse(result)
    }

    @Test
    fun `areSimilar returns false for short non-matching strings`() {
        val result = normalizer.areSimilar("ABC", "XYZ")
        assertFalse(result)
    }
}
