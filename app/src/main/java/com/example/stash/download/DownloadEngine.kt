package com.example.stash.download

import com.example.stash.model.Platform
import com.example.stash.model.TrackInfo
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.io.File
import java.io.InputStreamReader

/**
 * Core download engine wrapping yt-dlp via ProcessBuilder.
 *
 * Always downloads the best available audio stream without any format conversion.
 * Format conversion is handled separately by ConversionEngine.
 */
class DownloadEngine {

    private val gson = Gson()

    /**
     * Downloads the best audio for the given request using yt-dlp.
     *
     * Always downloads the highest quality audio available. Does NOT convert formats —
     * that is handled by ConversionEngine in a separate phase.
     *
     * @param request The download request with URL and output configuration.
     * @param onProgress Optional callback for download progress (percent, eta, speed).
     * @return Absolute path to the downloaded raw audio file.
     * @throws DownloadException if the download fails.
     */
    suspend fun download(
        request: DownloadRequest,
        onProgress: ((Float, Long, String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        println("Starting download: ${request.url} → ${request.outputDir}")

        val outputDir = File(request.outputDir)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // Use the track's safeFileName for output — each track has a unique name
        val outputTemplate = File(
            outputDir,
            "${request.trackInfo.safeFileName}.%(ext)s"
        ).absolutePath

        val cmd = mutableListOf(
            "yt-dlp",
            "-o", outputTemplate,
            "--no-check-certificates",
            "--no-warnings",
            "--socket-timeout", "30",
            "--retries", "3",
            "--fragment-retries", "3",
            "--no-continue",
            "--force-overwrites",
            "--fixup", "never",
            "--newline",
            // CRITICAL: Prevent yt-dlp from expanding playlist URLs.
            // We always pass individual video URLs, never playlists.
            "--no-playlist"
        )

        if (request.format == DownloadFormat.MP4) {
            val heightLimit = when (request.quality) {
                DownloadQuality.LOW -> 360
                DownloadQuality.MID -> 720
                DownloadQuality.HIGH -> 1080
            }
            cmd.add("-f")
            cmd.add("bestvideo[height<=${heightLimit}][ext=mp4]+bestaudio[ext=m4a]/best[height<=${heightLimit}]/best")
            cmd.add("--merge-output-format")
            cmd.add("mp4")
        } else {
            cmd.add("-f")
            cmd.add("bestaudio")
            cmd.add("--extract-audio")
        }

        cmd.add(request.url)

        var process: Process? = null
        try {
            process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val p = process

            // Read output stream line-by-line as it comes in
            p.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (!this@withContext.isActive) {
                        throw CancellationException("Download cancelled")
                    }
                    println("yt-dlp: $line") // For debug logging
                    if (line.contains("[download]")) {
                        val percent = parsePercent(line)
                        val speed = parseSpeed(line)
                        if (percent != null) {
                            onProgress?.invoke(percent, 0L, speed)
                        } else {
                            onProgress?.invoke(0f, 0L, speed)
                        }
                    }
                }
            }

            val exitCode = p.waitFor()
            if (exitCode != 0) {
                throw DownloadException("yt-dlp exited with code $exitCode")
            }

            println("Download complete.")

            val outputFile = findOutputFile(outputDir, request.trackInfo.safeFileName)
                ?: throw DownloadException("Download completed but output file not found")

            return@withContext outputFile.absolutePath

        } catch (e: Exception) {
            if (e is CancellationException) {
                println("Download coroutine cancelled, destroying yt-dlp process...")
            } else {
                System.err.println("Download failed: ${e.message}")
                e.printStackTrace()
            }
            throw e
        } finally {
            process?.let {
                if (it.isAlive) {
                    it.destroyForcibly()
                    println("Forcibly destroyed yt-dlp process")
                }
            }
        }
    }

    /**
     * Extracts metadata from a URL using yt-dlp's --dump-json.
     * For playlists, expands into individual track entries with full metadata.
     *
     * IMPORTANT: Does NOT use --flat-playlist because flat mode returns
     * minimal metadata (missing title, artist, duration for album tracks).
     * Instead uses full JSON extraction to get complete per-track info.
     *
     * @param url The URL to extract metadata from.
     * @return List of TrackInfo extracted from the URL.
     */
    suspend fun extractInfo(
        url: String,
        flatPlaylist: Boolean = false,
        onTrackExtracted: ((Int) -> Unit)? = null
    ): List<TrackInfo> = withContext(Dispatchers.IO) {
        println("Extracting info from: $url")

        val cmd = mutableListOf(
            "yt-dlp",
            "--dump-json",
            "--no-download",
            "--no-warnings"
        )
        if (flatPlaylist) {
            cmd.add("--flat-playlist")
        }
        cmd.add(url)

        var process: Process? = null
        try {
            process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val p = process
            val tracks = mutableListOf<TrackInfo>()
            var count = 0
            val errorLines = mutableListOf<String>()

            p.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (!this@withContext.isActive) {
                        throw CancellationException("Metadata extraction cancelled")
                    }
                    if (line.isNotBlank()) {
                        if (line.startsWith("{")) {
                            try {
                                val json = gson.fromJson(line, JsonObject::class.java)
                                val track = jsonToTrackInfo(json, url)
                                tracks.add(track)
                                count++
                                onTrackExtracted?.invoke(count)
                            } catch (e: Exception) {
                                // Ignore json parsing error for single bad line
                            }
                        } else {
                            if (line.contains("ERROR:") || line.contains("warning", ignoreCase = true) || errorLines.size < 10) {
                                errorLines.add(line)
                            }
                        }
                    }
                }
            }

