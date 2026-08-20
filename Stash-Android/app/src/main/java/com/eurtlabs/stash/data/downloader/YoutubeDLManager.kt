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

    suspend fun searchMedia(query: String, filter: com.eurtlabs.stash.data.model.SearchFilter = com.eurtlabs.stash.data.model.SearchFilter.ALL): List<com.eurtlabs.stash.data.model.SearchResultItem> = withContext(Dispatchers.IO) {
        val searchPrefix = when (filter) {
            com.eurtlabs.stash.data.model.SearchFilter.MUSIC -> "ytsearch10:$query music"
            com.eurtlabs.stash.data.model.SearchFilter.ARTISTS -> "ytsearch10:$query official artist channel"
            com.eurtlabs.stash.data.model.SearchFilter.VIDEOS -> "ytsearch10:$query"
            com.eurtlabs.stash.data.model.SearchFilter.ALL -> "ytsearch10:$query"
        }

        try {
            val request = YoutubeDLRequest(searchPrefix).apply {
                addOption("--dump-json")
                addOption("--flat-playlist")
                addOption("--no-warnings")
                addOption("--socket-timeout", "15")
            }
            val response = YoutubeDL.getInstance().execute(request)
            val jsonLines = response.out?.lines()?.filter { it.isNotBlank() } ?: emptyList()

            val results = mutableListOf<com.eurtlabs.stash.data.model.SearchResultItem>()
            for (line in jsonLines) {
                try {
                    val json = org.json.JSONObject(line)
                    val id = json.optString("id")
                    val title = json.optString("title")
                    if (id.isNotBlank() && title.isNotBlank()) {
                        val uploader = json.optString("uploader", json.optString("channel", "YouTube"))
                        val duration = json.optLong("duration", 0L)
                        val durationStr = if (duration > 0) {
                            val mins = duration / 60
                            val secs = duration % 60
                            String.format("%d:%02d", mins, secs)
                        } else ""
                        val thumbnail = json.optString("thumbnail", "")
                        val url = json.optString("url", "https://www.youtube.com/watch?v=$id")
                        val isAudio = filter == com.eurtlabs.stash.data.model.SearchFilter.MUSIC || filter == com.eurtlabs.stash.data.model.SearchFilter.ARTISTS

                        results.add(
                            com.eurtlabs.stash.data.model.SearchResultItem(
                                id = id,
                                title = title,
                                artist = uploader,
                                durationText = durationStr,
                                thumbnailUrl = thumbnail.ifEmpty { null },
                                url = if (url.startsWith("http")) url else "https://www.youtube.com/watch?v=$id",
                                isAudio = isAudio
                            )
                        )
                    }
                } catch (je: Exception) {
                    // Ignore line parse exception
                }
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for query: $query", e)
            emptyList()
        }
    }

    suspend fun extractMetadata(url: String): List<TrackInfo> = withContext(Dispatchers.IO) {
        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-warnings")
                addOption("--socket-timeout", "20")
            }
            val videoInfo: VideoInfo = YoutubeDL.getInstance().getInfo(request)
            listOf(videoInfoToTrackInfo(videoInfo, url))
        } catch (e: Exception) {
            Log.e(TAG, "Metadata extraction failed for $url", e)
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
                addOption("-f", "ba/b")
                addOption("-x")
                addOption("--audio-format", format.ext)
                addOption("--audio-quality", "0")
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
