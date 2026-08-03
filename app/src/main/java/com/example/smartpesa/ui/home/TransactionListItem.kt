package com.example.smartpesa.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.util.CurrencyFormatter
import com.example.smartpesa.util.RelativeTimeFormatter

/**
 * Reusable transaction list item component
 * Used in Home screen (recent list) and will be reused in Transactions tab
 *
 * @param transaction The transaction to display
 * @param onClick Optional click handler
 */
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: (() -> Unit)? = null
) {
    val isIncome = transaction.type == TransactionType.INCOME

    val amountColor = if (isIncome) {
        MaterialTheme.colorScheme.tertiary // Green for income
    } else {
        MaterialTheme.colorScheme.error // Red for expense
    }

    val icon = getTransactionIcon(transaction)
    val relativeTime = RelativeTimeFormatter.format(transaction.timestamp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon and description
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Amount
            Text(
                text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor
            )
        }
    }
}

/**
 * Get icon for transaction based on its properties
 * Uses description keywords and source to determine appropriate icon
 */
private fun getTransactionIcon(transaction: Transaction): ImageVector {
    val desc = transaction.description.lowercase()

    return when {
        desc.contains("received") || desc.contains("from") -> Icons.Default.CallReceived
        desc.contains("sent") || desc.contains("to") -> Icons.Default.CallMade
        desc.contains("withdrawal") || desc.contains("withdraw") -> Icons.Default.LocalAtm
        desc.contains("airtime") -> Icons.Default.Phone
        desc.contains("deposit") -> Icons.Default.AccountBalance
        desc.contains("token") || desc.contains("kplc") -> Icons.Default.ElectricBolt
        desc.contains("fuliza") -> Icons.Default.CreditCard
        transaction.type == TransactionType.INCOME -> Icons.Default.TrendingUp
        transaction.type == TransactionType.EXPENSE -> Icons.Default.TrendingDown
        else -> Icons.Default.SwapHoriz
    }
}
