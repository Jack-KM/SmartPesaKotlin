package com.example.smartpesa.data.sms

import android.util.Log
import com.example.smartpesa.data.categorization.AutoCategorizationEngine
import com.example.smartpesa.data.local.entity.Fuliza
import com.example.smartpesa.data.local.entity.FulizaAccessCharge
import com.example.smartpesa.data.local.entity.FulizaRepayment
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionCost
import com.example.smartpesa.data.local.entity.TransactionCostType
import com.example.smartpesa.data.local.entity.TransactionProvider
import com.example.smartpesa.data.local.entity.TransactionType as EntityTransactionType
import com.example.smartpesa.data.mpesa.MpesaSmsParser
import com.example.smartpesa.data.mpesa.ParsedTransaction
import com.example.smartpesa.data.mpesa.TransactionType as MpesaTransactionType
import com.example.smartpesa.data.repository.CategoryRepository
import com.example.smartpesa.data.repository.FulizaRepository
import com.example.smartpesa.data.repository.TransactionCostRepository
import com.example.smartpesa.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MpesaMessageProcessor @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionCostRepository: TransactionCostRepository,
    private val mpesaSmsParser: MpesaSmsParser,
    private val autoCategorizationEngine: AutoCategorizationEngine,
    private val categoryRepository: CategoryRepository,
    private val fulizaRepository: FulizaRepository
) {
    suspend fun process(text: String, timestamp: Long, source: String): MpesaProcessResult {
        if (text.isBlank()) return MpesaProcessResult.Ignored
        val parsed = mpesaSmsParser.parse(text, timestamp) ?: return MpesaProcessResult.Ignored

        return try {
            when (parsed.type) {
                MpesaTransactionType.FULIZA_ACCESS -> handleFulizaAccess(parsed)
                MpesaTransactionType.FULIZA_REPAYMENT -> handleFulizaRepayment(parsed)
                else -> handleTransaction(parsed, source)
            }
            MpesaProcessResult.Processed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process M-Pesa message", e)
            MpesaProcessResult.Retry
        }
    }

    private suspend fun handleTransaction(parsed: ParsedTransaction, source: String) {
        if (transactionRepository.getByMpesaCode(parsed.mpesaCode) != null) return

        val transaction = parsed.toTransaction(source)
        val suggestion = runCatching { autoCategorizationEngine.suggestCategory(transaction) }.getOrNull()
        val finalTransaction = if (suggestion?.categoryId != null) {
            val categoryName = categoryRepository.getCategoryById(suggestion.categoryId).first()?.name
            transaction.copy(
                categoryId = suggestion.categoryId,
                category = categoryName ?: transaction.category,
                isAutoCategorized = true
            )
        } else {
            transaction
        }

        val id = transactionRepository.insertTransaction(finalTransaction)
        if (finalTransaction.feeAmount > 0.0) {
            transactionCostRepository.insertCost(
                TransactionCost(
                    transactionId = id,
                    costAmount = finalTransaction.feeAmount,
                    costType = TransactionCostType.FEE,
                    provider = TransactionProvider.MPESA
                )
            )
        }
        if (suggestion?.categoryId != null) {
            runCatching {
                autoCategorizationEngine.recordCategorization(
                    transaction = finalTransaction.copy(id = id),
                    categoryId = suggestion.categoryId,
                    wasUserCorrection = false
                )
            }.onFailure { Log.w(TAG, "Failed to record categorization", it) }
        }
    }

    private suspend fun handleFulizaAccess(parsed: ParsedTransaction) {
        val timestamp = parsed.timestamp.toLocalDateTime()
        val allFuliza = fulizaRepository.getAllFuliza().first()
        val active = allFuliza.filter { it.currentBalance > 0.0 }.maxByOrNull { it.updatedAt }
        val outstanding = parsed.fulizaOutstanding ?: ((active?.currentBalance ?: 0.0) + parsed.amount)
        val accessFee = parsed.fulizaAccessFee ?: 0.0
        val pairedTx = transactionRepository.getByMpesaCode(parsed.mpesaCode)

        if (pairedTx != null && accessFee > 0.0) {
            val newFee = pairedTx.feeAmount + accessFee
            transactionRepository.updateTransaction(pairedTx.copy(feeAmount = newFee))
            transactionCostRepository.deleteCostsByTransactionId(pairedTx.id)
            transactionCostRepository.insertCost(
                TransactionCost(
                    transactionId = pairedTx.id,
                    costAmount = newFee,
                    costType = TransactionCostType.FEE,
                    provider = TransactionProvider.MPESA
                )
            )
        }

        val charge = FulizaAccessCharge(accessFee, timestamp, pairedTx?.id).takeIf { accessFee > 0.0 }
        if (active == null) {
            fulizaRepository.insertFuliza(
                Fuliza(
                    currentBalance = outstanding,
                    totalAccessFees = accessFee,
                    dueDate = parsed.fulizaDueDate,
                    accessCharges = listOfNotNull(charge),
                    updatedAt = timestamp
                )
            )
        } else {
            fulizaRepository.updateFuliza(
                active.copy(
                    currentBalance = outstanding,
                    totalAccessFees = active.totalAccessFees + accessFee,
                    dueDate = parsed.fulizaDueDate ?: active.dueDate,
                    accessCharges = active.accessCharges + listOfNotNull(charge),
                    updatedAt = timestamp
                )
            )
        }
    }

    private suspend fun handleFulizaRepayment(parsed: ParsedTransaction) {
        val active = fulizaRepository.getAllFuliza().first().maxByOrNull { it.updatedAt } ?: return
        val timestamp = parsed.timestamp.toLocalDateTime()
        val balance = maxOf(0.0, active.currentBalance - parsed.amount)
        fulizaRepository.updateFuliza(
            active.copy(
                currentBalance = balance,
                repaymentHistory = active.repaymentHistory + FulizaRepayment(parsed.amount, timestamp),
                dueDate = if (balance == 0.0) null else active.dueDate,
                updatedAt = timestamp
            )
        )
    }

    private fun ParsedTransaction.toTransaction(source: String): Transaction {
        return Transaction(
            amount = amount,
            feeAmount = feeAmount ?: 0.0,
            description = buildDescription(),
            type = when (type) {
                MpesaTransactionType.RECEIVE, MpesaTransactionType.DEPOSIT -> EntityTransactionType.INCOME
                else -> EntityTransactionType.EXPENSE
            },
            timestamp = timestamp.toLocalDateTime(),
            categoryId = null,
            counterparty = counterpartyName.orEmpty(),
            accountName = "M-Pesa",
            source = source,
            mpesaMessage = rawSmsBody,
            mpesaCode = mpesaCode,
            originalSmsBody = rawSmsBody
        )
    }

    private fun ParsedTransaction.buildDescription(): String = when (type) {
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

    private fun Long.toLocalDateTime() = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    private companion object {
        const val TAG = "MpesaMessageProcessor"
    }
}

enum class MpesaProcessResult {
    Processed,
    Ignored,
    Retry
}
