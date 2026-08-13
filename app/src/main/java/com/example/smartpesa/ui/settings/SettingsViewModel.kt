package com.example.smartpesa.ui.settings

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Account
import com.example.smartpesa.data.local.entity.AccountType
import com.example.smartpesa.data.local.entity.Budget
import com.example.smartpesa.data.local.entity.BudgetPeriod
import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.local.entity.Fuliza
import com.example.smartpesa.data.local.entity.FulizaRepayment
import com.example.smartpesa.data.local.entity.Loan
import com.example.smartpesa.data.local.entity.LoanPayment
import com.example.smartpesa.data.local.entity.LoanType
import com.example.smartpesa.data.local.entity.RecurringConfig
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionCost
import com.example.smartpesa.data.local.entity.TransactionCostType
import com.example.smartpesa.data.local.entity.TransactionProvider
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.preferences.BudgetPeriodPreference
import com.example.smartpesa.data.preferences.DateFormatPreference
import com.example.smartpesa.data.preferences.LanguagePreference
import com.example.smartpesa.data.preferences.ThemePreference
import com.example.smartpesa.data.preferences.UserPreferencesRepository
import com.example.smartpesa.data.preferences.WeekStartPreference
import com.example.smartpesa.data.repository.AccountRepository
import com.example.smartpesa.data.repository.BudgetRepository
import com.example.smartpesa.data.repository.CategoryRepository
import com.example.smartpesa.data.repository.FulizaRepository
import com.example.smartpesa.data.repository.LoanRepository
import com.example.smartpesa.data.repository.TransactionCostRepository
import com.example.smartpesa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionCostRepository: TransactionCostRepository,
    private val loanRepository: LoanRepository,
    private val fulizaRepository: FulizaRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactionCount: StateFlow<Int> = transactionRepository.getAllTransactions()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _captureStatus = MutableStateFlow(CaptureModeStatus())
    val captureStatus: StateFlow<CaptureModeStatus> = _captureStatus.asStateFlow()

    private val _showCategoryDialog = MutableStateFlow(false)
    val showCategoryDialog: StateFlow<Boolean> = _showCategoryDialog.asStateFlow()

    private val _editingCategory = MutableStateFlow<Category?>(null)
    val editingCategory: StateFlow<Category?> = _editingCategory.asStateFlow()

    private val _categoryName = MutableStateFlow("")
    val categoryName: StateFlow<String> = _categoryName.asStateFlow()

    private val _categoryType = MutableStateFlow(TransactionType.EXPENSE)
    val categoryType: StateFlow<TransactionType> = _categoryType.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    val displayName: StateFlow<String> = userPreferencesRepository.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val themePreference: StateFlow<ThemePreference> = userPreferencesRepository.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreference.SYSTEM)

    val currencyPreference: StateFlow<String> = userPreferencesRepository.currencyPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "KES")

    val dateFormat: StateFlow<String> = userPreferencesRepository.dateFormatPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DateFormatPreference.LONG.pattern)

    val firstDayOfWeek: StateFlow<String> = userPreferencesRepository.weekStartPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekStartPreference.MON.label)

    val budgetPeriodDefault: StateFlow<String> = userPreferencesRepository.defaultBudgetPeriodPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetPeriodPreference.MONTHLY.label)

    val languagePreference: StateFlow<String> = userPreferencesRepository.languagePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanguagePreference.EN.code)

    val notificationsEnabled: StateFlow<Boolean> = userPreferencesRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val dailySummaryEnabled: StateFlow<Boolean> = userPreferencesRepository.dailySummaryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val budgetAlertsEnabled: StateFlow<Boolean> = userPreferencesRepository.budgetAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val largeTransactionAlertsEnabled: StateFlow<Boolean> = userPreferencesRepository.largeTransactionAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val autoReadMpesaSms: StateFlow<Boolean> = userPreferencesRepository.autoReadMpesaSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val hasCompletedPermissionSetup: StateFlow<Boolean> = userPreferencesRepository.hasCompletedPermissionSetup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val appVersion: String by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            "$versionName (Build $versionCode)"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    init {
        refreshCaptureStatus()
    }

    fun refreshCaptureStatus() {
        val smsPermissionGranted = checkSmsPermission()
        val notificationListenerEnabled = com.example.smartpesa.util.NotificationListenerUtil.isNotificationListenerEnabled(context)

        _captureStatus.value = CaptureModeStatus(
            smsPermissionGranted = smsPermissionGranted,
            notificationListenerEnabled = notificationListenerEnabled,
            clipboardAvailable = true
        )
    }

    private fun checkSmsPermission(): Boolean {
        val receiveSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val readSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return receiveSms && readSms
    }

    fun showAddCategoryDialog() {
        _editingCategory.value = null
        _categoryName.value = ""
        _categoryType.value = TransactionType.EXPENSE
        _showCategoryDialog.value = true
        _validationError.value = null
    }

    fun showEditCategoryDialog(category: Category) {
        _editingCategory.value = category
        _categoryName.value = category.name
        _categoryType.value = category.type
        _showCategoryDialog.value = true
        _validationError.value = null
    }

    fun hideCategoryDialog() {
        _showCategoryDialog.value = false
        _editingCategory.value = null
        _categoryName.value = ""
        _categoryType.value = TransactionType.EXPENSE
        _validationError.value = null
    }

    fun onCategoryNameChanged(name: String) {
        _categoryName.value = name
    }

    fun onCategoryTypeChanged(type: TransactionType) {
        _categoryType.value = type
    }

    fun saveCategory() {
        val name = _categoryName.value.trim()
        if (name.isEmpty()) {
            _validationError.value = "Category name cannot be empty"
            return
        }

        val duplicate = categories.value.any { it.name.equals(name, ignoreCase = true) && it.id != _editingCategory.value?.id }
        if (duplicate) {
            _validationError.value = "Category already exists"
            return
        }

        viewModelScope.launch {
            val editingCategory = _editingCategory.value
            if (editingCategory != null) {
                categoryRepository.updateCategory(
                    editingCategory.copy(
                        name = name,
                        type = _categoryType.value
                    )
                )
            } else {
                categoryRepository.insertCategory(
                    Category(
                        name = name,
                        type = _categoryType.value,
                        color = defaultColorForType(_categoryType.value)
                    )
                )
            }
            hideCategoryDialog()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            if (transactionRepository.countByCategoryName(category.name) > 0) {
                _validationError.value = "Cannot delete category with transactions"
                return@launch
            }
            categoryRepository.deleteCategory(category)
        }
    }

    fun saveDisplayName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDisplayName(name)
        }
    }

    fun setThemePreference(value: ThemePreference) {
        viewModelScope.launch { userPreferencesRepository.setThemePreference(value) }
    }

    fun setCurrencyPreference(currency: String) {
        viewModelScope.launch { userPreferencesRepository.setCurrencyPreference(currency) }
    }

    fun setDateFormat(dateFormat: String) {
        viewModelScope.launch { userPreferencesRepository.setDateFormatPreference(dateFormat) }
    }

    fun setFirstDayOfWeek(day: String) {
        viewModelScope.launch { userPreferencesRepository.setWeekStartPreference(day) }
    }

    fun setBudgetPeriodDefault(period: String) {
        viewModelScope.launch { userPreferencesRepository.setDefaultBudgetPeriodPreference(period) }
    }

    fun setLanguagePreference(language: String) {
        viewModelScope.launch { userPreferencesRepository.setLanguagePreference(language) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setDailySummaryEnabled(enabled) }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setBudgetAlertsEnabled(enabled) }
    }

    fun setLargeTransactionAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setLargeTransactionAlertsEnabled(enabled) }
    }

    fun setAutoReadMpesaSms(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setAutoReadMpesaSms(enabled) }
    }

    fun setPermissionSetupDone(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setPermissionSetupDone(enabled) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            transactionRepository.deleteAllTransactions()
            transactionCostRepository.deleteAllCosts()
            budgetRepository.deleteAllBudgets()
            loanRepository.deleteAllLoans()
            fulizaRepository.deleteAllFuliza()
            categoryRepository.deleteAllCategories()
            accountRepository.deleteAllAccounts()
        }
    }

    suspend fun buildCsvExport(): String {
        val transactions = transactionRepository.getAllTransactions().first()
        return buildString {
            appendLine("id,date,type,amount,fee,account,category,description,mpesaCode,source")
            transactions.forEach { transaction ->
                appendLine(
                    listOf(
                        transaction.id.toString(),
                        csv(transaction.timestamp.toString()),
                        csv(transaction.type.name),
                        transaction.amount.toString(),
                        transaction.feeAmount.toString(),
                        csv(transaction.accountName.orEmpty()),
                        csv(transaction.category),
                        csv(transaction.description),
                        csv(transaction.mpesaCode.orEmpty()),
                        csv(transaction.source)
                    ).joinToString(",")
                )
            }
        }
    }

    suspend fun buildBackupJson(): String {
        val accounts = accountRepository.getAllAccounts().first()
        val categories = categoryRepository.getAllCategories().first()
        val transactions = transactionRepository.getAllTransactions().first()
        val costs = transactionCostRepository.getAllCosts().first()
        val budgets = budgetRepository.getAllBudgets().first()
        val loans = loanRepository.getAllLoans().first()
        val fuliza = fulizaRepository.getAllFuliza().first()
        val costsByTransaction = costs.groupBy { it.transactionId }

        return JSONObject().apply {
            put("version", 1)
            put("accounts", JSONArray(accounts.map { it.toBackupJson() }))
            put("categories", JSONArray(categories.map { it.toBackupJson() }))
            put("transactions", JSONArray(transactions.map { it.toBackupJson(costsByTransaction[it.id].orEmpty()) }))
            put("budgets", JSONArray(budgets.map { it.toBackupJson() }))
            put("loans", JSONArray(loans.map { it.toBackupJson() }))
            put("fuliza", JSONArray(fuliza.map { it.toBackupJson() }))
        }.toString()
    }

    suspend fun importBackup(uri: Uri): String? {
        return try {
            val payload = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            } ?: return "Could not read backup file"
            importBackupJson(payload)
            null
        } catch (_: Exception) {
            "Backup file corrupt or unsupported"
        }
    }

    private fun defaultColorForType(type: TransactionType): String {
        return when (type) {
            TransactionType.INCOME -> "#7ED9A4"
            TransactionType.EXPENSE -> "#F2B8C0"
        }
    }

    private suspend fun importBackupJson(rawJson: String) {
        val root = JSONObject(rawJson)
        require(root.optInt("version", 0) == 1) { "Unsupported backup version" }

        val existingAccounts = accountRepository.getAllAccounts().first()
        val existingCategories = categoryRepository.getAllCategories().first()
        val existingTransactions = transactionRepository.getAllTransactions().first()

        val categoryIdMap = mutableMapOf<Long, Long>()
        val categoryArray = root.optJSONArray("categories") ?: JSONArray()
        val topLevelCategories = mutableListOf<JSONObject>()
        val childCategories = mutableListOf<JSONObject>()
        for (index in 0 until categoryArray.length()) {
            val categoryJson = categoryArray.getJSONObject(index)
            if (categoryJson.isNull("parentCategoryId")) topLevelCategories += categoryJson else childCategories += categoryJson
        }

        (topLevelCategories + childCategories).forEach { categoryJson ->
            val name = categoryJson.optString("name").trim()
            if (name.isBlank()) return@forEach
            val existing = existingCategories.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (existing != null) {
                categoryIdMap[categoryJson.optLong("id")] = existing.id
                return@forEach
            }

            val parentId = if (categoryJson.isNull("parentCategoryId")) null else categoryIdMap[categoryJson.optLong("parentCategoryId")]
            val insertedId = categoryRepository.insertCategory(
                com.example.smartpesa.data.local.entity.Category(
                    name = name,
                    type = TransactionType.valueOf(categoryJson.optString("type", TransactionType.EXPENSE.name)),
                    color = categoryJson.optString("color", "#6200EE"),
                    icon = categoryJson.optString("icon").takeIf { it.isNotBlank() },
                    parentCategoryId = parentId
                )
            )
            categoryIdMap[categoryJson.optLong("id")] = insertedId
        }

        val accountArray = root.optJSONArray("accounts") ?: JSONArray()
        for (index in 0 until accountArray.length()) {
            val accountJson = accountArray.getJSONObject(index)
            val name = accountJson.optString("name").trim()
            if (name.isBlank()) continue
            if (existingAccounts.none { it.name.equals(name, ignoreCase = true) }) {
                accountRepository.insertAccount(accountJson.toAccount())
            }
        }

        val transactionArray = root.optJSONArray("transactions") ?: JSONArray()
        for (index in 0 until transactionArray.length()) {
            val transactionJson = transactionArray.getJSONObject(index)
            val mpesaCode = transactionJson.optString("mpesaCode").trim()
            if (mpesaCode.isNotBlank() && existingTransactions.any { it.mpesaCode.equals(mpesaCode, ignoreCase = true) }) {
                continue
            }

            val transactionId = transactionRepository.insertTransaction(transactionJson.toTransaction(categoryIdMap))
            val costArray = transactionJson.optJSONArray("costs") ?: JSONArray()
            if (costArray.length() > 0) {
                transactionCostRepository.insertCosts(
                    buildList {
                        for (costIndex in 0 until costArray.length()) {
                            add(costArray.getJSONObject(costIndex).toTransactionCost(transactionId))
                        }
                    }
                )
            }
        }

        val budgetArray = root.optJSONArray("budgets") ?: JSONArray()
        for (index in 0 until budgetArray.length()) {
            val budgetJson = budgetArray.getJSONObject(index)
            val categoryName = budgetJson.optString("category").trim()
            val categoryId = categoryIdMap[budgetJson.optLong("categoryId")]
                ?: existingCategories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }?.id
                ?: continue
            budgetRepository.insertBudget(budgetJson.toBudget(categoryId))
        }

        val loanArray = root.optJSONArray("loans") ?: JSONArray()
        for (index in 0 until loanArray.length()) {
            loanRepository.insertLoan(loanArray.getJSONObject(index).toLoan())
        }

        val fulizaArray = root.optJSONArray("fuliza") ?: JSONArray()
        for (index in 0 until fulizaArray.length()) {
            fulizaRepository.insertFuliza(fulizaArray.getJSONObject(index).toFuliza())
        }
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun Account.toBackupJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("openingBalance", openingBalance)
        put("phoneNumber", phoneNumber ?: JSONObject.NULL)
        put("isDefault", isDefault)
        put("createdAt", createdAt.toString())
        put("updatedAt", updatedAt.toString())
    }

    private fun Category.toBackupJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("color", color)
        put("icon", icon ?: JSONObject.NULL)
        put("parentCategoryId", parentCategoryId ?: JSONObject.NULL)
    }

    private fun Transaction.toBackupJson(costs: List<TransactionCost>): JSONObject = JSONObject().apply {
        put("id", id)
        put("amount", amount)
        put("feeAmount", feeAmount)
        put("description", description)
        put("type", type.name)
        put("timestamp", timestamp.toString())
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("category", category)
        put("counterparty", counterparty)
        put("accountName", accountName ?: JSONObject.NULL)
        put("isRecurring", isRecurring)
        put("recurringFrequency", recurringConfig?.frequency ?: JSONObject.NULL)
        put("recurringInterval", recurringConfig?.interval ?: JSONObject.NULL)
        put("recurringNextRunAt", recurringConfig?.nextRunAt?.toString() ?: JSONObject.NULL)
        put("relatedLoanId", relatedLoanId ?: JSONObject.NULL)
        put("relatedFulizaId", relatedFulizaId ?: JSONObject.NULL)
        put("source", source)
        put("mpesaMessage", mpesaMessage ?: JSONObject.NULL)
        put("mpesaCode", mpesaCode ?: JSONObject.NULL)
        put("originalSmsBody", originalSmsBody ?: JSONObject.NULL)
        put("costs", JSONArray(costs.map { it.toBackupJson() }))
    }

    private fun TransactionCost.toBackupJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("transactionId", transactionId)
        put("costAmount", costAmount)
        put("costType", costType.name)
        put("provider", provider.name)
    }

    private fun Budget.toBackupJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("categoryId", categoryId)
        put("accountName", accountName ?: JSONObject.NULL)
        put("limit", limit)
        put("period", period.name)
        put("startDate", startDate.toString())
        put("endDate", endDate?.toString() ?: JSONObject.NULL)
        put("spent", spent)
        put("remaining", remaining)
        put("category", category)
    }

    private fun Loan.toBackupJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("amount", amount)
        put("interestRate", interestRate)
        put("type", type.name)
        put("counterparty", counterparty)
        put("startDate", startDate.toString())
        put("dueDate", dueDate.toString())
        put("remainingBalance", remainingBalance)
        put("payments", JSONArray(payments.map { it.toBackupJson() }))
    }

    private fun LoanPayment.toBackupJson(): JSONObject = JSONObject().apply {
        put("amount", amount)
        put("timestamp", timestamp.toString())
        put("transactionId", transactionId ?: JSONObject.NULL)
        put("note", note)
    }

    private fun Fuliza.toBackupJson(): JSONObject = JSONObject().apply {
        put("id", id.toLong())
        put("currentBalance", currentBalance)
        put("availableLimit", availableLimit)
        put("totalAccessFees", totalAccessFees)
        put("dueDate", dueDate ?: JSONObject.NULL)
        put("repaymentHistory", JSONArray(repaymentHistory.map { it.toBackupJson() }))
        put("updatedAt", updatedAt.toString())
    }

    private fun FulizaRepayment.toBackupJson(): JSONObject = JSONObject().apply {
        put("amount", amount)
        put("timestamp", timestamp.toString())
        put("transactionId", transactionId ?: JSONObject.NULL)
    }

    private fun JSONObject.toAccount(): Account = Account(
        name = optString("name"),
        type = AccountType.valueOf(optString("type", AccountType.OTHER.name)),
        openingBalance = optDouble("openingBalance", 0.0),
        phoneNumber = optString("phoneNumber").takeIf { it.isNotBlank() },
        isDefault = optBoolean("isDefault", false),
        createdAt = LocalDateTime.parse(optString("createdAt")),
        updatedAt = LocalDateTime.parse(optString("updatedAt"))
    )

    private fun JSONObject.toTransaction(categoryIdMap: Map<Long, Long>): Transaction {
        val categoryId = if (isNull("categoryId")) null else categoryIdMap[optLong("categoryId")]
        val recurringConfig = if (optString("recurringFrequency").isBlank() && isNull("recurringNextRunAt")) {
            null
        } else {
            RecurringConfig(
                frequency = optString("recurringFrequency", "monthly"),
                interval = optInt("recurringInterval", 1),
                nextRunAt = if (isNull("recurringNextRunAt")) null else LocalDateTime.parse(optString("recurringNextRunAt"))
            )
        }
        return Transaction(
            amount = optDouble("amount", 0.0),
            feeAmount = optDouble("feeAmount", 0.0),
            description = optString("description"),
            type = TransactionType.valueOf(optString("type", TransactionType.EXPENSE.name)),
            timestamp = LocalDateTime.parse(optString("timestamp")),
            categoryId = categoryId,
            category = optString("category", ""),
            counterparty = optString("counterparty", ""),
            accountName = optString("accountName").takeIf { it.isNotBlank() },
            isRecurring = optBoolean("isRecurring", false),
            recurringConfig = recurringConfig,
            relatedLoanId = if (isNull("relatedLoanId")) null else optLong("relatedLoanId"),
            relatedFulizaId = if (isNull("relatedFulizaId")) null else optLong("relatedFulizaId"),
            source = optString("source", "Manual"),
            mpesaMessage = optString("mpesaMessage").takeIf { it.isNotBlank() },
            mpesaCode = optString("mpesaCode").takeIf { it.isNotBlank() },
            originalSmsBody = optString("originalSmsBody").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.toTransactionCost(transactionId: Long): TransactionCost = TransactionCost(
        transactionId = transactionId,
        costAmount = optDouble("costAmount", 0.0),
        costType = TransactionCostType.valueOf(optString("costType", TransactionCostType.OTHER.name)),
        provider = TransactionProvider.valueOf(optString("provider", TransactionProvider.MPESA.name))
    )

    private fun JSONObject.toBudget(categoryId: Long): Budget = Budget(
        categoryId = categoryId,
        accountName = optString("accountName").takeIf { it.isNotBlank() },
        limit = optDouble("limit", 0.0),
        period = BudgetPeriod.valueOf(optString("period", BudgetPeriod.MONTHLY.name)),
        startDate = LocalDateTime.parse(optString("startDate")),
        endDate = if (isNull("endDate")) null else LocalDateTime.parse(optString("endDate")),
        spent = optDouble("spent", 0.0),
        remaining = optDouble("remaining", 0.0),
        category = optString("category", "")
    )

    private fun JSONObject.toLoan(): Loan = Loan(
        amount = optDouble("amount", 0.0),
        interestRate = optDouble("interestRate", 0.0),
        type = LoanType.valueOf(optString("type", LoanType.BORROWED.name)),
        counterparty = optString("counterparty", ""),
        startDate = LocalDateTime.parse(optString("startDate")),
        dueDate = LocalDateTime.parse(optString("dueDate")),
        remainingBalance = optDouble("remainingBalance", 0.0),
        payments = optJSONArray("payments")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toLoanPayment())
                }
            }
        }.orEmpty()
    )

    private fun JSONObject.toLoanPayment(): LoanPayment = LoanPayment(
        amount = optDouble("amount", 0.0),
        timestamp = LocalDateTime.parse(optString("timestamp")),
        transactionId = if (isNull("transactionId")) null else optLong("transactionId"),
        note = optString("note", "")
    )

    private fun JSONObject.toFuliza(): Fuliza = Fuliza(
        currentBalance = optDouble("currentBalance", 0.0),
        availableLimit = optDouble("availableLimit", 0.0),
        totalAccessFees = optDouble("totalAccessFees", 0.0),
        dueDate = if (isNull("dueDate")) null else optString("dueDate").ifBlank { null },
        repaymentHistory = optJSONArray("repaymentHistory")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toFulizaRepayment())
                }
            }
        }.orEmpty(),
        updatedAt = LocalDateTime.parse(optString("updatedAt"))
    )

    private fun JSONObject.toFulizaRepayment(): FulizaRepayment = FulizaRepayment(
        amount = optDouble("amount", 0.0),
        timestamp = LocalDateTime.parse(optString("timestamp")),
        transactionId = if (isNull("transactionId")) null else optLong("transactionId")
    )
}

data class CaptureModeStatus(
    val smsPermissionGranted: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val clipboardAvailable: Boolean = true
)
