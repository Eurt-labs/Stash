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
import java.util.UUID

object YoutubeDLManager {

    private const val TAG = "YoutubeDLManager"

    fun getDefaultOutputDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "Music")
        val stashDir = File(baseDir, "Stash")
        if (!stashDir.exists()) {
            stashDir.mkdirs()
        }
        return stashDir
    }

    suspend fun extractMetadata(url: String): List<TrackInfo> = withContext(Dispatchers.IO) {
        try {
            val videoInfo: VideoInfo = YoutubeDL.getInstance().getInfo(url)
            listOf(videoInfoToTrackInfo(videoInfo, url))
        } catch (e: Exception) {
            Log.w(TAG, "Fast metadata getInfo failed for $url, attempting request fallback", e)
            try {
                val request = YoutubeDLRequest(url).apply {
                    addOption("--no-warnings")
                    addOption("--socket-timeout", "20")
                }
                val videoInfo = YoutubeDL.getInstance().getInfo(request)
                listOf(videoInfoToTrackInfo(videoInfo, url))
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Complete metadata extraction failed for $url", fallbackEx)
                // Fallback basic track info so user can still proceed with download
                val safeName = sanitizeFileName("Stash Media - ${System.currentTimeMillis()}")
                listOf(
                    TrackInfo(
                        id = UUID.randomUUID().toString(),
                        title = "Media Download",
                        artists = listOf("YouTube"),
                        durationMs = 0L,
                        sourceUrl = url,
                        safeFileName = safeName
                    )
                )
            }
        }
    }

    suspend fun downloadTrack(
        context: Context,
        trackInfo: TrackInfo,
        quality: DownloadQuality,
        format: DownloadFormat,
        outputDir: File,
        processId: String,
        onProgress: (progress: Float, speed: String, eta: String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val safeName = trackInfo.safeFileName.ifBlank { "Track_${System.currentTimeMillis()}" }
        val destinationTemplate = "${outputDir.absolutePath}/$safeName.%(ext)s"
        val targetUrl = trackInfo.youtubeUrl ?: trackInfo.sourceUrl

        val request = YoutubeDLRequest(targetUrl).apply {
            addOption("-o", destinationTemplate)
            addOption("--no-mtime")
            addOption("--no-check-certificates")
            addOption("--no-warnings")
            addOption("--socket-timeout", "30")
            addOption("--retries", "5")
            addOption("--fragment-retries", "5")
            addOption("--geo-bypass")
            addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

            if (format.isAudioOnly) {
                addOption("-x")
                addOption("--audio-format", format.ext)
                addOption("--audio-quality", quality.valueOption)
            } else {
                addOption("-f", quality.valueOption)
                addOption("--merge-output-format", format.ext)
            }
        }

        try {
            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                val etaStr = if (etaInSeconds > 0) "${etaInSeconds}s" else ""
                val speedMatch = Regex("(\\d+(?:\\.\\d+)?(?:KiB|MiB|GiB)/s)").find(line ?: "")
                val speedStr = speedMatch?.value ?: ""
                onProgress(progress, speedStr, etaStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download execution failed for $targetUrl", e)
            throw e
        }

        // Locate output file with extension fallback
        val possibleExtensions = listOf(format.ext, "mp3", "m4a", "opus", "flac", "wav", "mp4", "mkv", "webm")
        var outputFile: File? = null
        for (ext in possibleExtensions) {
            val testFile = File(outputDir, "$safeName.$ext")
            if (testFile.exists() && testFile.length() > 0L) {
                outputFile = testFile
                break
            }
        }

        if (outputFile == null) {
            outputFile = outputDir.listFiles()?.filter {
                it.isFile && it.length() > 0L && (it.name.contains(safeName) || it.nameWithoutExtension == safeName)
            }?.maxByOrNull { it.lastModified() }
        }

        outputFile ?: throw IllegalStateException("Downloaded media file was not found on disk")
    }

    private fun videoInfoToTrackInfo(info: VideoInfo, sourceUrl: String): TrackInfo {
        val title = info.title ?: "Unknown Title"
        val artist = info.uploader ?: "Unknown Artist"
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
            .take(100)
    }
}
