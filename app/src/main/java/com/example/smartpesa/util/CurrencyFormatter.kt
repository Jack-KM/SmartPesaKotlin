package com.example.smartpesa.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Currency formatter for SmartPesa
 * Always formats as "KES " prefix with zero decimal digits
 */
object CurrencyFormatter {

    private val formatter = NumberFormat.getInstance(Locale("en", "KE")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
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
