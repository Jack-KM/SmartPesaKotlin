package com.example.smartpesa.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

object CurrencyFormatter {
    private val currencyCode = AtomicReference("KES")
    private val formatter = NumberFormat.getInstance(Locale("en", "KE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun setCurrencyCode(code: String) {
        currencyCode.set(code.trim().ifBlank { "KES" }.uppercase(Locale.ROOT))
    }

    fun format(amount: Double): String = "${currencyCode.get()} ${formatter.format(amount)}"
    fun format(amount: Long): String = "${currencyCode.get()} ${formatter.format(amount)}"
}

object DateFormatFormatter {
    private val pattern = AtomicReference("dd MMM yyyy")

    fun setPattern(value: String) {
        pattern.set(value.trim().ifBlank { "dd MMM yyyy" })
    }

    fun formatDateTime(value: LocalDateTime): String = value.format(DateTimeFormatter.ofPattern(pattern.get() + ", HH:mm"))
    fun formatDate(value: LocalDate): String = value.format(DateTimeFormatter.ofPattern(pattern.get()))
    fun formatTime(value: LocalTime): String = value.format(DateTimeFormatter.ofPattern("HH:mm"))
}
