package com.example.smartpesa.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.util.CurrencyFormatter
import java.time.format.DateTimeFormatter

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: (() -> Unit)? = null
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val categoryName = transaction.category.ifBlank { "Uncategorized" }
    val description = transaction.description
    val fundingAccount = resolveFundingAccount(transaction.counterparty, transaction.source)
    val totalAmount = transaction.amount + transaction.feeAmount

    val amountColor = if (isIncome) {
        Color(0xFF10B981) // Modern green for income
    } else {
        Color(0xFFEF4444) // Modern red for expense
    }

    val stripeColor = if (isIncome) {
        Color(0xFF10B981).copy(alpha = 0.8f)
    } else {
        Color(0xFFEF4444).copy(alpha = 0.8f)
    }

    val categoryColor = getCategoryColor(categoryName)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val formattedDate = transaction.timestamp.format(dateFormatter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored stripe indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(96.dp)
                    .background(stripeColor)
            )

            // Main content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left section: Icon + Info
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon in colored circle
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

                    // Transaction details
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Description (main text)
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        // Category badge
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

                        // Date and source
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Lightning icon for notification-sourced transactions
                            if (transaction.source != "Manual") {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFBBF24).copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = "From notification",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .size(12.dp)
                                    )
                                }
                            }

                            // Sparkle icon for auto-categorized transactions
                            if (transaction.isAutoCategorized) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Auto-categorized",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .size(12.dp)
                                    )
                                }
                            }

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

                // Right section: Amount
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Main amount
                    Text(
                        text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.format(totalAmount)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = amountColor
                    )

                    // Fee if applicable
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
