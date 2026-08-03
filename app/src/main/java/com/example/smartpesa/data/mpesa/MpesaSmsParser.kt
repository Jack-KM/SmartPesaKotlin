package com.example.smartpesa.data.mpesa

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for M-Pesa SMS messages
 * Extracts transaction details from various M-Pesa SMS formats
 */
@Singleton
class MpesaSmsParser @Inject constructor() {

    companion object {
        // Common patterns used across multiple transaction types

        // M-Pesa transaction code at the start: "UG9QXAXODW Confirmed."
        private val CODE_PATTERN = Pattern.compile(
            "^([A-Z0-9]+)\\s+Confirmed",
            Pattern.CASE_INSENSITIVE
        )

        // Amount in various formats: "Ksh50.00", "KSH.50", "Ksh 50.00", "Ksh12,000.00"
        // Non-greedy to avoid capturing multiple amounts in one match
        private val AMOUNT_PATTERN = Pattern.compile(
            "(?:Ksh|KES|KSH)\\.?\\s*([0-9,]+\\.?[0-9]*)",
            Pattern.CASE_INSENSITIVE
        )

        // Balance: "New M-PESA balance is Ksh0.00" or "Your M-PESA balance is 1218.98"
        private val BALANCE_PATTERN = Pattern.compile(
            "(?:New M-PESA balance is|Your M-PESA balance is)\\s+(?:Ksh|KES)\\.?\\s*([0-9,]+\\.?[0-9]*)",
            Pattern.CASE_INSENSITIVE
        )

        // Transaction cost: "Transaction cost, Ksh0.00" or "Transaction cost, Ksh57.00"
        private val FEE_PATTERN = Pattern.compile(
            "Transaction cost,?\\s+(?:Ksh|KES)\\.?\\s*([0-9,]+\\.?[0-9]*)",
            Pattern.CASE_INSENSITIVE
        )

        // Transaction-type specific patterns

        // SEND: "Ksh50.00 sent to TORY RUKWARO 0758625343"
        // Handles both full and masked phone numbers (0758***343)
        private val SEND_PATTERN = Pattern.compile(
            "sent to\\s+([A-Z\\s]+?)(?:\\s+|,\\s*)(\\d{4}\\*?\\*?\\*?\\d{3})?",
            Pattern.CASE_INSENSITIVE
        )

        // RECEIVE: "You have received Ksh2,000.00 from BRIAN MBOGO"
        // May include phone number: "from hermela abebe 0725***211"
        private val RECEIVE_PATTERN = Pattern.compile(
            "You have received.*?from\\s+([A-Z\\s]+?)(?:\\s+(\\d{4}\\*?\\*?\\*?\\d{3}))?(?:\\s+on)",
            Pattern.CASE_INSENSITIVE
        )

        // PAYBILL: "sent to CASCADE INDUSTRIES LTD for account 254715485059"
        // OR "paid to DORCAS WANJIKU GATHINJI." (without account number)
        private val PAYBILL_PATTERN = Pattern.compile(
            "(?:sent to|paid to)\\s+([A-Z\\s&.]+?)(?:\\s+for account\\s+([A-Z0-9#_]+))?"
                    + "(?:\\s+on|\\s+\\.|New M-PESA)",
            Pattern.CASE_INSENSITIVE
        )

        // WITHDRAWAL: "Withdraw Ksh18,310.00 from 3000431 - Unipros Agriculture"
        private val WITHDRAWAL_PATTERN = Pattern.compile(
            "Withdraw.*?from\\s+(\\d+)\\s*-?\\s*([A-Z\\s&.]+)?",
            Pattern.CASE_INSENSITIVE
        )

        // AIRTIME: "You bought Ksh5.00 of airtime"
        private val AIRTIME_PATTERN = Pattern.compile(
            "You bought.*?of airtime",
            Pattern.CASE_INSENSITIVE
        )

        // TOKEN_PURCHASE: "sent to KPLC PREPAID for account 37221513775"
        private val TOKEN_PATTERN = Pattern.compile(
            "sent to KPLC",
            Pattern.CASE_INSENSITIVE
        )

        // DEPOSIT: "Give Ksh12,000.00 cash to Unipros Agriculture"
        private val DEPOSIT_PATTERN = Pattern.compile(
            "Give.*?cash to\\s+([A-Z\\s&.]+?)(?:\\s+on|New M-PESA)",
            Pattern.CASE_INSENSITIVE
        )

        // FULIZA_REPAYMENT: "Ksh 781.02 from your M-PESA has been used to fully pay your outstanding Fuliza"
        private val FULIZA_REPAYMENT_PATTERN = Pattern.compile(
            "from your M-PESA has been used to.*?pay.*?Fuliza",
            Pattern.CASE_INSENSITIVE
        )
    }

