package com.eurtlabs.stash.data.transcoder

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import com.eurtlabs.stash.data.model.TrackInfo
import com.yausername.youtubedl_android.ffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaTagger {

    private const val TAG = "MediaTagger"

    /**
     * Tags media file and notifies Android MediaStore scanner
     */
    suspend fun tagAndScan(context: Context, file: File, trackInfo: TrackInfo): File = withContext(Dispatchers.IO) {
        try {
            // Trigger Android Media Scanner so it shows up in music players
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("audio/*", "video/*")
            ) { path, uri ->
                Log.d(TAG, "Scanned $path into MediaStore: $uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Media scanner notification failed", e)
        }
        file
    }
}
