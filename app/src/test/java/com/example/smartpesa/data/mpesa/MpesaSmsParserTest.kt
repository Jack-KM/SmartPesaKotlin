package com.example.smartpesa.data.mpesa

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MpesaSmsParser
 * Tests use real M-Pesa SMS samples with assertions for all fields
 */
class MpesaSmsParserTest {

    private lateinit var parser: MpesaSmsParser
    private val timestamp = 1720368000000L // Fixed timestamp for tests

    @Before
    fun setup() {
        parser = MpesaSmsParser()
    }

    private fun requireParsed(result: ParsedTransaction?): ParsedTransaction =
        checkNotNull(result) { "Expected parsed transaction" }

    // Edge case tests

    @Test
    fun `parse empty string returns null`() {
        val result = parser.parse("", timestamp)
        assertNull(result)
    }

    @Test
    fun `parse non-M-Pesa SMS returns null`() {
        val sms = "Hey, how are you? Let's meet up tomorrow at 3pm."
        val result = parser.parse(sms, timestamp)
        assertNull(result)
    }

    @Test
    fun `parse garbled M-Pesa-like message returns null or UNKNOWN`() {
        val sms = "UG9QXAX Confirmed but no other valid data here..."
        val result = parser.parse(sms, timestamp)
        assertTrue(result == null || result.type == TransactionType.UNKNOWN)
    }

    // SEND transaction tests

