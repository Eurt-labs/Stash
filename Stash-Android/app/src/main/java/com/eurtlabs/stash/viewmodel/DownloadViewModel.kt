package com.eurtlabs.stash.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eurtlabs.stash.data.downloader.LogManager
import com.eurtlabs.stash.data.downloader.YoutubeDLManager
import com.eurtlabs.stash.data.model.ColorTheme
import com.eurtlabs.stash.data.model.DownloadBatch
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadItem
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.DownloadState
import com.eurtlabs.stash.data.model.MediaType
import com.eurtlabs.stash.data.model.NavigationTab
import com.eurtlabs.stash.data.model.Platform
import com.eurtlabs.stash.data.model.SearchFilter
import com.eurtlabs.stash.data.model.SearchResultItem
import com.eurtlabs.stash.data.model.StashSettings
import com.eurtlabs.stash.data.model.TrackInfo
import com.eurtlabs.stash.data.parser.LinkParser
import com.eurtlabs.stash.data.storage.LibraryStore
import com.eurtlabs.stash.data.storage.StorageManager
import com.eurtlabs.stash.data.transcoder.MediaTagger
import com.eurtlabs.stash.service.DownloadForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _batches = MutableStateFlow<List<DownloadBatch>>(emptyList())
    val batches: StateFlow<List<DownloadBatch>> = _batches.asStateFlow()

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val _fetchingMessage = MutableStateFlow("")
    val fetchingMessage: StateFlow<String> = _fetchingMessage.asStateFlow()

    // Search State
    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    // First launch state
    private val _showStorageDialog = MutableStateFlow(StorageManager.isFirstLaunch(application))
    val showStorageDialog: StateFlow<Boolean> = _showStorageDialog.asStateFlow()

    private val _settings = MutableStateFlow(
        StashSettings(
            outputDir = StorageManager.getDisplayStoragePath(application),
            isFirstLaunchDone = !StorageManager.isFirstLaunch(application),
            mediaType = MediaType.AUDIO,
            audioFormat = DownloadFormat.MP3,
            audioQuality = DownloadQuality.AUDIO_320K,
            videoFormat = DownloadFormat.MP4,
            videoQuality = DownloadQuality.VIDEO_1080P,
            theme = ColorTheme.OBSIDIAN
        )
    )
    val settings: StateFlow<StashSettings> = _settings.asStateFlow()

    init {
        // Load persisted library records and scan disk on startup
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val stored = LibraryStore.loadLibrary(app)
            if (stored.isNotEmpty()) {
                _batches.value = stored
            }
        }
    }

    fun updateTheme(theme: ColorTheme) {
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateMediaType(mediaType: MediaType) {
        _settings.value = _settings.value.copy(mediaType = mediaType)
    }

    fun updateFormat(format: DownloadFormat) {
        if (format.isAudioOnly) {
            _settings.value = _settings.value.copy(
                mediaType = MediaType.AUDIO,
                audioFormat = format
            )
        } else {
            _settings.value = _settings.value.copy(
                mediaType = MediaType.VIDEO,
                videoFormat = format
            )
        }
    }

    fun updateQuality(quality: DownloadQuality) {
        if (quality.isAudioOnly) {
            _settings.value = _settings.value.copy(audioQuality = quality)
        } else {
            _settings.value = _settings.value.copy(videoQuality = quality)
        }
    }

    fun confirmStorageDefault() {
        val app = getApplication<Application>()
        StorageManager.setUseDefaultStorage(app)
        _settings.value = _settings.value.copy(
            outputDir = StorageManager.getDisplayStoragePath(app),
            isFirstLaunchDone = true
        )
        _showStorageDialog.value = false
    }

    fun confirmStorageCustom(uri: Uri) {
        val app = getApplication<Application>()
        StorageManager.setCustomStorage(app, uri)
        _settings.value = _settings.value.copy(
            outputDir = StorageManager.getDisplayStoragePath(app),
            customDirUri = uri.toString(),
            isFirstLaunchDone = true
        )
        _showStorageDialog.value = false
    }

    fun openStorageDialog() {
        _showStorageDialog.value = true
    }

    fun dismissStorageDialog() {
        _showStorageDialog.value = false
    }

    fun setSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = YoutubeDLManager.searchMedia(query.trim(), _searchFilter.value)
                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Search error: ${e.message}", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun parseAndEnqueue(url: String) {
        val parsedLink = LinkParser.parse(url)
        if (parsedLink == null) {
            LogManager.append("DownloadViewModel", "Invalid URL: $url")
            return
        }

        viewModelScope.launch {
            _isFetching.value = true
            _fetchingMessage.value = "Analyzing link and fetching metadata..."

            try {
                val tracks = YoutubeDLManager.extractMetadata(url)
                if (tracks.isNotEmpty()) {
                    val batchId = UUID.randomUUID().toString()
                    val batchTitle = tracks.firstOrNull()?.title ?: "Download Batch"
                    val currentSettings = _settings.value
                    val currentFormat = if (currentSettings.mediaType == MediaType.AUDIO) currentSettings.audioFormat else currentSettings.videoFormat
                    val currentQuality = if (currentSettings.mediaType == MediaType.AUDIO) currentSettings.audioQuality else currentSettings.videoQuality

                    val items = tracks.map { track ->
                        DownloadItem(
                            id = UUID.randomUUID().toString(),
                            batchId = batchId,
                            track = track,
                            quality = currentQuality,
                            format = currentFormat,
                            state = DownloadState.QUEUED
                        )
                    }

                    val newBatch = DownloadBatch(
                        id = batchId,
                        title = batchTitle,
                        platform = parsedLink.platform,
                        totalTracks = items.size,
                        items = items
                    )

                    _batches.value = listOf(newBatch) + _batches.value
                    processQueue()
                }
            } catch (e: Exception) {
                LogManager.append("DownloadViewModel", "Error fetching metadata: ${e.message}")
            } finally {
                _isFetching.value = false
                _fetchingMessage.value = ""
            }
        }
    }

    fun enqueueTrackFromSearch(item: SearchResultItem) {
        val batchId = UUID.randomUUID().toString()
        val currentSettings = _settings.value
        val currentFormat = if (currentSettings.mediaType == MediaType.AUDIO) currentSettings.audioFormat else currentSettings.videoFormat
        val currentQuality = if (currentSettings.mediaType == MediaType.AUDIO) currentSettings.audioQuality else currentSettings.videoQuality

        val trackInfo = TrackInfo(
            id = item.id,
            title = item.title,
            artists = listOf(item.artist),
            durationMs = 0L,
            albumArtUrl = item.thumbnailUrl,
            source = Platform.YOUTUBE,
            sourceUrl = item.url,
            safeFileName = "${item.artist} - ${item.title}".replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        )

        val downloadItem = DownloadItem(
            id = UUID.randomUUID().toString(),
            batchId = batchId,
            track = trackInfo,
            quality = currentQuality,
            format = currentFormat,
            state = DownloadState.QUEUED
        )

        val newBatch = DownloadBatch(
            id = batchId,
            title = item.title,
            platform = Platform.YOUTUBE,
            totalTracks = 1,
            items = listOf(downloadItem)
        )

        _batches.value = listOf(newBatch) + _batches.value
        processQueue()
    }

    fun enqueueAllSearchResults(items: List<SearchResultItem>, artistName: String) {
        if (items.isEmpty()) return
        val batchId = UUID.randomUUID().toString()
        val currentSettings = _settings.value
        val currentFormat = if (currentSettings.mediaType == MediaType.AUDIO) currentSettings.audioFormat else currentSettings.videoFormat
        val currentQuality = if (currentSettings.mediaType == MediaType.AUDIO) currentSettings.audioQuality else currentSettings.videoQuality

        val downloadItems = items.map { item ->
            val trackInfo = TrackInfo(
                id = item.id,
                title = item.title,
                artists = listOf(item.artist),
                durationMs = 0L,
                albumArtUrl = item.thumbnailUrl,
                source = Platform.YOUTUBE,
                sourceUrl = item.url,
                safeFileName = "${item.artist} - ${item.title}".replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            )
            DownloadItem(
                id = UUID.randomUUID().toString(),
                batchId = batchId,
                track = trackInfo,
                quality = currentQuality,
                format = currentFormat,
                state = DownloadState.QUEUED
            )
        }

        val newBatch = DownloadBatch(
            id = batchId,
            title = "$artistName - Full Discography (${items.size} Tracks)",
            platform = Platform.YOUTUBE,
            totalTracks = downloadItems.size,
            items = downloadItems
        )

        _batches.value = listOf(newBatch) + _batches.value
        processQueue()
    }

    private fun processQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val outputDir = StorageManager.getTargetOutputDir(context)

            val currentBatches = _batches.value
            for (batch in currentBatches) {
                for (item in batch.items) {
                    if (item.state == DownloadState.QUEUED) {
                        updateItemState(item.id, DownloadState.DOWNLOADING, 0f, "Starting download...")
                        DownloadForegroundService.start(context, "Downloading ${item.track.title}")

                        try {
                            val downloadedFile = YoutubeDLManager.downloadTrack(
                                context = context,
                                trackInfo = item.track,
                                quality = item.quality,
                                format = item.format,
                                outputDir = outputDir,
                                processId = item.id
                            ) { progress, speed, eta ->
                                val speedText = if (speed.isNotBlank()) " • $speed" else ""
                                val etaText = if (eta.isNotBlank()) " (ETA: $eta)" else ""
                                updateItemState(
                                    item.id,
                                    DownloadState.DOWNLOADING,
                                    progress,
                                    "Downloading: ${progress.toInt()}%$speedText$etaText"
                                )
                                DownloadForegroundService.updateProgress(context, item.track.title, progress.toInt())
                            }

                            updateItemState(item.id, DownloadState.TAGGING, 100f, "Embedding metadata...")
                            val finalFile = MediaTagger.tagAndScan(context, downloadedFile, item.track)

                            updateItemState(item.id, DownloadState.COMPLETED, 100f, "Completed", finalPath = finalFile.absolutePath)
                            // Persist to LibraryStore
                            LibraryStore.saveLibrary(context, _batches.value)
                        } catch (e: Exception) {
                            Log.e("DownloadViewModel", "Failed to download item ${item.track.title}", e)
                            updateItemState(item.id, DownloadState.FAILED, 0f, "Failed", error = e.message)
                        }
                    }
                }
            }

            DownloadForegroundService.stop(context)
        }
    }

    private fun updateItemState(
        itemId: String,
        state: DownloadState,
        progress: Float,
        statusMessage: String,
        finalPath: String? = null,
        error: String? = null
    ) {
        _batches.value = _batches.value.map { batch ->
            batch.copy(
                items = batch.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            state = state,
                            progress = progress,
                            statusMessage = statusMessage,
                            finalFilePath = finalPath ?: item.finalFilePath,
                            errorMessage = error ?: item.errorMessage
                        )
                    } else item
                }
            )
        }
    }

    fun retryItem(itemId: String) {
        updateItemState(itemId, DownloadState.QUEUED, 0f, "Queued for retry", error = null)
        processQueue()
    }

    fun removeBatch(batchId: String) {
        _batches.value = _batches.value.filter { it.id != batchId }
        viewModelScope.launch(Dispatchers.IO) {
            LibraryStore.saveLibrary(getApplication(), _batches.value)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val stored = LibraryStore.loadLibrary(app)
            if (stored.isNotEmpty()) {
                _batches.value = stored
            }
        }
    }
}
