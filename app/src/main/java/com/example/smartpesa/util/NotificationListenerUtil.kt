package com.example.smartpesa.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Utility for checking and managing notification listener permission
 * Used for M-Pesa notification capture as an alternative to SMS access
 */
object NotificationListenerUtil {

    /**
     * Check if notification listener permission is enabled for this app
     * Returns true if the user has granted notification access in system settings
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val packageName = context.packageName
        return enabledListeners.contains(packageName)
    }

    /**
     * Open system settings to enable notification listener
     * Takes user to Notification Access settings screen
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Get the service component name for this app's notification listener
     * Used for checking if the specific service is enabled
     */
    fun getServiceComponentName(context: Context): String {
        return "${context.packageName}/com.example.smartpesa.service.MpesaNotificationListenerService"
    }
}
