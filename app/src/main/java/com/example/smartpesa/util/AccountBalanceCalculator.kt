package com.example.smartpesa.util

import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType

fun Transaction.balanceImpactFor(accountName: String): Double {
    val target = accountName.trim()
    if (target.isBlank()) return 0.0

    if (this.accountName?.trim()?.equals(target, ignoreCase = true) == true) {
        return when (type) {
            TransactionType.INCOME -> amount - feeAmount
            TransactionType.EXPENSE -> -(amount + feeAmount)
        }
    }

    val counterpartyText = counterparty.trim()
    val parts = counterpartyText.split("→", limit = 2).map { it.trim() }
    return when {
        parts.size == 2 && parts[0].equals(target, ignoreCase = true) -> -(amount + feeAmount)
        parts.size == 2 && parts[1].equals(target, ignoreCase = true) -> amount - feeAmount
        counterpartyText.equals(target, ignoreCase = true) -> when (type) {
            TransactionType.INCOME -> amount - feeAmount
            TransactionType.EXPENSE -> -(amount + feeAmount)
        }
        else -> 0.0
    }
}

fun Transaction.isLinkedToAccount(accountName: String): Boolean = balanceImpactFor(accountName) != 0.0