            val exitCode = p.waitFor()
            if (exitCode != 0 && tracks.isEmpty()) {
                val errorMsg = errorLines.joinToString("\n").trim().takeIf { it.isNotBlank() }
                    ?: "yt-dlp exited with code $exitCode"
                throw DownloadException("yt-dlp extraction failed: $errorMsg")
            }

            return@withContext tracks
        } catch (e: Exception) {
            if (e is CancellationException) {
                println("Metadata extraction coroutine cancelled, destroying yt-dlp process...")
            } else {
                System.err.println("Info extraction failed: ${e.message}")
                e.printStackTrace()
            }
            throw e
        } finally {
            process?.let {
                if (it.isAlive) {
                    it.destroyForcibly()
                    println("Forcibly destroyed yt-dlp metadata process")
                }
            }
        }
    }

    /**
     * Updates yt-dlp by running `yt-dlp -U`.
     *
     * @return Output message from the update command.
     */
    suspend fun updateYtDlp(): String = withContext(Dispatchers.IO) {
        println("Checking/Updating yt-dlp...")
        val cmd = listOf("yt-dlp", "-U")
        var process: Process? = null
        try {
            process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                output.trim()
            } else {
                "Update failed with exit code $exitCode:\n$output".trim()
            }
        } catch (e: Exception) {
            System.err.println("yt-dlp update failed: ${e.message}")
            e.printStackTrace()
            "Error: Failed to execute yt-dlp update command. Is yt-dlp installed and on system PATH?\nDetails: ${e.message}"
        } finally {
            process?.let {
                if (it.isAlive) {
                    it.destroyForcibly()
                }
            }
        }
    }

    private fun findOutputFile(dir: File, baseName: String): File? {
        return dir.listFiles()
            ?.filter { it.nameWithoutExtension == baseName }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun jsonToTrackInfo(json: JsonObject, sourceUrl: String): TrackInfo {
        val title = json.get("title")?.asString ?: "Unknown Title"
        val artist = json.get("artist")?.asString
            ?: json.get("channel")?.asString
            ?: json.get("uploader")?.asString
            ?: "Unknown Artist"
        val album = json.get("album")?.asString
        val durationMs = (json.get("duration")?.asDouble?.times(1000))?.toLong() ?: 0L
        var thumbnail = json.get("thumbnail")?.asString
        if (thumbnail == null && json.has("thumbnails")) {
            val arr = json.getAsJsonArray("thumbnails")
            if (arr != null && arr.size() > 0) {
                thumbnail = arr.get(arr.size() - 1).asJsonObject.get("url")?.asString
            }
        }

        val detectedPlatform = when {
            sourceUrl.contains("instagram.com") -> Platform.INSTAGRAM
            sourceUrl.contains("music.youtube.com") -> Platform.YOUTUBE_MUSIC
            sourceUrl.contains("youtube.com") || sourceUrl.contains("youtu.be") -> Platform.YOUTUBE
            else -> Platform.OTHER
        }

        // CRITICAL: Build the individual video URL from the JSON, NOT from sourceUrl.
        // sourceUrl may be a playlist URL, but we need the per-video URL.
        val videoId = json.get("id")?.asString
        val videoUrl = json.get("webpage_url")?.asString
            ?: if (videoId != null) "https://www.youtube.com/watch?v=$videoId"
            else json.get("url")?.asString
            ?: sourceUrl

        return TrackInfo(
            title = title,
            artists = listOf(artist),
            album = album,
            durationMs = durationMs,
            albumArtUrl = thumbnail,
            source = detectedPlatform,
            sourceUrl = sourceUrl,
            // Store the individual video URL, NOT the playlist URL
            youtubeUrl = if (detectedPlatform == Platform.INSTAGRAM || detectedPlatform == Platform.OTHER) null else videoUrl
        )
    }

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

    private fun parsePercent(line: String): Float? {
        // Line typically looks like: [download]  12.3% of 5.67MiB at 1.23MiB/s ETA 00:04
        val percentIndex = line.indexOf("%")
        if (percentIndex != -1) {
            val start = line.lastIndexOf(' ', percentIndex)
            if (start != -1) {
                return line.substring(start + 1, percentIndex).trim().toFloatOrNull()
            }
        }
        return null
    }
}

class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
