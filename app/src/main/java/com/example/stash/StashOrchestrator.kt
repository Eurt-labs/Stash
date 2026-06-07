package com.example.stash

import com.example.stash.download.*
import com.example.stash.model.ContentType
import com.example.stash.model.Platform
import com.example.stash.model.TrackInfo
import com.example.stash.parser.LinkParser
import com.example.stash.parser.ParsedLink

import com.example.stash.storage.FileManager
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main entry point for the Stash download algorithm (Desktop Version).
 *
 * Orchestrates the 5-phase sequential pipeline:
 * 1. FETCH    — Scrape metadata from link (Spotify/YouTube)
 * 2. DOWNLOAD — yt-dlp downloads one-by-one from temp JSON manifest
 * 3. CONVERT  — FFmpeg converts one-by-one to user-selected format/quality
 * 4. MOVE     — Tag and move to user-selected output folder
 * 5. CLEANUP  — Delete temp JSON manifest and cache files
 */
class StashOrchestrator {

    companion object {
        private const val TAG = "StashOrchestrator"
    }


    private val downloadEngine = DownloadEngine()
    private val fileManager = FileManager()

    val queueManager = DownloadQueueManager()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── User-configurable state ──

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val _fetchingStatus = MutableStateFlow("")
    val fetchingStatus: StateFlow<String> = _fetchingStatus.asStateFlow()

    /** The user-selected output directory. Defaults to ~/Downloads/Stash */
    private val _outputDir = MutableStateFlow(fileManager.getDefaultDownloadDir())
    val outputDir: StateFlow<String> = _outputDir.asStateFlow()

    /** The user-selected download quality. Defaults to HIGH. */
    private val _quality = MutableStateFlow(DownloadQuality.HIGH)
    val quality: StateFlow<DownloadQuality> = _quality.asStateFlow()

    /** The user-selected download format. Defaults to AUTO (Auto-Detect). */
    private val _format = MutableStateFlow(DownloadFormat.AUTO)
    val format: StateFlow<DownloadFormat> = _format.asStateFlow()

    /**
     * Exposes the download queue state (batches) for UI observation.
     */
    val downloadBatches: StateFlow<Map<String, DownloadBatch>>
        get() = queueManager.batches

    // ── User configuration setters ──

    /**
     * Sets the output directory where downloaded files will be saved.
     */
    fun setOutputDirectory(path: String) {
        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()
        _outputDir.value = dir.absolutePath
        println("Output directory set to: ${dir.absolutePath}")
    }

    /**
     * Sets the download quality preset.
     */
    fun setQuality(quality: DownloadQuality) {
        _quality.value = quality
        println("Quality set to: ${quality.label}")
    }

    /**
     * Sets the download format.
     */
    fun setFormat(format: DownloadFormat) {
        _format.value = format
        println("Format set to: ${format.label}")
    }

    /**
     * Fetches and returns track metadata from the link without initiating a download.
     */
    suspend fun fetchMetadata(link: String): List<TrackInfo> {
        val parsedLink = LinkParser.parse(link)
            ?: throw IllegalArgumentException("Unsupported link: $link")
        println("Fetching metadata for: ${parsedLink.platform} / ${parsedLink.contentType}")
        
        _isFetching.value = true
        _fetchingStatus.value = "Parsing link..."
        return try {
            fetchTracks(parsedLink)
        } finally {
            _isFetching.value = false
            _fetchingStatus.value = ""
        }
    }

