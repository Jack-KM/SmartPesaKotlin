package com.example.smartpesa.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * BroadcastReceiver for incoming SMS messages
 * Filters M-Pesa SMS and hands off to WorkManager for processing
 *
 * Why WorkManager instead of processing inline:
 * - BroadcastReceivers have ~10 second execution limit
 * - Database operations (Room) can take longer, especially with deduplication checks
 * - WorkManager ensures reliable execution even if app is backgrounded or killed
 * - WorkManager provides retry logic and persistence
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"

        // M-Pesa sender ID - adjust if your carrier uses a different format
        // Common variations: "MPESA", "M-PESA", "SAFARICOM", "Mpesa"
        private const val MPESA_SENDER_ID = "MPESA"

        // WorkManager input data keys
        const val KEY_SMS_BODY = "sms_body"
        const val KEY_SMS_TIMESTAMP = "sms_timestamp"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        // Extract SMS messages from intent
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) {
            Log.d(TAG, "No SMS messages found in intent")
            return
        }

        // Process each message
        messages.forEach { message ->
            processSmsMessage(context, message)
        }
    }

    private fun processSmsMessage(context: Context, message: SmsMessage) {
        val sender = message.displayOriginatingAddress ?: ""
        val body = message.messageBody ?: ""
        val timestamp = message.timestampMillis

        Log.d(TAG, "Received SMS from: $sender")

        // Filter to M-Pesa messages only
        // Case-insensitive check to handle variations (MPESA, Mpesa, M-PESA, etc.)
        if (!sender.contains(MPESA_SENDER_ID, ignoreCase = true)) {
            Log.d(TAG, "Not an M-Pesa SMS, ignoring")
            return
        }

        Log.d(TAG, "M-Pesa SMS detected, enqueueing work")

        // Hand off to WorkManager for processing
        // This ensures reliable execution even if the app is killed
        val workRequest = OneTimeWorkRequestBuilder<SmsProcessingWorker>()
            .setInputData(
                workDataOf(
                    KEY_SMS_BODY to body,
                    KEY_SMS_TIMESTAMP to timestamp
                )
            )
            .addTag("sms_processing")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