    /**
     * Parse an M-Pesa SMS message
     * @param smsBody The SMS message body
     * @param timestamp SMS timestamp in milliseconds
     * @return ParsedTransaction if successful, null if not an M-Pesa message or unparseable
     */
    fun parse(smsBody: String, timestamp: Long): ParsedTransaction? {
        // Basic validation
        if (smsBody.isBlank() || !smsBody.contains("Confirmed", ignoreCase = true)) {
            return null
        }

        // Extract transaction code (required for all M-Pesa messages)
        val mpesaCode = extractCode(smsBody) ?: return null

        // Determine transaction type and extract relevant fields
        return when {
            FULIZA_REPAYMENT_PATTERN.matcher(smsBody).find() -> {
                parseFulizaRepayment(smsBody, mpesaCode, timestamp)
            }
            WITHDRAWAL_PATTERN.matcher(smsBody).find() -> {
                parseWithdrawal(smsBody, mpesaCode, timestamp)
            }
            DEPOSIT_PATTERN.matcher(smsBody).find() -> {
                parseDeposit(smsBody, mpesaCode, timestamp)
            }
            AIRTIME_PATTERN.matcher(smsBody).find() -> {
                parseAirtime(smsBody, mpesaCode, timestamp)
            }
            TOKEN_PATTERN.matcher(smsBody).find() -> {
                parseTokenPurchase(smsBody, mpesaCode, timestamp)
            }
            RECEIVE_PATTERN.matcher(smsBody).find() -> {
                parseReceive(smsBody, mpesaCode, timestamp)
            }
            SEND_PATTERN.matcher(smsBody).find() && !smsBody.contains("for account", ignoreCase = true) -> {
                parseSend(smsBody, mpesaCode, timestamp)
            }
            PAYBILL_PATTERN.matcher(smsBody).find() -> {
                parsePaybill(smsBody, mpesaCode, timestamp)
            }
            else -> {
                // Unrecognized format - return UNKNOWN type with basic info
                ParsedTransaction(
                    type = TransactionType.UNKNOWN,
                    amount = extractAmount(smsBody) ?: 0.0,
                    mpesaCode = mpesaCode,
                    balance = extractBalance(smsBody),
                    rawSmsBody = smsBody,
                    timestamp = timestamp
                )
            }
        }
    }

    private fun parseSend(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        val matcher = SEND_PATTERN.matcher(smsBody)
        val name = if (matcher.find()) matcher.group(1)?.trim() else null
        val phone = if (matcher.group(2) != null) matcher.group(2)?.trim() else null

        return ParsedTransaction(
            type = TransactionType.SEND,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = extractFee(smsBody),
            counterpartyName = name,
            counterpartyPhone = phone,
            mpesaCode = code,
            balance = extractBalance(smsBody),
            rawSmsBody = smsBody,
            timestamp = timestamp
        )
    }

