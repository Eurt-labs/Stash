package com.example.stash.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Manages download progress notifications for the Stash app.
 *
 * Creates:
 * - A persistent summary notification showing overall download progress
 * - Individual progress notifications per download item (grouped)
 *
 * Handles notification channel creation for Android 8.0+ (O).
 */
class DownloadNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "stash_downloads"
        const val CHANNEL_NAME = "Downloads"
        const val SUMMARY_NOTIFICATION_ID = 1000
        const val GROUP_KEY = "com.example.stash.DOWNLOADS"

        /** Base ID for individual download notifications. Offset by download index. */
        private const val INDIVIDUAL_BASE_ID = 2000
        private var nextNotificationId = INDIVIDUAL_BASE_ID
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Map download ID → notification ID
    private val notificationIds = mutableMapOf<String, Int>()

    init {
        createNotificationChannel()
    }

    /**
     * Creates the notification channel (required for Android 8.0+).
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Low = no sound, shows in tray
        ).apply {
            description = "Shows download progress for music and videos"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Creates or updates a foreground service notification showing overall progress.
     *
     * @param activeCount Number of currently downloading items.
     * @param queuedCount Number of items waiting in queue.
     * @param completedCount Number of completed downloads.
     * @return The notification for use with [startForeground].
     */
    fun createSummaryNotification(
        activeCount: Int,
        queuedCount: Int,
        completedCount: Int
    ): Notification {
        val text = buildString {
            if (activeCount > 0) append("Downloading $activeCount")
            if (queuedCount > 0) {
                if (isNotEmpty()) append(" · ")
                append("$queuedCount queued")
            }
            if (completedCount > 0) {
                if (isNotEmpty()) append(" · ")
                append("$completedCount done")
            }
            if (isEmpty()) append("Download service running")
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Stash Downloads")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    /**
     * Shows or updates a progress notification for an individual download.
     */
    fun showProgressNotification(item: DownloadItem) {
        val notifId = notificationIds.getOrPut(item.id) { nextNotificationId++ }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(item.trackInfo.title)
            .setContentText(item.trackInfo.artists.joinToString(", "))
            .setGroup(GROUP_KEY)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        when (item.state) {
            DownloadState.QUEUED -> {
                builder.setContentText("Waiting in queue...")
                builder.setProgress(0, 0, true)
            }
            DownloadState.SEARCHING -> {
                builder.setContentText("Searching YouTube...")
                builder.setProgress(0, 0, true)
            }
            DownloadState.DOWNLOADING -> {
                val percent = (item.progress * 100).toInt()
                builder.setProgress(100, percent, false)
                builder.setContentText("$percent% ${item.speed ?: ""}")
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
            }
            DownloadState.CONVERTING -> {
                builder.setContentText("Converting...")
                builder.setProgress(0, 0, true)
            }
            DownloadState.TAGGING -> {
                builder.setContentText("Adding metadata...")
                builder.setProgress(0, 0, true)
            }
            DownloadState.COMPLETE -> {
                builder.setContentText("Download complete")
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setProgress(0, 0, false)
                builder.setOngoing(false)
                builder.setAutoCancel(true)
            }
            DownloadState.FAILED -> {
                builder.setContentText("Failed: ${item.error ?: "Unknown error"}")
                builder.setSmallIcon(android.R.drawable.stat_notify_error)
                builder.setProgress(0, 0, false)
                builder.setOngoing(false)
            }
            DownloadState.CANCELLED -> {
                builder.setContentText("Cancelled")
                builder.setProgress(0, 0, false)
                builder.setOngoing(false)
                builder.setAutoCancel(true)
            }
            DownloadState.PAUSED -> {
                builder.setContentText("Paused")
                builder.setProgress(100, (item.progress * 100).toInt(), false)
                builder.setOngoing(false)
            }
        }

        notificationManager.notify(notifId, builder.build())
    }

    /**
     * Removes the notification for a specific download.
     */
    fun cancelNotification(downloadId: String) {
        notificationIds[downloadId]?.let { notifId ->
            notificationManager.cancel(notifId)
            notificationIds.remove(downloadId)
        }
    }

    /**
     * Removes all download notifications.
     */
    fun cancelAll() {
        notificationIds.values.forEach { notificationManager.cancel(it) }
        notificationIds.clear()
        notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
    }
}
