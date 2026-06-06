package com.example.stash.download

import com.example.stash.model.Platform
import com.example.stash.model.TrackInfo
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader

/**
 * Core download engine wrapping yt-dlp via ProcessBuilder.
 */
class DownloadEngine {

    private val gson = Gson()

    suspend fun download(
        request: DownloadRequest,
        onProgress: ((Float, Long, String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        println("Starting download: ${request.url} → ${request.outputDir}")

        val outputDir = File(request.outputDir)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

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
            "--prefer-free-formats",
            "--newline"
        )

        configureFormat(cmd, request)
        cmd.add(request.url)

        try {
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            // Read output stream line-by-line as it comes in
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
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

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw DownloadException("yt-dlp exited with code $exitCode")
            }

            println("Download complete.")

            val outputFile = findOutputFile(outputDir, request.trackInfo.safeFileName)
                ?: throw DownloadException("Download completed but output file not found")

            return@withContext outputFile.absolutePath

        } catch (e: Exception) {
            System.err.println("Download failed: ${e.message}")
            e.printStackTrace()
            throw DownloadException("Download failed: ${e.message}", e)
        }
    }

    suspend fun extractInfo(url: String): List<TrackInfo> = withContext(Dispatchers.IO) {
        println("Extracting info from: $url")

        val cmd = listOf(
            "yt-dlp",
            "--dump-json",
            "--no-download",
            "--no-warnings",
            "--flat-playlist",
            url
        )

        try {
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val lines = InputStreamReader(process.inputStream).readLines()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw DownloadException("yt-dlp extraction failed with code $exitCode")
            }

            return@withContext lines
                .filter { it.isNotBlank() && it.startsWith("{") }
                .mapNotNull { line ->
                    try {
                        val json = gson.fromJson(line, JsonObject::class.java)
                        jsonToTrackInfo(json, url)
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            System.err.println("Info extraction failed: ${e.message}")
            e.printStackTrace()
            throw DownloadException("Failed to extract info: ${e.message}", e)
        }
    }

    private fun configureFormat(cmd: MutableList<String>, request: DownloadRequest) {
        if (request.format.isVideo) {
            val heightLimit = when (request.format) {
                DownloadFormat.VIDEO_360 -> 360
                DownloadFormat.VIDEO_720 -> 720
                DownloadFormat.VIDEO_1080 -> 1080
                DownloadFormat.VIDEO_BEST -> 0
                else -> 720
            }

            if (heightLimit > 0) {
                cmd.addAll(listOf("-f", "bestvideo[height<=$heightLimit]+bestaudio/best[height<=$heightLimit]"))
            } else {
                cmd.addAll(listOf("-f", "bestvideo+bestaudio/best"))
            }
            cmd.addAll(listOf("--merge-output-format", "mp4"))

        } else {
            cmd.addAll(listOf("-f", "bestaudio"))
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
        val thumbnail = json.get("thumbnail")?.asString

        val detectedPlatform = when {
            sourceUrl.contains("instagram.com") -> Platform.INSTAGRAM
            sourceUrl.contains("music.youtube.com") -> Platform.YOUTUBE_MUSIC
            else -> Platform.YOUTUBE
        }

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
