package com.example.smartpesa.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.ui.theme.ExpenseRed
import com.example.smartpesa.ui.theme.IncomeGreen
import com.example.smartpesa.util.CurrencyFormatter
import com.example.smartpesa.util.RelativeTimeFormatter

/**
 * Reusable transaction list item component.
 * Used in the Home screen (recent list) and the Transactions tab.
 *
 * @param transaction The transaction to display
 * @param onClick Optional click handler (null renders a non-clickable card)
 */
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: (() -> Unit)? = null
) {
    val shape = MaterialTheme.shapes.medium
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            TransactionListItemContent(transaction)
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            TransactionListItemContent(transaction)
        }
    }
}

@Composable
private fun TransactionListItemContent(transaction: Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed

    val icon = getTransactionIcon(transaction)
    val relativeTime = RelativeTimeFormatter.format(transaction.timestamp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon inside a softly tinted circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        // Middle: description + metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = buildString {
                    append(relativeTime)
                    transaction.mpesaCode?.let { append("  \u00b7  $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Right: amount + optional fee
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = accentColor
            )

            if (transaction.feeAmount > 0) {
                Text(
                    text = "Fee ${CurrencyFormatter.format(transaction.feeAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Get icon for transaction based on its properties.
 * Uses description keywords and source to determine appropriate icon.
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
