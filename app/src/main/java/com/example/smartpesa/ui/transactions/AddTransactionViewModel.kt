package com.example.smartpesa.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.mpesa.MpesaSmsParser
import com.example.smartpesa.data.local.entity.Loan
import com.example.smartpesa.data.local.entity.LoanPayment
import com.example.smartpesa.data.local.entity.Fuliza
import com.example.smartpesa.data.local.entity.FulizaAccessCharge
import com.example.smartpesa.data.local.entity.LOAN_GIVEN_CATEGORY
import com.example.smartpesa.data.local.entity.LOAN_INTEREST_CATEGORY
import com.example.smartpesa.data.local.entity.LOAN_RECEIVED_CATEGORY
import com.example.smartpesa.data.local.entity.LoanType
import com.example.smartpesa.data.local.entity.isLoanCategory
import com.example.smartpesa.data.local.entity.loanTypeForCategory
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionCost
import com.example.smartpesa.data.local.entity.TransactionCostType
import com.example.smartpesa.data.local.entity.TransactionProvider
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.FulizaRepository
import com.example.smartpesa.data.repository.LoanRepository
import com.example.smartpesa.data.repository.TransactionCostRepository
import com.example.smartpesa.data.repository.TransactionRepository
import com.example.smartpesa.util.Validation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val transactionCostRepository: TransactionCostRepository,
    private val loanRepository: LoanRepository,
    private val fulizaRepository: FulizaRepository,
    private val mpesaSmsParser: MpesaSmsParser,
    private val categoryRepository: com.example.smartpesa.data.repository.CategoryRepository,
    private val autoCategorizationEngine: com.example.smartpesa.data.categorization.AutoCategorizationEngine
) : ViewModel() {

    private val editingTransactionId: Long? = savedStateHandle.get<Long>("transactionId")?.takeIf { it > 0 }
    private var editingTransaction: Transaction? = null

    private val _autofill = MutableStateFlow<TransactionFormDraft?>(null)
    val autofill: StateFlow<TransactionFormDraft?> = _autofill.asStateFlow()
    private val _recordedMessage = MutableStateFlow<String?>(null)
    val recordedMessage: StateFlow<String?> = _recordedMessage.asStateFlow()
    val loans = loanRepository.getAllLoans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isEditing: Boolean
        get() = editingTransactionId != null

    init {
        savedStateHandle.get<String>("message")?.takeIf { it.isNotBlank() }?.let {
            pasteFromText(it)
        }

        editingTransactionId?.let { transactionId ->
            viewModelScope.launch {
                editingTransaction = transactionRepository.getTransactionById(transactionId).firstOrNull()
                editingTransaction?.let { _autofill.value = it.toDraft() }
            }
        }
    }

    fun pasteFromText(text: String) {
        viewModelScope.launch {
            _recordedMessage.value = recordFulizaAccessIfNeeded(text)
            if (_recordedMessage.value == null) autofillFromText(text)
        }
    }

    fun consumeRecordedMessage() {
        _recordedMessage.value = null
    }

    private suspend fun recordFulizaAccessIfNeeded(text: String): String? {
        val parsed = mpesaSmsParser.parse(text.trim(), System.currentTimeMillis()) ?: return null
        if (parsed.type != com.example.smartpesa.data.mpesa.TransactionType.FULIZA_ACCESS) return null

        val timestamp = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(parsed.timestamp),
            ZoneId.systemDefault()
        )
        val accessFee = parsed.fulizaAccessFee ?: 0.0
        val pairedTransaction = transactionRepository.getByMpesaCode(parsed.mpesaCode)

        if (pairedTransaction != null && accessFee > 0.0) {
            val newFee = pairedTransaction.feeAmount + accessFee
            transactionRepository.updateTransaction(pairedTransaction.copy(feeAmount = newFee))
            transactionCostRepository.deleteCostsByTransactionId(pairedTransaction.id)
            transactionCostRepository.insertCost(
                TransactionCost(
                    transactionId = pairedTransaction.id,
                    costAmount = newFee,
                    costType = TransactionCostType.FEE,
                    provider = TransactionProvider.MPESA
                )
            )
        }

        val records = fulizaRepository.getAllFuliza().first()
        val active = records.filter { it.currentBalance > 0.0 }.maxByOrNull { it.updatedAt } ?: records.maxByOrNull { it.updatedAt }
        val outstanding = parsed.fulizaOutstanding ?: parsed.amount
        if (active == null) {
            fulizaRepository.insertFuliza(
                Fuliza(
                    currentBalance = outstanding,
                    totalAccessFees = accessFee,
                    dueDate = parsed.fulizaDueDate,
                    accessCharges = listOfNotNull(FulizaAccessCharge(accessFee, timestamp, pairedTransaction?.id).takeIf { accessFee > 0.0 }),
                    updatedAt = timestamp
                )
            )
        } else {
            fulizaRepository.updateFuliza(
                active.copy(
                    currentBalance = outstanding,
                    totalAccessFees = active.totalAccessFees + accessFee,
                    dueDate = parsed.fulizaDueDate ?: active.dueDate,
                    accessCharges = active.accessCharges + listOfNotNull(FulizaAccessCharge(accessFee, timestamp, pairedTransaction?.id).takeIf { accessFee > 0.0 }),
                    updatedAt = timestamp
                )
            )
        }

        return if (pairedTransaction == null) {
            "Fuliza recorded"
        } else {
            "Fuliza recorded and fee added"
        }
    }

    private suspend fun autofillFromText(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        val parsed = mpesaSmsParser.parse(trimmedText, System.currentTimeMillis())
        _autofill.value = if (parsed != null) {
            val timestamp = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(parsed.timestamp),
                ZoneId.systemDefault()
            )
            val fallbackCategory = when (parsed.type) {
                com.example.smartpesa.data.mpesa.TransactionType.RECEIVE,
                com.example.smartpesa.data.mpesa.TransactionType.DEPOSIT -> "Income"
                com.example.smartpesa.data.mpesa.TransactionType.PAYBILL -> "Bills & Utilities"
                com.example.smartpesa.data.mpesa.TransactionType.BUY_GOODS -> "Shopping"
                com.example.smartpesa.data.mpesa.TransactionType.WITHDRAWAL -> "Other Expenses"
                com.example.smartpesa.data.mpesa.TransactionType.AIRTIME -> "Airtime"
                com.example.smartpesa.data.mpesa.TransactionType.TOKEN_PURCHASE -> "Electricity (KPLC)"
                else -> "Other Expenses"
            }
            val tab = if (parsed.type == com.example.smartpesa.data.mpesa.TransactionType.RECEIVE || parsed.type == com.example.smartpesa.data.mpesa.TransactionType.DEPOSIT) {
                AddTransactionTab.Income
            } else {
                AddTransactionTab.Expense
            }
            val tempTransaction = Transaction(
                amount = parsed.amount,
                feeAmount = parsed.feeAmount ?: 0.0,
                description = parsed.counterpartyName?.trim().orEmpty().ifBlank { fallbackCategory },
                type = if (tab == AddTransactionTab.Income) TransactionType.INCOME else TransactionType.EXPENSE,
                timestamp = timestamp,
                categoryId = null,
                category = fallbackCategory,
                counterparty = parsed.counterpartyName?.trim().orEmpty(),
                accountName = "M-Pesa",
                mpesaMessage = trimmedText,
                mpesaCode = parsed.mpesaCode,
                source = "M-Pesa SMS"
            )
            val learnedCategory = findLastCategoryForPayee(tempTransaction.counterparty)
            val category = learnedCategory
                ?: runCatching { autoCategorizationEngine.suggestCategory(tempTransaction) }.getOrNull()
                ?.categoryId
                ?.let { categoryRepository.getCategoryById(it).firstOrNull()?.name }
                ?: fallbackCategory

            TransactionFormDraft(
                tab = tab,
                category = category,
                amount = parsed.amount.toString(),
                cost = parsed.feeAmount?.takeIf { it > 0.0 }?.toString().orEmpty(),
                title = parsed.counterpartyName?.trim().orEmpty().ifBlank { category },
                description = "",
                mpesaMessage = trimmedText,
                expenseAccount = "M-Pesa",
                incomeAccount = "M-Pesa",
                transferFrom = "M-Pesa",
                transferTo = "Cash",
                selectedDate = timestamp.toLocalDate(),
                selectedTime = timestamp.toLocalTime().withSecond(0).withNano(0)
            )
        } else {
            TransactionFormDraft(
                description = trimmedText,
                title = trimmedText.take(40),
                mpesaMessage = trimmedText
            )
        }
    }

    private suspend fun findLastCategoryForPayee(payee: String): String? {
        val normalizedPayee = payee.trim()
        if (normalizedPayee.isBlank()) return null
        return transactionRepository.getAllTransactions().first()
            .filter { it.categoryId != null && it.category.isNotBlank() }
            .firstOrNull {
                val title = it.description.lineSequence().firstOrNull()?.trim().orEmpty()
                it.counterparty.equals(normalizedPayee, ignoreCase = true) ||
                    title.equals(normalizedPayee, ignoreCase = true)
            }
            ?.category
    }

    suspend fun saveTransaction(
        tab: AddTransactionTab,
        amountText: String,
        costText: String,
        category: String,
        expenseAccount: String,
        incomeAccount: String,
        transferFrom: String,
        transferTo: String,
        title: String,
        description: String,
        mpesaMessage: String,
        selectedDate: LocalDate,
        selectedTime: LocalTime,
        relatedLoanId: Long? = null,
        isWorkTransaction: Boolean = false
    ): String? {
        if (tab == AddTransactionTab.Transfer && transferFrom == transferTo) {
            return "Transfer accounts must be different"
        }

        Validation.amountError(amountText)?.let { return it }

        val amount = amountText.toDoubleOrNull() ?: return "Enter valid amount"
        val cost = costText.takeIf { it.isNotBlank() }?.let {
            Validation.amountError(it, 1_000_000.0)?.let { error -> return error }
            it.toDoubleOrNull() ?: 0.0
        } ?: 0.0
        val timestamp = LocalDateTime.of(selectedDate, selectedTime)
        Validation.dateError(timestamp)?.let { return it }

        val normalizedCategory = category.trim()
        val loanType = loanTypeForCategory(normalizedCategory)

        // Look up categoryId from category name for proper categorization learning
        val resolvedCategoryId = categoryRepository.getAllCategories().first()
            .find { it.name.equals(normalizedCategory, ignoreCase = true) }?.id
        val isInterestPayment = normalizedCategory == LOAN_INTEREST_CATEGORY
        if (isInterestPayment && relatedLoanId == null) {
            return "Select loan for interest"
        }

        val type = if (tab == AddTransactionTab.Income) TransactionType.INCOME else TransactionType.EXPENSE
        val transactionDescription = listOf(title.trim(), description.trim())
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .ifBlank { category }
        val parsedMpesaForSave = mpesaMessage.takeIf { it.isNotBlank() }?.let {
            mpesaSmsParser.parse(it, timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }
        val payee = parsedMpesaForSave?.counterpartyName?.trim()
            ?: title.trim().takeIf { it.isNotBlank() }
        val counterparty = when (tab) {
            AddTransactionTab.Expense -> payee ?: expenseAccount
            AddTransactionTab.Income -> payee ?: incomeAccount
            AddTransactionTab.Transfer -> "$transferFrom → $transferTo"
        }
        val accountName = when (tab) {
            AddTransactionTab.Expense -> expenseAccount
            AddTransactionTab.Income -> incomeAccount
            AddTransactionTab.Transfer -> transferFrom
        }
        val provider = deriveProvider(expenseAccount, incomeAccount, transferFrom, transferTo)

        val currentTransaction = editingTransactionId?.let { existingId ->
            editingTransaction ?: transactionRepository.getTransactionById(existingId).firstOrNull()
        }

        val transaction = if (currentTransaction != null) {
            currentTransaction.copy(
                amount = amount,
                feeAmount = cost,
                description = transactionDescription,
                type = type,
                timestamp = timestamp,
                categoryId = resolvedCategoryId,
                category = normalizedCategory,
                counterparty = counterparty,
                accountName = accountName,
                relatedLoanId = if (normalizedCategory.isLoanCategory()) relatedLoanId ?: currentTransaction.relatedLoanId else null,
                isWorkTransaction = isWorkTransaction,
                mpesaMessage = mpesaMessage.takeIf { it.isNotBlank() },
                mpesaCode = parsedMpesaForSave?.mpesaCode ?: currentTransaction.mpesaCode,
                isAutoCategorized = false // Manual edit - clear auto flag
            )
        } else {
            Transaction(
                amount = amount,
                feeAmount = cost,
                description = transactionDescription,
                type = type,
                timestamp = timestamp,
                categoryId = resolvedCategoryId,
                category = normalizedCategory,
                counterparty = counterparty,
                accountName = accountName,
                relatedLoanId = if (normalizedCategory.isLoanCategory()) relatedLoanId else null,
                isWorkTransaction = isWorkTransaction,
                mpesaMessage = mpesaMessage.takeIf { it.isNotBlank() },
                mpesaCode = parsedMpesaForSave?.mpesaCode,
                source = "Manual",
                isAutoCategorized = false // Manual creation - not auto-categorized
            )
        }

        val transactionId = if (currentTransaction != null) {
            transactionRepository.updateTransaction(transaction)
            transaction.id
        } else {
            transactionRepository.insertTransaction(transaction)
        }

        // Record categorization for learning (if categoryId is set)
        if (resolvedCategoryId != null) {
            try {
                val savedTransaction = transaction.copy(id = transactionId)
                autoCategorizationEngine.recordCategorization(
                    transaction = savedTransaction,
                    categoryId = resolvedCategoryId,
                    wasUserCorrection = currentTransaction != null // It's a correction if editing existing
                )
            } catch (e: Exception) {
                // Don't fail the save if learning fails
                android.util.Log.w("AddTransactionVM", "Failed to record categorization for learning", e)
            }
        }

        if (normalizedCategory.isLoanCategory()) {
            val existingLoanId = relatedLoanId ?: currentTransaction?.relatedLoanId
            if (existingLoanId == null && !isInterestPayment) {
                val newLoanId = loanRepository.insertLoan(
                    Loan(
                        amount = amount,
                        interestRate = 0.0,
                        type = loanType ?: LoanType.BORROWED,
                        counterparty = title.trim().ifBlank { normalizedCategory },
                        startDate = timestamp,
                        dueDate = timestamp.plusMonths(1),
                        remainingBalance = amount,
                        payments = emptyList()
                    )
                )
                transactionRepository.updateTransaction(transaction.copy(id = transactionId, relatedLoanId = newLoanId))
            } else if (existingLoanId != null) {
                val currentLoan = loanRepository.getLoanById(existingLoanId).firstOrNull()
                if (currentLoan != null) {
                    val updatedLoan = currentLoan.copy(
                        remainingBalance = if (isInterestPayment) currentLoan.remainingBalance else currentLoan.remainingBalance + amount,
                        payments = currentLoan.payments + LoanPayment(
                            amount = amount,
                            timestamp = timestamp,
                            transactionId = transactionId,
                            note = normalizedCategory
                        )
                    )
                    loanRepository.updateLoan(updatedLoan)
                }
                transactionRepository.updateTransaction(transaction.copy(id = transactionId, relatedLoanId = existingLoanId))
            }
        }

        transactionCostRepository.deleteCostsByTransactionId(transactionId)
        if (cost > 0.0) {
            transactionCostRepository.insertCost(
                TransactionCost(
                    transactionId = transactionId,
                    costAmount = cost,
                    costType = TransactionCostType.FEE,
                    provider = provider
                )
            )
        }

        return null
    }

    private fun deriveProvider(
        expenseAccount: String,
        incomeAccount: String,
        transferFrom: String,
        transferTo: String
    ): TransactionProvider {
        val accountText = listOf(expenseAccount, incomeAccount, transferFrom, transferTo)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .lowercase()

        return when {
            accountText.contains("bank") -> TransactionProvider.BANK
            accountText.contains("atm") || accountText.contains("cash") -> TransactionProvider.ATM
            accountText.contains("card") -> TransactionProvider.CARD
            else -> TransactionProvider.MPESA
        }
    }
}

