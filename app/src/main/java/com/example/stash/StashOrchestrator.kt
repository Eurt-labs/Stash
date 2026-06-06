package com.example.stash

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
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
 * Main entry point for the Stash download algorithm.
 *
 * **Zero authentication required.** Just paste a link and it downloads.
 *
 * This orchestrator ties together all components:
 * 1. **Link Parsing** → Identifies the platform and content type
 * 2. **Metadata Extraction** → Scrapes public Spotify pages / uses yt-dlp for YouTube
 * 3. **YouTube Matching** → Resolves Spotify tracks to YouTube URLs
 * 4. **Download Execution** → Manages the concurrent download queue
 * 5. **Post-Processing** → Tags files and registers in MediaStore
 *
 * ## Usage
 * ```kotlin
 * val orchestrator = StashOrchestrator(context)
 *
 * // Download a Spotify playlist (no API key needed!)
 * orchestrator.processLink(
 *     link = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M",
 *     quality = DownloadQuality.AUDIO_320,
 *     format = DownloadFormat.MP3
 * )
 *
 * // Download a YouTube video
 * orchestrator.processLink(
 *     link = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
 *     format = DownloadFormat.VIDEO_720
 * )
 *
 * // Observe progress
 * lifecycleScope.launch {
 *     orchestrator.downloadItems.collect { items ->
 *         items.forEach { (id, item) ->
 *             println("${item.trackInfo.title}: ${item.state} ${(item.progress * 100).toInt()}%")
 *         }
 *     }
 * }
 * ```
 */
class StashOrchestrator(private val context: Context) {

    companion object {
        private const val TAG = "StashOrchestrator"
    }

    // No API keys, no auth — uses public web scraping
    private val spotifyScraper = SpotifyWebScraper()
    private val downloadEngine = DownloadEngine(context)
    private val fileManager = FileManager(context)

    val queueManager = DownloadQueueManager(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Service binding ──
    private var downloadService: DownloadService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DownloadService.LocalBinder
            downloadService = binder.getService()
            downloadService?.attachQueueManager(queueManager)
            serviceBound = true
            Log.d(TAG, "Bound to DownloadService")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
            serviceBound = false
            Log.d(TAG, "Unbound from DownloadService")
        }
    }

    /**
     * Exposes the download queue state (batches) for UI observation.
     */
    val downloadBatches: StateFlow<Map<String, DownloadBatch>>
        get() = queueManager.batches

    /**
     * Processes a Spotify or YouTube link and starts downloading.
     * **No authentication required for any platform.**
     *
     * @param link A Spotify or YouTube URL
     * @param outputDir Target directory (defaults to Downloads/Stash/)
     * @param quality Audio quality preset
     * @param format Download format (audio or video)
     * @return List of tracks that were enqueued for download
     * @throws IllegalArgumentException if the link is not supported
     */
    /**
     * Fetches and returns track metadata from the link without initiating a download.
     */
    suspend fun fetchMetadata(link: String): List<TrackInfo> {
        val parsedLink = LinkParser.parse(link)
            ?: throw IllegalArgumentException("Unsupported link: $link")
        Log.d(TAG, "Fetching metadata for: ${parsedLink.platform} / ${parsedLink.contentType}")
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

        startDownloadService()

        val cacheDir = File(context.cacheDir, "downloads").apply {
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
        Log.d(TAG, "Enqueued batch '$batchName' with ${requests.size} download(s) to cache directory")
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
                Log.e(TAG, "Failed to process link: $link", e)
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
     * Cancels all downloads.
     */
    fun cancelAll() = queueManager.cancelAll()

    /**
     * Cleans up resources. Call when the app is being destroyed.
     */
    fun shutdown() {
        queueManager.shutdown()
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
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

    /**
     * Starts the foreground download service.
     */
    private fun startDownloadService() {
        if (!serviceBound) {
            val intent = DownloadService.newIntent(context)
            context.startForegroundService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
}
