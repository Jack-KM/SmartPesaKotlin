package com.example.smartpesa.data.sms

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType as EntityTransactionType
import com.example.smartpesa.data.mpesa.MpesaSmsParser
import com.example.smartpesa.data.mpesa.TransactionType as MpesaTransactionType
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Worker for processing M-Pesa SMS messages
 *
 * Uses Hilt for dependency injection (@HiltWorker + @AssistedInject)
 * Parses SMS, checks for duplicates, and persists to Room
 *
 * Why WorkManager:
 * - Guaranteed execution even if app is killed
 * - Automatic retry on failure
 * - Constraint-based execution (network, battery, etc.)
 * - Persistent across device reboots
 */
@HiltWorker
class SmsProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val mpesaSmsParser: MpesaSmsParser
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SmsProcessingWorker"
    }

    override suspend fun doWork(): Result {
        val smsBody = inputData.getString(SmsReceiver.KEY_SMS_BODY)
        val smsTimestamp = inputData.getLong(SmsReceiver.KEY_SMS_TIMESTAMP, System.currentTimeMillis())

        if (smsBody.isNullOrBlank()) {
            Log.w(TAG, "SMS body is null or blank")
            // This is not a worker failure - just invalid input
            return Result.success()
        }

        Log.d(TAG, "Processing SMS: ${smsBody.take(50)}...")

        // Parse the SMS
        val parsedTransaction = mpesaSmsParser.parse(smsBody, smsTimestamp)

        if (parsedTransaction == null) {
            Log.d(TAG, "SMS is not a valid M-Pesa transaction or unparseable")
            // Not a failure - just not an M-Pesa message
            return Result.success()
        }

        if (parsedTransaction.type == MpesaTransactionType.UNKNOWN) {
            Log.d(TAG, "M-Pesa message format not recognized")
            // Store it anyway with UNKNOWN type for manual review
            // User can later categorize it manually
        }

        Log.d(TAG, "Parsed transaction: ${parsedTransaction.type}, amount: ${parsedTransaction.amount}, code: ${parsedTransaction.mpesaCode}")

        // Check for duplicate by M-Pesa code
        // M-Pesa can occasionally re-deliver SMS, so we need to deduplicate
        val existingTransaction = transactionRepository.getByMpesaCode(parsedTransaction.mpesaCode)
        if (existingTransaction != null) {
            Log.d(TAG, "Transaction already exists (duplicate M-Pesa code: ${parsedTransaction.mpesaCode}), skipping")
            return Result.success()
        }

        // Convert ParsedTransaction to Transaction entity
        val transaction = parsedTransaction.toTransaction()

        // Insert into database
        try {
            val insertedId = transactionRepository.insertTransaction(transaction)
            Log.d(TAG, "Transaction inserted successfully with ID: $insertedId")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert transaction", e)
            // Retry on database errors (could be temporary)
            return Result.retry()
        }
    }

    /**
     * Convert ParsedTransaction (from SMS parser) to Transaction entity (for Room)
     * Maps M-Pesa transaction types to simple INCOME/EXPENSE categories
     */
    private fun com.example.smartpesa.data.mpesa.ParsedTransaction.toTransaction(): Transaction {
        // Map M-Pesa transaction type to INCOME/EXPENSE
        val entityType = when (this.type) {
            MpesaTransactionType.RECEIVE,
            MpesaTransactionType.DEPOSIT -> EntityTransactionType.INCOME

            MpesaTransactionType.SEND,
            MpesaTransactionType.PAYBILL,
            MpesaTransactionType.BUY_GOODS,
            MpesaTransactionType.WITHDRAWAL,
            MpesaTransactionType.AIRTIME,
            MpesaTransactionType.TOKEN_PURCHASE,
            MpesaTransactionType.FULIZA_REPAYMENT -> EntityTransactionType.EXPENSE

            MpesaTransactionType.UNKNOWN -> EntityTransactionType.EXPENSE // Default to expense for unknown
        }

        // Build description from counterparty and transaction type
        val description = buildDescription()

        // Convert Unix timestamp (milliseconds) to LocalDateTime
        val localDateTime = Instant.ofEpochMilli(this.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        return Transaction(
            amount = this.amount,
            feeAmount = this.feeAmount ?: 0.0,
            description = description,
            type = entityType,
            timestamp = localDateTime,
            categoryId = null, // Will be auto-categorized later or set manually by user
            source = "M-Pesa SMS",
            mpesaCode = this.mpesaCode,
            originalSmsBody = this.rawSmsBody
        )
    }

    /**
     * Build a human-readable description from the parsed transaction
     */
    private fun com.example.smartpesa.data.mpesa.ParsedTransaction.buildDescription(): String {
        return when (this.type) {
            MpesaTransactionType.SEND -> "Sent to ${counterpartyName ?: "Unknown"}"
            MpesaTransactionType.RECEIVE -> "Received from ${counterpartyName ?: "Unknown"}"
            MpesaTransactionType.PAYBILL -> "Paid to ${counterpartyName ?: "Paybill"}"
            MpesaTransactionType.BUY_GOODS -> "Bought from ${counterpartyName ?: "Buy Goods"}"
            MpesaTransactionType.WITHDRAWAL -> "Withdrawal from ${counterpartyName ?: "Agent"}"
            MpesaTransactionType.AIRTIME -> "Airtime purchase"
            MpesaTransactionType.TOKEN_PURCHASE -> "Token purchase (${counterpartyName ?: "Utility"})"
            MpesaTransactionType.DEPOSIT -> "Deposit to ${counterpartyName ?: "Agent"}"
            MpesaTransactionType.FULIZA_REPAYMENT -> "Fuliza repayment"
            MpesaTransactionType.UNKNOWN -> "M-Pesa transaction (${counterpartyName ?: "Unknown"})"
        }
    }
}
