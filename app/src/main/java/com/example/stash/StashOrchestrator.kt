package com.example.stash

import com.example.stash.download.*
import com.example.stash.model.ContentType
import com.example.stash.model.Platform
import com.example.stash.model.TrackInfo
import com.example.stash.parser.LinkParser
import com.example.stash.parser.ParsedLink
import com.example.stash.spotify.SpotifyWebScraper
import com.example.stash.storage.FileManager
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Main entry point for the Stash download algorithm (Desktop Version).
 */
class StashOrchestrator {

    companion object {
        private const val TAG = "StashOrchestrator"
    }

    private val spotifyScraper = SpotifyWebScraper()
    private val downloadEngine = DownloadEngine()
    private val fileManager = FileManager()

    val queueManager = DownloadQueueManager()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Exposes the download queue state (batches) for UI observation.
     */
    val downloadBatches: StateFlow<Map<String, DownloadBatch>>
        get() = queueManager.batches

    /**
     * Fetches and returns track metadata from the link without initiating a download.
     */
    suspend fun fetchMetadata(link: String): List<TrackInfo> {
        val parsedLink = LinkParser.parse(link)
            ?: throw IllegalArgumentException("Unsupported link: $link")
        println("Fetching metadata for: ${parsedLink.platform} / ${parsedLink.contentType}")
        return fetchTracks(parsedLink)
    }

    /**
     * Enqueues tracks for download, saving them to the secure cache directory first.
     */
    fun enqueueTracks(
        tracks: List<TrackInfo>,
        batchName: String,
        quality: DownloadQuality = DownloadQuality.AUDIO_320,
        format: DownloadFormat = DownloadFormat.MP3
    ) {
        if (tracks.isEmpty()) return

        val userHome = System.getProperty("user.home")
        val cacheDir = File(userHome, ".stash_cache").apply {
            if (!exists()) mkdirs()
        }.absolutePath

        val requests = tracks.map { track ->
            DownloadRequest(
                url = track.youtubeUrl ?: track.sourceUrl,
                trackInfo = track,
                outputDir = cacheDir,
                quality = quality,
                format = format
            )
        }

        queueManager.enqueueBatch(batchName, requests)
        println("Enqueued batch '$batchName' with ${requests.size} download(s) to cache directory")
    }

    /**
     * Processes a Spotify or YouTube link and starts downloading.
     */
    suspend fun processLink(
        link: String,
        outputDir: String? = null,
        quality: DownloadQuality = DownloadQuality.AUDIO_320,
        format: DownloadFormat = DownloadFormat.MP3
    ): List<TrackInfo> {
        val tracks = fetchMetadata(link)
        val batchName = if (tracks.size == 1) {
            tracks[0].title
        } else {
            tracks[0].album ?: "Stash Playlist"
        }
        enqueueTracks(tracks, batchName, quality, format)
        return tracks
    }

    /**
     * Processes multiple links at once (batch download).
     */
    suspend fun processLinks(
        links: List<String>,
        outputDir: String? = null,
        quality: DownloadQuality = DownloadQuality.AUDIO_320,
        format: DownloadFormat = DownloadFormat.MP3
    ): List<TrackInfo> {
        val allTracks = mutableListOf<TrackInfo>()
        for (link in links) {
            try {
                val tracks = processLink(link, outputDir, quality, format)
                allTracks.addAll(tracks)
            } catch (e: Exception) {
                System.err.println("Failed to process link: $link")
                e.printStackTrace()
            }
        }
        return allTracks
    }

    /**
     * Validates a link without starting any downloads.
     */
    fun validateLink(link: String): ParsedLink? = LinkParser.parse(link)

    /**
     * Cancels a specific download.
     */
    fun cancelDownload(id: String) = queueManager.cancel(id)

    /**
     * Cancels all downloads within a specific batch only.
     */
    fun cancelBatch(batchId: String) = queueManager.cancelBatch(batchId)

    /**
     * Cancels all downloads.
     */
    fun cancelAll() = queueManager.cancelAll()

    /**
     * Cleans up resources. Call when the app is being destroyed.
     */
    fun shutdown() {
        queueManager.shutdown()
        scope.cancel()
    }

    // ──────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Fetches track metadata based on the parsed link type.
     * No API keys needed for any platform.
     */
    private suspend fun fetchTracks(parsedLink: ParsedLink): List<TrackInfo> {
        return when (parsedLink.platform) {
            Platform.SPOTIFY -> fetchSpotifyTracks(parsedLink)
            Platform.YOUTUBE, Platform.YOUTUBE_MUSIC -> fetchYouTubeTracks(parsedLink)
            Platform.INSTAGRAM -> fetchInstagramTracks(parsedLink)
        }
    }

    /**
     * Extracts Instagram post/reel metadata via yt-dlp.
     */
    private suspend fun fetchInstagramTracks(parsedLink: ParsedLink): List<TrackInfo> {
        return downloadEngine.extractInfo(parsedLink.originalUrl)
    }

    /**
     * Extracts Spotify track metadata by scraping the public page.
     * No Client ID, no Client Secret, no API key — just public HTML.
     */
    private suspend fun fetchSpotifyTracks(parsedLink: ParsedLink): List<TrackInfo> =
        withContext(Dispatchers.IO) {
            spotifyScraper.extractTracks(parsedLink)
        }

    /**
     * Extracts track info from YouTube/YouTube Music links via yt-dlp.
     */
    private suspend fun fetchYouTubeTracks(parsedLink: ParsedLink): List<TrackInfo> {
        val url = when {
            parsedLink.contentType == ContentType.PLAYLIST ->
                "https://www.youtube.com/playlist?list=${parsedLink.id}"
            parsedLink.platform == Platform.YOUTUBE_MUSIC ->
                "https://music.youtube.com/watch?v=${parsedLink.id}"
            else ->
                "https://www.youtube.com/watch?v=${parsedLink.id}"
        }

        return downloadEngine.extractInfo(url).map { track ->
            track.copy(youtubeUrl = track.sourceUrl)
        }
    }
}
