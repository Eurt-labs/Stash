package com.example.stash

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application class that initializes required libraries on app startup.
 *
 * Initializes:
 * - **YoutubeDL** (yt-dlp engine) — required before any download or search operations
 * - **FFmpeg** — required for audio extraction and format conversion
 *
 * Must be registered in AndroidManifest.xml:
 * ```xml
 * <application android:name=".StashApplication" ... />
 * ```
 */
class StashApplication : Application() {

    companion object {
        private const val TAG = "StashApp"

        /** Global flag indicating whether yt-dlp initialization succeeded. */
        @Volatile
        var isYtDlpInitialized = false
            private set
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        initializeYoutubeDL()
    }

    /**
     * Initializes the YoutubeDL and FFmpeg libraries.
     *
     * This is a blocking operation on first run (~2-3 seconds) as it extracts
     * the bundled Python + yt-dlp binaries. Subsequent startups are faster.
     */
    private fun initializeYoutubeDL() {
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d(TAG, "YoutubeDL and FFmpeg initialized successfully")

            isYtDlpInitialized = true

            // Auto-update yt-dlp in the background to keep up with YouTube changes
            appScope.launch {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(this@StashApplication)
                    Log.d(TAG, "yt-dlp auto-update check completed")
                } catch (e: Exception) {
                    Log.w(TAG, "yt-dlp auto-update failed (non-fatal): ${e.message}")
                }
            }

        } catch (e: YoutubeDLException) {
            Log.e(TAG, "Failed to initialize YoutubeDL", e)
            isYtDlpInitialized = false
        }
    }
}
