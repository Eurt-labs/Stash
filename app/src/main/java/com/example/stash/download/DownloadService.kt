package com.example.stash.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Foreground service that keeps downloads running when the app is backgrounded.
 *
 * Required for Android 14+ (API 34+) which restricts background work.
 * Uses `foregroundServiceType="dataSync"` to indicate data transfer.
 *
 * Lifecycle:
 * 1. Started via [Context.startForegroundService] when a download is enqueued
 * 2. Runs in foreground with a persistent notification
 * 3. Stops itself when all downloads are complete/cancelled
 */
class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"

        /**
         * Creates an intent to start this service.
         */
        fun newIntent(context: Context): Intent {
            return Intent(context, DownloadService::class.java)
        }
    }

    private lateinit var notificationManager: DownloadNotificationManager
    private var queueManager: DownloadQueueManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Binder for activity binding
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        notificationManager = DownloadNotificationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Start as foreground service with initial notification
        val notification = notificationManager.createSummaryNotification(
            activeCount = 0,
            queuedCount = 0,
            completedCount = 0
        )

        startForeground(
            DownloadNotificationManager.SUMMARY_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        return START_NOT_STICKY
    }

    /**
     * Attaches a [DownloadQueueManager] to this service and starts observing its state.
     * Called by the orchestrator after binding.
     */
    fun attachQueueManager(manager: DownloadQueueManager) {
        this.queueManager = manager
        observeDownloadState()
    }

    /**
     * Observes the download queue state and updates notifications accordingly.
     * Auto-stops the service when all downloads finish.
     */
    private fun observeDownloadState() {
        serviceScope.launch {
            queueManager?.downloadItems?.collectLatest { items ->
                if (items.isEmpty()) return@collectLatest

                val activeCount = items.values.count {
                    it.state == DownloadState.DOWNLOADING ||
                    it.state == DownloadState.SEARCHING ||
                    it.state == DownloadState.CONVERTING ||
                    it.state == DownloadState.TAGGING
                }
                val queuedCount = items.values.count { it.state == DownloadState.QUEUED }
                val completedCount = items.values.count { it.state == DownloadState.COMPLETE }

                // Update summary notification
                val notification = notificationManager.createSummaryNotification(
                    activeCount = activeCount,
                    queuedCount = queuedCount,
                    completedCount = completedCount
                )
                notificationManager.let {
                    val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.notify(DownloadNotificationManager.SUMMARY_NOTIFICATION_ID, notification)
                }

                // Update individual notifications
                items.values.forEach { item ->
                    notificationManager.showProgressNotification(item)
                }

                // Auto-stop when everything is done
                if (activeCount == 0 && queuedCount == 0) {
                    Log.d(TAG, "All downloads finished. Stopping service in 5 seconds.")
                    delay(5000) // Brief delay to let user see completion
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }
}
