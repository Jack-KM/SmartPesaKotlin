package com.example.smartpesa.ui.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.LoanType
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.ui.home.TransactionListItem
import com.example.smartpesa.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailsScreen(
    viewModel: LoanDetailsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onRecordPayment: (Long) -> Unit = {},
    onEditLoan: (Long) -> Unit = {},
    onTransactionClick: (Long) -> Unit = {}
) {
    val loan by viewModel.loan.collectAsState()
    val relatedTransactions by viewModel.relatedTransactions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopAppBar(
                title = { Text("Loan Details") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loan?.let { onEditLoan(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit loan")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete loan")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val currentLoan = loan) {
            null -> BoxLoader(modifier = Modifier.padding(paddingValues))
            else -> {
                val totalPaid = currentLoan.amount - currentLoan.remainingBalance
                val percentPaid = if (currentLoan.amount > 0) ((totalPaid / currentLoan.amount) * 100).toInt() else 0

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LoanSummaryCard(currentLoan, percentPaid)
                    }
                    item {
                        LoanDetailsInfoCard(currentLoan)
                    }
                    item {
                        Text(text = "Loan activity", style = MaterialTheme.typography.titleLarge)
                    }
                    if (relatedTransactions.isEmpty()) {
                        item {
                            Text(
                                text = "No linked transactions yet.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val groupedTransactions = relatedTransactions.groupBy { it.timestamp.toLocalDate() }
                        groupedTransactions.toSortedMap(compareByDescending { it }).forEach { (date, transactions) ->
                            item(key = "loan_day_$date") {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${transactions.size} transaction${if (transactions.size == 1) "" else "s"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(transactions, key = { it.id }) { transaction ->
                                TransactionListItem(transaction = transaction, onClick = { onTransactionClick(transaction.id) })
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onRecordPayment(currentLoan.id) }) { Text("Record payment") }
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (viewModel.markAsPaid()) {
                                            snackbarHostState.showSnackbar("Loan marked as paid")
                                        }
                                    }
                                },
                                enabled = currentLoan.remainingBalance == 0.0
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark as paid")
                            }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { showDeleteDialog = true }, enabled = currentLoan.remainingBalance == 0.0 && currentLoan.payments.isEmpty()) {
                            Text("Delete loan")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete loan?") },
            text = { Text("Delete this loan only when no payments exist and balance is zero?") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        if (viewModel.deleteLoan()) {
                            snackbarHostState.showSnackbar("Loan deleted")
                            onBackPressed()
                        } else {
                            snackbarHostState.showSnackbar("Loan still has balance or payments")
                        }
                        showDeleteDialog = false
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
private fun LoanSummaryCard(loan: com.example.smartpesa.data.local.entity.Loan, percentPaid: Int) {
    val isBorrowed = loan.type == LoanType.BORROWED
    val totalPaid = loan.amount - loan.remainingBalance
    val progressFraction = if (loan.amount > 0.0) {
        (totalPaid / loan.amount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val stripeColor = if (isBorrowed) {
        Color(0xFFEF4444) // Red for borrowed
    } else {
        Color(0xFF10B981) // Green for lent
    }

    val accentColor = if (isBorrowed) {
        Color(0xFFEF4444)
    } else {
        Color(0xFF10B981)
    }

    val icon = if (isBorrowed) Icons.Default.TrendingDown else Icons.Default.TrendingUp
    val typeLabel = if (isBorrowed) "Borrowed from" else "Lent to"

    val today = LocalDate.now()
    val daysUntilDue = ChronoUnit.DAYS.between(today, loan.dueDate)
    val dueDateText = when {
        daysUntilDue < 0 -> "Overdue by ${-daysUntilDue} days"
        daysUntilDue == 0L -> "Due today"
        daysUntilDue == 1L -> "Due tomorrow"
        daysUntilDue <= 7 -> "Due in $daysUntilDue days"
        else -> "Due ${loan.dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
    }

    val dueDateColor = when {
        daysUntilDue < 0 -> Color(0xFFEF4444)
        daysUntilDue <= 3 -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(180.dp)
                    .background(stripeColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = loan.counterparty.ifBlank { "Unknown" },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Amount info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Original amount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(loan.amount),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(loan.remainingBalance),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = accentColor
                        )
                    }
                }

                // Progress
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${percentPaid.coerceIn(0, 100)}% repaid",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = CurrencyFormatter.format(totalPaid),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.12f)
                    )
                }

                // Due date
                Text(
                    text = dueDateText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = dueDateColor
                )
            }
        }
    }
}

@Composable
private fun LoanDetailsInfoCard(loan: com.example.smartpesa.data.local.entity.Loan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Loan details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Counterparty
            InfoRow(
                icon = Icons.Default.Person,
                label = "Counterparty",
                value = loan.counterparty.ifBlank { "Unknown" }
            )

            // Type
            InfoRow(
                icon = if (loan.type == LoanType.BORROWED) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                label = "Type",
                value = if (loan.type == LoanType.BORROWED) "Borrowed" else "Lent"
            )

            // Interest rate
            InfoRow(
                icon = Icons.Default.Percent,
                label = "Interest rate",
                value = "${loan.interestRate}%"
            )

            // Start date
            InfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Start date",
                value = loan.startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            )

            // Due date
            InfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Due date",
                value = loan.dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BoxLoader(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
