package com.eurtlabs.stash.data.downloader

import android.content.Context
import android.os.Environment
import android.util.Log
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.Platform
import com.eurtlabs.stash.data.model.SearchFilter
import com.eurtlabs.stash.data.model.SearchResultItem
import com.eurtlabs.stash.data.model.TrackInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

    suspend fun searchMedia(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val searchPrefix = when (filter) {
            SearchFilter.MUSIC -> "ytsearch10:$query music"
            SearchFilter.ARTISTS -> "ytsearch10:$query official artist channel"
            SearchFilter.VIDEOS -> "ytsearch10:$query"
            SearchFilter.ALL -> "ytsearch10:$query"
        }

        try {
            val request = YoutubeDLRequest(searchPrefix).apply {
                addOption("--dump-json")
                addOption("--flat-playlist")
                addOption("--no-warnings")
                addOption("--socket-timeout", "15")
                addOption("--extractor-args", "youtube:player_client=android,web")
            }
            val response = YoutubeDL.getInstance().execute(request)
            val jsonLines = response.out?.lines()?.filter { it.isNotBlank() } ?: emptyList()

            val results = mutableListOf<SearchResultItem>()
            for (line in jsonLines) {
                try {
                    val json = JSONObject(line)
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

                        // Extract high-res thumbnail with reliable fallback
                        var thumbnail = json.optString("thumbnail", "")
                        if (thumbnail.isEmpty()) {
                            val thumbsArray = json.optJSONArray("thumbnails")
                            if (thumbsArray != null && thumbsArray.length() > 0) {
                                thumbnail = thumbsArray.optJSONObject(thumbsArray.length() - 1)?.optString("url", "") ?: ""
                            }
                        }
                        if (thumbnail.isEmpty() && id.isNotBlank()) {
                            thumbnail = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                        }

                        val url = json.optString("url", "https://www.youtube.com/watch?v=$id")
                        val finalUrl = if (url.startsWith("http")) url else "https://www.youtube.com/watch?v=$id"
                        val isAudio = filter == SearchFilter.MUSIC || filter == SearchFilter.ARTISTS

                        results.add(
                            SearchResultItem(
                                id = id,
                                title = title,
                                artist = uploader,
                                durationText = durationStr,
                                thumbnailUrl = thumbnail.ifEmpty { null },
                                url = finalUrl,
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
                addOption("--extractor-args", "youtube:player_client=android,web")
            }
            val videoInfo: VideoInfo = YoutubeDL.getInstance().getInfo(request)
            listOf(videoInfoToTrackInfo(videoInfo, url))
        } catch (e: Exception) {
            Log.e(TAG, "Metadata extraction failed for $url", e)
            val videoId = Regex("v=([a-zA-Z0-9_-]{11})").find(url)?.groupValues?.getOrNull(1) ?: UUID.randomUUID().toString().take(11)
            val safeName = sanitizeFileName("Stash Media - ${System.currentTimeMillis()}")
            listOf(
                TrackInfo(
                    id = videoId,
                    title = "Media Download",
                    artists = listOf("YouTube"),
                    durationMs = 0L,
                    albumArtUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
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
            addOption("--retries", "10")
            addOption("--fragment-retries", "10")
            addOption("--geo-bypass")
            addOption("--extractor-args", "youtube:player_client=android,web")

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
        val fallbackThumb = if (info.id != null) "https://i.ytimg.com/vi/${info.id}/hqdefault.jpg" else null

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
            albumArtUrl = info.thumbnail?.ifEmpty { fallbackThumb } ?: fallbackThumb,
            source = detectedPlatform,
            sourceUrl = sourceUrl,
            youtubeUrl = if (detectedPlatform != Platform.OTHER) info.webpageUrl else null,
            safeFileName = safeName
        )
    }

    private fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(100)
    }
}
