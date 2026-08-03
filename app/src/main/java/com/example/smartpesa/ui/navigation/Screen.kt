package com.example.smartpesa.ui.navigation

/**
 * Sealed class representing navigation destinations
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Transactions : Screen("transactions")
    data object Budget : Screen("budget")
    data object Settings : Screen("settings")

    // Capture mode screens
    data object CaptureMode : Screen("capture_mode")
    data object SmsPermission : Screen("sms_permission")
    data object ManualPaste : Screen("manual_paste")
}
