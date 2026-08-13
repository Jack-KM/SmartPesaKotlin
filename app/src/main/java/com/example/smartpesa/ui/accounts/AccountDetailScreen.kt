package com.example.smartpesa.ui.accounts

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.ui.components.EmptyStateScreen
import com.example.smartpesa.util.DateFormatFormatter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val ScreenBackground = Color(0xFF0F1216)
private val CardSurface = Color(0xFF1B2027)
private val DividerColor = Color(0x14FFFFFF)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFF9AA3AD)
private val AccentIndigo = Color(0xFF7A7FF6)
private val IncomeGreen = Color(0xFF7ED9A4)
private val ExpenseRose = Color(0xFFF2B8C0)

@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onEditAccount: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {}
) {
    val account by viewModel.account.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val currentBalance by viewModel.currentBalance.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditOpeningBalance by remember { mutableStateOf(false) }
    var openingBalanceInput by rememberSaveable { mutableStateOf("") }
    var showReconcile by remember { mutableStateOf(false) }
    var actualBalanceInput by rememberSaveable { mutableStateOf("") }

    val currentAccount = account

    LaunchedEffect(currentAccount?.openingBalance) {
        openingBalanceInput = currentAccount?.openingBalance?.toString().orEmpty()
    }

    Scaffold(
        topBar = {
            CompactTopAppBar(
                title = { Text("Account Details") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBackPressed) {
                        androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val accountValue = currentAccount
        if (accountValue == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) { }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(accountValue.name, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatMoney(currentBalance, accountValue.currencyCode ?: "KES"),
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                            color = if (currentBalance >= 0) IncomeGreen else ExpenseRose,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${accountValue.type.name.lowercase().replaceFirstChar { it.uppercaseChar() }} · ${accountValue.currencyCode ?: "KES"}",
                            color = SecondaryText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showEditOpeningBalance = true }) { Text("Edit opening amount") }
                            Button(onClick = { showReconcile = true }) { Text("Reconcile balance") }
                        }
                        if (accountValue.isDefault) {
                            Text("Default account", color = AccentIndigo, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Transaction history", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        if (transactions.isEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No transactions yet for this account", color = SecondaryText)
                            }
                        } else {
                            transactions.sortedBy { it.timestamp }.forEach { transaction ->
                                AccountTransactionRow(
                                    transactionId = transaction.id,
                                    title = transaction.description.ifBlank { transaction.category.ifBlank { "Transaction" } },
                                    subtitle = "${transaction.category.ifBlank { "Uncategorized" }} · ${DateFormatFormatter.formatDateTime(transaction.timestamp)}",
                                    amount = signedAmount(transaction.amount, transaction.feeAmount, transaction.type, accountValue.currencyCode ?: "KES"),
                                    amountColor = if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRose,
                                    onClick = { onTransactionClick(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showEditOpeningBalance) {
        AlertDialog(
            onDismissRequest = { showEditOpeningBalance = false },
            title = { Text("Edit opening amount") },
            text = {
                OutlinedTextField(
                    value = openingBalanceInput,
                    onValueChange = { openingBalanceInput = it },
                    label = { Text("Opening amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    openingBalanceInput.toDoubleOrNull()?.let {
                        viewModel.saveOpeningBalance(it)
                        scope.launch { snackbarHostState.showSnackbar("Opening amount updated") }
                    }
                    showEditOpeningBalance = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditOpeningBalance = false }) { Text("Cancel") } }
        )
    }

    if (showReconcile) {
        val preview = actualBalanceInput.toDoubleOrNull()?.let { viewModel.buildReconcilePreview(it) }
        AlertDialog(
            onDismissRequest = { showReconcile = false },
            title = { Text("Reconcile balance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = actualBalanceInput,
                        onValueChange = { actualBalanceInput = it },
                        label = { Text("Actual balance") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (preview != null) {
                        val diffText = formatMoney(kotlin.math.abs(preview.difference), currentAccount!!.currencyCode ?: "KES")
                        Text(
                            text = if (preview.difference > 0) "Missing: $diffText" else if (preview.difference < 0) "Extra: $diffText" else "Already balanced",
                            color = if (preview.difference == 0.0) SecondaryText else if (preview.difference > 0) ExpenseRose else IncomeGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        actualBalanceInput.toDoubleOrNull()?.let { viewModel.reconcile(it) }
                        showReconcile = false
                        scope.launch { snackbarHostState.showSnackbar("Reconciliation saved") }
                    },
                    enabled = preview != null && kotlin.math.abs(preview.difference) >= 0.01
                ) { Text("Create adjustment") }
            },
            dismissButton = { TextButton(onClick = { showReconcile = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AccountTransactionRow(
    transactionId: Long,
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AccentIndigo.copy(alpha = 0.16f), CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = PrimaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(amount, color = amountColor, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onClick) { Text("Open") }
        }
    }
}

private fun signedAmount(amount: Double, fee: Double, type: TransactionType, currencyCode: String): String {
    val total = amount + fee
    val signed = if (type == TransactionType.INCOME) total else -total
    return formatMoney(signed, currencyCode)
}

private fun formatMoney(amount: Double, currencyCode: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$currencyCode ${formatter.format(amount)}"
}
