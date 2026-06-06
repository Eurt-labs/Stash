package com.example.stash.download

import android.content.Context
import android.util.Log
import com.example.stash.model.Platform
import com.example.stash.model.TrackInfo
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Core download engine wrapping youtubedl-android (yt-dlp).
 *
 * Handles:
 * - Audio-only extraction (MP3, M4A, OGG, Opus)
 * - Video download (360p, 720p, 1080p, best)
 * - Progress reporting via callback
 * - YouTube video info extraction (for direct YouTube links)
 *
 * Make sure to call [YoutubeDL.getInstance().init(context)] before using this class.
 */
class DownloadEngine(private val context: Context) {

    companion object {
        private const val TAG = "DownloadEngine"
    }

    private val gson = Gson()

    /**
     * Downloads audio/video from the given URL with the specified quality and format.
     *
     * @param request The [DownloadRequest] containing URL, output dir, quality, format, etc.
     * @param onProgress Callback for progress updates (percent 0-100, eta in seconds).
     * @return The file path of the downloaded file.
     * @throws DownloadException if the download fails.
     */
    suspend fun download(
        request: DownloadRequest,
        onProgress: ((Float, Long, String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting download: ${request.url} → ${request.outputDir}")

        // Ensure output directory exists
        val outputDir = File(request.outputDir)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // Build the output filename template
        val outputTemplate = File(
            outputDir,
            "${request.trackInfo.safeFileName}.%(ext)s"
        ).absolutePath

        val ytdlRequest = YoutubeDLRequest(request.url)

        // Output path
        ytdlRequest.addOption("-o", outputTemplate)

        // Configure format/quality based on request
        configureFormat(ytdlRequest, request)

        // Prevent hanging on geo-restricted or unavailable content
        ytdlRequest.addOption("--no-check-certificates")
        ytdlRequest.addOption("--no-warnings")
        ytdlRequest.addOption("--socket-timeout", "30")
        ytdlRequest.addOption("--retries", "3")

        try {
            val response = YoutubeDL.getInstance().execute(
                ytdlRequest
            ) { progress, etaInSeconds, line ->
                val speed = parseSpeed(line)
                onProgress?.invoke(progress, etaInSeconds.toLong(), speed)
            }

            Log.d(TAG, "Download complete. Output: ${response.out}")

            // Find the actual output file (extension may differ from template)
            val outputFile = findOutputFile(outputDir, request.trackInfo.safeFileName)
                ?: throw DownloadException("Download completed but output file not found")

            return@withContext outputFile.absolutePath

        } catch (e: Exception) {
            if (e is DownloadException) throw e
            Log.e(TAG, "Download failed: ${e.message}", e)
            throw DownloadException("Download failed: ${e.message}", e)
        }
    }

    /**
     * Extracts metadata from a YouTube URL without downloading.
     * Used when the user pastes a direct YouTube link.
     *
     * @param url YouTube video or playlist URL.
     * @return List of [TrackInfo] objects.
     */
    suspend fun extractInfo(url: String): List<TrackInfo> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Extracting info from: $url")

        val request = YoutubeDLRequest(url)
        request.addOption("--dump-json")
        request.addOption("--no-download")
        request.addOption("--no-warnings")
        request.addOption("--flat-playlist")

        try {
            val response = YoutubeDL.getInstance().execute(request)
            val output = response.out ?: return@withContext emptyList()

            return@withContext output.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val json = gson.fromJson(line, JsonObject::class.java)
                        jsonToTrackInfo(json, url)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse info JSON", e)
                        null
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, "Info extraction failed: ${e.message}", e)
            throw DownloadException("Failed to extract info: ${e.message}", e)
        }
    }

