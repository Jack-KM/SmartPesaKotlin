package com.example.smartpesa.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.ui.components.InfoCard
import com.example.smartpesa.ui.home.TransactionListItem
import com.example.smartpesa.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailsScreen(
    viewModel: BudgetDetailsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onEditBudget: (Long) -> Unit = {},
    onTransactionClick: (Long) -> Unit = {}
) {
    val budget by viewModel.budget.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopAppBar(
                title = { Text("Budget Details") },
                actions = {
                    IconButton(onClick = { budget?.let { onEditBudget(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit budget")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete budget")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val currentBudget = budget) {
            null -> BoxLoader(modifier = Modifier.padding(paddingValues))
            else -> {
                val spent = transactions.sumOf { it.amount }
                val remaining = currentBudget.limit - spent
                val percentage = if (currentBudget.limit > 0) ((spent / currentBudget.limit) * 100).toInt() else 0
                val progressColor = when {
                    percentage < 70 -> MaterialTheme.colorScheme.primary
                    percentage <= 100 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        BudgetSummaryCard(currentBudget, spent, remaining, percentage, progressColor)
                    }

                    item {
                        InfoCard(
                            "Period",
                            "${currentBudget.startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))} - ${currentBudget.endDate?.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) ?: "Open-ended"}"
                        )
                    }

                    item {
                        InfoCard(
                            "Daily Average",
                            if (transactions.isEmpty()) "KES 0" else CurrencyFormatter.format(spent / transactions.size)
                        )
                    }

                    item {
                        InfoCard(
                            "Projected Total",
                            if (transactions.isEmpty()) CurrencyFormatter.format(spent) else CurrencyFormatter.format(spent * (currentBudget.endDate?.dayOfMonth ?: currentBudget.startDate.month.length(false)) / maxOf(1, transactions.size))
                        )
                    }

                    if (percentage >= 80) {
                        item {
                            InfoCard("Budget Alert", "You have spent ${percentage}% of this budget")
                        }
                    }

                    if (percentage >= 100) {
                        item {
                            InfoCard("Critical Alert", "Budget is over limit")
                        }
                    }

                    item {
                        Text(text = "Transactions", style = MaterialTheme.typography.titleLarge)
                    }

                    if (transactions.isEmpty()) {
                        item {
                            Text(
                                text = "No transactions in this budget yet.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(transactions) { transaction ->
                            TransactionListItem(transaction = transaction, onClick = { onTransactionClick(transaction.id) })
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onEditBudget(currentBudget.id) }) { Text("Edit budget") }
                            OutlinedButton(onClick = { showDeleteDialog = true }) { Text("Delete budget") }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete budget?") },
            text = { Text("Delete this budget and remove progress tracking?") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        viewModel.deleteBudget()
                        snackbarHostState.showSnackbar("Budget deleted")
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
private fun BudgetSummaryCard(
    budget: com.example.smartpesa.data.local.entity.Budget,
    spent: Double,
    remaining: Double,
    percentage: Int,
    progressColor: androidx.compose.ui.graphics.Color
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = budget.category.ifBlank { "Budget" }, style = MaterialTheme.typography.titleLarge)
            Text(text = "${CurrencyFormatter.format(spent)} / ${CurrencyFormatter.format(budget.limit)}", color = progressColor)
            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor
            )
            Text(text = "Remaining ${CurrencyFormatter.format(remaining.coerceAtLeast(0.0))}")
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