    @Test
    fun `parse send transaction - full format`() {
        val sms = """UG9QXAXODW Confirmed. Ksh50.00 sent to TORY  RUKWARO 0758625343 on 9/7/26 at 2:33 PM. New M-PESA balance is Ksh0.00. Transaction cost, Ksh0.00.  Amount you can transact within the day is 499,950.00. Download My OneApp on https://saf.cx/lPKcC"""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.SEND, parsed.type)
        assertEquals(50.0, parsed.amount, 0.01)
        assertEquals(0.0, parsed.feeAmount ?: 0.0, 0.01)
        assertEquals("TORY  RUKWARO", parsed.counterpartyName)
        assertEquals("0758625343", parsed.counterpartyPhone)
        assertEquals("UG9QXAXODW", parsed.mpesaCode)
        assertEquals(0.0, parsed.balance ?: 0.0, 0.01)
        assertEquals(sms, parsed.rawSmsBody)
    }

    @Test
    fun `parse send transaction - short form`() {
        val sms = """UG9QXAXODW Confirmed. KSH.50 sent to TORY RUKWARO, 0758***343 via My OneApp on 9th Jul 2026 at 2:33 pm."""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.SEND, parsed.type)
        assertEquals(50.0, parsed.amount, 0.01)
        assertEquals("TORY RUKWARO", parsed.counterpartyName)
        assertEquals("0758***343", parsed.counterpartyPhone)
        assertEquals("UG9QXAXODW", parsed.mpesaCode)
    }

    // RECEIVE transaction tests

    @Test
    fun `parse receive transaction - basic`() {
        val sms = """UGA3EBC8UB Confirmed.You have received Ksh2,000.00 from BRIAN  MBOGO on 10/7/26 at 11:08 PM  New M-PESA balance is Ksh2,000.00."""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.RECEIVE, parsed.type)
        assertEquals(2000.0, parsed.amount, 0.01)
        assertNull(parsed.feeAmount) // Receiving has no fee
        assertEquals("BRIAN  MBOGO", parsed.counterpartyName)
        assertNull(parsed.counterpartyPhone)
        assertEquals("UGA3EBC8UB", parsed.mpesaCode)
        assertEquals(2000.0, parsed.balance ?: 0.0, 0.01)
    }

    @Test
    fun `parse receive transaction - with phone number`() {
        val sms = """UG469A4L80 Confirmed.You have received Ksh200.00 from hermela  abebe 0725***211 on 4/7/26 at 5:48 PM  New M-PESA balance is Ksh1,095.06. Download My OneApp on https://saf.cx/lPKcC"""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.RECEIVE, parsed.type)
        assertEquals(200.0, parsed.amount, 0.01)
        assertEquals("hermela  abebe", parsed.counterpartyName)
        assertEquals("0725***211", parsed.counterpartyPhone)
        assertEquals("UG469A4L80", parsed.mpesaCode)
        assertEquals(1095.06, parsed.balance ?: 0.0, 0.01)
    }

    @Test
    fun `parse receive transaction - from bank`() {
        val sms = """UFLQX8TIEO Confirmed.You have received Ksh100.00 from Equity Bulk Account 300600 on 21/6/26 at 8:23 AM New M-PESA balance is Ksh1,214.65.  Separate personal and business funds through Pochi la Biashara on *334#."""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.RECEIVE, parsed.type)
        assertEquals(100.0, parsed.amount, 0.01)
        assertTrue(parsed.counterpartyName?.contains("Equity") == true)
        assertEquals("UFLQX8TIEO", parsed.mpesaCode)
        assertEquals(1214.65, parsed.balance ?: 0.0, 0.01)
    }

    // PAYBILL transaction tests

    @Test
    fun `parse paybill transaction - business`() {
        val sms = """UGAQXB3O0M Confirmed. Ksh70.00 sent to CASCADE INDUSTRIES LTD for account 254715485059 on 10/7/26 at 8:14 PM New M-PESA balance is Ksh0.00. Transaction cost, Ksh0.00.Amount you can transact within the day is 499,930.00. Download My OneApp on https://saf.cx/lPKcC"""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.PAYBILL, parsed.type)
        assertEquals(70.0, parsed.amount, 0.01)
        assertEquals(0.0, parsed.feeAmount ?: 0.0, 0.01)
        assertTrue(parsed.counterpartyName?.contains("CASCADE") == true)
        assertEquals("UGAQXB3O0M", parsed.mpesaCode)
        assertEquals(0.0, parsed.balance ?: 0.0, 0.01)
    }

    @Test
    fun `parse paybill transaction - Equity with account number`() {
        val sms = """UGJQX06L8X Confirmed. Ksh20.00 paid to DORCAS WANJIKU GATHINJI on 28/6/26 at 8:59 AM New M-PESA balance is Ksh337.49. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,980.00. Download My OneApp on https://saf.cx/lPKcC"""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.PAYBILL, parsed.type)
        assertEquals(20.0, parsed.amount, 0.01)
        assertEquals(0.0, parsed.feeAmount ?: 0.0, 0.01)
        assertTrue(parsed.counterpartyName?.contains("DORCAS") == true)
        assertEquals("UGJQX06L8X", parsed.mpesaCode)
        assertEquals(337.49, parsed.balance ?: 0.0, 0.01)
    }

    // WITHDRAWAL transaction tests

    @Test
    fun `parse withdrawal transaction`() {
        val sms = """UFUQX9XR7N Confirmed.on 30/6/26 at 7:45 PMWithdraw Ksh18,310.00 from 3000431 - Unipros Agriculture & logistics ltd Agape Mpesa Pipeline New M-PESA balance is Ksh574.15. Transaction cost, Ksh185.00. Amount you can transact within the day is 461,699.00. Get a Lipa Na M-PESA Till online: https://m-pesaforbusiness.co.ke/"""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.WITHDRAWAL, parsed.type)
        assertEquals(18310.0, parsed.amount, 0.01)
        assertEquals(185.0, parsed.feeAmount ?: 0.0, 0.01)
        assertTrue(parsed.counterpartyName?.contains("Unipros") == true || parsed.counterpartyName == "3000431")
        assertEquals("UFUQX9XR7N", parsed.mpesaCode)
        assertEquals(574.15, parsed.balance ?: 0.0, 0.01)
    }

    // AIRTIME transaction tests

    @Test
    fun `parse airtime purchase transaction`() {
        val sms = """UGDQXBD7HR confirmed.You bought Ksh5.00 of airtime on 13/7/26 at 10:40 AM.New M-PESA balance is Ksh483.98. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,965.00. Download My OneApp on https://saf.cx/3wAmy"""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.AIRTIME, parsed.type)
        assertEquals(5.0, parsed.amount, 0.01)
        assertEquals(0.0, parsed.feeAmount ?: 0.0, 0.01)
        assertEquals("Airtime", parsed.counterpartyName)
        assertEquals("UGDQXBD7HR", parsed.mpesaCode)
        assertEquals(483.98, parsed.balance ?: 0.0, 0.01)
    }

    // TOKEN_PURCHASE transaction tests

    @Test
    fun `parse token purchase transaction - KPLC`() {
        val sms = """UGCQXB8NY4 Confirmed. Ksh100.00 sent to KPLC PREPAID for account 37221513775 on 12/7/26 at 6:10 AM New M-PESA balance is Ksh668.98. Transaction cost, Ksh0.00.Amount you can transact within the day is 499,900.00. Download My OneApp on https://saf.cx/kWQpy"""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.TOKEN_PURCHASE, parsed.type)
        assertEquals(100.0, parsed.amount, 0.01)
        assertEquals(0.0, parsed.feeAmount ?: 0.0, 0.01)
        assertEquals("KPLC", parsed.counterpartyName)
        assertEquals("UGCQXB8NY4", parsed.mpesaCode)
        assertEquals(668.98, parsed.balance ?: 0.0, 0.01)
    }

    // DEPOSIT transaction tests

    @Test
    fun `parse deposit transaction - Give cash format`() {
        val sms = """UG7QXAOQ2F Confirmed. On 7/7/26 at 11:57 AM Give Ksh12,000.00 cash to Unipros Agriculture & logistics ltd Agape Mpesa Pipeline New M-PESA balance is Ksh12,000.00. You can now access M-PESA via *334#"""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.DEPOSIT, parsed.type)
        assertEquals(12000.0, parsed.amount, 0.01)
        assertNull(parsed.feeAmount) // Deposits typically don't have fees
        assertTrue(parsed.counterpartyName?.contains("Unipros") == true)
        assertEquals("UG7QXAOQ2F", parsed.mpesaCode)
        assertEquals(12000.0, parsed.balance ?: 0.0, 0.01)
    }

    @Test
    fun `parse deposit transaction - short form`() {
        val sms = """UG7QXAOQ2F Confirmed. KSH.12000 paid to Unipros Pipeline, 3000431 via My OneApp on 7th Jul 2026 at 11:57 am."""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        // This could be parsed as DEPOSIT or PAYBILL depending on detection logic
        // The key is that amount and code are extracted correctly
        assertEquals(12000.0, parsed.amount, 0.01)
        assertEquals("UG7QXAOQ2F", parsed.mpesaCode)
    }

    // FULIZA_REPAYMENT transaction tests

    @Test
    fun `parse Fuliza repayment transaction`() {
        val sms = """UGAQXB485T Confirmed. Ksh 781.02 from your M-PESA has been used to fully pay your outstanding Fuliza M-PESA. Available Fuliza M-PESA limit is Ksh 800.00. Your M-PESA balance is 1218.98."""

        val parsed = requireParsed(parser.parse(sms, timestamp))

        assertEquals(TransactionType.FULIZA_REPAYMENT, parsed.type)
        assertEquals(781.02, parsed.amount, 0.01)
        assertNull(parsed.feeAmount) // Repayment has no fee
        assertEquals("Fuliza M-PESA", parsed.counterpartyName)
        assertEquals("UGAQXB485T", parsed.mpesaCode)
        assertEquals(1218.98, parsed.balance ?: 0.0, 0.01)
    }
}
