package com.example.smartpesa.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.smartpesa.data.sms.MpesaMessageProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NotificationListenerService for capturing M-Pesa notifications
 * Alternative to SMS access - captures the same transaction data from notifications
 *
 * Requires notification listener permission in system settings
 */
@AndroidEntryPoint
class MpesaNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var processor: MpesaMessageProcessor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MpesaNotificationListener"
        private const val MPESA_PACKAGE = "com.safaricom.mpesa.customer"
        private const val SAFARICOM_PACKAGE = "com.safaricom.mpesaapp"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // Filter for M-Pesa notifications
        val packageName = sbn.packageName
        if (packageName != MPESA_PACKAGE && packageName != SAFARICOM_PACKAGE) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract notification text
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        // Combine title and text for parsing
        val fullText = if (title.isNotBlank() && text.isNotBlank()) {
            "$title $bigText"
        } else {
            bigText
        }

        if (fullText.isBlank()) {
            return
        }

        Log.d(TAG, "M-Pesa notification received: $fullText")

        // Parse and save transaction (use notification post time)
        serviceScope.launch {
            try {
                processor.process(fullText, sbn.postTime, "M-Pesa Notification")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing M-Pesa notification", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "M-Pesa notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "M-Pesa notification listener disconnected")
    }
}
