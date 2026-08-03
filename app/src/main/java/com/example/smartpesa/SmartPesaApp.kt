package com.example.smartpesa

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.smartpesa.data.initialization.CategoryInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class with Hilt and WorkManager configuration
 *
 * Implements Configuration.Provider to provide custom WorkManager configuration
 * This is required for @HiltWorker injection to work in SmsProcessingWorker
 */
@HiltAndroidApp
class SmartPesaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var categoryInitializer: CategoryInitializer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize categories on first app launch
        applicationScope.launch {
            categoryInitializer.initializeIfNeeded()
        }
    }

    /**
     * Provide WorkManager configuration with Hilt WorkerFactory
     * This enables dependency injection in Workers annotated with @HiltWorker
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
