package com.eurtlabs.stash

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.yausername.youtubedl_android.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class StashApplication : Application() {

    companion object {
        const val CHANNEL_ID = "stash_download_channel"
        private const val TAG = "StashApp"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initNativeEngines()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun initNativeEngines() {
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d(TAG, "YoutubeDL and FFmpeg native engines successfully initialized at startup.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YoutubeDL / FFmpeg engines", e)
        }
    }
}
