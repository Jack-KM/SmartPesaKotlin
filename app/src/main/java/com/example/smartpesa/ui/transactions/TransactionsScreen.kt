package com.example.smartpesa.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

private val ScreenBackground = Color(0xFF0F1216)
private val CardSurface = Color(0xFF1B2027)
private val DividerTint = Color(0x14FFFFFF)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFF9AA3AD)
private val ExpenseAccent = Color(0xFFF2B8C0)
private val IncomeAccent = Color(0xFF7ED9A4)
private val ActiveAccent = Color(0xFF7ED9A4)
private val ChipBorder = Color(0x22FFFFFF)
private val ActionRed = Color(0xFFE57373)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onEditTransaction: (Long) -> Unit = {},
    onClipboardImport: (String) -> Unit = {}
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val chipListState = rememberLazyListState()
    val showEmpty = filteredTransactions.isEmpty()

    Scaffold(
        containerColor = ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopAppBar(
                containerColor = ScreenBackground,
                titleContentColor = PrimaryText,
                navigationIconContentColor = PrimaryText,
                actionIconContentColor = PrimaryText,
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboardText = clipboardManager.getText()?.text?.trim().orEmpty()
                        if (clipboardText.isNotBlank()) {
                            onClipboardImport(clipboardText)
                        } else {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Clipboard empty") }
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Import clipboard")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onClear = { viewModel.onSearchQueryChanged("") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = viewModel::onFilterSelected,
                state = chipListState,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = ActiveAccent) }
                }

                showEmpty -> {
                    EmptyResultsState(
                        hasActiveFilters = searchQuery.isNotBlank() || selectedFilter != TransactionFilter.ALL || selectedAccount != null,
                        onClearFilters = viewModel::clearFilters
                    )
                }

                else -> {
                    TransactionList(
                        transactions = filteredTransactions,
                        modifier = Modifier.fillMaxSize(),
                        onTransactionClick = onTransactionClick,
                        onEditTransaction = onEditTransaction,
                        onDeleteTransaction = { transaction ->
                            coroutineScope.launch {
                                viewModel.deleteTransaction(transaction)
                                snackbarHostState.showSnackbar("Transaction deleted")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = { Text("Search by name or M-Pesa code") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CardSurface,
            unfocusedContainerColor = CardSurface,
            disabledContainerColor = CardSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = PrimaryText,
            unfocusedTextColor = PrimaryText,
            focusedPlaceholderColor = SecondaryText,
            unfocusedPlaceholderColor = SecondaryText,
            focusedLeadingIconColor = SecondaryText,
            unfocusedLeadingIconColor = SecondaryText,
            focusedTrailingIconColor = SecondaryText,
            unfocusedTrailingIconColor = SecondaryText,
            cursorColor = ActiveAccent
        )
    )
}

@Composable
private fun FilterChipsRow(
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val startFade = state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
    val endFade = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 < TransactionFilter.entries.lastIndex || state.canScrollForward

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .drawWithContent {
                drawContent()
                val fadeWidth = 24.dp.toPx()
                if (startFade) {
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(ScreenBackground, Color.Transparent)),
                        topLeft = Offset.Zero,
                        size = Size(fadeWidth, size.height)
                    )
                }
                if (endFade) {
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, ScreenBackground)),
                        topLeft = Offset(size.width - fadeWidth, 0f),
                        size = Size(fadeWidth, size.height)
                    )
                }
            }
    ) {
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(TransactionFilter.entries.toTypedArray()) { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CardSurface,
                        labelColor = PrimaryText,
                        iconColor = SecondaryText,
                        selectedContainerColor = ActiveAccent.copy(alpha = 0.18f),
                        selectedLabelColor = ActiveAccent,
                        selectedLeadingIconColor = ActiveAccent,
                        selectedTrailingIconColor = ActiveAccent
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filter == selectedFilter,
                        borderColor = if (filter == selectedFilter) ActiveAccent.copy(alpha = 0.7f) else ChipBorder,
                        selectedBorderColor = ActiveAccent.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionList(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    onTransactionClick: (Long) -> Unit = {},
    onEditTransaction: (Long) -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit
) {
    val groupedTransactions = transactions.groupBy { it.timestamp.toLocalDate() }
    val sortedDates = groupedTransactions.keys.sortedDescending()
    val listState = rememberLazyListState()
    var openedTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { isScrolling ->
            if (isScrolling) openedTransactionId = null
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortedDates.forEach { date ->
            val transactionsForDate = groupedTransactions[date].orEmpty()
            val dayTotal = transactionsForDate.sumOf { transaction ->
                val total = transaction.amount + transaction.feeAmount
                if (transaction.type == TransactionType.INCOME) total else -total
            }

            stickyHeader(key = "header_$date") {
                DayHeader(date = date, dayTotal = dayTotal)
            }

            items(transactionsForDate, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    isOpen = openedTransactionId == transaction.id,
                    onOpenChange = { isOpen -> openedTransactionId = if (isOpen) transaction.id else null },
                    onClick = {
                        openedTransactionId = null
                        onTransactionClick(transaction.id)
                    },
                    onEdit = {
                        openedTransactionId = null
                        onEditTransaction(transaction.id)
                    },
                    onDelete = {
                        openedTransactionId = null
                        onDeleteTransaction(transaction)
                    }
                )
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, dayTotal: Double) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val label = when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }
    val accent = if (dayTotal >= 0) IncomeAccent else ExpenseAccent
    val totalText = "${if (dayTotal >= 0) "+" else "-"}${CurrencyFormatter.format(abs(dayTotal))}"

    Surface(
        color = ScreenBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = PrimaryText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = totalText, color = accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val categoryName = transaction.category.ifBlank { "Uncategorized" }
    val description = transaction.description
    val fundingAccount = resolveFundingAccount(transaction.counterparty, transaction.source)

    val amountColor = if (isIncome) {
        Color(0xFF10B981)
    } else {
        Color(0xFFEF4444)
    }

    val stripeColor = if (isIncome) {
        Color(0xFF10B981).copy(alpha = 0.8f)
    } else {
        Color(0xFFEF4444).copy(alpha = 0.8f)
    }

    val categoryColor = getCategoryColor(categoryName)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val formattedDate = transaction.timestamp.format(dateFormatter)

    val density = LocalDensity.current
    val revealWidthPx = with(density) { 104.dp.toPx() }
    var offsetX by rememberSaveable(transaction.id) { mutableStateOf(0f) }

    LaunchedEffect(isOpen, revealWidthPx) {
        offsetX = if (isOpen) -revealWidthPx else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ActionButton(icon = Icons.Default.Edit, tint = Color(0xFF10B981), onClick = { onOpenChange(false); onEdit() })
            Spacer(modifier = Modifier.size(8.dp))
            ActionButton(icon = Icons.Default.Delete, tint = Color(0xFFEF4444), onClick = { onOpenChange(false); onDelete() })
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(revealWidthPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-revealWidthPx, 0f)
                            if (offsetX == 0f) onOpenChange(false)
                        },
                        onDragEnd = {
                            val shouldOpen = offsetX < -(revealWidthPx * 0.45f)
                            offsetX = if (shouldOpen) -revealWidthPx else 0f
                            onOpenChange(shouldOpen)
                        }
                    )
                }
                .clickable {
                    if (offsetX < 0f) {
                        offsetX = 0f
                        onOpenChange(false)
                    } else {
                        onClick()
                    }
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(96.dp)
                        .background(stripeColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = categoryColor.copy(alpha = 0.12f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(categoryName),
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = categoryColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(categoryName),
                                        contentDescription = null,
                                        tint = categoryColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = categoryColor
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = fundingAccount,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.format(transaction.amount + transaction.feeAmount)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = amountColor
                        )

                        if (transaction.feeAmount > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "fee ${CurrencyFormatter.format(transaction.feeAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.16f),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint)
        }
    }
}

@Composable
private fun EmptyResultsState(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasActiveFilters) "No Matching Transactions" else "No Transactions",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasActiveFilters) {
                "No transactions match your search or filter. Try adjusting your criteria."
            } else {
                "You don't have any transactions yet. Start by importing an M-Pesa message."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = SecondaryText
        )
        if (hasActiveFilters) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onClearFilters) {
                Text("Clear filters")
            }
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    val normalized = category.lowercase()

    return when {
        normalized.contains("food") || normalized.contains("restaurant") || normalized.contains("takeout") -> Icons.Default.Restaurant
        normalized.contains("transport") || normalized.contains("matatu") || normalized.contains("boda") || normalized.contains("uber") -> Icons.Default.DirectionsBus
        normalized.contains("bill") || normalized.contains("utility") || normalized.contains("rent") || normalized.contains("water") || normalized.contains("internet") || normalized.contains("electricity") -> Icons.Default.ReceiptLong
        normalized.contains("shopping") || normalized.contains("clothing") || normalized.contains("electronics") -> Icons.Default.ShoppingBag
        normalized.contains("health") || normalized.contains("medical") || normalized.contains("pharmacy") -> Icons.Default.LocalHospital
        normalized.contains("education") || normalized.contains("school") || normalized.contains("books") || normalized.contains("courses") -> Icons.Default.School
        normalized.contains("work") || normalized.contains("salary") || normalized.contains("business") || normalized.contains("freelance") -> Icons.Default.Work
        normalized.contains("gift") -> Icons.Default.CardGiftcard
        normalized.contains("saving") || normalized.contains("invest") -> Icons.Default.Savings
        normalized.contains("loan") || normalized.contains("interest") -> Icons.Default.AccountBalanceWallet
        normalized.contains("airtime") || normalized.contains("kplc") || normalized.contains("token") -> Icons.Default.ElectricBolt
        normalized.contains("income") || normalized.contains("deposit") -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.Category
    }
}

