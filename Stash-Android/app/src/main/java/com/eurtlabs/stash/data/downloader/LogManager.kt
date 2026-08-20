package com.eurtlabs.stash.data.downloader

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {

    private const val TAG = "StashLogManager"
    private val logBuffer = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @Synchronized
    fun append(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp][$tag] $message"
        logBuffer.add(entry)
        if (logBuffer.size > 500) {
            logBuffer.removeAt(0)
        }
        Log.d(tag, message)
    }

    @Synchronized
    fun getFullLog(): String {
        return if (logBuffer.isEmpty()) {
            "No diagnostic logs recorded yet."
        } else {
            logBuffer.joinToString("\n")
        }
    }

    fun exportLogs(context: Context) {
        try {
            val logText = "=== STASH DIAGNOSTIC LOGS ===\n" +
                    "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                    "Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n\n" +
                    getFullLog()

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, logText)
                putExtra(Intent.EXTRA_TITLE, "Stash Diagnostics Log")
                type = "text/plain"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export Stash Diagnostic Logs").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share logs", e)
        }
    }
}
