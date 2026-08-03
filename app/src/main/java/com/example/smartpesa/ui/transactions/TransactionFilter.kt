package com.example.smartpesa.ui.transactions

/**
 * Filter options for transaction list
 * Used in filter chips on Transactions screen
 */
enum class TransactionFilter(val label: String) {
    ALL("All"),
    SEND("Send"),
    RECEIVE("Receive"),
    PAYBILL("Paybill"),
    BUY_GOODS("Buy Goods"),
    WITHDRAWAL("Withdrawal"),
    AIRTIME("Airtime");

    /**
     * Check if a transaction description matches this filter
     * Uses keywords from descriptions built in SmsProcessingWorker
     */
    fun matches(description: String): Boolean {
        return when (this) {
            ALL -> true
            SEND -> description.contains("Sent to", ignoreCase = true)
            RECEIVE -> description.contains("Received from", ignoreCase = true)
            PAYBILL -> description.contains("Paid to", ignoreCase = true)
            BUY_GOODS -> description.contains("Bought from", ignoreCase = true)
            WITHDRAWAL -> description.contains("Withdrawal", ignoreCase = true)
            AIRTIME -> description.contains("Airtime", ignoreCase = true)
        }
    }
}