private fun getCategoryColor(category: String): Color {
    val normalized = category.lowercase()

    return when {
        normalized.contains("food") || normalized.contains("restaurant") || normalized.contains("takeout") || normalized.contains("market") -> Color(0xFF10B981)
        normalized.contains("transport") || normalized.contains("matatu") || normalized.contains("boda") || normalized.contains("uber") || normalized.contains("fuel") || normalized.contains("parking") -> Color(0xFF3B82F6)
        normalized.contains("bill") || normalized.contains("utility") || normalized.contains("rent") || normalized.contains("water") || normalized.contains("internet") || normalized.contains("electricity") || normalized.contains("airtime") -> Color(0xFFF59E0B)
        normalized.contains("shopping") || normalized.contains("clothing") || normalized.contains("electronics") || normalized.contains("personal care") || normalized.contains("household") -> Color(0xFFA855F7)
        normalized.contains("health") || normalized.contains("medical") || normalized.contains("pharmacy") || normalized.contains("insurance") -> Color(0xFFEF4444)
        normalized.contains("entertainment") || normalized.contains("movies") || normalized.contains("events") || normalized.contains("hobbies") || normalized.contains("sports") -> Color(0xFFEC4899)
        normalized.contains("education") || normalized.contains("school") || normalized.contains("books") || normalized.contains("courses") || normalized.contains("stationery") -> Color(0xFF14B8A6)
        normalized.contains("personal") || normalized.contains("family") || normalized.contains("childcare") || normalized.contains("gifts") || normalized.contains("development") -> Color(0xFF8B5CF6)
        normalized.contains("loan") || normalized.contains("interest") -> Color(0xFF1E40AF)
        normalized.contains("income") || normalized.contains("salary") || normalized.contains("business") || normalized.contains("freelance") || normalized.contains("invest") -> Color(0xFF10B981)
        else -> Color(0xFF64748B)
    }
}

private fun resolveFundingAccount(counterparty: String, source: String): String {
    val accountText = counterparty.trim().ifBlank { source.trim() }
    if (accountText.isBlank()) return "Manual"

    val parts = accountText.split("→", limit = 2).map { it.trim() }
    return if (parts.size == 2) parts.first().ifBlank { accountText } else accountText
}
