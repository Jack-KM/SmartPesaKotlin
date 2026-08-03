package com.example.smartpesa.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.ui.home.TransactionListItem
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Transactions screen with full list, search, and filter
 * Reuses TransactionListItem from Home slice
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onClear = { viewModel.onSearchQueryChanged("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter chips
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = viewModel::onFilterSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Transaction list or empty state
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                filteredTransactions.isEmpty() -> {
                    EmptyResultsState(
                        hasActiveFilters = searchQuery.isNotBlank() || selectedFilter != TransactionFilter.ALL,
                        onClearFilters = viewModel::clearFilters
                    )
                }

                else -> {
                    TransactionList(
                        transactions = filteredTransactions,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Search bar component
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search by name or M-Pesa code...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors()
    )
}

/**
 * Filter chips row
 */
@Composable
private fun FilterChipsRow(
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TransactionFilter.entries.toTypedArray()) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

/**
 * Transaction list grouped by date
 */
@Composable
private fun TransactionList(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    // Group transactions by date
    val groupedTransactions = transactions.groupBy { transaction ->
        transaction.timestamp.toLocalDate()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedTransactions.forEach { (date, transactionsForDate) ->
            // Date section header
            item(key = "header_$date") {
                DateSectionHeader(date = date)
            }

            // Transactions for this date
            items(
                items = transactionsForDate,
                key = { it.id }
            ) { transaction ->
                TransactionListItem(transaction = transaction)
            }
        }
    }
}

/**
 * Date section header
 * Shows "Today", "Yesterday", or formatted date
 */
@Composable
private fun DateSectionHeader(date: LocalDate) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val headerText = when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }

    Text(
        text = headerText,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Empty state when no results match filters
 */
@Composable
private fun EmptyResultsState(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasActiveFilters) "No Matching Transactions" else "No Transactions",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasActiveFilters)
                "No transactions match your search or filter. Try adjusting your criteria."
            else
                "You don't have any transactions yet. Start by capturing M-Pesa SMS messages.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasActiveFilters) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = onClearFilters) {
                Text("Clear Filters")
            }
        }
    }
}

/**
 * Extension to convert LocalDateTime to LocalDate
 */
private fun LocalDateTime.toLocalDate(): LocalDate = this.toLocalDate()
