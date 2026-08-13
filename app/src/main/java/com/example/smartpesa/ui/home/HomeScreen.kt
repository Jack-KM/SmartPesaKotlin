package com.example.smartpesa.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.util.CurrencyFormatter
import java.time.LocalDateTime
import kotlin.math.abs

private data class DashboardAccount(
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

private val dashboardAccounts = listOf(
    DashboardAccount(
        name = "M-Pesa",
        subtitle = "Default account",
        icon = Icons.Default.PhoneAndroid,
        color = Color(0xFF00A98F)
    ),
    DashboardAccount(
        name = "Cash",
        subtitle = "Wallet ready",
        icon = Icons.Default.AccountBalanceWallet,
        color = Color(0xFF6D4C41)
    ),
    DashboardAccount(
        name = "Airtel Money",
        subtitle = "Default account",
        icon = Icons.Default.ReceiptLong,
        color = Color(0xFFD32F2F)
    )
)

private val chartColors = listOf(
    Color(0xFF6C63FF),
    Color(0xFF00BFA6),
    Color(0xFFFF8A65),
    Color(0xFFFFB300),
    Color(0xFF42A5F5)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToPermissions: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onAccountClick: (String) -> Unit = {}
) {
    val displayName by viewModel.displayName.collectAsState()
    val monthlyOverview by viewModel.monthlyOverview.collectAsState()
    val previousMonthlyOverview by viewModel.previousMonthlyOverview.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val spendingCategories by viewModel.spendingCategories.collectAsState()
    val accountBalances by viewModel.accountBalances.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val dailyTip by viewModel.dailyTip.collectAsState()
    val currentMonthName = viewModel.getCurrentMonthName()
    val previousMonthName = viewModel.getPreviousMonthName()
    val now = LocalDateTime.now()
    val greeting = greetingForHour(now.hour)
    val greetingIcon = timeOfDayIcon(now.hour)
    var nameInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(displayName) {
        displayName ?: return@LaunchedEffect
        if (displayName.isNullOrBlank()) {
            nameInput = ""
        } else {
            nameInput = displayName.orEmpty()
        }
    }

    if (displayName == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    if (displayName.isNullOrBlank()) {
        NamePromptDialog(
            value = nameInput,
            onValueChange = { nameInput = it },
            onSave = {
                val trimmedName = nameInput.trim()
                if (trimmedName.isNotBlank()) viewModel.saveDisplayName(trimmedName)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            CompactTopAppBar(
                title = { Text("Homepage") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$greeting, ${displayName?.ifBlank { "Friend" } ?: "Friend"}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentMonthName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Icon(
                            imageVector = greetingIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp).size(28.dp)
                        )
                    }
                }
            }

            item {
                DailyTipCard(tip = dailyTip)
            }

            item {
                BalanceCard(
                    currentMonthName = currentMonthName,
                    previousMonthName = previousMonthName,
                    currentOverview = monthlyOverview,
                    previousOverview = previousMonthlyOverview,
                    totalBalance = totalBalance
                )
            }

            item {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(dashboardAccounts) { account ->
                        AccountCard(
                            account = account,
                            balance = accountBalances[account.name] ?: 0.0,
                            onClick = { onAccountClick(account.name) }
                        )
                    }
                }
            }

            item {
                SpendingChartCard(categories = spendingCategories)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Max 7",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    EmptyTransactionsCard(onNavigateToPermissions = onNavigateToPermissions)
                }
            } else {
                items(recentTransactions) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTipCard(tip: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Daily tip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(
    currentMonthName: String,
    previousMonthName: String,
    currentOverview: MonthlyOverview,
    previousOverview: MonthlyOverview,
    totalBalance: Double
) {
    val balanceChange = percentChange(currentOverview.net, previousOverview.net)
    val balanceChangeColor = when {
        balanceChange == null -> MaterialTheme.colorScheme.onPrimaryContainer
        balanceChange >= 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val balanceChangeText = when {
        balanceChange == null && currentOverview.net == 0.0 -> "No change vs $previousMonthName"
        balanceChange == null -> "New vs $previousMonthName"
        balanceChange >= 0 -> "↑ ${formatPercent(balanceChange)} vs $previousMonthName"
        else -> "↓ ${formatPercent(abs(balanceChange))} vs $previousMonthName"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = currentMonthName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                    Text(
                        text = "Total balance",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
                ) {
                    Text(
                        text = balanceChangeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = balanceChangeColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Text(
                text = CurrencyFormatter.format(totalBalance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Income and expense this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    label = "Total income",
                    value = CurrencyFormatter.format(currentOverview.totalReceived),
                    modifier = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.tertiary
                )
                StatTile(
                    label = "Total expense",
                    value = CurrencyFormatter.format(currentOverview.totalSpent),
                    modifier = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: DashboardAccount,
    balance: Double,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = account.color.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = account.color.copy(alpha = 0.16f)
            ) {
                Icon(
                    imageVector = account.icon,
                    contentDescription = null,
                    tint = account.color,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = CurrencyFormatter.format(balance),
                    style = MaterialTheme.typography.titleSmall,
                    color = account.color,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = account.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpendingChartCard(categories: List<SpendingCategorySummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Spending by category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (categories.isEmpty()) {
                Text(
                    text = "No spending recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val maxAmount = categories.maxOfOrNull { it.amount } ?: 0.0
                        categories.forEachIndexed { index, category ->
                            val color = chartColors[index % chartColors.size]
                            SpendingBarRow(
                                category = category,
                                color = color,
                                maxAmount = maxAmount
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        categories.forEachIndexed { index, category ->
                            val color = chartColors[index % chartColors.size]
                            CategoryLegendRow(
                                category = category,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingBarRow(
    category: SpendingCategorySummary,
    color: Color,
    maxAmount: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = CurrencyFormatter.format(category.amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(widthFraction(category.amount, maxAmount))
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun CategoryLegendRow(
    category: SpendingCategorySummary,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(color)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = category.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = CurrencyFormatter.format(category.amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyTransactionsCard(onNavigateToPermissions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No recent transactions yet.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Add your first transaction to start tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onNavigateToPermissions) {
                Text("Add transaction")
            }
        }
    }
}

@Composable
private fun NamePromptDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Set your name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "This name shows on homepage and settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onSave,
                    enabled = value.trim().isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save name")
                }
            }
        }
    }
}

@Composable
private fun widthFraction(amount: Double, maxAmount: Double): Float {
    if (maxAmount <= 0.0) return 0.0f
    return (amount / maxAmount).toFloat().coerceIn(0.08f, 1f)
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Good night"
}

private fun timeOfDayIcon(hour: Int): ImageVector = when (hour) {
    in 5..11 -> Icons.Default.Brightness7
    in 12..16 -> Icons.Default.Brightness5
    in 17..20 -> Icons.Default.Brightness3
    else -> Icons.Default.Brightness4
}

private fun percentChange(current: Double, previous: Double): Double? {
    if (previous == 0.0) return if (current == 0.0) 0.0 else null
    return ((current - previous) / abs(previous)) * 100.0
}

private fun formatPercent(value: Double): String = String.format("%.0f%%", value)
