package com.example.smartpesa.data.sms

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SmsProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val processor: MpesaMessageProcessor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val body = inputData.getString(SmsReceiver.KEY_SMS_BODY).orEmpty()
        val timestamp = inputData.getLong(SmsReceiver.KEY_SMS_TIMESTAMP, System.currentTimeMillis())

        return when (processor.process(body, timestamp, "M-Pesa SMS")) {
            MpesaProcessResult.Processed, MpesaProcessResult.Ignored -> Result.success()
            MpesaProcessResult.Retry -> {
                Log.w(TAG, "Retrying M-Pesa SMS processing")
                Result.retry()
            }
        }
    }

    private companion object {
        const val TAG = "SmsProcessingWorker"
    }
}
