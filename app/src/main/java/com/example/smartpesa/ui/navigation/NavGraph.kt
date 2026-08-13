package com.example.smartpesa.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartpesa.ui.budget.BudgetScreen
import com.example.smartpesa.ui.budget.BudgetDetailsScreen
import com.example.smartpesa.ui.categories.CategoriesScreen
import com.example.smartpesa.ui.home.HomeScreen
import com.example.smartpesa.data.preferences.UserPreferencesRepository
import com.example.smartpesa.ui.settings.SettingsScreen
import com.example.smartpesa.ui.accounts.AccountsScreen
import com.example.smartpesa.ui.accounts.AccountDetailScreen
import com.example.smartpesa.ui.transactions.TransactionsScreen
import com.example.smartpesa.ui.transactions.AddTransactionScreen
import com.example.smartpesa.ui.transactions.TransactionDetailsScreen
import com.example.smartpesa.ui.permissions.SmsPermissionScreen
import com.example.smartpesa.ui.loans.LoanDetailsScreen
import com.example.smartpesa.ui.loans.LoansScreen
import com.example.smartpesa.ui.work.WorkAccountScreen
import com.example.smartpesa.ui.screens.FulizaScreen
import com.example.smartpesa.ui.screens.TransactionCostsScreen
import com.example.smartpesa.ui.screens.AboutScreen
import com.example.smartpesa.ui.screens.ReportsScreen
import com.example.smartpesa.ui.navigation.budgetDetailsRoute
import com.example.smartpesa.ui.navigation.addTransactionRoute
import com.example.smartpesa.ui.navigation.loanDetailsRoute
import com.example.smartpesa.ui.navigation.transactionDetailsRoute
import com.example.smartpesa.ui.navigation.transactionsRoute
import kotlinx.coroutines.launch

const val TRANSACTION_SAVED_MESSAGE_KEY = "transaction_saved_message"

/**
 * Main navigation graph for SmartPesa
 * Includes bottom navigation bar and screen navigation
 */