    /**
     * Updates yt-dlp to the latest version.
     * Should be called periodically to keep up with YouTube changes.
     */
    suspend fun updateYtDlp() = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().updateYoutubeDL(context)
            Log.d(TAG, "yt-dlp updated successfully")
        } catch (e: Exception) {
            Log.w(TAG, "yt-dlp update failed: ${e.message}", e)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Configures yt-dlp format options based on the download request.
     */
    private fun configureFormat(ytdlRequest: YoutubeDLRequest, request: DownloadRequest) {
        if (request.format.isVideo) {
            // Video download
            val heightLimit = when (request.format) {
                DownloadFormat.VIDEO_360 -> 360
                DownloadFormat.VIDEO_720 -> 720
                DownloadFormat.VIDEO_1080 -> 1080
                DownloadFormat.VIDEO_BEST -> 0 // No limit
                else -> 720
            }

            if (heightLimit > 0) {
                ytdlRequest.addOption("-f", "bestvideo[height<=$heightLimit]+bestaudio/best[height<=$heightLimit]")
            } else {
                ytdlRequest.addOption("-f", "bestvideo+bestaudio/best")
            }
            ytdlRequest.addOption("--merge-output-format", "mp4")

        } else {
            // Audio-only extraction
            ytdlRequest.addOption("-x") // Extract audio
            ytdlRequest.addOption("--audio-format", request.format.extension)

            when (request.format) {
                DownloadFormat.MP3 -> {
                    ytdlRequest.addOption("--audio-quality", "${request.quality.bitrateKbps}K")
                }
                DownloadFormat.M4A -> {
                    ytdlRequest.addOption("--audio-quality", "0") // Best quality for M4A
                }
                DownloadFormat.OGG -> {
                    ytdlRequest.addOption("--audio-quality", "${request.quality.bitrateKbps}K")
                }
                DownloadFormat.OPUS -> {
                    ytdlRequest.addOption("--audio-quality", "0")
                }
                else -> {} // Video formats handled above
            }
        }
    }

    /**
     * Finds the output file in the directory matching the base name.
     * yt-dlp may change the extension during conversion.
     */
    private fun findOutputFile(dir: File, baseName: String): File? {
        return dir.listFiles()
            ?.filter { it.nameWithoutExtension == baseName }
            ?.maxByOrNull { it.lastModified() }
    }

    /**
     * Converts a yt-dlp JSON info dict to a [TrackInfo].
     */
    private fun jsonToTrackInfo(json: JsonObject, sourceUrl: String): TrackInfo {
        val title = json.get("title")?.asString ?: "Unknown Title"
        val artist = json.get("artist")?.asString
            ?: json.get("channel")?.asString
            ?: json.get("uploader")?.asString
            ?: "Unknown Artist"
        val album = json.get("album")?.asString
        val durationMs = (json.get("duration")?.asDouble?.times(1000))?.toLong() ?: 0L
        val thumbnail = json.get("thumbnail")?.asString

        val detectedPlatform = when {
            sourceUrl.contains("instagram.com") -> Platform.INSTAGRAM
            sourceUrl.contains("music.youtube.com") -> Platform.YOUTUBE_MUSIC
            else -> Platform.YOUTUBE
        }

        // Determine the best URL for this entry
        val videoUrl = json.get("webpage_url")?.asString
            ?: json.get("url")?.asString
            ?: if (detectedPlatform == Platform.INSTAGRAM) sourceUrl else "https://www.youtube.com/watch?v=${json.get("id")?.asString}"

        return TrackInfo(
            title = title,
            artists = listOf(artist),
            album = album,
            durationMs = durationMs,
            albumArtUrl = thumbnail,
            source = detectedPlatform,
            sourceUrl = sourceUrl,
            youtubeUrl = if (detectedPlatform == Platform.INSTAGRAM) null else videoUrl
        )
    }

    /**
     * Parses the speed and size from the yt-dlp progress line.
     * Example: "[download]  12.3% of ~15.42MiB at  2.56MiB/s ETA 00:05"
     */
    private fun parseSpeed(line: String): String {
        if (line.isBlank()) return ""
        val atIndex = line.indexOf(" at ")
        val etaIndex = line.indexOf(" ETA ")
        if (atIndex != -1 && etaIndex != -1 && etaIndex > atIndex) {
            val speed = line.substring(atIndex + 4, etaIndex).trim()
            val ofIndex = line.indexOf(" of ")
            if (ofIndex != -1 && ofIndex < atIndex) {
                val size = line.substring(ofIndex + 4, atIndex).trim()
                return "$speed ($size)"
            }
            return speed
        }
        return ""
    }
}

/**
 * Exception thrown when a download operation fails.
 */
class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
