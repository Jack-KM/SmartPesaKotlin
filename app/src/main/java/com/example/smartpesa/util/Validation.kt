package com.example.smartpesa.util

import java.time.LocalDateTime

object Validation {

    fun amountError(value: String, maxAmount: Double = 10_000_000.0): String? {
        val amount = value.toDoubleOrNull() ?: return "Enter valid amount"
        if (amount <= 0) return "Amount must be positive"
        if (amount > maxAmount) return "Amount must be below ${CurrencyFormatter.format(maxAmount)}"
        if (!value.matches(Regex("^\\d+(\\.\\d{1,2})?$"))) return "Use at most 2 decimal places"
        return null
    }

    fun dateError(value: LocalDateTime, allowFuture: Boolean = false): String? {
        if (!allowFuture && value.isAfter(LocalDateTime.now())) return "Date cannot be in future"
        if (value.year < 2000) return "Date is too old"
        return null
    }
}
