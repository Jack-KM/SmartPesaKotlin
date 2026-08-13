package com.example.smartpesa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import com.example.smartpesa.data.local.entity.FulizaRepayment
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.ui.components.EmptyStateScreen
import com.example.smartpesa.ui.components.InfoCard
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.util.CurrencyFormatter
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(onCreateFirstItem: () -> Unit = {}, onOpenLoanDetails: (Long) -> Unit = {}) {
    Scaffold(topBar = { CompactTopAppBar(title = { Text("Loans") }) }) { paddingValues ->
        EmptyStateScreen(
            modifier = Modifier.padding(paddingValues),
            title = "No loans recorded",
            message = "Track borrowed or lent money here. Record a payment from transaction flow.",
            actionLabel = "Record first payment",
            onAction = onCreateFirstItem,
            icon = Icons.Filled.AccountBalanceWallet
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulizaScreen(
    viewModel: FulizaViewModel = hiltViewModel(),
    onCreateFirstItem: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { CompactTopAppBar(title = { Text("Fuliza M-PESA") }) }
    ) { paddingValues ->
        if (!uiState.hasData) {
            EmptyStateScreen(
                modifier = Modifier.padding(paddingValues),
                title = "No Fuliza activity",
                message = "When you use Fuliza M-PESA, your balance and access fees will appear here automatically.",
                actionLabel = "Add transaction",
                onAction = onCreateFirstItem,
                icon = Icons.Filled.CreditCard
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isPaidOff)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (uiState.isPaidOff) "Fuliza Cleared" else "Outstanding Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (uiState.isPaidOff)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = CurrencyFormatter.format(uiState.outstandingBalance),
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (uiState.isPaidOff)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (!uiState.isPaidOff && uiState.dueDate != null) {
                            Text(
                                text = "Due: ${uiState.dueDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // Access fees summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Access Fees Charged",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Auto-deducted on repayment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyFormatter.format(uiState.totalAccessFees),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (uiState.accessCharges.isNotEmpty()) {
                item {
                    Text(
                        text = "Fuliza charges",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(uiState.accessCharges, key = { it.timestamp.toString() }) { charge ->
                    FulizaAccessChargeRow(charge)
                }
            }

            // Repayment history
            if (uiState.repaymentHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Repayment History",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(uiState.repaymentHistory, key = { it.timestamp.toString() }) { repayment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = repayment.timestamp.toLocalDate().format(
                                        java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = repayment.timestamp.toLocalTime().toString().take(5),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "- ${CurrencyFormatter.format(repayment.amount)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FulizaAccessChargeRow(charge: FulizaAccessFeeEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = charge.timestamp.toLocalDate().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${charge.title} · ${charge.timestamp.toLocalTime().toString().take(5)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.format(charge.amount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCostsScreen(
    viewModel: TransactionCostsViewModel = hiltViewModel(),
    onCreateFirstItem: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    Scaffold(
        topBar = {
            CompactTopAppBar(
                title = { Text("Transaction Costs") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!uiState.hasAnyFees) {
            EmptyStateScreen(
                modifier = Modifier.padding(paddingValues),
                title = "No transaction fees recorded",
                message = "Fees from M-Pesa and other transactions will appear here automatically.",
                actionLabel = "Add transaction",
                onAction = onCreateFirstItem,
                icon = Icons.Filled.ReceiptLong
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectPreviousMonth() }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    text = selectedMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { viewModel.selectNextMonth() }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
            }

            // Month total
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total fees",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = CurrencyFormatter.format(uiState.monthTotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (uiState.dayGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No fees for ${selectedMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.dayGroups.forEach { day ->
                        item(key = "header_${day.date}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = day.date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(day.totalFee),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        items(day.items, key = { "entry_${it.id}" }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${entry.category} · ${entry.time}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.format(entry.feeAmount),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.error
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBackPressed: () -> Unit) {
    Scaffold(topBar = { CompactTopAppBar(title = { Text("Reports") }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard("Reports Coming Soon", "Monthly income vs expenses, category breakdown, spending trends, and top expense categories are planned.")
            Text(text = "Planned report types", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            listOf(
                "Monthly income vs expenses chart",
                "Category breakdown chart",
                "Spending trends line chart",
                "Top expense categories",
                "Budget adherence report",
                "Cash flow projection"
            ).forEach { item ->
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(text = item, modifier = Modifier.padding(16.dp))
                }
            }
            OutlinedButton(onClick = onBackPressed) { Text("Back") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { CompactTopAppBar(title = { Text("About") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoCard("SmartPesa", "Version 1.0.0")
            }
            item {
                InfoCard("Developer", "OpenAI Codex build for SmartPesa")
            }
            item {
                InfoCard("Privacy Policy", "Add app privacy policy link here.")
            }
            item {
                InfoCard("Terms of Service", "Add app terms link here.")
            }
            item {
                InfoCard("Contact Support", "support@smartpesa.app")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Try SmartPesa for tracking M-Pesa transactions."
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share app"))
                    }) { Text("Share app") }
                    OutlinedButton(onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@smartpesa.app")
                        }
                        context.startActivity(emailIntent)
                    }) { Text("Contact support") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Open source licenses screen not wired yet") }
                    }) { Text("Open source licenses") }
                    OutlinedButton(onClick = onBackPressed) { Text("Back") }
                }
            }
        }
    }
}