    private fun parseReceive(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        val matcher = RECEIVE_PATTERN.matcher(smsBody)
        val name = if (matcher.find()) matcher.group(1)?.trim() else null
        val phone = if (matcher.groupCount() >= 2 && matcher.group(2) != null)
            matcher.group(2)?.trim() else null

        return ParsedTransaction(
            type = TransactionType.RECEIVE,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = null, // Receiving money has no fee
            counterpartyName = name,
            counterpartyPhone = phone,
            mpesaCode = code,
            balance = extractBalance(smsBody),
            rawSmsBody = smsBody,
            timestamp = timestamp
        )
    }

    private fun parsePaybill(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        val matcher = PAYBILL_PATTERN.matcher(smsBody)
        val name = if (matcher.find()) matcher.group(1)?.trim() else null
        val account = if (matcher.groupCount() >= 2 && matcher.group(2) != null)
            matcher.group(2)?.trim() else null

        return ParsedTransaction(
            type = TransactionType.PAYBILL,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = extractFee(smsBody),
            counterpartyName = name ?: account, // Use account number if no name
            mpesaCode = code,
            balance = extractBalance(smsBody),
            rawSmsBody = smsBody,
            timestamp = timestamp
        )
    }

    private fun parseWithdrawal(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        val matcher = WITHDRAWAL_PATTERN.matcher(smsBody)
        val agentCode = if (matcher.find()) matcher.group(1)?.trim() else null
        val agentName = if (matcher.groupCount() >= 2 && matcher.group(2) != null)
            matcher.group(2)?.trim() else null

        return ParsedTransaction(
            type = TransactionType.WITHDRAWAL,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = extractFee(smsBody),
            counterpartyName = agentName ?: agentCode,
            mpesaCode = code,
            balance = extractBalance(smsBody),
            rawSmsBody = smsBody,
            timestamp = timestamp
        )
    }

    private fun parseAirtime(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        return ParsedTransaction(
            type = TransactionType.AIRTIME,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = extractFee(smsBody),
            counterpartyName = "Airtime",
            mpesaCode = code,
            balance = extractBalance(smsBody),
            rawSmsBody = smsBody,
            timestamp = timestamp
        )
    }

    private fun parseTokenPurchase(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        return ParsedTransaction(
            type = TransactionType.TOKEN_PURCHASE,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = extractFee(smsBody),
            counterpartyName = "KPLC",
            mpesaCode = code,
            balance = extractBalance(smsBody),
            rawSmsBody = smsBody,
            timestamp = timestamp
        )
    }

    private fun parseDeposit(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        val matcher = DEPOSIT_PATTERN.matcher(smsBody)
        val agentName = if (matcher.find()) matcher.group(1)?.trim() else null

        return ParsedTransaction(
            type = TransactionType.DEPOSIT,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = null, // Deposits typically don't have fees
            counterpartyName = agentName,
            mpesaCode = code,
            balance = extractBalance(smsBody),
            rawSmsBody = smsBody,
            timestamp = timestamp
        )
    }

    private fun parseFulizaRepayment(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        return ParsedTransaction(
            type = TransactionType.FULIZA_REPAYMENT,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = null,
            counterpartyName = "Fuliza M-PESA",
            mpesaCode = code,
            balance = extractBalance(smsBody),
            rawSmsBody = smsBody,
            timestamp = timestamp
        )
    }

    // Helper functions to extract common fields

    private fun extractCode(smsBody: String): String? {
        val matcher = CODE_PATTERN.matcher(smsBody)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractAmount(smsBody: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(smsBody)
        return if (matcher.find()) {
            // Remove commas and parse as double
            matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        } else null
    }

    private fun extractBalance(smsBody: String): Double? {
        val matcher = BALANCE_PATTERN.matcher(smsBody)
        return if (matcher.find()) {
            matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        } else null
    }

    private fun extractFee(smsBody: String): Double? {
        val matcher = FEE_PATTERN.matcher(smsBody)
        return if (matcher.find()) {
            matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        } else null
    }
}
