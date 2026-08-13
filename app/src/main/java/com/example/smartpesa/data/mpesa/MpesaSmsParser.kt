package com.example.smartpesa.data.mpesa

import java.util.regex.Pattern
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
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
            "(?:New M-PESA balance is|Your M-PESA balance is)\\s+(?:(?:Ksh|KES)\\.?\\s*)?([0-9,]+\\.?[0-9]*)",
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

        // FULIZA_ACCESS: "You have accessed Ksh500.00 from Fuliza M-PESA"
        // OR notification: "Fuliza M-PESA amount is Ksh40.00. Access Fee charged Ksh0.40."
        private val FULIZA_ACCESS_PATTERN = Pattern.compile(
            "accessed.*?from Fuliza|Fuliza.*?limit.*?Ksh|Fuliza M-PESA amount is",
            Pattern.CASE_INSENSITIVE
        )

        // Access fee in Fuliza notification: "Access Fee charged Ksh 0.40"
        private val FULIZA_ACCESS_FEE_PATTERN = Pattern.compile(
            "Access Fee charged\\s+(?:Ksh|KES)\\.?\\s*([0-9,]+\\.?[0-9]*)",
            Pattern.CASE_INSENSITIVE
        )

        // Outstanding in Fuliza notification: "Total Fuliza M-PESA outstanding amount is Ksh748.38"
        private val FULIZA_OUTSTANDING_PATTERN = Pattern.compile(
            "outstanding amount is\\s+(?:Ksh|KES)\\.?\\s*([0-9,]+\\.?[0-9]*)",
            Pattern.CASE_INSENSITIVE
        )

        // Due date in Fuliza notification: "due on 10/09/26"
        private val FULIZA_DUE_PATTERN = Pattern.compile(
            "due on\\s+([0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4})",
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

        val messageTimestamp = extractMessageTimestamp(smsBody) ?: timestamp

        // Extract transaction code (required for all M-Pesa messages)
        val mpesaCode = extractCode(smsBody) ?: return null

        // Determine transaction type and extract relevant fields
        return when {
            FULIZA_REPAYMENT_PATTERN.matcher(smsBody).find() -> {
                parseFulizaRepayment(smsBody, mpesaCode, messageTimestamp)
            }
            FULIZA_ACCESS_PATTERN.matcher(smsBody).find() -> {
                parseFulizaAccess(smsBody, mpesaCode, messageTimestamp)
            }
            WITHDRAWAL_PATTERN.matcher(smsBody).find() -> {
                parseWithdrawal(smsBody, mpesaCode, messageTimestamp)
            }
            DEPOSIT_PATTERN.matcher(smsBody).find() -> {
                parseDeposit(smsBody, mpesaCode, messageTimestamp)
            }
            AIRTIME_PATTERN.matcher(smsBody).find() -> {
                parseAirtime(smsBody, mpesaCode, messageTimestamp)
            }
            TOKEN_PATTERN.matcher(smsBody).find() -> {
                parseTokenPurchase(smsBody, mpesaCode, messageTimestamp)
            }
            RECEIVE_PATTERN.matcher(smsBody).find() -> {
                parseReceive(smsBody, mpesaCode, messageTimestamp)
            }
            SEND_PATTERN.matcher(smsBody).find() && !smsBody.contains("for account", ignoreCase = true) -> {
                parseSend(smsBody, mpesaCode, messageTimestamp)
            }
            PAYBILL_PATTERN.matcher(smsBody).find() -> {
                parsePaybill(smsBody, mpesaCode, messageTimestamp)
            }
            else -> {
                // Unrecognized format - return UNKNOWN type with basic info
                ParsedTransaction(
                    type = TransactionType.UNKNOWN,
                    amount = extractAmount(smsBody) ?: 0.0,
                    mpesaCode = mpesaCode,
                    balance = extractBalance(smsBody),
                    rawSmsBody = smsBody,
                    timestamp = messageTimestamp
                )
            }
        }
    }

    private fun extractMessageTimestamp(smsBody: String): Long? {
        val patterns = listOf(
            Pattern.compile(
                "(?i)\\bon\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[AP]M)"
            ),
            Pattern.compile(
                "(?i)\\bon\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2})"
            )
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(smsBody)
            if (!matcher.find()) continue

            val dateText = matcher.group(1)
            val timeText = matcher.group(2)
            val parsedDate = parseMessageDate(dateText) ?: continue
            val parsedTime = parseMessageTime(timeText) ?: continue
            return LocalDateTime.of(parsedDate, parsedTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

        return null
    }

    private fun parseMessageDate(value: String): LocalDate? {
        val normalized = value.replace('-', '/')
        val formats = listOf("d/M/uuuu", "dd/MM/uuuu", "d/M/uu", "dd/MM/uu")

        for (pattern in formats) {
            try {
                return LocalDate.parse(normalized, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
            } catch (_: DateTimeParseException) {
            }
        }

        return null
    }

    private fun parseMessageTime(value: String): LocalTime? {
        val normalized = value.trim().uppercase(Locale.ENGLISH)
        val formats = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")

        for (pattern in formats) {
            try {
                return LocalTime.parse(normalized, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
            } catch (_: DateTimeParseException) {
            }
        }

        return null
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

    private fun parseFulizaAccess(smsBody: String, code: String, timestamp: Long): ParsedTransaction {
        val accessFeeMatcher = FULIZA_ACCESS_FEE_PATTERN.matcher(smsBody)
        val accessFee = if (accessFeeMatcher.find())
            accessFeeMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() else null

        val outstandingMatcher = FULIZA_OUTSTANDING_PATTERN.matcher(smsBody)
        val outstanding = if (outstandingMatcher.find())
            outstandingMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() else null

        val dueMatcher = FULIZA_DUE_PATTERN.matcher(smsBody)
        val dueDate = if (dueMatcher.find()) dueMatcher.group(1) else null

        return ParsedTransaction(
            type = TransactionType.FULIZA_ACCESS,
            amount = extractAmount(smsBody) ?: 0.0,
            feeAmount = extractFee(smsBody),
            fulizaAccessFee = accessFee,
            fulizaOutstanding = outstanding,
            fulizaDueDate = dueDate,
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
