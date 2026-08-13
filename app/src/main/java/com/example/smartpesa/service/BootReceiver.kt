package com.example.smartpesa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * NotificationListenerService instances are system-bound after permission is granted.
 * Starting one manually as a foreground service crashes on Android 8+.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed; notification listener will be rebound by the system")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
