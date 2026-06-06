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

        lateinit var orchestrator: StashOrchestrator
            private set
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        orchestrator = StashOrchestrator(this)
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

            // CRITICAL FIX: Force execute permissions on the ffmpeg binary.
            // Without this, yt-dlp hangs forever at 100% because it can't call
            // ffmpeg for DASH stream concatenation (error=13 Permission denied).
            fixFFmpegPermissions()

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

    /**
     * Walks the app's internal directories to find the bundled ffmpeg binary
     * and ensures it has execute (+x) permissions. On some Android devices/versions,
     * the binary loses its execute bit after extraction, causing yt-dlp to hang.
     */
    private fun fixFFmpegPermissions() {
        val searchDirs = listOfNotNull(
            noBackupFilesDir,
            java.io.File(applicationInfo.nativeLibraryDir)
        )
        
        for (dir in searchDirs) {
            if (!dir.exists()) continue
            dir.walkTopDown().forEach { file ->
                if (file.name.contains("ffmpeg", ignoreCase = true) ||
                    file.name.contains("ffprobe", ignoreCase = true)
                ) {
                    if (!file.canExecute()) {
                        val success = file.setExecutable(true, false)
                        Log.d(TAG, "Set execute permission on ${file.absolutePath}: $success")
                    } else {
                        Log.d(TAG, "FFmpeg binary already executable: ${file.absolutePath}")
                    }
                }
            }
        }
    }
}
