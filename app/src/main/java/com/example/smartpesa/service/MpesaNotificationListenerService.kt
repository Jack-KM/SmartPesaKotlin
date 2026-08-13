package com.example.smartpesa.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.mpesa.MpesaSmsParser
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * NotificationListenerService for capturing M-Pesa notifications
 * Alternative to SMS access - captures the same transaction data from notifications
 *
 * Requires notification listener permission in system settings
 */
@AndroidEntryPoint
class MpesaNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var mpesaSmsParser: MpesaSmsParser

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MpesaNotificationListener"
        private const val MPESA_PACKAGE = "com.safaricom.mpesa.customer"
        private const val SAFARICOM_PACKAGE = "com.safaricom.mpesaapp"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // Filter for M-Pesa notifications
        val packageName = sbn.packageName
        if (packageName != MPESA_PACKAGE && packageName != SAFARICOM_PACKAGE) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract notification text
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        // Combine title and text for parsing
        val fullText = if (title.isNotBlank() && text.isNotBlank()) {
            "$title $bigText"
        } else {
            bigText
        }

        if (fullText.isBlank()) {
            return
        }

        Log.d(TAG, "M-Pesa notification received: $fullText")

        // Parse and save transaction (use notification post time)
        serviceScope.launch {
            try {
                processNotification(fullText, sbn.postTime)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing M-Pesa notification", e)
            }
        }
    }

    private suspend fun processNotification(notificationText: String, timestamp: Long) {
        // Check if this is an M-Pesa transaction notification
        if (!isMpesaTransaction(notificationText)) {
            Log.d(TAG, "Not an M-Pesa transaction notification, skipping")
            return
        }

        // Parse the notification text using existing SMS parser
        val parsedTransaction = mpesaSmsParser.parse(notificationText, timestamp)
        if (parsedTransaction == null) {
            Log.w(TAG, "Failed to parse M-Pesa notification: $notificationText")
            return
        }

        // Check for duplicate (same M-Pesa code)
        val existingTransaction = parsedTransaction.mpesaCode.let { code ->
            transactionRepository.getByMpesaCode(code)
        }

        if (existingTransaction != null) {
            Log.d(TAG, "Transaction ${parsedTransaction.mpesaCode} already exists, skipping duplicate")
            return
        }

        // Convert to Transaction entity
        val transaction = Transaction(
            amount = parsedTransaction.amount,
            feeAmount = parsedTransaction.feeAmount ?: 0.0,
            description = buildDescription(parsedTransaction),
            type = mapTransactionType(parsedTransaction.type),
            timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(parsedTransaction.timestamp),
                ZoneId.systemDefault()
            ),
            categoryId = null, // User can categorize later
            source = "M-Pesa Notification",
            mpesaMessage = notificationText,
            mpesaCode = parsedTransaction.mpesaCode,
            originalSmsBody = notificationText
        )

        // Save to database
        val id = transactionRepository.insertTransaction(transaction)
        Log.d(TAG, "Saved transaction from notification: ID=$id, Code=${parsedTransaction.mpesaCode}")
    }

    /**
     * Map M-Pesa transaction type to entity transaction type
     */
    private fun mapTransactionType(mpesaType: com.example.smartpesa.data.mpesa.TransactionType): com.example.smartpesa.data.local.entity.TransactionType {
        return when (mpesaType) {
            com.example.smartpesa.data.mpesa.TransactionType.RECEIVE,
            com.example.smartpesa.data.mpesa.TransactionType.DEPOSIT -> {
                com.example.smartpesa.data.local.entity.TransactionType.INCOME
            }
            else -> {
                com.example.smartpesa.data.local.entity.TransactionType.EXPENSE
            }
        }
    }

    /**
     * Build description from parsed transaction
     */
    private fun buildDescription(parsed: com.example.smartpesa.data.mpesa.ParsedTransaction): String {
        val counterparty = parsed.counterpartyName ?: "Unknown"
        return when (parsed.type) {
            com.example.smartpesa.data.mpesa.TransactionType.SEND -> "Sent to $counterparty"
            com.example.smartpesa.data.mpesa.TransactionType.RECEIVE -> "Received from $counterparty"
            com.example.smartpesa.data.mpesa.TransactionType.PAYBILL -> "Paid to $counterparty"
            com.example.smartpesa.data.mpesa.TransactionType.BUY_GOODS -> "Bought from $counterparty"
            com.example.smartpesa.data.mpesa.TransactionType.WITHDRAWAL -> "Withdrawal from $counterparty"
            com.example.smartpesa.data.mpesa.TransactionType.AIRTIME -> "Airtime purchase"
            com.example.smartpesa.data.mpesa.TransactionType.TOKEN_PURCHASE -> "Token purchase ($counterparty)"
            com.example.smartpesa.data.mpesa.TransactionType.DEPOSIT -> "Deposit at $counterparty"
            com.example.smartpesa.data.mpesa.TransactionType.FULIZA_REPAYMENT -> "Fuliza M-PESA repayment"
            com.example.smartpesa.data.mpesa.TransactionType.FULIZA_ACCESS -> "Fuliza M-PESA access"
            com.example.smartpesa.data.mpesa.TransactionType.UNKNOWN -> "Transaction: $counterparty"
        }
    }

    /**
     * Check if notification text is an M-Pesa transaction
     * M-Pesa transaction notifications contain specific keywords
     */
    private fun isMpesaTransaction(text: String): Boolean {
        val lowerText = text.lowercase()

        // M-Pesa transaction keywords
        val transactionKeywords = listOf(
            "confirmed",
            "ksh",
            "sent to",
            "received from",
            "paid to",
            "bought from",
            "withdrawal",
            "airtime",
            "mpesa",
            "m-pesa"
        )

        return transactionKeywords.any { lowerText.contains(it) }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "M-Pesa notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "M-Pesa notification listener disconnected")
    }
}
