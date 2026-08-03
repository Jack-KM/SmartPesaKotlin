package com.example.smartpesa.util

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utility for formatting timestamps as relative time strings
 * Examples: "Just now", "5m ago", "2h ago", "Yesterday", "3 days ago", "12 Jul"
 */
object RelativeTimeFormatter {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM")

    /**
     * Format a timestamp as a relative time string
     * @param timestamp The timestamp to format
     * @param now Current time (defaults to now, can be overridden for testing)
     */
    fun format(timestamp: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
        val duration = Duration.between(timestamp, now)
        val seconds = duration.seconds
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            else -> timestamp.format(dateFormatter)
        }
    }
}
