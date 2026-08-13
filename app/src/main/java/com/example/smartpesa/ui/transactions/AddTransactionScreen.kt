package com.example.smartpesa.ui.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.Loan
import com.example.smartpesa.data.local.entity.LOAN_GIVEN_CATEGORY
import com.example.smartpesa.data.local.entity.LOAN_INTEREST_CATEGORY
import com.example.smartpesa.data.local.entity.LOAN_RECEIVED_CATEGORY
import com.example.smartpesa.data.local.entity.isLoanCategory
import com.example.smartpesa.util.CurrencyFormatter
import com.example.smartpesa.util.Validation
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

enum class AddTransactionTab { Expense, Income, Transfer }
private enum class SheetMode { ExpenseCategory, IncomeCategory, LoanSelection, ExpenseAccount, IncomeAccount, TransferFrom, TransferTo }

private data class CategoryOption(val name: String, val icon: ImageVector, val color: Color)
private data class CategoryGroup(val name: String, val icon: ImageVector, val color: Color, val subcategories: List<CategoryOption>)
private data class AccountOption(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

private val expenseCategoryGroups = listOf(
    CategoryGroup(
        name = "Food & Dining",
        icon = Icons.Filled.Restaurant,
        color = Color(0xFF4CAF50),
        subcategories = listOf(
            CategoryOption("Groceries", Icons.Filled.ShoppingBag, Color(0xFF4CAF50)),
            CategoryOption("Restaurants", Icons.Filled.Restaurant, Color(0xFF4CAF50)),
            CategoryOption("Takeout", Icons.Filled.CardGiftcard, Color(0xFF4CAF50)),
            CategoryOption("Market", Icons.Filled.ShoppingBag, Color(0xFF4CAF50))
        )
    ),
    CategoryGroup(
        name = "Transport",
        icon = Icons.Filled.DirectionsBus,
        color = Color(0xFF2196F3),
        subcategories = listOf(
            CategoryOption("Matatu/Bus", Icons.Filled.DirectionsBus, Color(0xFF2196F3)),
            CategoryOption("Boda Boda", Icons.Filled.SwapHoriz, Color(0xFF2196F3)),
            CategoryOption("Uber/Taxi", Icons.Filled.DirectionsBus, Color(0xFF2196F3)),
            CategoryOption("Fuel", Icons.Filled.ElectricBolt, Color(0xFF2196F3)),
            CategoryOption("Parking", Icons.Filled.Category, Color(0xFF2196F3))
        )
    ),
    CategoryGroup(
        name = "Bills & Utilities",
        icon = Icons.Filled.ReceiptLong,
        color = Color(0xFFFF9800),
        subcategories = listOf(
            CategoryOption("Electricity (KPLC)", Icons.Filled.ElectricBolt, Color(0xFFFF9800)),
            CategoryOption("Water", Icons.Filled.ReceiptLong, Color(0xFFFF9800)),
            CategoryOption("Internet", Icons.Filled.PhoneAndroid, Color(0xFFFF9800)),
            CategoryOption("Airtime", Icons.Filled.PhoneAndroid, Color(0xFFFF9800)),
            CategoryOption("Rent", Icons.Filled.ReceiptLong, Color(0xFFFF9800))
        )
    ),
    CategoryGroup(
        name = "Shopping",
        icon = Icons.Filled.ShoppingBag,
        color = Color(0xFF9C27B0),
        subcategories = listOf(
            CategoryOption("Clothing", Icons.Filled.ShoppingBag, Color(0xFF9C27B0)),
            CategoryOption("Electronics", Icons.Filled.Category, Color(0xFF9C27B0)),
            CategoryOption("Personal Care", Icons.Filled.LocalHospital, Color(0xFF9C27B0)),
            CategoryOption("Household Items", Icons.Filled.ShoppingBag, Color(0xFF9C27B0))
        )
    ),
    CategoryGroup(
        name = "Health",
        icon = Icons.Filled.LocalHospital,
        color = Color(0xFFF44336),
        subcategories = listOf(
            CategoryOption("Medical", Icons.Filled.LocalHospital, Color(0xFFF44336)),
            CategoryOption("Pharmacy", Icons.Filled.LocalHospital, Color(0xFFF44336)),
            CategoryOption("Insurance", Icons.Filled.Savings, Color(0xFFF44336))
        )
    ),
    CategoryGroup(
        name = "Entertainment",
        icon = Icons.Filled.CardGiftcard,
        color = Color(0xFFE91E63),
        subcategories = listOf(
            CategoryOption("Movies", Icons.Filled.CardGiftcard, Color(0xFFE91E63)),
            CategoryOption("Events", Icons.Filled.CardGiftcard, Color(0xFFE91E63)),
            CategoryOption("Hobbies", Icons.Filled.CardGiftcard, Color(0xFFE91E63)),
            CategoryOption("Sports", Icons.Filled.CardGiftcard, Color(0xFFE91E63))
        )
    ),
    CategoryGroup(
        name = "Education",
        icon = Icons.Filled.School,
        color = Color(0xFF009688),
        subcategories = listOf(
            CategoryOption("School Fees", Icons.Filled.School, Color(0xFF009688)),
            CategoryOption("Books", Icons.Filled.School, Color(0xFF009688)),
            CategoryOption("Courses", Icons.Filled.Work, Color(0xFF009688)),
            CategoryOption("Stationery", Icons.Filled.Category, Color(0xFF009688))
        )
    ),
    CategoryGroup(
        name = "Personal & Family",
        icon = Icons.Filled.Work,
        color = Color(0xFF673AB7),
        subcategories = listOf(
            CategoryOption("Childcare", Icons.Filled.Work, Color(0xFF673AB7)),
            CategoryOption("Gifts", Icons.Filled.CardGiftcard, Color(0xFF673AB7)),
            CategoryOption("Personal Development", Icons.Filled.Savings, Color(0xFF673AB7))
        )
    ),
    CategoryGroup(
        name = LOAN_GIVEN_CATEGORY,
        icon = Icons.Filled.SwapHoriz,
        color = Color(0xFFEF6C00),
        subcategories = emptyList()
    ),
    CategoryGroup(
        name = LOAN_INTEREST_CATEGORY,
        icon = Icons.Filled.Savings,
        color = Color(0xFF6A1B9A),
        subcategories = emptyList()
    ),
    CategoryGroup(
        name = "Other Expenses",
        icon = Icons.Filled.Category,
        color = Color(0xFF607D8B),
        subcategories = emptyList()
    )
)

private val incomeCategoryGroups = listOf(
    CategoryGroup(
        name = "Income",
        icon = Icons.Filled.AccountBalanceWallet,
        color = Color(0xFF4CAF50),
        subcategories = listOf(
            CategoryOption("Salary", Icons.Filled.Work, Color(0xFF4CAF50)),
            CategoryOption("Business", Icons.Filled.Work, Color(0xFF4CAF50)),
            CategoryOption("Freelance", Icons.Filled.Work, Color(0xFF4CAF50)),
            CategoryOption("Gifts Received", Icons.Filled.CardGiftcard, Color(0xFF4CAF50)),
            CategoryOption("Investments", Icons.Filled.Savings, Color(0xFF4CAF50))
        )
    ),
    CategoryGroup(
        name = "Other Income",
        icon = Icons.Filled.Category,
        color = Color(0xFF607D8B),
        subcategories = emptyList()
    ),
    CategoryGroup(
        name = LOAN_RECEIVED_CATEGORY,
        icon = Icons.Filled.AccountBalanceWallet,
        color = Color(0xFF1565C0),
        subcategories = emptyList()
    )
)

private val accountOptions = listOf(
    AccountOption("M-Pesa", Icons.Filled.PhoneAndroid, Color(0xFF25A55B)),
    AccountOption("Cash", Icons.Filled.AccountBalanceWallet, Color(0xFF1E88E5)),
    AccountOption("Airtel Money", Icons.Filled.PhoneAndroid, Color(0xFFE53935))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBackPressed: () -> Unit,
    onSaved: (String) -> Unit = {},
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    var tab by rememberSaveable { mutableStateOf(AddTransactionTab.Expense) }
    var expenseCategory by rememberSaveable { mutableStateOf(expenseCategoryGroups.first().name) }
    var incomeCategory by rememberSaveable { mutableStateOf(incomeCategoryGroups.first().name) }
    var expenseAccount by rememberSaveable { mutableStateOf(accountOptions.first().name) }
    var incomeAccount by rememberSaveable { mutableStateOf(accountOptions.first().name) }
    var transferFrom by rememberSaveable { mutableStateOf(accountOptions.first().name) }
    var transferTo by rememberSaveable { mutableStateOf(accountOptions[1].name) }
    var selectedLoanId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var cost by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var selectedTime by rememberSaveable { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var isWorkTransaction by rememberSaveable { mutableStateOf(false) }
    var sheetMode by remember { mutableStateOf<SheetMode?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val editableBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    val editableShape = RoundedCornerShape(16.dp)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val autofill by viewModel.autofill.collectAsState()
    val recordedMessage by viewModel.recordedMessage.collectAsState()
    val isEditing = viewModel.isEditing
    val amountError = Validation.amountError(amount)
    val costError = if (cost.isBlank()) null else Validation.amountError(cost)
    val transferError = if (tab == AddTransactionTab.Transfer && transferFrom == transferTo) "Transfer accounts must be different" else null
    val loans by viewModel.loans.collectAsState()

    LaunchedEffect(Unit) {
        if (!isEditing) {
            val clipboardText = clipboardManager.getText()?.text?.trim().orEmpty()
            if (clipboardText.isNotBlank()) {
                viewModel.pasteFromText(clipboardText)
            }
        }
    }

    LaunchedEffect(recordedMessage) {
        val message = recordedMessage ?: return@LaunchedEffect
        viewModel.consumeRecordedMessage()
        onSaved(message)
    }

    LaunchedEffect(autofill) {
        autofill?.let { draft ->
            tab = draft.tab
            expenseCategory = draft.category.ifBlank { expenseCategory }
            incomeCategory = draft.category.ifBlank { incomeCategory }
            expenseAccount = draft.expenseAccount
            incomeAccount = draft.incomeAccount
            transferFrom = draft.transferFrom
            transferTo = draft.transferTo
            amount = draft.amount
            cost = draft.cost
            title = draft.title
            description = draft.description
            selectedDate = draft.selectedDate
            selectedTime = draft.selectedTime
            isWorkTransaction = draft.isWorkTransaction
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopBar(
                isEditing = isEditing,
                onBackPressed = onBackPressed,
                onPastePressed = {
                    val pastedText = clipboardManager.getText()?.text?.trim().orEmpty()
                    if (pastedText.isNotBlank()) {
                        viewModel.pasteFromText(pastedText)
                    } else {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Clipboard empty") }
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val error = viewModel.saveTransaction(
                                tab = tab,
                                amountText = amount,
                                costText = cost,
                                category = when (tab) {
                                    AddTransactionTab.Expense -> expenseCategory
                                    AddTransactionTab.Income -> incomeCategory
                                    AddTransactionTab.Transfer -> "Transfer"
                                },
                                expenseAccount = expenseAccount,
                                incomeAccount = incomeAccount,
                                transferFrom = transferFrom,
                                transferTo = transferTo,
                                title = title,
                                description = description,
                                mpesaMessage = autofill?.mpesaMessage.orEmpty(),
                                selectedDate = selectedDate,
                                selectedTime = selectedTime,
                                relatedLoanId = selectedLoanId,
                                isWorkTransaction = isWorkTransaction
                            )
                            if (error == null) {
                                onSaved(if (isEditing) "Transaction updated" else "Transaction saved")
                            } else {
                                snackbarHostState.showSnackbar(error)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    shape = editableShape,
                    enabled = amountError == null && costError == null && transferError == null
                ) {
                    Text(
                        text = when (tab) {
                            AddTransactionTab.Expense -> if (isEditing) "Update Expense" else "Save Expense"
                            AddTransactionTab.Income -> if (isEditing) "Update Income" else "Save Income"
                            AddTransactionTab.Transfer -> if (isEditing) "Update Transfer" else "Save Transfer"
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabRow(selectedTabIndex = tab.ordinal) {
                AddTransactionTab.entries.forEachIndexed { index, currentTab ->
                    Tab(
                        selected = tab.ordinal == index,
                        onClick = { tab = currentTab },
                        text = { Text(currentTab.name) }
                    )
                }
            }

            when (tab) {
                AddTransactionTab.Expense -> ExpenseIncomeForm(
                    category = expenseCategory,
                    onCategoryClick = { sheetMode = SheetMode.ExpenseCategory },
                    loanLabel = selectedLoanId?.let { id -> loans.firstOrNull { it.id == id }?.let { loan -> "Linked loan: ${loan.counterparty.ifBlank { "Loan #$id" }} · ${CurrencyFormatter.format(loan.remainingBalance)}" } },
                    account = expenseAccount,
                    onAccountSelected = { selectedAccount -> expenseAccount = selectedAccount },
                    amount = amount,
                    onAmountChange = { amount = it },
                    cost = cost,
                    onCostChange = { cost = it },
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    selectedDate = selectedDate,
                    selectedTime = selectedTime,
                    onDateClick = { openDatePicker(context, selectedDate) { selectedDate = it } },
                    onTimeClick = { openTimePicker(context, selectedTime) { selectedTime = it } },
                    editableBackground = editableBackground,
                    editableShape = editableShape
                )

                AddTransactionTab.Income -> ExpenseIncomeForm(
                    category = incomeCategory,
                    onCategoryClick = { sheetMode = SheetMode.IncomeCategory },
                    loanLabel = selectedLoanId?.let { id -> loans.firstOrNull { it.id == id }?.let { loan -> "Linked loan: ${loan.counterparty.ifBlank { "Loan #$id" }} · ${CurrencyFormatter.format(loan.remainingBalance)}" } },
                    account = incomeAccount,
                    onAccountSelected = { selectedAccount -> incomeAccount = selectedAccount },
                    amount = amount,
                    onAmountChange = { amount = it },
                    cost = cost,
                    onCostChange = { cost = it },
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    selectedDate = selectedDate,
                    selectedTime = selectedTime,
                    onDateClick = { openDatePicker(context, selectedDate) { selectedDate = it } },
                    onTimeClick = { openTimePicker(context, selectedTime) { selectedTime = it } },
                    editableBackground = editableBackground,
                    editableShape = editableShape
                )

                AddTransactionTab.Transfer -> TransferForm(
                    fromAccount = transferFrom,
                    toAccount = transferTo,
                    onFromClick = { sheetMode = SheetMode.TransferFrom },
                    onToClick = { sheetMode = SheetMode.TransferTo },
                    amount = amount,
                    onAmountChange = { amount = it },
                    cost = cost,
                    onCostChange = { cost = it },
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    editableBackground = editableBackground,
                    editableShape = editableShape
                )
            }

            if (transferError != null) {
                Text(text = transferError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (amountError != null) {
                Text(text = amountError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (costError != null) {
                Text(text = costError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            WorkTransactionRow(
                checked = isWorkTransaction,
                onCheckedChange = { isWorkTransaction = it },
                editableBackground = editableBackground,
                editableShape = editableShape
            )

            Spacer(modifier = Modifier.height(180.dp))
        }
    }

    when (sheetMode) {
        SheetMode.ExpenseCategory -> CategorySheet(
            title = "Expense Categories",
            categoryGroups = expenseCategoryGroups,
            onDismiss = { sheetMode = null },
            onCategorySelected = {
                expenseCategory = it
                selectedLoanId = null
                sheetMode = if (it == LOAN_GIVEN_CATEGORY || it == LOAN_INTEREST_CATEGORY) SheetMode.LoanSelection else null
            }
        )

        SheetMode.IncomeCategory -> CategorySheet(
            title = "Income Categories",
            categoryGroups = incomeCategoryGroups,
            onDismiss = { sheetMode = null },
            onCategorySelected = {
                incomeCategory = it
                selectedLoanId = null
                sheetMode = if (it == LOAN_RECEIVED_CATEGORY) SheetMode.LoanSelection else null
            }
        )

        SheetMode.LoanSelection -> LoanSelectionSheet(
            loans = loans,
            category = when (tab) {
                AddTransactionTab.Expense -> expenseCategory
                AddTransactionTab.Income -> incomeCategory
                else -> ""
            },
            onDismiss = { sheetMode = null },
            onLoanSelected = {
                selectedLoanId = it
                sheetMode = null
            },
            onCreateNew = { selectedLoanId = null; sheetMode = null }
        )

        SheetMode.ExpenseAccount -> AccountSheet(
            title = "Select Account",
            onDismiss = { sheetMode = null },
            onAccountSelected = {
                expenseAccount = it
                sheetMode = null
            }
        )

        SheetMode.IncomeAccount -> AccountSheet(
            title = "Select Account",
            onDismiss = { sheetMode = null },
            onAccountSelected = {
                incomeAccount = it
                sheetMode = null
            }
        )

        SheetMode.TransferFrom -> AccountSheet(
            title = "From Account",
            onDismiss = { sheetMode = null },
            onAccountSelected = {
                transferFrom = it
                if (transferFrom == transferTo) {
                    transferTo = accountOptions.firstOrNull { option -> option.name != transferFrom }?.name ?: transferTo
                }
                sheetMode = null
            }
        )

        SheetMode.TransferTo -> AccountSheet(
            title = "To Account",
            onDismiss = { sheetMode = null },
            onAccountSelected = {
                transferTo = it
                if (transferFrom == transferTo) {
                    transferFrom = accountOptions.firstOrNull { option -> option.name != transferTo }?.name ?: transferFrom
                }
                sheetMode = null
            }
        )

        null -> Unit
    }
}

@Composable
private fun CompactTopBar(
    isEditing: Boolean,
    onBackPressed: () -> Unit,
    onPastePressed: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isEditing) "Edit Transaction" else "Add Transaction",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPastePressed) {
                Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
            }
        }
    }
}

@Composable
private fun ExpenseIncomeForm(
    category: String,
    onCategoryClick: () -> Unit,
    loanLabel: String? = null,
    account: String,
    onAccountSelected: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    cost: String,
    onCostChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    editableBackground: Color,
    editableShape: RoundedCornerShape
) {
    val dateText = if (selectedDate == LocalDate.now()) "Today" else selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    val timeText = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CategorySelectorRow(category = category, onClick = onCategoryClick, editableBackground = editableBackground, editableShape = editableShape)
        if (!loanLabel.isNullOrBlank()) {
            Text(text = loanLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AmountField(
                value = amount,
                onValueChange = onAmountChange,
                label = "Amount",
                modifier = Modifier.weight(7f),
                editableBackground = editableBackground,
                editableShape = editableShape
            )
            AmountField(
                value = cost,
                onValueChange = onCostChange,
                label = "Cost",
                modifier = Modifier.weight(3f),
                editableBackground = editableBackground,
                editableShape = editableShape
            )
        }

        DateTimeRow(
            dateText = dateText,
            timeText = timeText,
            onDateClick = onDateClick,
            onTimeClick = onTimeClick,
            editableBackground = editableBackground,
            editableShape = editableShape
        )

        AccountRow(account = account, onAccountSelected = onAccountSelected, editableShape = editableShape)

        EditableField(
            value = title,
            onValueChange = onTitleChange,
            label = "Title",
            singleLine = true,
            editableBackground = editableBackground,
            editableShape = editableShape
        )

        EditableField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Description",
            minLines = 3,
            maxLines = 6,
            editableBackground = editableBackground,
            editableShape = editableShape
        )
    }
}

@Composable
private fun TransferForm(
    fromAccount: String,
    toAccount: String,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    cost: String,
    onCostChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    editableBackground: Color,
    editableShape: RoundedCornerShape
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = editableBackground,
            shape = editableShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransferAccountBox(label = "To", account = toAccount, onClick = onToClick, modifier = Modifier.weight(1f), editableShape = editableShape)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                TransferAccountBox(label = "From", account = fromAccount, onClick = onFromClick, modifier = Modifier.weight(1f), editableShape = editableShape)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AmountField(
                value = amount,
                onValueChange = onAmountChange,
                label = "Amount",
                modifier = Modifier.weight(7f),
                editableBackground = editableBackground,
                editableShape = editableShape
            )
            AmountField(
                value = cost,
                onValueChange = onCostChange,
                label = "Cost",
                modifier = Modifier.weight(3f),
                editableBackground = editableBackground,
                editableShape = editableShape
            )
        }

        EditableField(
            value = title,
            onValueChange = onTitleChange,
            label = "Title",
            singleLine = true,
            editableBackground = editableBackground,
            editableShape = editableShape
        )

        EditableField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Description",
            minLines = 3,
            maxLines = 6,
            editableBackground = editableBackground,
            editableShape = editableShape
        )
    }
}

@Composable
private fun CategorySelectorRow(
    category: String,
    onClick: () -> Unit,
    editableBackground: Color,
    editableShape: RoundedCornerShape
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = editableBackground,
        shape = editableShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Category, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Category")
            Spacer(modifier = Modifier.weight(1f))
            Text(text = category, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
private fun AccountRow(
    account: String,
    onAccountSelected: (String) -> Unit,
    editableShape: RoundedCornerShape
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        accountOptions.forEach { option ->
            AccountCard(
                account = option,
                selected = option.name == account,
                onClick = { onAccountSelected(option.name) },
                modifier = Modifier.weight(1f),
                editableShape = editableShape
            )
        }
    }
}

@Composable
private fun WorkTransactionRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    editableBackground: Color,
    editableShape: RoundedCornerShape
) {
    Surface(
        color = editableBackground,
        shape = editableShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Work, contentDescription = null)
                Column {
                    Text(text = "Work transaction")
                    Text(
                        text = "Move out of personal totals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun AccountCard(
    account: AccountOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    editableShape: RoundedCornerShape
) {
    val background = if (selected) account.color else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) account.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val contentColor = if (selected) contrastColor(account.color) else account.color

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = background,
        shape = editableShape,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(account.icon, contentDescription = null, tint = contentColor)
            Text(
                text = account.name,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TransferAccountBox(
    label: String,
    account: String,
    onClick: () -> Unit,
    modifier: Modifier,
    editableShape: RoundedCornerShape
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = editableShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = account)
            }
        }
    }
}

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    editableBackground: Color,
    editableShape: RoundedCornerShape
) {
    EditableField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        prefix = { Text("KES") },
        editableBackground = editableBackground,
        editableShape = editableShape
    )
}

@Composable
private fun EditableField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 3,
    prefix: (@Composable (() -> Unit))? = null,
    editableBackground: Color,
    editableShape: RoundedCornerShape
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        prefix = prefix,
        shape = editableShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = editableBackground,
            unfocusedContainerColor = editableBackground,
            disabledContainerColor = editableBackground,
            errorContainerColor = editableBackground
        )
    )
}

@Composable
private fun DateTimeRow(
    dateText: String,
    timeText: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    editableBackground: Color,
    editableShape: RoundedCornerShape
) {
    Surface(
        color = editableBackground,
        shape = editableShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDateClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = dateText)
            }

            VerticalDivider(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTimeClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = timeText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(
    title: String,
    categoryGroups: List<CategoryGroup>,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    var selectedGroup by remember { mutableStateOf<CategoryGroup?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedGroup != null) {
                    IconButton(onClick = { selectedGroup = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to categories")
                    }
                }
                Text(
                    text = selectedGroup?.name ?: title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            val currentGroups = selectedGroup?.subcategories?.takeIf { it.isNotEmpty() }
            if (currentGroups == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 520.dp)
                ) {
                    items(categoryGroups) { group ->
                        CategoryTile(
                            label = group.name,
                            icon = group.icon,
                            color = group.color,
                            onClick = {
                                if (group.subcategories.isEmpty()) {
                                    onCategorySelected(group.name)
                                } else {
                                    selectedGroup = group
                                }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 520.dp)
                ) {
                    items(currentGroups) { category ->
                        CategoryTile(
                            label = category.name,
                            icon = category.icon,
                            color = category.color,
                            onClick = { onCategorySelected(category.name) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanSelectionSheet(
    loans: List<Loan>,
    category: String,
    onDismiss: () -> Unit,
    onLoanSelected: (Long) -> Unit,
    onCreateNew: () -> Unit
) {
    val interestOnly = category == LOAN_INTEREST_CATEGORY

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (interestOnly) "Choose loan for interest" else "Choose loan",
                style = MaterialTheme.typography.titleMedium
            )

            if (loans.isEmpty()) {
                Text(
                    text = if (interestOnly) "No loans yet. Create loan first, then record interest." else "No loans yet. Save to create new loan record.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    loans.forEach { loan ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLoanSelected(loan.id) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = loan.counterparty.ifBlank { "Loan #${loan.id}" }, style = MaterialTheme.typography.titleMedium)
                                Text(text = CurrencyFormatter.format(loan.remainingBalance), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (loan.type == com.example.smartpesa.data.local.entity.LoanType.BORROWED) "Borrowed" else "Lent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (!interestOnly) {
                Button(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) { Text("Create new loan") }
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Keep as transaction only") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(
    title: String,
    onDismiss: () -> Unit,
    onAccountSelected: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            accountOptions.forEach { account ->
                val background = account.color.copy(alpha = 0.18f)
                val contentColor = contrastColor(background)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAccountSelected(account.name) },
                    color = background,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, account.color.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(account.icon, contentDescription = null, tint = contentColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = account.name, color = contentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun contrastColor(background: Color): Color = if (background.luminance() > 0.5f) Color.Black else Color.White

private fun openDatePicker(
    context: android.content.Context,
    currentDate: LocalDate,
    onSelected: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onSelected(LocalDate.of(year, month + 1, day)) },
        currentDate.year,
        currentDate.monthValue - 1,
        currentDate.dayOfMonth
    ).show()
}

private fun openTimePicker(
    context: android.content.Context,
    currentTime: LocalTime,
    onSelected: (LocalTime) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(LocalTime.of(hour, minute)) },
        currentTime.hour,
        currentTime.minute,
        true
    ).show()
}
