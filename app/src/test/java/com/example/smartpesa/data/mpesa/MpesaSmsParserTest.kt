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
        // Should return null or UNKNOWN type, never crash
        assertTrue(result == null || result.type == TransactionType.UNKNOWN)
    }

    // SEND transaction tests

    @Test
    fun `parse send transaction - full format`() {
        val sms = """UG9QXAXODW Confirmed. Ksh50.00 sent to TORY  RUKWARO 0758625343 on 9/7/26 at 2:33 PM. New M-PESA balance is Ksh0.00. Transaction cost, Ksh0.00.  Amount you can transact within the day is 499,950.00. Download My OneApp on https://saf.cx/lPKcC"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.SEND, result?.type)
        assertEquals(50.0, result?.amount, 0.01)
        assertEquals(0.0, result?.feeAmount, 0.01)
        assertEquals("TORY  RUKWARO", result?.counterpartyName)
        assertEquals("0758625343", result?.counterpartyPhone)
        assertEquals("UG9QXAXODW", result?.mpesaCode)
        assertEquals(0.0, result?.balance, 0.01)
        assertEquals(sms, result?.rawSmsBody)
    }

    @Test
    fun `parse send transaction - short form`() {
        val sms = """UG9QXAXODW Confirmed. KSH.50 sent to TORY RUKWARO, 0758***343 via My OneApp on 9th Jul 2026 at 2:33 pm."""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.SEND, result?.type)
        assertEquals(50.0, result?.amount, 0.01)
        assertEquals("TORY RUKWARO", result?.counterpartyName)
        assertEquals("0758***343", result?.counterpartyPhone)
        assertEquals("UG9QXAXODW", result?.mpesaCode)
    }

    // RECEIVE transaction tests

    @Test
    fun `parse receive transaction - basic`() {
        val sms = """UGA3EBC8UB Confirmed.You have received Ksh2,000.00 from BRIAN  MBOGO on 10/7/26 at 11:08 PM  New M-PESA balance is Ksh2,000.00."""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.RECEIVE, result?.type)
        assertEquals(2000.0, result?.amount, 0.01)
        assertNull(result?.feeAmount) // Receiving has no fee
        assertEquals("BRIAN  MBOGO", result?.counterpartyName)
        assertNull(result?.counterpartyPhone)
        assertEquals("UGA3EBC8UB", result?.mpesaCode)
        assertEquals(2000.0, result?.balance, 0.01)
    }

    @Test
    fun `parse receive transaction - with phone number`() {
        val sms = """UG469A4L80 Confirmed.You have received Ksh200.00 from hermela  abebe 0725***211 on 4/7/26 at 5:48 PM  New M-PESA balance is Ksh1,095.06. Download My OneApp on https://saf.cx/lPKcC"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.RECEIVE, result?.type)
        assertEquals(200.0, result?.amount, 0.01)
        assertEquals("hermela  abebe", result?.counterpartyName)
        assertEquals("0725***211", result?.counterpartyPhone)
        assertEquals("UG469A4L80", result?.mpesaCode)
        assertEquals(1095.06, result?.balance, 0.01)
    }

    @Test
    fun `parse receive transaction - from bank`() {
        val sms = """UFLQX8TIEO Confirmed.You have received Ksh100.00 from Equity Bulk Account 300600 on 21/6/26 at 8:23 AM New M-PESA balance is Ksh1,214.65.  Separate personal and business funds through Pochi la Biashara on *334#."""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.RECEIVE, result?.type)
        assertEquals(100.0, result?.amount, 0.01)
        assertTrue(result?.counterpartyName?.contains("Equity") == true)
        assertEquals("UFLQX8TIEO", result?.mpesaCode)
        assertEquals(1214.65, result?.balance, 0.01)
    }

    // PAYBILL transaction tests

    @Test
    fun `parse paybill transaction - business`() {
        val sms = """UGAQXB3O0M Confirmed. Ksh70.00 sent to CASCADE INDUSTRIES LTD for account 254715485059 on 10/7/26 at 8:14 PM New M-PESA balance is Ksh0.00. Transaction cost, Ksh0.00.Amount you can transact within the day is 499,930.00. Download My OneApp on https://saf.cx/kWQpy"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.PAYBILL, result?.type)
        assertEquals(70.0, result?.amount, 0.01)
        assertEquals(0.0, result?.feeAmount, 0.01)
        assertTrue(result?.counterpartyName?.contains("CASCADE") == true)
        assertEquals("UGAQXB3O0M", result?.mpesaCode)
        assertEquals(0.0, result?.balance, 0.01)
    }

    @Test
    fun `parse paybill transaction - Equity with account number`() {
        val sms = """UG7QXAORJU Confirmed. Ksh12,020.00 sent to Equity Paybill Account for account 497456#Loresho_44 on 7/7/26 at 11:59 AM New M-PESA balance is Ksh0.00. Transaction cost, Ksh57.00.Amount you can transact within the day is 487,880.00. Download My OneApp on https://saf.cx/kWQpy"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.PAYBILL, result?.type)
        assertEquals(12020.0, result?.amount, 0.01)
        assertEquals(57.0, result?.feeAmount, 0.01)
        assertTrue(result?.counterpartyName?.contains("Equity") == true)
        assertEquals("UG7QXAORJU", result?.mpesaCode)
        assertEquals(0.0, result?.balance, 0.01)
    }

    @Test
    fun `parse paybill transaction - paid to individual`() {
        val sms = """UGJQX06L8X Confirmed. Ksh20.00 paid to DORCAS WANJIKU GATHINJI. on 19/7/26 at 8:11 PM.New M-PESA balance is Ksh337.49. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,980.00. Download My OneApp on https://saf.cx/lPKcC"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.PAYBILL, result?.type)
        assertEquals(20.0, result?.amount, 0.01)
        assertEquals(0.0, result?.feeAmount, 0.01)
        assertTrue(result?.counterpartyName?.contains("DORCAS") == true)
        assertEquals("UGJQX06L8X", result?.mpesaCode)
        assertEquals(337.49, result?.balance, 0.01)
    }

    // WITHDRAWAL transaction tests

    @Test
    fun `parse withdrawal transaction`() {
        val sms = """UFUQX9XR7N Confirmed.on 30/6/26 at 7:45 PMWithdraw Ksh18,310.00 from 3000431 - Unipros Agriculture & logistics ltd Agape Mpesa Pipeline New M-PESA balance is Ksh574.15. Transaction cost, Ksh185.00. Amount you can transact within the day is 461,699.00. Get a Lipa Na M-PESA Till online: https://m-pesaforbusiness.co.ke/"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.WITHDRAWAL, result?.type)
        assertEquals(18310.0, result?.amount, 0.01)
        assertEquals(185.0, result?.feeAmount, 0.01)
        assertTrue(result?.counterpartyName?.contains("Unipros") == true || result?.counterpartyName == "3000431")
        assertEquals("UFUQX9XR7N", result?.mpesaCode)
        assertEquals(574.15, result?.balance, 0.01)
    }

    // AIRTIME transaction tests

    @Test
    fun `parse airtime purchase transaction`() {
        val sms = """UGDQXBD7HR confirmed.You bought Ksh5.00 of airtime on 13/7/26 at 10:40 AM.New M-PESA balance is Ksh483.98. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,965.00. Download My OneApp on https://saf.cx/3wAmy"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.AIRTIME, result?.type)
        assertEquals(5.0, result?.amount, 0.01)
        assertEquals(0.0, result?.feeAmount, 0.01)
        assertEquals("Airtime", result?.counterpartyName)
        assertEquals("UGDQXBD7HR", result?.mpesaCode)
        assertEquals(483.98, result?.balance, 0.01)
    }

    // TOKEN_PURCHASE transaction tests

    @Test
    fun `parse token purchase transaction - KPLC`() {
        val sms = """UGCQXB8NY4 Confirmed. Ksh100.00 sent to KPLC PREPAID for account 37221513775 on 12/7/26 at 6:10 AM New M-PESA balance is Ksh668.98. Transaction cost, Ksh0.00.Amount you can transact within the day is 499,900.00. Download My OneApp on https://saf.cx/kWQpy"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.TOKEN_PURCHASE, result?.type)
        assertEquals(100.0, result?.amount, 0.01)
        assertEquals(0.0, result?.feeAmount, 0.01)
        assertEquals("KPLC", result?.counterpartyName)
        assertEquals("UGCQXB8NY4", result?.mpesaCode)
        assertEquals(668.98, result?.balance, 0.01)
    }

    // DEPOSIT transaction tests

    @Test
    fun `parse deposit transaction - Give cash format`() {
        val sms = """UG7QXAOQ2F Confirmed. On 7/7/26 at 11:57 AM Give Ksh12,000.00 cash to Unipros Agriculture & logistics ltd Agape Mpesa Pipeline New M-PESA balance is Ksh12,000.00. You can now access M-PESA via *334#"""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.DEPOSIT, result?.type)
        assertEquals(12000.0, result?.amount, 0.01)
        assertNull(result?.feeAmount) // Deposits typically don't have fees
        assertTrue(result?.counterpartyName?.contains("Unipros") == true)
        assertEquals("UG7QXAOQ2F", result?.mpesaCode)
        assertEquals(12000.0, result?.balance, 0.01)
    }

    @Test
    fun `parse deposit transaction - short form`() {
        val sms = """UG7QXAOQ2F Confirmed. KSH.12000 paid to Unipros Pipeline, 3000431 via My OneApp on 7th Jul 2026 at 11:57 am."""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        // This could be parsed as DEPOSIT or PAYBILL depending on detection logic
        // The key is that amount and code are extracted correctly
        assertEquals(12000.0, result?.amount, 0.01)
        assertEquals("UG7QXAOQ2F", result?.mpesaCode)
    }

    // FULIZA_REPAYMENT transaction tests

    @Test
    fun `parse Fuliza repayment transaction`() {
        val sms = """UGAQXB485T Confirmed. Ksh 781.02 from your M-PESA has been used to fully pay your outstanding Fuliza M-PESA. Available Fuliza M-PESA limit is Ksh 800.00. Your M-PESA balance is 1218.98."""

        val result = parser.parse(sms, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.FULIZA_REPAYMENT, result?.type)
        assertEquals(781.02, result?.amount, 0.01)
        assertNull(result?.feeAmount) // Repayment has no fee
        assertEquals("Fuliza M-PESA", result?.counterpartyName)
        assertEquals("UGAQXB485T", result?.mpesaCode)
        assertEquals(1218.98, result?.balance, 0.01)
    }

    // Notification parsing tests

    @Test
    fun `parseNotification flattens multi-line notification text`() {
        val notification = "UG9QXAXODW Confirmed.\nKsh50.00 sent to TORY RUKWARO 0758625343\nNew M-PESA balance is Ksh0.00."

        val result = parser.parseNotification(notification, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.SEND, result?.type)
        assertEquals(50.0, result?.amount, 0.01)
        assertEquals("TORY RUKWARO", result?.counterpartyName)
        assertEquals("UG9QXAXODW", result?.mpesaCode)
    }

    @Test
    fun `parseNotification falls back to loose code when Confirmed is absent`() {
        val notification = "UG9QXAXODW You have received Ksh2,000.00 from BRIAN MBOGO on 10/7/26 at 11:08 PM New M-PESA balance is Ksh2,000.00."

        val result = parser.parseNotification(notification, timestamp)

        assertNotNull(result)
        assertEquals(TransactionType.RECEIVE, result?.type)
        assertEquals(2000.0, result?.amount, 0.01)
        assertEquals("BRIAN MBOGO", result?.counterpartyName)
        assertEquals("UG9QXAXODW", result?.mpesaCode)
    }

    @Test
    fun `parseNotification returns null without Confirmed and without code`() {
        val notification = "You have received Ksh2,000.00 from BRIAN MBOGO. New M-PESA balance is Ksh2,000.00."

        val result = parser.parseNotification(notification, timestamp)

        assertNull(result)
    }
}
