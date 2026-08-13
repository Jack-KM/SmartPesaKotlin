package com.example.smartpesa.data.sms

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartpesa.data.local.entity.Fuliza
import com.example.smartpesa.data.local.entity.FulizaAccessCharge
import com.example.smartpesa.data.local.entity.FulizaRepayment
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType as EntityTransactionType
import com.example.smartpesa.data.mpesa.MpesaSmsParser
import com.example.smartpesa.data.mpesa.TransactionType as MpesaTransactionType
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
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
    private val mpesaSmsParser: MpesaSmsParser,
    private val autoCategorizationEngine: com.example.smartpesa.data.categorization.AutoCategorizationEngine,
    private val categoryRepository: com.example.smartpesa.data.repository.CategoryRepository,
    private val fulizaRepository: com.example.smartpesa.data.repository.FulizaRepository
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

        // Handle Fuliza transactions separately - don't create regular transactions for these
        when (parsedTransaction.type) {
            MpesaTransactionType.FULIZA_ACCESS -> {
                return handleFulizaAccess(parsedTransaction)
            }
            MpesaTransactionType.FULIZA_REPAYMENT -> {
                return handleFulizaRepayment(parsedTransaction)
            }
            else -> {
                // Continue with normal transaction processing
            }
        }

        // Check for duplicate by M-Pesa code
        // M-Pesa can occasionally re-deliver SMS, so we need to deduplicate
        val existingTransaction = transactionRepository.getByMpesaCode(parsedTransaction.mpesaCode)
        if (existingTransaction != null) {
            Log.d(TAG, "Transaction already exists (duplicate M-Pesa code: ${parsedTransaction.mpesaCode}), skipping")
            return Result.success()
        }

        // Convert ParsedTransaction to Transaction entity
        val transaction = parsedTransaction.toTransaction()

        // Auto-categorize the transaction
        val categorySuggestion = try {
            autoCategorizationEngine.suggestCategory(transaction)
        } catch (e: Exception) {
            Log.w(TAG, "Auto-categorization failed, proceeding without category", e)
            null
        }

        // Apply suggested category if available
        val finalTransaction = if (categorySuggestion?.categoryId != null) {
            val categoryName = categoryRepository.getCategoryById(categorySuggestion.categoryId).first()?.name
            Log.d(TAG, "Auto-categorized: categoryId=${categorySuggestion.categoryId}, " +
                    "confidence=${categorySuggestion.confidence}, source=${categorySuggestion.source}")
            transaction.copy(
                categoryId = categorySuggestion.categoryId,
                category = categoryName ?: transaction.category,
                isAutoCategorized = true
            )
        } else {
            transaction
        }

        // Insert into database
        try {
            val insertedId = transactionRepository.insertTransaction(finalTransaction)
            Log.d(TAG, "Transaction inserted successfully with ID: $insertedId")

            // Record categorization for learning (if categorized)
            if (categorySuggestion?.categoryId != null) {
                try {
                    autoCategorizationEngine.recordCategorization(
                        transaction = finalTransaction,
                        categoryId = categorySuggestion.categoryId,
                        wasUserCorrection = false
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to record categorization for learning", e)
                    // Don't fail the whole operation if learning fails
                }
            }

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
            MpesaTransactionType.FULIZA_REPAYMENT,
            MpesaTransactionType.FULIZA_ACCESS -> EntityTransactionType.EXPENSE

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
            counterparty = this.counterpartyName ?: "",
            source = "M-Pesa SMS",
            mpesaMessage = this.rawSmsBody,
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
            MpesaTransactionType.FULIZA_ACCESS -> "Fuliza access"
            MpesaTransactionType.UNKNOWN -> "M-Pesa transaction (${counterpartyName ?: "Unknown"})"
        }
    }

    /**
     * Handle Fuliza access (borrowing) by creating/updating Fuliza balance
     * This does NOT create a regular transaction
     */
    private suspend fun handleFulizaAccess(parsedTransaction: com.example.smartpesa.data.mpesa.ParsedTransaction): Result {
        try {
            val timestamp = Instant.ofEpochMilli(parsedTransaction.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            // The outstanding balance from the SMS is the authoritative source of truth.
            // Use it directly; fall back to adding the drawn amount if not present.
            val outstandingBalance = parsedTransaction.fulizaOutstanding
                ?: run {
                    val allFuliza = fulizaRepository.getAllFuliza().first()
                    val prev = allFuliza.filter { it.currentBalance > 0.0 }.maxByOrNull { it.updatedAt }
                    (prev?.currentBalance ?: 0.0) + parsedTransaction.amount
                }

            val accessFee = parsedTransaction.fulizaAccessFee ?: 0.0

            // Find the paired normal transaction (same mpesaCode) and add the access fee to it
            val pairedTx = transactionRepository.getByMpesaCode(parsedTransaction.mpesaCode)
            if (pairedTx != null && accessFee > 0.0) {
                transactionRepository.updateTransaction(
                    pairedTx.copy(feeAmount = pairedTx.feeAmount + accessFee)
                )
                Log.d(TAG, "Added Fuliza access fee $accessFee to transaction ${pairedTx.id}")
            }
            val accessCharge = FulizaAccessCharge(accessFee, timestamp, pairedTx?.id).takeIf { accessFee > 0.0 }

            // Upsert Fuliza record: one active record tracks the current state
            val allFuliza = fulizaRepository.getAllFuliza().first()
            val activeFuliza = allFuliza.filter { it.currentBalance > 0.0 }.maxByOrNull { it.updatedAt }

            if (activeFuliza != null) {
                val updated = activeFuliza.copy(
                    currentBalance = outstandingBalance,
                    totalAccessFees = activeFuliza.totalAccessFees + accessFee,
                    dueDate = parsedTransaction.fulizaDueDate ?: activeFuliza.dueDate,
                    accessCharges = activeFuliza.accessCharges + listOfNotNull(accessCharge),
                    updatedAt = timestamp
                )
                fulizaRepository.updateFuliza(updated)
                Log.d(TAG, "Updated Fuliza: outstanding=$outstandingBalance, accessFee=$accessFee, due=${updated.dueDate}")
            } else {
                val newFuliza = Fuliza(
                    currentBalance = outstandingBalance,
                    totalAccessFees = accessFee,
                    dueDate = parsedTransaction.fulizaDueDate,
                    accessCharges = listOfNotNull(accessCharge),
                    updatedAt = timestamp
                )
                val id = fulizaRepository.insertFuliza(newFuliza)
                Log.d(TAG, "Created Fuliza record id=$id, outstanding=$outstandingBalance, due=${newFuliza.dueDate}")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle Fuliza access", e)
            return Result.retry()
        }
    }

    /**
     * Handle Fuliza repayment by reducing Fuliza balance and adding to repayment history
     * This does NOT create a regular transaction
     */
    private suspend fun handleFulizaRepayment(parsedTransaction: com.example.smartpesa.data.mpesa.ParsedTransaction): Result {
        try {
            val allFuliza = fulizaRepository.getAllFuliza().first()
            // Match the most recent active Fuliza record
            val activeFuliza = allFuliza.maxByOrNull { it.updatedAt }

            if (activeFuliza == null) {
                Log.w(TAG, "Fuliza repayment received but no Fuliza record found — ignoring")
                return Result.success()
            }

            val timestamp = Instant.ofEpochMilli(parsedTransaction.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            val newRepayment = FulizaRepayment(
                amount = parsedTransaction.amount,
                timestamp = timestamp,
                transactionId = null
            )

            // Reduce balance; Safaricom sends this after auto-deduction so balance may already be 0
            val newBalance = maxOf(0.0, activeFuliza.currentBalance - parsedTransaction.amount)

            val updated = activeFuliza.copy(
                currentBalance = newBalance,
                repaymentHistory = activeFuliza.repaymentHistory + newRepayment,
                dueDate = if (newBalance == 0.0) null else activeFuliza.dueDate,
                updatedAt = timestamp
            )

            fulizaRepository.updateFuliza(updated)
            Log.d(TAG, "Fuliza repayment: amount=${parsedTransaction.amount}, new balance=$newBalance")

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle Fuliza repayment", e)
            return Result.retry()
        }
    }
}
