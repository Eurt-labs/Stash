package com.example.stash

import com.example.stash.download.*
import com.example.stash.model.ContentType
import com.example.stash.model.Platform
import com.example.stash.model.TrackInfo
import com.example.stash.parser.LinkParser
import com.example.stash.parser.ParsedLink

import com.example.stash.storage.FileManager
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main entry point for the Stash download algorithm (Desktop Version).
 *
 * Orchestrates the 5-phase sequential pipeline:
 * 1. FETCH    — Scrape metadata from link (YouTube/Instagram/Other)
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

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _updateStatus = MutableStateFlow("")
    val updateStatus: StateFlow<String> = _updateStatus.asStateFlow()

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
    suspend fun fetchMetadata(link: String, flatPlaylist: Boolean = false): List<TrackInfo> {
        val parsedLink = LinkParser.parse(link)
            ?: throw IllegalArgumentException("Unsupported link: $link")
        println("Fetching metadata for: ${parsedLink.platform} / ${parsedLink.contentType}")
        
        _isFetching.value = true
        _fetchingStatus.value = "Parsing link..."
        return try {
            fetchTracks(parsedLink, flatPlaylist)
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
        isIndividualTrack: Boolean = false,
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
                if (track.source == Platform.YOUTUBE_MUSIC || track.sourceUrl.startsWith("ytsearch")) {
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
                format = resolvedFormat,
                isIndividualTrack = isIndividualTrack
            )
        }

        queueManager.enqueueBatch(batchName, requests, outputDir)
        println("Enqueued batch '$batchName' with ${requests.size} download(s)")
        println("  Quality: ${quality.label}")
        println("  Format: ${format.label}")
        println("  Output: $outputDir")
    }

    /**
     * Processes a YouTube or other platform link and starts the download pipeline.
     * Uses the current quality, format, and output dir settings.
     */
    suspend fun processLink(
        link: String,
        quality: DownloadQuality = _quality.value,
        format: DownloadFormat = _format.value,
        outputDir: String = _outputDir.value
    ): List<TrackInfo> {
        val parsedLink = LinkParser.parse(link)
            ?: throw IllegalArgumentException("Unsupported link: $link")

        val isChannel = parsedLink.originalUrl.contains("/@") ||
                parsedLink.originalUrl.contains("/channel/") ||
                parsedLink.originalUrl.contains("/c/")

        val channelArtistData = if (isChannel) {
            _isFetching.value = true
            _fetchingStatus.value = "Resolving channel artist..."
            val resolvedData = withContext(Dispatchers.IO) {
                fetchChannelData(parsedLink.originalUrl)
            }
            _isFetching.value = false
            _fetchingStatus.value = ""
            if (resolvedData != null) {
                resolvedData.first to resolvedData.second
            } else {
                parsedLink.id to null
            }
        } else {
            null to null
        }

        val artistName = channelArtistData.first
        val playlistId = channelArtistData.second

        val fetchLink = if (isChannel) {
            if (playlistId != null) {
                "https://www.youtube.com/playlist?list=$playlistId"
            } else {
                "ytsearch150:$artistName music.youtube.com"
            }
        } else {
            link
        }

        val isPlaylistOrSearch = parsedLink.contentType == ContentType.PLAYLIST ||
                isChannel ||
                parsedLink.originalUrl.startsWith("ytsearch")

        val tracks = fetchMetadata(fetchLink, flatPlaylist = isPlaylistOrSearch)
        if (tracks.isEmpty()) return tracks

        val isSearchOrChannel = isChannel || parsedLink.originalUrl.startsWith("ytsearch")
        val artistQuery = artistName ?: if (parsedLink.originalUrl.startsWith("ytsearch")) parsedLink.id else null

        val filteredTracks = if (isSearchOrChannel && artistQuery != null) {
            val normalizedQuery = normalizeName(artistQuery)
            tracks
                .map { track ->
                    val updatedArtists = if (isChannel && artistName != null) {
                        listOf(artistName)
                    } else if (track.artists.isEmpty() || track.artists.first() == "Unknown Artist") {
                        listOf(artistQuery)
                    } else {
                        track.artists
                    }
                    track.copy(artists = updatedArtists, sourceUrl = parsedLink.originalUrl)
                }
                .filter { track ->
                    track.artists.any { artist ->
                        val normalizedArtist = normalizeName(artist)
                        normalizedArtist.contains(normalizedQuery) || normalizedQuery.contains(normalizedArtist)
                    }
                }
        } else {
            tracks
        }

        if (filteredTracks.isEmpty()) return emptyList()

        val defaultGroupName = if (isSearchOrChannel && artistQuery != null) {
            artistQuery
        } else if (filteredTracks.size == 1) {
            filteredTracks[0].title
        } else {
            "Stash Playlist"
        }

        val groupedTracks = if (isSearchOrChannel) {
            mapOf(defaultGroupName to filteredTracks)
        } else {
            filteredTracks.groupBy {
                it.album?.trim()?.takeIf { name -> name.isNotBlank() } ?: defaultGroupName
            }
        }

        val isIndividualTrack = !isPlaylistOrSearch
        groupedTracks.forEach { (albumName, albumTracks) ->
            enqueueTracks(albumTracks, albumName, isIndividualTrack, quality, format, outputDir)
        }

        return filteredTracks
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

    /**
     * Checks and updates yt-dlp dependencies asynchronously.
     */
    fun checkForUpdates() {
        if (_isUpdating.value) return
        _isUpdating.value = true
        _updateStatus.value = "Checking for updates..."
        scope.launch {
            try {
                val result = downloadEngine.updateYtDlp()
                _updateStatus.value = result
            } catch (e: Exception) {
                _updateStatus.value = "Update failed: ${e.message}"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    /**
     * Resets the update status string back to empty.
     */
    fun clearUpdateStatus() {
        _updateStatus.value = ""
    }

    // ──────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────

    private suspend fun fetchTracks(parsedLink: ParsedLink, flatPlaylist: Boolean = false): List<TrackInfo> {
        return when (parsedLink.platform) {
            Platform.YOUTUBE, Platform.YOUTUBE_MUSIC -> fetchYouTubeTracks(parsedLink, flatPlaylist)
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
    private suspend fun fetchYouTubeTracks(parsedLink: ParsedLink, flatPlaylist: Boolean = false): List<TrackInfo> {
        val isChannel = parsedLink.originalUrl.contains("/@") ||
                parsedLink.originalUrl.contains("/channel/") ||
                parsedLink.originalUrl.contains("/c/")

        val url = if (isChannel) {
            _fetchingStatus.value = "Resolving channel artist..."
            val resolvedData = withContext(Dispatchers.IO) {
                fetchChannelData(parsedLink.originalUrl)
            }
            val artistName = resolvedData?.first ?: parsedLink.id
            val playlistId = resolvedData?.second
            
            println("Resolved channel artist name: $artistName, playlist ID: $playlistId")
            
            if (playlistId != null) {
                "https://www.youtube.com/playlist?list=$playlistId"
            } else {
                "ytsearch150:$artistName music.youtube.com"
            }
        } else {
            when {
                parsedLink.originalUrl.startsWith("ytsearch") ->
                    parsedLink.originalUrl
                parsedLink.contentType == ContentType.PLAYLIST ->
                    "https://www.youtube.com/playlist?list=${parsedLink.id}"
                parsedLink.platform == Platform.YOUTUBE_MUSIC ->
                    "https://music.youtube.com/watch?v=${parsedLink.id}"
                else ->
                    "https://www.youtube.com/watch?v=${parsedLink.id}"
            }
        }

        _fetchingStatus.value = "Fetching metadata from YouTube..."
        return downloadEngine.extractInfo(url, flatPlaylist) { count ->
            _fetchingStatus.value = "Fetching metadata from YouTube (extracted $count tracks)..."
        }
    }

    private fun fetchChannelData(url: String): Pair<String, String?>? {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val html = response.body?.string() ?: return null
                
                val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
                val title = titleMatch?.groupValues?.get(1)?.trim() ?: return null
                val artistName = cleanChannelTitle(title)
                
                val playlistId = findTopSongsPlaylistId(html)
                Pair(artistName, playlistId)
            }
        } catch (e: Exception) {
            System.err.println("Failed to fetch channel data: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun findTopSongsPlaylistId(html: String): String? {
        val pushRegex = Regex("""initialData\.push\(\{path:\s*'(.*?)'.*?data:\s*'(.*?)'\s*\}\);""", RegexOption.DOT_MATCHES_ALL)
        val matches = pushRegex.findAll(html)
        
        val gson = com.google.gson.Gson()
        for (match in matches) {
            try {
                val escapedData = match.groupValues[2]
                val decoded = unescapeJsHex(escapedData)
                    .replace("\\\"", "\"")
                    .replace("\\/", "/")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                
                val json = gson.fromJson(decoded, com.google.gson.JsonElement::class.java)
                val playlistId = searchForTopSongsPlaylistId(json)
                if (playlistId != null) {
                    return playlistId
                }
            } catch (e: Exception) {
                System.err.println("Error parsing push in findTopSongsPlaylistId: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // Fallback: search for any OLAK5uy_ playlist ID in the html using regex
        val fallbackRegex = Regex("""OLAK5uy_[a-zA-Z0-9_-]+""")
        val fallbackMatch = fallbackRegex.find(html)
        if (fallbackMatch != null) {
            return fallbackMatch.value
        }
        
        return null
    }

    private fun unescapeJsHex(input: String): String {
        val regex = Regex("""\\x([0-9a-fA-F]{2})""")
        return regex.replace(input) { matchResult ->
            val hex = matchResult.groupValues[1]
            val char = hex.toInt(16).toChar()
            char.toString()
        }
    }

    private fun searchForTopSongsPlaylistId(element: com.google.gson.JsonElement): String? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            
            var isTopSongs = false
            if (obj.has("title")) {
                val titleEl = obj.get("title")
                if (titleEl.isJsonObject) {
                    val titleObj = titleEl.asJsonObject
                    if (titleObj.has("runs")) {
                        val runs = titleObj.getAsJsonArray("runs")
                        val runsText = StringBuilder()
                        for (run in runs) {
                            if (run.isJsonObject && run.asJsonObject.has("text")) {
                                runsText.append(run.asJsonObject.get("text").asString)
                            }
                        }
                        if (runsText.toString().trim().lowercase() == "top songs") {
                            isTopSongs = true
                        }
                    }
                }
            }
            
            if (isTopSongs) {
                val extracted = extractOlakId(obj)
                if (extracted != null) return extracted
            }
            
            for (entry in obj.entrySet()) {
                val res = searchForTopSongsPlaylistId(entry.value)
                if (res != null) return res
            }
        } else if (element.isJsonArray) {
            val arr = element.asJsonArray
            for (item in arr) {
                val res = searchForTopSongsPlaylistId(item)
                if (res != null) return res
            }
        }
        return null
    }

    private fun extractOlakId(element: com.google.gson.JsonElement): String? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("playlistId")) {
                val pId = obj.get("playlistId").asString
                if (pId.startsWith("OLAK5uy_")) {
                    return pId
                }
            }
            if (obj.has("browseId")) {
                val bId = obj.get("browseId").asString
                if (bId.startsWith("OLAK5uy_")) {
                    return bId
                }
                if (bId.startsWith("VLOLAK5uy_")) {
                    return bId.substring(2)
                }
            }
            for (entry in obj.entrySet()) {
                val res = extractOlakId(entry.value)
                if (res != null) return res
            }
        } else if (element.isJsonArray) {
            val arr = element.asJsonArray
            for (item in arr) {
                val res = extractOlakId(item)
                if (res != null) return res
            }
        }
        return null
    }

    private fun cleanChannelTitle(title: String): String {
        var clean = title
            .replace(" - YouTube Music", "", ignoreCase = true)
            .replace(" - YouTube", "", ignoreCase = true)

        // Remove (@handle) if present, e.g. "The Weeknd (@theweeknd)"
        clean = Regex("""\s*\(@[a-zA-Z0-9_.-]+\)""").replace(clean, "")

        // Unescape common HTML entities
        clean = clean
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

        return clean.trim()
    }

    private fun normalizeName(name: String): String {
        return name.lowercase()
            .replace(Regex("""\s+"""), "")
            .replace(Regex("""[^a-z0-9]"""), "")
            .replace("topic", "")
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
