package com.example.smartpesa.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartpesa.ui.budget.BudgetScreen
import com.example.smartpesa.ui.home.HomeScreen
import com.example.smartpesa.ui.settings.SettingsScreen
import com.example.smartpesa.ui.transactions.TransactionsScreen
import com.example.smartpesa.ui.capturemode.CaptureModeSelectionScreen
import com.example.smartpesa.ui.capturemode.ManualPasteScreen
import com.example.smartpesa.ui.permissions.SmsPermissionScreen

/**
 * Main navigation graph for SmartPesa.
 * Includes bottom navigation bar and screen navigation.
 */
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show the bottom bar on the four main tabs, not on sub-screens
    // (capture mode, permission, manual paste).
    val showBottomBar = currentDestination?.hierarchy?.any { destination ->
        bottomNavItems.any { it.screen.route == destination.route }
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPermissions = {
                        navController.navigate(Screen.CaptureMode.route)
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen()
            }
            composable(Screen.Budget.route) {
                BudgetScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // Capture mode screens
            composable(Screen.CaptureMode.route) {
                CaptureModeSelectionScreen(
                    onSmsAutoSelected = {
                        navController.navigate(Screen.SmsPermission.route)
                    },
                    onManualPasteSelected = {
                        navController.navigate(Screen.ManualPaste.route)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.SmsPermission.route) {
                SmsPermissionScreen(
                    onPermissionsGranted = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    },
                    onManualEntryRequested = {
                        navController.navigate(Screen.ManualPaste.route)
                    }
                )
            }

            composable(Screen.ManualPaste.route) {
                ManualPasteScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onSmsProcessed = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                )
            }
        }
    }
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
