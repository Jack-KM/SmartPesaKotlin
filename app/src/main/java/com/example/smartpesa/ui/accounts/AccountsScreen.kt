package com.example.smartpesa.ui.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.Account
import com.example.smartpesa.data.local.entity.AccountType
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.util.CurrencyFormatter
import kotlinx.coroutines.launch

private val ScreenBackground = Color(0xFF0F1216)
private val CardSurface = Color(0xFF1B2027)
private val DividerColor = Color(0x14FFFFFF)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFF9AA3AD)
private val AccentIndigo = Color(0xFF7A7FF6)
private val DangerRed = Color(0xFFE35B68)
private val IncomeGreen = Color(0xFF7ED9A4)
private val ExpenseRose = Color(0xFFF2B8C0)

private enum class AccountDeleteMode { MOVE, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {},
    onOpenAccountDetail: (Long) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val accounts by viewModel.accounts.collectAsState()
    val counts by viewModel.transactionCounts.collectAsState()
    val balances by viewModel.accountBalances.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditor by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var formName by rememberSaveable { mutableStateOf("") }
    var formType by rememberSaveable { mutableStateOf(AccountType.MPESA) }
    var formCurrencyCode by rememberSaveable { mutableStateOf("KES") }
    var formOpeningBalance by rememberSaveable { mutableStateOf("") }
    var formPhone by rememberSaveable { mutableStateOf("") }

    var menuAccountId by remember { mutableStateOf<Long?>(null) }
    var deleteTarget by remember { mutableStateOf<Account?>(null) }
    var deleteInfo by remember { mutableStateOf<DeleteInfo?>(null) }
    var deleteMode by rememberSaveable { mutableStateOf(AccountDeleteMode.MOVE) }
    var moveTargetName by rememberSaveable { mutableStateOf("") }
    var typedDelete by rememberSaveable { mutableStateOf("") }

    fun openEditor(account: Account? = null) {
        editingAccount = account
        formName = account?.name.orEmpty()
        formType = account?.type ?: AccountType.MPESA
        formCurrencyCode = account?.currencyCode ?: "KES"
        formOpeningBalance = account?.openingBalance?.let { if (it == 0.0) "" else it.toString() }.orEmpty()
        formPhone = account?.phoneNumber.orEmpty()
        showEditor = true
    }

    fun closeEditor() {
        showEditor = false
        editingAccount = null
        formName = ""
        formType = AccountType.MPESA
        formCurrencyCode = "KES"
        formOpeningBalance = ""
        formPhone = ""
    }

    fun openDeleteDialog(account: Account) {
        scope.launch {
            deleteInfo = viewModel.getDeleteInfo(account)
            deleteTarget = account
            deleteMode = AccountDeleteMode.MOVE
            moveTargetName = deleteInfo?.remainingAccounts?.firstOrNull()?.name.orEmpty()
            typedDelete = ""
        }
    }

    suspend fun performDelete(account: Account, info: DeleteInfo, mode: AccountDeleteMode) {
        val useMove = mode == AccountDeleteMode.MOVE && info.remainingAccounts.isNotEmpty() && moveTargetName.isNotBlank()
        viewModel.deleteAccount(
            account = account,
            deleteTransactions = info.transactionCount > 0,
            deleteBudgets = info.budgetCount > 0,
            moveToAccountName = if (useMove) moveTargetName else null
        )
        if (info.transactionCount == 0 && info.budgetCount == 0) {
            val result = snackbarHostState.showSnackbar(
                message = "Account deleted",
                actionLabel = "Undo",
                withDismissAction = true
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.restoreAccount(account)
            }
        } else {
            snackbarHostState.showSnackbar("Account deleted")
        }
    }

