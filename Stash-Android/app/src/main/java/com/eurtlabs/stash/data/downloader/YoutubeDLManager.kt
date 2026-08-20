package com.eurtlabs.stash.data.downloader

import android.content.Context
import android.os.Environment
import android.util.Log
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.Platform
import com.eurtlabs.stash.data.model.TrackInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object YoutubeDLManager {

    private const val TAG = "YoutubeDLManager"

    fun getDefaultOutputDir(context: Context): File {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val stashDir = File(musicDir, "Stash")
        if (!stashDir.exists()) {
            stashDir.mkdirs()
        }
        return stashDir
    }

    suspend fun extractMetadata(url: String): List<TrackInfo> = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply {
            addOption("--dump-json")
            addOption("--no-download")
            addOption("--no-warnings")
            addOption("--no-check-certificates")
            addOption("--socket-timeout", 30)
            addOption("--ignore-errors")
        }

        try {
            val videoInfo: VideoInfo = YoutubeDL.getInstance().getInfo(request)
            listOf(videoInfoToTrackInfo(videoInfo, url))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract metadata for $url", e)
            throw e
        }
    }

    suspend fun downloadTrack(
        context: Context,
        trackInfo: TrackInfo,
        quality: DownloadQuality,
        format: DownloadFormat,
        outputDir: File,
        processId: String,
        onProgress: (progress: Float, speed: String, eta: String) => Unit
    ): File = withContext(Dispatchers.IO) {
        val safeName = trackInfo.safeFileName
        val destinationTemplate = "${outputDir.absolutePath}/$safeName.%(ext)s"
        val targetUrl = trackInfo.youtubeUrl ?: trackInfo.sourceUrl

        val request = YoutubeDLRequest(targetUrl).apply {
            addOption("-o", destinationTemplate)
            addOption("--no-check-certificates")
            addOption("--no-warnings")
            addOption("--socket-timeout", 30)
            addOption("--retries", 5)
            addOption("--fragment-retries", 5)
            addOption("--geo-bypass")
            addOption("--user-agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")

            if (format.isAudioOnly) {
                addOption("-x")
                addOption("--audio-format", format.ext)
                val bitrate = when (quality) {
                    DownloadQuality.QUALITY_4K, DownloadQuality.QUALITY_2K, DownloadQuality.HIGH -> "320k"
                    DownloadQuality.MID -> "192k"
                    DownloadQuality.LOW -> "128k"
                }
                addOption("--audio-quality", bitrate)
            } else {
                when (quality) {
                    DownloadQuality.LOW -> addOption("-f", "bv*[height<=480]+ba/b[height<=480]/bv*+ba/b")
                    DownloadQuality.MID -> addOption("-f", "bv*[height<=720]+ba/b[height<=720]/bv*+ba/b")
                    DownloadQuality.HIGH -> addOption("-f", "bv*[height<=1080]+ba/b[height<=1080]/bv*+ba/b")
                    DownloadQuality.QUALITY_2K -> addOption("-f", "bv*[height<=1440]+ba/b[height<=1440]/bv*+ba/b")
                    DownloadQuality.QUALITY_4K -> addOption("-f", "bv*+ba/b")
                }
                addOption("--merge-output-format", "mp4")
            }
        }

        val response = YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
            val etaStr = if (etaInSeconds > 0) "${etaInSeconds}s" else ""
            onProgress(progress, "", etaStr)
        }

        val outputFile = File(outputDir, "$safeName.${format.ext}")
        if (outputFile.exists()) {
            outputFile
        } else {
            // Find matched file if extension slightly differs
            outputDir.listFiles { file -> file.nameWithoutExtension == safeName }?.firstOrNull()
                ?: throw IllegalStateException("Download completed but output file was not found")
        }
    }

    private fun videoInfoToTrackInfo(info: VideoInfo, sourceUrl: String): TrackInfo {
        val title = info.title ?: "Unknown Title"
        val artist = info.uploader ?: info.artist ?: "Unknown Artist"
        val durationMs = (info.duration * 1000L).coerceAtLeast(0L)
        val safeFileName = sanitizeFileName("$artist - $title")

        val detectedPlatform = when {
            sourceUrl.contains("music.youtube.com") -> Platform.YOUTUBE_MUSIC
            sourceUrl.contains("youtube.com") || sourceUrl.contains("youtu.be") -> Platform.YOUTUBE
            else -> Platform.OTHER
        }

        return TrackInfo(
            id = info.id ?: safeFileName,
            title = title,
            artists = listOf(artist),
            durationMs = durationMs,
            albumArtUrl = info.thumbnail,
            source = detectedPlatform,
            sourceUrl = sourceUrl,
            youtubeUrl = if (detectedPlatform != Platform.OTHER) info.webpageUrl else null,
            safeFileName = safeFileName
        )
    }

    private fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
    }
}
