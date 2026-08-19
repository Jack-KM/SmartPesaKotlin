package com.example.smartpesa.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType as EntityTransactionType
import com.example.smartpesa.data.mpesa.MpesaSmsParser
import com.example.smartpesa.data.mpesa.ParsedTransaction
import com.example.smartpesa.data.mpesa.TransactionType as MpesaTransactionType
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * NotificationListenerService for capturing M-Pesa notifications.
 *
 * This is an alternative to SMS access: it captures the same transaction data from
 * the notifications posted by the M-PESA app.
 *
 * Requires the "notification listener" permission in system settings.
 */
@AndroidEntryPoint
class MpesaNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var mpesaSmsParser: MpesaSmsParser

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * In-memory guard against double-processing the same M-Pesa code while this
     * process is alive. The database lookup below handles deduplication across
     * process restarts; this set covers the window between two notifications for
     * the same transaction arriving before the first insert commits.
     */
    private val processedCodes = ConcurrentHashMap<String, Boolean>()

    companion object {
        private const val TAG = "MpesaNotificationListener"

        // Keep a generous cap on the in-memory guard set so it never grows unbounded.
        private const val MAX_PROCESSED_CODES = 2000
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // Only care about M-Pesa notifications. Package names change across app
        // updates, so match any package that looks like M-Pesa / Safaricom rather
        // than a hard-coded list.
        if (!isMpesaPackage(sbn.packageName)) return

        val notificationText = extractNotificationText(sbn) ?: run {
            Log.d(TAG, "M-Pesa notification from ${sbn.packageName} had no readable text")
            return
        }

        Log.d(TAG, "M-Pesa notification received from ${sbn.packageName}")

        // onNotificationPosted runs on the main thread; move all DB/parsing work
        // off the main thread.
        serviceScope.launch {
            try {
                processNotification(notificationText, sbn.postTime)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing M-Pesa notification", e)
            }
        }
    }

    private suspend fun processNotification(notificationText: String, timestamp: Long) {
        // Cheap pre-filter before the more expensive parse.
        if (!isMpesaTransaction(notificationText)) {
            Log.d(TAG, "Not an M-Pesa transaction notification, skipping")
            return
        }

        val parsedTransaction = mpesaSmsParser.parseNotification(notificationText, timestamp)
        if (parsedTransaction == null) {
            Log.w(TAG, "Failed to parse M-Pesa notification")
            return
        }

        // A transaction without an amount and without a code is not useful.
        if (parsedTransaction.amount <= 0.0 && parsedTransaction.mpesaCode.isBlank()) {
            Log.w(TAG, "Parsed notification has no amount or code, skipping")
            return
        }

        // In-memory dedupe: skip if we already handled this code in this process.
        if (parsedTransaction.mpesaCode.isNotBlank() &&
            processedCodes.putIfAbsent(parsedTransaction.mpesaCode, true) != null
        ) {
            Log.d(TAG, "Transaction ${parsedTransaction.mpesaCode} already seen, skipping duplicate")
            return
        }

        // Database dedupe: skip if the code already exists (e.g. captured via SMS).
        val existingTransaction = parsedTransaction.mpesaCode
            .takeIf { it.isNotBlank() }
            ?.let { transactionRepository.getByMpesaCode(it) }

        if (existingTransaction != null) {
            Log.d(TAG, "Transaction ${parsedTransaction.mpesaCode} already exists, skipping duplicate")
            return
        }

        val transaction = parsedTransaction.toTransaction(notificationText)

        try {
            val id = transactionRepository.insertTransaction(transaction)
            Log.d(TAG, "Saved transaction from notification: ID=$id, Code=${parsedTransaction.mpesaCode}")
            trimProcessedCodes()
        } catch (e: Exception) {
            // Remove the guard so a transient failure can be retried on the next post.
            if (parsedTransaction.mpesaCode.isNotBlank()) {
                processedCodes.remove(parsedTransaction.mpesaCode)
            }
            throw e
        }
    }

    /**
     * Extract the human-readable text from a notification.
     *
     * M-Pesa posts its message across a variety of extras depending on the app
     * version and notification style, so we gather every field that can carry text
     * and join them together. Notably, the body is frequently delivered as an array
     * of lines via EXTRA_TEXT_LINES rather than as EXTRA_TEXT / EXTRA_BIG_TEXT.
     */
    private fun extractNotificationText(sbn: StatusBarNotification): String? {
        val extras = sbn.notification?.extras ?: return null
        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            value?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
        }

        add(extras.getCharSequence(Notification.EXTRA_TITLE))
        add(extras.getCharSequence(Notification.EXTRA_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))

        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { add(it) }

        return parts.distinct().joinToString(" ").trim().takeIf { it.isNotBlank() }
    }

    /**
     * True if the notification comes from an M-Pesa / Safaricom package.
     * Package names change across updates, so a substring match is more reliable
     * than an exact list.
     */
    private fun isMpesaPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("mpesa") || lower.contains("safaricom")
    }

    /**
     * Cheap keyword check used before parsing.
     */
    private fun isMpesaTransaction(text: String): Boolean {
        val lower = text.lowercase()

        val transactionKeywords = listOf(
            "confirmed",
            "ksh",
            "kes",
            "sent to",
            "received",
            "paid to",
            "bought",
            "withdraw",
            "airtime",
            "mpesa",
            "m-pesa",
            "fuliza",
            "pochi",
            "transaction cost",
            "new m-pesa balance"
        )

        return transactionKeywords.any { lower.contains(it) }
    }

    /**
     * Keep the in-memory guard set from growing without bound.
     */
    private fun trimProcessedCodes() {
        if (processedCodes.size > MAX_PROCESSED_CODES) {
            processedCodes.clear()
        }
    }

    /**
     * Convert a parsed M-Pesa notification into the Room entity.
     */
    private fun ParsedTransaction.toTransaction(notificationText: String): Transaction {
        return Transaction(
            amount = amount,
            feeAmount = feeAmount ?: 0.0,
            description = buildDescription(),
            type = mapTransactionType(type),
            timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ),
            categoryId = null, // User can categorize later
            source = "M-Pesa Notification",
            mpesaCode = mpesaCode.takeIf { it.isNotBlank() },
            originalSmsBody = notificationText
        )
    }

    /**
     * Map M-Pesa transaction type to the simplified INCOME/EXPENSE entity type.
     */
    private fun mapTransactionType(mpesaType: MpesaTransactionType): EntityTransactionType {
        return when (mpesaType) {
            MpesaTransactionType.RECEIVE,
            MpesaTransactionType.DEPOSIT -> EntityTransactionType.INCOME

            else -> EntityTransactionType.EXPENSE
        }
    }

    /**
     * Build a human-readable description from the parsed transaction.
     */
    private fun ParsedTransaction.buildDescription(): String {
        val counterparty = counterpartyName ?: "Unknown"
        return when (type) {
            MpesaTransactionType.SEND -> "Sent to $counterparty"
            MpesaTransactionType.RECEIVE -> "Received from $counterparty"
            MpesaTransactionType.PAYBILL -> "Paid to $counterparty"
            MpesaTransactionType.BUY_GOODS -> "Bought from $counterparty"
            MpesaTransactionType.WITHDRAWAL -> "Withdrawal from $counterparty"
            MpesaTransactionType.AIRTIME -> "Airtime purchase"
            MpesaTransactionType.TOKEN_PURCHASE -> "Token purchase ($counterparty)"
            MpesaTransactionType.DEPOSIT -> "Deposit at $counterparty"
            MpesaTransactionType.FULIZA_REPAYMENT -> "Fuliza M-PESA repayment"
            MpesaTransactionType.UNKNOWN -> "Transaction: $counterparty"
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "M-Pesa notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "M-Pesa notification listener disconnected")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
