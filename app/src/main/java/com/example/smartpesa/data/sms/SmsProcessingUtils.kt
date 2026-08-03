package com.example.smartpesa.data.sms

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * Utility functions for SMS processing
 * Provides manual SMS processing as a fallback when user doesn't grant SMS permissions
 */
object SmsProcessingUtils {

    /**
     * Process a manually provided SMS (e.g., from clipboard paste)
     * Reuses the same WorkManager pipeline as automatic SMS processing
     *
     * This is the fallback path from the Flutter version carried forward:
     * User can paste M-Pesa SMS instead of granting SMS permission
     *
     * @param context Application context
     * @param smsBody The SMS message text to process
     * @return WorkRequest ID for tracking
     */
    fun processManualSms(context: Context, smsBody: String): String {
        val workRequest = OneTimeWorkRequestBuilder<SmsProcessingWorker>()
            .setInputData(
                workDataOf(
                    SmsReceiver.KEY_SMS_BODY to smsBody,
                    SmsReceiver.KEY_SMS_TIMESTAMP to System.currentTimeMillis()
                )
            )
            .addTag("sms_processing")
            .addTag("manual_entry")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        return workRequest.id.toString()
    }

    /**
     * Check if an SMS looks like an M-Pesa message before processing
     * Quick pre-check to avoid enqueuing work for non-M-Pesa messages
     */
    fun looksLikeMpesaSms(smsBody: String): Boolean {
        return smsBody.contains("Confirmed", ignoreCase = true) &&
                (smsBody.contains("M-PESA", ignoreCase = true) ||
                 smsBody.contains("Ksh", ignoreCase = true) ||
                 smsBody.contains("sent to", ignoreCase = true) ||
                 smsBody.contains("received", ignoreCase = true))
    }
}
