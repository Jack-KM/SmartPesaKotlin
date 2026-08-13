package com.example.smartpesa.ui.transactions

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val ScreenBackground = androidx.compose.ui.graphics.Color(0xFF0F1216)
private val CardSurface = androidx.compose.ui.graphics.Color(0xFF1B2027)
private val PrimaryText = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val SecondaryText = androidx.compose.ui.graphics.Color(0xFF9AA3AD)
private val DividerColor = androidx.compose.ui.graphics.Color(0x14FFFFFF)
private val ExpenseAccent = androidx.compose.ui.graphics.Color(0xFFF2B8C0)
private val IncomeAccent = androidx.compose.ui.graphics.Color(0xFF8AD7B4)
private val ActionRed = androidx.compose.ui.graphics.Color(0xFFE57373)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    viewModel: TransactionDetailsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onEditTransaction: (Long) -> Unit = {}
) {
    val transaction by viewModel.transaction.collectAsState()
    val costs by viewModel.costs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopAppBar(
                containerColor = ScreenBackground,
                titleContentColor = PrimaryText,
                navigationIconContentColor = PrimaryText,
                actionIconContentColor = PrimaryText,
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryText
                            )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        transaction?.let { currentTransaction ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "SmartPesa transaction")
                                putExtra(Intent.EXTRA_TEXT, buildShareText(currentTransaction))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share transaction"))
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share transaction", tint = PrimaryText)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = ScreenBackground,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, DividerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { transaction?.let { onEditTransaction(it.id) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ActionRed),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ActionRed
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    ) { paddingValues ->
        when (val currentTransaction = transaction) {
            null -> BoxLoader(modifier = Modifier.padding(paddingValues))
            else -> {
                val totalFees = maxOf(currentTransaction.feeAmount, costs.sumOf { it.costAmount })
                val totalAmount = currentTransaction.amount + totalFees
                val isIncome = currentTransaction.type == TransactionType.INCOME
                val accent = if (isIncome) IncomeAccent else ExpenseAccent
                val signedAmount = "${if (isIncome) "+" else "-"}KES ${CurrencyFormatter.format(abs(totalAmount)).removePrefix("KES ").trim()}"
                val mpesaCode = currentTransaction.mpesaCode?.trim().orEmpty()
                val dateText = currentTransaction.timestamp.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                val categoryName = currentTransaction.category.ifBlank { "Uncategorized" }
                val categoryTint = categoryColor(categoryName)
                val fundingAccount = resolveFundingAccount(currentTransaction.counterparty, currentTransaction.source)
                val descriptionText = currentTransaction.description.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { categoryName }
                val feeText = if (totalFees > 0.0) CurrencyFormatter.format(totalFees) else "None"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScreenBackground)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroCard(
                        accent = accent,
                        iconTint = categoryTint,
                        amount = signedAmount,
                        isIncome = isIncome,
                        dateText = dateText,
                        sourceLabel = if (fundingAccount.isBlank()) "M-Pesa SMS" else fundingAccount,
                        transactionTypeLabel = if (isIncome) "INCOME" else "EXPENSE"
                    )

                    DetailsCard(
                        categoryName = categoryName,
                        mpesaCode = mpesaCode,
                        feeText = feeText,
                        descriptionText = descriptionText,
                        onCopyCode = {
                            if (mpesaCode.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(mpesaCode))
                                coroutineScope.launch { snackbarHostState.showSnackbar("Copied") }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete transaction?") },
            text = { Text("Delete this transaction and remove it from totals?") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        viewModel.deleteTransaction()
                        snackbarHostState.showSnackbar("Transaction deleted")
                        showDeleteDialog = false
                        onBackPressed()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HeroCard(
    accent: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    amount: String,
    isIncome: Boolean,
    dateText: String,
    sourceLabel: String,
    transactionTypeLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.width(28.dp).height(28.dp),
                        shape = CircleShape,
                        color = iconTint.copy(alpha = 0.18f)
                    ) {
                        BoxCenter {
                        Icon(imageVector = Icons.Filled.PhoneAndroid, contentDescription = null, tint = iconTint)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = transactionTypeLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = accent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = dateText,
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Text(
                text = amount,
                color = if (isIncome) IncomeAccent else ExpenseAccent,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.PhoneAndroid, contentDescription = null, tint = SecondaryText)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = sourceLabel.ifBlank { "M-Pesa SMS" },
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(
    categoryName: String,
    mpesaCode: String,
    feeText: String,
    descriptionText: String,
    onCopyCode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DetailRow(label = "Category", value = categoryName)
            DividerLine()
            DetailRow(
                label = "M-Pesa Code",
                value = mpesaCode.ifBlank { "—" },
                trailing = if (mpesaCode.isNotBlank()) {
                    @Composable {
                        IconButton(onClick = onCopyCode) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", tint = SecondaryText)
                        }
                    }
                } else null,
                monospace = true
            )
            DividerLine()
            DetailRow(label = "Fees", value = feeText)
            DividerLine()
            DescriptionRow(label = "Description", value = descriptionText)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    trailing: (@Composable () -> Unit)? = null,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = SecondaryText,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailing?.invoke()
    }
}

@Composable
private fun DescriptionRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(text = label, color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DividerLine() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerColor)
    )
}

@Composable
private fun BoxLoader(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ExpenseAccent)
    }
}

@Composable
private fun BoxCenter(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun buildShareText(transaction: com.example.smartpesa.data.local.entity.Transaction): String {
    return buildString {
        appendLine("Amount: ${CurrencyFormatter.format(transaction.amount)}")
        appendLine("Type: ${transaction.type.name}")
        appendLine("Category: ${transaction.category.ifBlank { "Uncategorized" }}")
        appendLine("Description: ${transaction.description}")
        val mpesaCode = transaction.mpesaCode
        if (mpesaCode != null) appendLine("M-Pesa Code: $mpesaCode")
        if (transaction.counterparty.isNotBlank()) appendLine("Counterparty: ${transaction.counterparty}")
    }
}

private fun resolveFundingAccount(counterparty: String, source: String): String {
    val accountText = counterparty.trim().ifBlank { source.trim() }
    if (accountText.isBlank()) return ""

    val parts = accountText.split("→", limit = 2).map { it.trim() }
    return if (parts.size == 2) parts.first().ifBlank { accountText } else accountText
}

private fun categoryColor(category: String): androidx.compose.ui.graphics.Color {
    val normalized = category.lowercase()
    return when {
        normalized.contains("food") || normalized.contains("restaurant") || normalized.contains("takeout") || normalized.contains("market") -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        normalized.contains("transport") || normalized.contains("matatu") || normalized.contains("boda") || normalized.contains("uber") || normalized.contains("fuel") || normalized.contains("parking") -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        normalized.contains("bill") || normalized.contains("utility") || normalized.contains("rent") || normalized.contains("water") || normalized.contains("internet") || normalized.contains("electricity") || normalized.contains("airtime") -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        normalized.contains("shopping") || normalized.contains("clothing") || normalized.contains("electronics") || normalized.contains("personal care") || normalized.contains("household") -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
        normalized.contains("health") || normalized.contains("medical") || normalized.contains("pharmacy") || normalized.contains("insurance") -> androidx.compose.ui.graphics.Color(0xFFF44336)
        normalized.contains("entertainment") || normalized.contains("movies") || normalized.contains("events") || normalized.contains("hobbies") || normalized.contains("sports") -> androidx.compose.ui.graphics.Color(0xFFE91E63)
        normalized.contains("education") || normalized.contains("school") || normalized.contains("books") || normalized.contains("courses") || normalized.contains("stationery") -> androidx.compose.ui.graphics.Color(0xFF009688)
        normalized.contains("personal") || normalized.contains("family") || normalized.contains("childcare") || normalized.contains("gifts") || normalized.contains("development") -> androidx.compose.ui.graphics.Color(0xFF673AB7)
        normalized.contains("loan") || normalized.contains("interest") -> androidx.compose.ui.graphics.Color(0xFF1565C0)
        normalized.contains("income") || normalized.contains("salary") || normalized.contains("business") || normalized.contains("freelance") || normalized.contains("invest") -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        else -> androidx.compose.ui.graphics.Color(0xFF607D8B)
    }
}
