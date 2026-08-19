package com.example.smartpesa.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Currency formatter for SmartPesa
 * Always formats as "KES " prefix, showing decimals only when present
 * (e.g. "KES 1,234" or "KES 1,095.06").
 */
object CurrencyFormatter {

    private val formatter = NumberFormat.getInstance(Locale("en", "KE")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    /**
     * Format amount as KES currency string
     * @param amount The amount in KES (can be Double or Long)
     * @return Formatted string like "KES 1,234"
     */
    fun format(amount: Double): String {
        return "KES ${formatter.format(amount)}"
    }

    fun format(amount: Long): String {
        return "KES ${formatter.format(amount)}"
    }
}
