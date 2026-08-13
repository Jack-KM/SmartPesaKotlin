package com.example.smartpesa.ui.navigation

import android.net.Uri

/**
 * Sealed class representing navigation destinations
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Transactions : Screen("transactions")
    data object Budget : Screen("budget")
    data object Accounts : Screen("accounts")
    data object AccountDetails : Screen("accounts/{accountId}")
    data object Categories : Screen("categories")
    data object Loans : Screen("loans")
    data object Fuliza : Screen("fuliza")
    data object TransactionCosts : Screen("transaction_costs")
    data object Reports : Screen("reports")
    data object About : Screen("about")
    data object Settings : Screen("settings")
    data object AddTransaction : Screen("add_transaction")
    data object TransactionDetails : Screen("transaction/{transactionId}")
    data object BudgetDetails : Screen("budget/{budgetId}")
    data object LoanDetails : Screen("loan/{loanId}")
    data object WorkAccount : Screen("work_account")

    data object SmsPermission : Screen("sms_permission")
}

fun transactionDetailsRoute(transactionId: Long): String = "transaction/$transactionId"

fun addTransactionRoute(transactionId: Long? = null, message: String? = null): String {
    val queryParts = buildList {
        transactionId?.let { add("transactionId=$it") }
        message?.takeIf { it.isNotBlank() }?.let { add("message=${Uri.encode(it)}") }
    }
    return if (queryParts.isEmpty()) {
        Screen.AddTransaction.route
    } else {
        "${Screen.AddTransaction.route}?${queryParts.joinToString("&")}"
    }
}

fun transactionsRoute(account: String? = null): String =
    account?.takeIf { it.isNotBlank() }
        ?.let { "${Screen.Transactions.route}?account=${Uri.encode(it)}" }
        ?: Screen.Transactions.route

fun budgetDetailsRoute(budgetId: Long): String = "budget/$budgetId"

fun loanDetailsRoute(loanId: Long): String = "loan/$loanId"

fun accountDetailsRoute(accountId: Long): String = "accounts/$accountId"
