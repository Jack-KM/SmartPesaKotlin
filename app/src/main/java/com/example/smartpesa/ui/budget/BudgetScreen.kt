package com.example.smartpesa.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.ui.theme.ExpenseRed
import com.example.smartpesa.ui.theme.IncomeGreen
import com.example.smartpesa.ui.theme.WarningAmber
import com.example.smartpesa.util.CurrencyFormatter
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

/**
 * Budget screen showing category-wise budget progress and insights
 * Replaces placeholder Budget tab with real data-driven budget tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgetProgress by viewModel.budgetProgress.collectAsState()
    val budgetInsights by viewModel.budgetInsights.collectAsState()
    val showAddBudgetDialog by viewModel.showAddBudgetDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Budget - ${getCurrentMonthName()}")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (budgetProgress.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.showAddBudgetDialog() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Budget")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                budgetProgress.isEmpty() -> {
                    EmptyBudgetState(
                        onAddBudget = { viewModel.showAddBudgetDialog() }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Budget progress section
                        item {
                            Text(
                                text = "Budget Progress",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(budgetProgress) { progress ->
                            BudgetProgressCard(progress = progress)
                        }

                        // Insights section
                        if (budgetInsights.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Insights",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(budgetInsights) { insight ->
                                BudgetInsightCard(insight = insight)
                            }
                        }
                    }
                }
            }
        }

        // Add budget dialog
        if (showAddBudgetDialog) {
            AddBudgetDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.hideAddBudgetDialog() }
            )
        }
    }
}

/**
 * Budget progress card showing category, spent/budgeted amounts, and progress bar
 */
@Composable
private fun BudgetProgressCard(progress: BudgetProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Category name + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                BudgetStatusBadge(percentage = progress.percentage)
            }

            // Spent / budgeted amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CurrencyFormatter.format(progress.spent)} of ${CurrencyFormatter.format(progress.budgeted)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${progress.percentage}% used",
                    style = MaterialTheme.typography.labelLarge,
                    color = getProgressColor(progress.percentage)
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (progress.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = getProgressColor(progress.percentage),
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        }
    }
}

/**
 * Small status badge summarizing budget health.
 */
@Composable
private fun BudgetStatusBadge(percentage: Int) {
    val (text, color) = when {
        percentage >= 100 -> "Over budget" to ExpenseRed
        percentage >= 70 -> "Near limit" to WarningAmber
        else -> "On track" to IncomeGreen
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Budget insight card showing data-driven suggestions
 */
@Composable
private fun BudgetInsightCard(insight: BudgetInsight) {
    val (icon, color) = when (insight.severity) {
        InsightSeverity.LOW -> Icons.Default.CheckCircle to IncomeGreen
        InsightSeverity.MEDIUM -> Icons.Default.Warning to WarningAmber
        InsightSeverity.HIGH -> Icons.Default.Error to ExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )

            Text(
                text = insight.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Empty state when no budgets are set
 */
@Composable
private fun EmptyBudgetState(
    onAddBudget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Budgets Set",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Set monthly budgets for your spending categories to track your progress and get insights.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddBudget) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Budget")
        }
    }
}

/**
 * Get progress bar color based on percentage
 * Green: <70%, Amber: 70-100%, Red: >100%
 */
private fun getProgressColor(percentage: Int): Color {
    return when {
        percentage < 70 -> IncomeGreen
        percentage < 100 -> WarningAmber
        else -> ExpenseRed
    }
}

/**
 * Get current month name
 */
private fun getCurrentMonthName(): String {
    val month = YearMonth.now().month
    return month.getDisplayName(TextStyle.FULL, Locale.getDefault())
}
