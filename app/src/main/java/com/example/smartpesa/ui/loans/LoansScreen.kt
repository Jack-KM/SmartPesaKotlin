package com.example.smartpesa.ui.loans

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.Loan
import com.example.smartpesa.data.local.entity.LoanType
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.ui.components.EmptyStateScreen
import com.example.smartpesa.util.CurrencyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: LoansViewModel = hiltViewModel(),
    onCreateFirstItem: () -> Unit = {},
    onOpenLoanDetails: (Long) -> Unit = {}
) {
    val loans by viewModel.loans.collectAsState()

    Scaffold(topBar = { CompactTopAppBar(title = { Text("Loans") }) }) { paddingValues ->
        if (loans.isEmpty()) {
            EmptyStateScreen(
                modifier = Modifier.padding(paddingValues),
                title = "No loans recorded",
                message = "Track borrowed money, money given out, and loan interest here.",
                actionLabel = "Record first loan",
                onAction = onCreateFirstItem,
                icon = Icons.Filled.AccountBalanceWallet
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(text = "Loan balances", style = MaterialTheme.typography.titleLarge)
                }
                items(loans, key = { it.id }) { loan ->
                    LoanSummaryCard(
                        loan = loan,
                        onClick = { onOpenLoanDetails(loan.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanSummaryCard(
    loan: Loan,
    onClick: () -> Unit
) {
    val isBorrowed = loan.type == LoanType.BORROWED
    val percentPaid = if (loan.amount > 0.0) {
        (((loan.amount - loan.remainingBalance) / loan.amount) * 100).toInt()
    } else 0

    val totalPaid = loan.amount - loan.remainingBalance
    val progressFraction = if (loan.amount > 0.0) {
        (totalPaid / loan.amount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val stripeColor = if (isBorrowed) {
        Color(0xFFEF4444) // Red for borrowed (you owe)
    } else {
        Color(0xFF10B981) // Green for lent (owed to you)
    }

    val accentColor = if (isBorrowed) {
        Color(0xFFEF4444)
    } else {
        Color(0xFF10B981)
    }

    val icon = if (isBorrowed) Icons.Default.TrendingDown else Icons.Default.TrendingUp
    val typeLabel = if (isBorrowed) "Borrowed" else "Lent"

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
        daysUntilDue < 0 -> Color(0xFFEF4444) // Overdue - red
        daysUntilDue <= 3 -> Color(0xFFF59E0B) // Soon - orange
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored stripe indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(140.dp)
                    .background(stripeColor)
            )

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Icon + Type + Counterparty
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon in colored circle
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.12f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = loan.counterparty.ifBlank { typeLabel },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Amount information
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(loan.remainingBalance),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = accentColor
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Original",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(loan.amount),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Progress bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${percentPaid.coerceIn(0, 100)}% repaid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(totalPaid),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.12f)
                    )
                }

                // Due date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dueDateText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = dueDateColor
                    )

                    if (loan.interestRate > 0.0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${loan.interestRate}% interest",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