    Scaffold(
        topBar = {
            CompactTopAppBar(
                title = { Text("Accounts") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    IconButton(onClick = { openEditor() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add account")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(paddingValues)
        ) {
            if (accounts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AccentIndigo.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AccentIndigo,
                            modifier = Modifier.padding(20.dp).size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Add your first account", style = MaterialTheme.typography.headlineSmall, color = PrimaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Keep M-Pesa, cash, and bank balances in one place.",
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.9f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { openEditor() }) {
                        Text("Add account")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { account ->
                        val transactionCount = counts[account.name] ?: 0
                        val balance = balances[account.name] ?: 0.0
                        val expanded = menuAccountId == account.id

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, DividerColor),
                            onClick = { onOpenAccountDetail(account.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AccountTypeChip(account.type)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = account.name,
                                            color = PrimaryText,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (account.isDefault) {
                                            Badge(containerColor = AccentIndigo.copy(alpha = 0.16f), contentColor = AccentIndigo) {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${account.type.displayLabel} · $transactionCount transactions",
                                        color = SecondaryText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatMoney(balance, account.currencyCode ?: "KES"),
                                        color = if (balance >= 0) IncomeGreen else ExpenseRose,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Box {
                                        IconButton(onClick = { menuAccountId = account.id }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Account menu", tint = PrimaryText)
                                        }
                                        DropdownMenu(expanded = expanded, onDismissRequest = { menuAccountId = null }) {
                                            DropdownMenuItem(
                                                text = { Text("Make default") },
                                                onClick = {
                                                    menuAccountId = null
                                                    viewModel.makeDefault(account)
                                                },
                                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                onClick = {
                                                    menuAccountId = null
                                                    openEditor(account)
                                                },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = DangerRed) },
                                                onClick = {
                                                    menuAccountId = null
                                                    openDeleteDialog(account)
                                                },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ModalBottomSheet(
            onDismissRequest = { closeEditor() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardSurface,
            dragHandle = null
        ) {
            AccountEditorSheet(
                editingAccount = editingAccount,
                name = formName,
                onNameChange = { formName = it },
                type = formType,
                onTypeChange = { formType = it },
                currencyCode = formCurrencyCode,
                onCurrencyChange = { formCurrencyCode = it },
                openingBalance = formOpeningBalance,
                onOpeningBalanceChange = { formOpeningBalance = it },
                phone = formPhone,
                onPhoneChange = { formPhone = it },
                onDismiss = { closeEditor() },
                onSave = {
                    scope.launch {
                        val error = viewModel.saveAccount(editingAccount, formName, formType, formCurrencyCode, formOpeningBalance, formPhone)
                        if (error == null) {
                            closeEditor()
                            snackbarHostState.showSnackbar("Account saved")
                        } else {
                            snackbarHostState.showSnackbar(error)
                        }
                    }
                }
            )
        }
    }

    deleteTarget?.let { account ->
        val info = deleteInfo ?: return@let
        val simpleDelete = info.transactionCount == 0 && info.budgetCount == 0
        val lastAccountWithLinks = info.remainingAccounts.isEmpty() && !simpleDelete
        val hasChoice = !simpleDelete && info.remainingAccounts.isNotEmpty()

        if (simpleDelete) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null; deleteInfo = null },
                title = { Text("Delete account?") },
                text = {
                    Text(
                        buildString {
                            append("Delete ${account.name}?")
                            if (account.isDefault && info.nextDefaultAccount != null) {
                                append(" ${info.nextDefaultAccount.name} will become your default account.")
                            }
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            performDelete(account, info, AccountDeleteMode.DELETE)
                            deleteTarget = null
                            deleteInfo = null
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null; deleteInfo = null }) { Text("Cancel") }
                }
            )
        } else if (hasChoice) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null; deleteInfo = null },
                title = { Text("Delete ${account.name}?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Transactions: ${info.transactionCount} · Budgets: ${info.budgetCount}")
                        if (account.isDefault && info.nextDefaultAccount != null) {
                            Text("${info.nextDefaultAccount.name} will become your default account.")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = deleteMode == AccountDeleteMode.MOVE,
                                onClick = { deleteMode = AccountDeleteMode.MOVE }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Move transactions to…")
                                Text("Keeps linked transactions and budgets.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (deleteMode == AccountDeleteMode.MOVE) {
                            val moveAccounts = info.remainingAccounts.map { it.name }
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = moveTargetName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target account") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    moveAccounts.forEach { target ->
                                        DropdownMenuItem(
                                            text = { Text(target) },
                                            onClick = {
                                                moveTargetName = target
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = deleteMode == AccountDeleteMode.DELETE,
                                onClick = { deleteMode = AccountDeleteMode.DELETE }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete transactions too")
                                Text("Requires typing DELETE.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (deleteMode == AccountDeleteMode.DELETE) {
                            OutlinedTextField(
                                value = typedDelete,
                                onValueChange = { typedDelete = it },
                                label = { Text("Type DELETE") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    val canDelete = when (deleteMode) {
                        AccountDeleteMode.MOVE -> moveTargetName.isNotBlank()
                        AccountDeleteMode.DELETE -> typedDelete == "DELETE"
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                performDelete(account, info, deleteMode)
                                deleteTarget = null
                                deleteInfo = null
                            }
                        },
                        enabled = canDelete
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null; deleteInfo = null }) { Text("Cancel") }
                }
            )
        } else if (lastAccountWithLinks) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null; deleteInfo = null },
                title = { Text("Delete ${account.name}?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transactions: ${info.transactionCount} · Budgets: ${info.budgetCount}")
                        Text("Last account. Type DELETE to remove linked data.")
                        OutlinedTextField(
                            value = typedDelete,
                            onValueChange = { typedDelete = it },
                            label = { Text("Type DELETE") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                performDelete(account, info, AccountDeleteMode.DELETE)
                                deleteTarget = null
                                deleteInfo = null
                            }
                        },
                        enabled = typedDelete == "DELETE"
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null; deleteInfo = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun AccountTypeChip(type: AccountType) {
    val tint = when (type) {
        AccountType.MPESA -> AccentIndigo
        AccountType.BANK -> IncomeGreen
        AccountType.CASH -> ExpenseRose
        AccountType.OTHER -> SecondaryText
    }
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = tint.copy(alpha = 0.16f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AccountEditorSheet(
    editingAccount: Account?,
    name: String,
    onNameChange: (String) -> Unit,
    type: AccountType,
    onTypeChange: (AccountType) -> Unit,
    currencyCode: String,
    onCurrencyChange: (String) -> Unit,
    openingBalance: String,
    onOpeningBalanceChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val isEditing = editingAccount != null
    val canSave = name.trim().isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isEditing) "Edit account" else "Add account", style = MaterialTheme.typography.titleLarge, color = PrimaryText)
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = onSave, enabled = canSave) { Text(if (isEditing) "Save" else "Add") }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Type", color = SecondaryText, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountType.entries.forEach { accountType ->
                    FilterChip(
                        selected = type == accountType,
                        onClick = { onTypeChange(accountType) },
                        label = { Text(accountType.displayLabel) },
                        leadingIcon = {
                            Icon(
                                imageVector = accountType.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Currency", color = SecondaryText, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("KES", "USD", "UGX", "TZS").forEach { currency ->
                    FilterChip(
                        selected = currencyCode == currency,
                        onClick = { onCurrencyChange(currency) },
                        label = { Text(currency) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = openingBalance,
            onValueChange = onOpeningBalanceChange,
            label = { Text("Opening balance") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("M-Pesa phone (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

private val AccountType.icon: ImageVector
    get() = when (this) {
        AccountType.MPESA -> Icons.Default.PhoneAndroid
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CASH -> Icons.Default.Savings
        AccountType.OTHER -> Icons.Default.AccountBalanceWallet
    }

private val AccountType.displayLabel: String
    get() = when (this) {
        AccountType.MPESA -> "M-Pesa"
        AccountType.BANK -> "Bank"
        AccountType.CASH -> "Cash"
        AccountType.OTHER -> "Other"
    }

private fun formatMoney(amount: Double, currencyCode: String): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "KE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$currencyCode ${formatter.format(amount)}"
}