fun Transaction.toDraft(): TransactionFormDraft {
    val descriptionLines = description.split('\n')
    val title = descriptionLines.firstOrNull().orEmpty()
    val descriptionBody = descriptionLines.drop(1).joinToString("\n")
    val counterpartyText = counterparty.trim()
    val transferParts = counterpartyText.split("→", limit = 2).map { it.trim() }
    val isTransfer = transferParts.size == 2
    val categoryName = category.ifBlank { expenseCategoryDefault }
    val account = accountName?.trim().orEmpty().ifBlank { "M-Pesa" }

    return TransactionFormDraft(
        tab = when {
            isTransfer -> AddTransactionTab.Transfer
            type == TransactionType.INCOME -> AddTransactionTab.Income
            else -> AddTransactionTab.Expense
        },
        category = categoryName,
        amount = amount.toString(),
        cost = feeAmount.takeIf { it > 0.0 }?.toString().orEmpty(),
        title = title.ifBlank { categoryName },
        description = descriptionBody,
        mpesaMessage = mpesaMessage.orEmpty(),
        expenseAccount = when {
            isTransfer -> transferParts[0].ifBlank { "M-Pesa" }
            type == TransactionType.EXPENSE -> account
            else -> "M-Pesa"
        },
        incomeAccount = when {
            isTransfer -> transferParts[1].ifBlank { "Cash" }
            type == TransactionType.INCOME -> account
            else -> "M-Pesa"
        },
        transferFrom = if (isTransfer) transferParts[0].ifBlank { "M-Pesa" } else "M-Pesa",
        transferTo = if (isTransfer) transferParts[1].ifBlank { "Cash" } else "Cash",
        selectedDate = timestamp.toLocalDate(),
        selectedTime = timestamp.toLocalTime().withSecond(0).withNano(0),
        isWorkTransaction = isWorkTransaction
    )
}

data class TransactionFormDraft(
    val tab: AddTransactionTab = AddTransactionTab.Expense,
    val category: String = expenseCategoryDefault,
    val amount: String = "",
    val cost: String = "",
    val title: String = "",
    val description: String = "",
    val mpesaMessage: String = "",
    val expenseAccount: String = "M-Pesa",
    val incomeAccount: String = "M-Pesa",
    val transferFrom: String = "M-Pesa",
    val transferTo: String = "Cash",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val isWorkTransaction: Boolean = false
)

private const val expenseCategoryDefault = "Other Expenses"