    /**
     * Enqueues tracks for download using the current quality, format, and output dir settings.
     * Tracks are saved to a temp JSON manifest before downloading begins.
     */
    fun enqueueTracks(
        tracks: List<TrackInfo>,
        batchName: String,
        quality: DownloadQuality = _quality.value,
        format: DownloadFormat = _format.value,
        outputDir: String = _outputDir.value
    ) {
        if (tracks.isEmpty()) return

        val cacheDir = File(System.getProperty("user.home"), ".stash_cache").apply {
            if (!exists()) mkdirs()
        }.absolutePath

        val requests = tracks.map { track ->
            val resolvedFormat = if (format == DownloadFormat.AUTO) {
                if (track.source == Platform.YOUTUBE_MUSIC) {
                    DownloadFormat.MP3
                } else {
                    DownloadFormat.MP4
                }
            } else if (format == DownloadFormat.YOUTUBE_VIDEO || format == DownloadFormat.INSTAGRAM_VIDEO || format == DownloadFormat.OTHER_VIDEO) {
                DownloadFormat.MP4
            } else {
                format
            }

            DownloadRequest(
                url = track.youtubeUrl ?: track.sourceUrl,
                trackInfo = track,
                outputDir = cacheDir,
                quality = quality,
                format = resolvedFormat
            )
        }

        queueManager.enqueueBatch(batchName, requests, outputDir)
        println("Enqueued batch '$batchName' with ${requests.size} download(s)")
        println("  Quality: ${quality.label}")
        println("  Format: ${format.label}")
        println("  Output: $outputDir")
    }

    /**
     * Processes a Spotify or YouTube link and starts the download pipeline.
     * Uses the current quality, format, and output dir settings.
     */
    suspend fun processLink(
        link: String,
        quality: DownloadQuality = _quality.value,
        format: DownloadFormat = _format.value,
        outputDir: String = _outputDir.value
    ): List<TrackInfo> {
        val tracks = fetchMetadata(link)
        if (tracks.isEmpty()) return tracks

        // Group tracks by album name
        // If a track's album is null or blank, group under a default name
        val defaultGroupName = if (tracks.size == 1) {
            tracks[0].title
        } else {
            "Stash Playlist"
        }

        val groupedTracks = tracks.groupBy {
            it.album?.trim()?.takeIf { name -> name.isNotBlank() } ?: defaultGroupName
        }

        groupedTracks.forEach { (albumName, albumTracks) ->
            enqueueTracks(albumTracks, albumName, quality, format, outputDir)
        }

        return tracks
    }

    /**
     * Processes multiple links at once (batch download).
     */
    suspend fun processLinks(
        links: List<String>,
        quality: DownloadQuality = _quality.value,
        format: DownloadFormat = _format.value,
        outputDir: String = _outputDir.value
    ): List<TrackInfo> {
        val allTracks = mutableListOf<TrackInfo>()
        for (link in links) {
            try {
                val tracks = processLink(link, quality, format, outputDir)
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

    private suspend fun fetchTracks(parsedLink: ParsedLink): List<TrackInfo> {
        return when (parsedLink.platform) {
            Platform.YOUTUBE, Platform.YOUTUBE_MUSIC -> fetchYouTubeTracks(parsedLink)
            Platform.INSTAGRAM -> fetchInstagramTracks(parsedLink)
            Platform.OTHER -> fetchOtherTracks(parsedLink)
        }
    }

    /**
     * Extracts Instagram post/reel metadata via yt-dlp.
     */
    private suspend fun fetchInstagramTracks(parsedLink: ParsedLink): List<TrackInfo> {
        _fetchingStatus.value = "Fetching metadata from Instagram..."
        return downloadEngine.extractInfo(parsedLink.originalUrl) { count ->
            _fetchingStatus.value = "Fetching metadata from Instagram (extracted $count)..."
        }
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

        _fetchingStatus.value = "Fetching metadata from YouTube..."
        return downloadEngine.extractInfo(url) { count ->
            _fetchingStatus.value = "Fetching metadata from YouTube (extracted $count tracks)..."
        }
    }

    /**
     * Extracts generic/other post/reel/video metadata via yt-dlp.
     */
    private suspend fun fetchOtherTracks(parsedLink: ParsedLink): List<TrackInfo> {
        _fetchingStatus.value = "Fetching metadata from link..."
        return downloadEngine.extractInfo(parsedLink.originalUrl) { count ->
            _fetchingStatus.value = "Fetching metadata from link (extracted $count)..."
        }
    }
}