@Composable
fun NavGraph(
    userPreferencesRepository: UserPreferencesRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val showBottomBar = currentDestination?.hierarchy?.any {
        it.route?.startsWith(Screen.Home.route) == true ||
            it.route?.startsWith(Screen.Transactions.route) == true ||
            it.route?.startsWith(Screen.Budget.route) == true ||
            it.route?.startsWith(Screen.Accounts.route) == true ||
            it.route?.startsWith(Screen.Settings.route) == true
    } == true
    val showQuickAdd = currentDestination?.hierarchy?.any {
        it.route?.startsWith(Screen.Transactions.route) == true
    } == true
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    fun openDrawer() {
        coroutineScope.launch { drawerState.open() }
    }

    LaunchedEffect(navBackStackEntry) {
        val entry = navBackStackEntry ?: return@LaunchedEffect
        val savedMessage = entry.savedStateHandle.get<String>(TRANSACTION_SAVED_MESSAGE_KEY)
        if (!savedMessage.isNullOrBlank()) {
            entry.savedStateHandle.remove<String>(TRANSACTION_SAVED_MESSAGE_KEY)
            snackbarHostState.showSnackbar(savedMessage)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "SmartPesa", style = MaterialTheme.typography.titleLarge)
                    Text(text = "Menu", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                drawerItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        selected = selected,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {

        Scaffold(
        snackbarHost = {
            Box(modifier = Modifier.fillMaxSize()) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        },
        floatingActionButton = {
            if (showQuickAdd) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.route) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add transaction",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { routeMatches(it.route, item.screen.route) } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            alwaysShowLabel = true,
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ),
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
        ) { innerPadding ->
            NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = transitionDirection(initialState.destination.route, targetState.destination.route),
                    animationSpec = tween(220)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = transitionDirection(initialState.destination.route, targetState.destination.route),
                    animationSpec = tween(220)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = transitionDirection(initialState.destination.route, targetState.destination.route),
                    animationSpec = tween(220)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = transitionDirection(initialState.destination.route, targetState.destination.route),
                    animationSpec = tween(220)
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onMenuClick = { openDrawer() },
                    onNavigateToPermissions = { navController.navigate(Screen.AddTransaction.route) },
                    onTransactionClick = { transactionId ->
                        navController.navigate(transactionDetailsRoute(transactionId))
                    },
                    onAccountClick = { account ->
                        navController.navigate(transactionsRoute(account))
                    }
                )
            }
            composable(
                route = "${Screen.Transactions.route}?account={account}",
                arguments = listOf(navArgument("account") { type = NavType.StringType; defaultValue = "" })
            ) {
                TransactionsScreen(
                    onMenuClick = { openDrawer() },
                    onTransactionClick = { transactionId ->
                        navController.navigate(transactionDetailsRoute(transactionId))
                    },
                    onEditTransaction = { transactionId ->
                        navController.navigate(addTransactionRoute(transactionId))
                    },
                    onClipboardImport = { message ->
                        navController.navigate(addTransactionRoute(message = message))
                    }
                )
            }
            composable(Screen.Budget.route) {
                BudgetScreen(
                    onMenuClick = { openDrawer() },
                    onBudgetClick = { budgetId ->
                        navController.navigate(budgetDetailsRoute(budgetId))
                    }
                )
            }
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    onMenuClick = { openDrawer() },
                    onOpenAccountDetail = { accountId -> navController.navigate(accountDetailsRoute(accountId)) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(
                route = Screen.AccountDetails.route,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) {
                AccountDetailScreen(
                    onBackPressed = { navController.popBackStack() },
                    onEditAccount = { navController.navigate(Screen.Accounts.route) },
                    onTransactionClick = { transactionId -> navController.navigate(transactionDetailsRoute(transactionId)) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onMenuClick = { openDrawer() },
                    onOpenAccounts = { navController.navigate(Screen.Accounts.route) },
                    onOpenCaptureMode = { navController.navigate(Screen.SmsPermission.route) }
                )
            }
            composable(Screen.Categories.route) {
                CategoriesScreen(onMenuClick = { openDrawer() })
            }
            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(
                    onBackPressed = { navController.popBackStack() },
                    onSaved = { message ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(TRANSACTION_SAVED_MESSAGE_KEY, message)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = "${Screen.AddTransaction.route}?transactionId={transactionId}&message={message}",
                arguments = listOf(
                    navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("message") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                AddTransactionScreen(
                    onBackPressed = { navController.popBackStack() },
                    onSaved = { message ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(TRANSACTION_SAVED_MESSAGE_KEY, message)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.TransactionDetails.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) {
                TransactionDetailsScreen(
                    onBackPressed = { navController.popBackStack() },
                    onEditTransaction = { transactionId -> navController.navigate(addTransactionRoute(transactionId)) }
                )
            }
            composable(
                route = Screen.BudgetDetails.route,
                arguments = listOf(navArgument("budgetId") { type = NavType.LongType })
            ) {
                BudgetDetailsScreen(
                    onBackPressed = { navController.popBackStack() },
                    onEditBudget = { navController.navigate(Screen.Budget.route) },
                    onTransactionClick = { transactionId -> navController.navigate(transactionDetailsRoute(transactionId)) }
                )
            }
            composable(
                route = Screen.LoanDetails.route,
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) {
                LoanDetailsScreen(
                    onBackPressed = { navController.popBackStack() },
                    onRecordPayment = { navController.navigate(Screen.AddTransaction.route) },
                    onEditLoan = { navController.navigate(Screen.Loans.route) },
                    onTransactionClick = { transactionId -> navController.navigate(transactionDetailsRoute(transactionId)) }
                )
            }
            composable(Screen.Loans.route) {
                LoansScreen(
                    onCreateFirstItem = { navController.navigate(Screen.AddTransaction.route) },
                    onOpenLoanDetails = { loanId -> navController.navigate(loanDetailsRoute(loanId)) }
                )
            }
            composable(Screen.WorkAccount.route) {
                WorkAccountScreen(
                    onBackPressed = { navController.popBackStack() },
                    onTransactionClick = { transactionId -> navController.navigate(transactionDetailsRoute(transactionId)) }
                )
            }
            composable(Screen.Fuliza.route) {
                FulizaScreen(onCreateFirstItem = { navController.navigate(Screen.AddTransaction.route) })
            }
            composable(Screen.TransactionCosts.route) {
                TransactionCostsScreen(
                    onCreateFirstItem = { navController.navigate(Screen.AddTransaction.route) },
                    onBackPressed = { navController.popBackStack() }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(onBackPressed = { navController.popBackStack() })
            }
            composable(Screen.About.route) {
                AboutScreen(onBackPressed = { navController.popBackStack() })
            }

            composable(Screen.SmsPermission.route) {
                SmsPermissionScreen(
                    onPermissionsGranted = {
                        coroutineScope.launch { userPreferencesRepository.setPermissionSetupDone(true) }
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    },
                    onManualEntryRequested = { navController.navigate(Screen.AddTransaction.route) }
                )
            }
        }
        }
    }
}

private fun transitionDirection(fromRoute: String?, toRoute: String?): AnimatedContentTransitionScope.SlideDirection {
    val fromIndex = routeOrder.indexOf(fromRoute)
    val toIndex = routeOrder.indexOf(toRoute)

    return if (toIndex >= 0 && fromIndex >= 0 && toIndex > fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
}

private fun routeMatches(route: String?, screenRoute: String): Boolean {
    val baseRoute = route?.substringBefore("?")
    return baseRoute == screenRoute || route?.startsWith(screenRoute) == true
}

/**
 * Data class for bottom navigation items
 */
private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

/**
 * List of bottom navigation items
 */
private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home),
    BottomNavItem(Screen.Transactions, "Transactions", Icons.Filled.ReceiptLong),
    BottomNavItem(Screen.Budget, "Budget", Icons.Filled.AccountBalanceWallet),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings)
)

private data class DrawerItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

private val drawerItems = listOf(
    DrawerItem("Categories", Screen.Categories.route, Icons.Filled.Category),
    DrawerItem("Work Account", Screen.WorkAccount.route, Icons.Filled.Work),
    DrawerItem("Loans", Screen.Loans.route, Icons.Filled.AccountBalanceWallet),
    DrawerItem("Fuliza", Screen.Fuliza.route, Icons.Filled.AccountBalanceWallet),
    DrawerItem("Transaction Costs", Screen.TransactionCosts.route, Icons.Filled.ReceiptLong),
    DrawerItem("Reports", Screen.Reports.route, Icons.Filled.ReceiptLong),
    DrawerItem("About", Screen.About.route, Icons.Filled.Info)
)

private val routeOrder = listOf(
    Screen.Home.route,
    Screen.Transactions.route,
    Screen.Accounts.route,
    Screen.Budget.route,
    Screen.Settings.route,
    Screen.Categories.route,
    Screen.AddTransaction.route,
    Screen.TransactionDetails.route,
    Screen.BudgetDetails.route,
    Screen.LoanDetails.route,
    Screen.Loans.route,
    Screen.Fuliza.route,
    Screen.TransactionCosts.route,
    Screen.Reports.route,
    Screen.About.route,
    Screen.SmsPermission.route
)
