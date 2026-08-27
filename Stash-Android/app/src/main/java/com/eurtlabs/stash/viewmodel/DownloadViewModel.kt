package com.eurtlabs.stash.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eurtlabs.stash.data.downloader.LogManager
import com.eurtlabs.stash.data.downloader.YoutubeDLManager
import com.eurtlabs.stash.data.downloader.YoutubeLibraryFetcher
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
import com.eurtlabs.stash.data.storage.SettingsStore
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

    // Distinct Queue batches (active downloads) vs Persistent Library batches
    private val _queueBatches = MutableStateFlow<List<DownloadBatch>>(emptyList())
    val queueBatches: StateFlow<List<DownloadBatch>> = _queueBatches.asStateFlow()
    val batches: StateFlow<List<DownloadBatch>> = _queueBatches.asStateFlow() // Alias for Queue

    private val _libraryBatches = MutableStateFlow<List<DownloadBatch>>(emptyList())
    val libraryBatches: StateFlow<List<DownloadBatch>> = _libraryBatches.asStateFlow()

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val _fetchingMessage = MutableStateFlow("")
    val fetchingMessage: StateFlow<String> = _fetchingMessage.asStateFlow()

    private var fetchProcessId: String? = null

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
            mediaType = SettingsStore.loadMediaType(application),
            audioFormat = SettingsStore.loadAudioFormat(application),
            audioQuality = SettingsStore.loadAudioQuality(application),
            videoFormat = SettingsStore.loadVideoFormat(application),
            videoQuality = SettingsStore.loadVideoQuality(application),
            theme = SettingsStore.loadTheme(application)
        )
    )
    val settings: StateFlow<StashSettings> = _settings.asStateFlow()

    init {
        // Load persisted library records and scan disk on startup
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val stored = LibraryStore.loadLibrary(app)
            if (stored.isNotEmpty()) {
                _libraryBatches.value = stored
            }
        }
    }

    fun updateTheme(theme: ColorTheme) {
        SettingsStore.saveTheme(getApplication(), theme)
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateMediaType(mediaType: MediaType) {
        SettingsStore.saveMediaType(getApplication(), mediaType)
        _settings.value = _settings.value.copy(mediaType = mediaType)
    }

    fun updateFormat(format: DownloadFormat) {
        if (format.isAudioOnly) {
            SettingsStore.saveAudioFormat(getApplication(), format)
            _settings.value = _settings.value.copy(
                mediaType = MediaType.AUDIO,
                audioFormat = format
            )
        } else {
            SettingsStore.saveVideoFormat(getApplication(), format)
            _settings.value = _settings.value.copy(
                mediaType = MediaType.VIDEO,
                videoFormat = format
            )
        }
    }

    fun updateQuality(quality: DownloadQuality) {
        if (quality.isAudioOnly) {
            SettingsStore.saveAudioQuality(getApplication(), quality)
            _settings.value = _settings.value.copy(audioQuality = quality)
        } else {
            SettingsStore.saveVideoQuality(getApplication(), quality)
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
                if (query.trim().startsWith(":yt")) {
                    val results = YoutubeLibraryFetcher.fetchLibrary(query.trim())
                    _searchResults.value = results
                } else {
                    val results = YoutubeDLManager.searchMedia(query.trim(), _searchFilter.value)
                    _searchResults.value = results
                }
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Search error: ${e.message}", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun cancelFetch() {
        fetchProcessId?.let { id ->
            YoutubeDLManager.cancelFetch(id)
            fetchProcessId = null
        }
        _isFetching.value = false
        _fetchingMessage.value = "Cancelled"
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
            val processId = UUID.randomUUID().toString()
            fetchProcessId = processId

            try {
                val tracks = YoutubeDLManager.extractMetadata(url, processId) { progressLine ->
                    _fetchingMessage.value = progressLine
                }
                if (tracks.isNotEmpty()) {
                    val batchId = UUID.randomUUID().toString()
                    
                    val batchTitle = if (tracks.size > 1) {
                        tracks.firstOrNull()?.playlistName ?: tracks.firstOrNull()?.artists?.firstOrNull() ?: "Download Batch"
                    } else {
                        tracks.firstOrNull()?.title ?: "Download Batch"
                    }
                    
                    val currentSettings = _settings.value
                    val currentFormat = if (currentSettings.mediaType == MediaType.AUDIO) currentSettings.audioFormat else currentSettings.videoFormat
                    val currentQuality = if (currentSettings.mediaType == MediaType.AUDIO) currentSettings.audioQuality else currentSettings.videoQuality

                    val items = tracks.map { track ->
                        DownloadItem(
                            id = UUID.randomUUID().toString(),
                            batchId = batchId,
                            trackInfo = track,
                            quality = currentQuality,
                            format = currentFormat,
                            state = DownloadState.QUEUED
                        )
                    }

                    val newBatch = DownloadBatch(
                        id = batchId,
                        name = batchTitle,
                        items = items,
                        outputDir = currentSettings.outputDir,
                        quality = currentQuality,
                        format = currentFormat
                    )

                    _queueBatches.value = listOf(newBatch) + _queueBatches.value
                    processQueue()
                }
            } catch (e: Exception) {
                if (e.message?.contains("destroy") == true || e.message?.contains("cancel") == true) {
                    LogManager.append("DownloadViewModel", "Fetch was cancelled")
                } else {
                    LogManager.append("DownloadViewModel", "Error fetching metadata: ${e.message}")
                }
            } finally {
                if (fetchProcessId == processId) {
                    fetchProcessId = null
                    _isFetching.value = false
                    _fetchingMessage.value = ""
                }
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
            trackInfo = trackInfo,
            quality = currentQuality,
            format = currentFormat,
            state = DownloadState.QUEUED
        )

        val newBatch = DownloadBatch(
            id = batchId,
            name = item.title,
            items = listOf(downloadItem),
            outputDir = currentSettings.outputDir,
            quality = currentQuality,
            format = currentFormat
        )

        _queueBatches.value = listOf(newBatch) + _queueBatches.value
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
                trackInfo = trackInfo,
                quality = currentQuality,
                format = currentFormat,
                state = DownloadState.QUEUED
            )
        }

        val newBatch = DownloadBatch(
            id = batchId,
            name = "$artistName - Full Discography (${items.size} Tracks)",
            items = downloadItems,
            outputDir = currentSettings.outputDir,
            quality = currentQuality,
            format = currentFormat
        )

        _queueBatches.value = listOf(newBatch) + _queueBatches.value
        processQueue()
    }

    private var processingJob: kotlinx.coroutines.Job? = null

    private fun processQueue() {
        if (processingJob?.isActive == true) return

        processingJob = viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val outputDir = StorageManager.getTargetOutputDir(context)

            var hasMoreItems = true
            while (hasMoreItems) {
                hasMoreItems = false
                val currentBatches = _queueBatches.value

                for (batch in currentBatches) {
                    for (item in batch.items) {
                        // Dynamically resolve state in case it was cancelled/paused
                        val currentItemState = _queueBatches.value.flatMap { it.items }.find { it.id == item.id }?.state
                        
                        if (currentItemState == DownloadState.QUEUED) {
                            hasMoreItems = true
                            updateItemState(item.id, DownloadState.DOWNLOADING, 0f, "Starting download...")
                            DownloadForegroundService.start(context, "Downloading ${item.trackInfo.title}")

                            try {
                                val downloadedFile = YoutubeDLManager.downloadTrack(
                                    context = context,
                                    trackInfo = item.trackInfo,
                                    quality = item.quality,
                                    format = item.format,
                                    outputDir = outputDir,
                                    processId = item.id
                                ) { progress, speed, eta ->
                                    val speedText = if (speed.isNotBlank()) " • $speed" else ""
                                    val etaText = if (eta.isNotBlank()) " (ETA: $eta)" else ""
                                    updateItemState(
                                        itemId = item.id,
                                        state = DownloadState.DOWNLOADING,
                                        progress = progress,
                                        speed = speedText,
                                        eta = etaText,
                                        statusMessage = "Downloading: ${progress.toInt()}%$speedText$etaText"
                                    )
                                    DownloadForegroundService.updateProgress(context, item.trackInfo.title, progress.toInt())
                                }

                                updateItemState(item.id, DownloadState.TAGGING, 100f, "Embedding metadata...")
                                val finalFile = MediaTagger.tagAndScan(context, downloadedFile, item.trackInfo)

                                val completedItem = item.copy(
                                    state = DownloadState.COMPLETED,
                                    progress = 100f,
                                    statusMessage = "Completed",
                                    finalFilePath = finalFile.absolutePath
                                )

                                // Copy the completed file to the user's custom storage folder if they selected one
                                val subfolderName = if (batch.items.size > 1) {
                                    batch.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                                } else null
                                
                                StorageManager.copyToCustomStorage(context, finalFile, subfolderName)

                                updateItemState(item.id, DownloadState.COMPLETED, 100f, "Completed", finalPath = finalFile.absolutePath)

                                // Add to Persistent Library Batches (Independent from Queue)
                                addToLibrary(completedItem, batch)
                            } catch (e: Exception) {
                                if (e.message?.contains("destroy") == true || e.message?.contains("cancel") == true) {
                                    updateItemState(item.id, DownloadState.CANCELLED, 0f, "Cancelled")
                                } else {
                                    Log.e("DownloadViewModel", "Failed to download item ${item.trackInfo.title}", e)
                                    updateItemState(item.id, DownloadState.FAILED, 0f, "Failed", error = e.message)
                                }
                            }
                        }
                    }
                }
            }

            DownloadForegroundService.stop(context)
        }
    }

    private fun addToLibrary(completedItem: DownloadItem, sourceBatch: DownloadBatch) {
        val currentLib = _libraryBatches.value.toMutableList()
        val existingBatchIndex = currentLib.indexOfFirst { it.id == sourceBatch.id }

        if (existingBatchIndex >= 0) {
            val existing = currentLib[existingBatchIndex]
            val updatedItems = existing.items.filter { it.id != completedItem.id } + listOf(completedItem)
            currentLib[existingBatchIndex] = existing.copy(items = updatedItems)
        } else {
            val singleBatch = sourceBatch.copy(items = listOf(completedItem))
            currentLib.add(0, singleBatch)
        }

        _libraryBatches.value = currentLib
        viewModelScope.launch(Dispatchers.IO) {
            LibraryStore.saveLibrary(getApplication(), currentLib)
        }
    }

    private fun updateItemState(
        itemId: String,
        state: DownloadState,
        progress: Float,
        statusMessage: String,
        speed: String = "",
        eta: String = "",
        finalPath: String? = null,
        error: String? = null
    ) {
        _queueBatches.value = _queueBatches.value.map { batch ->
            batch.copy(
                items = batch.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            state = state,
                            progress = progress,
                            speed = speed.ifBlank { item.speed },
                            eta = eta.ifBlank { item.eta },
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

    fun cancelItem(itemId: String) {
        try {
            com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(itemId)
        } catch (e: Exception) {
            // Ignore if process already completed/not found
        }
        updateItemState(itemId, DownloadState.CANCELLED, 0f, "Download cancelled")
    }

    fun pauseItem(itemId: String) {
        try {
            com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(itemId)
        } catch (e: Exception) {
            // Ignore
        }
        updateItemState(itemId, DownloadState.IDLE, 0f, "Paused")
    }

    fun cancelBatch(batchId: String) {
        val batch = _queueBatches.value.find { it.id == batchId }
        batch?.items?.forEach { item ->
            if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.QUEUED) {
                try {
                    com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(item.id)
                } catch (e: Exception) {}
                updateItemState(item.id, DownloadState.CANCELLED, 0f, "Batch cancelled")
            }
        }
    }

    // Clears only from Queue (does NOT remove from Library)
    fun removeBatch(batchId: String) {
        cancelBatch(batchId)
        _queueBatches.value = _queueBatches.value.filter { it.id != batchId }
    }

    // Removes an individual item from Queue (does NOT remove from Library)
    fun removeItem(itemId: String) {
        cancelItem(itemId)
        _queueBatches.value = _queueBatches.value.mapNotNull { batch ->
            val remaining = batch.items.filter { it.id != itemId }
            if (remaining.isEmpty()) null else batch.copy(items = remaining)
        }
    }

    // Removes an item from Library, optionally deleting the file from device
    fun deleteLibraryItem(itemId: String, deleteFile: Boolean = false) {
        if (deleteFile) {
            val batch = _libraryBatches.value.find { it.items.any { item -> item.id == itemId } }
            val itemToDelete = batch?.items?.find { it.id == itemId }
            
            val subfolderName = if (batch != null && batch.items.size > 1) {
                batch.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            } else null
            
            itemToDelete?.finalFilePath?.let { path ->
                try {
                    val file = java.io.File(path)
                    val fileName = file.name
                    if (file.exists()) {
                        file.delete()
                    }
                    
                    // The file may also have been copied to a custom SAF storage folder, delete it from there too
                    StorageManager.deleteFromCustomStorage(getApplication(), fileName, subfolderName)
                    
                } catch (e: Exception) {
                    com.eurtlabs.stash.data.downloader.LogManager.append("Library", "Failed to delete file: ${e.message}")
                }
            }
        }
        _libraryBatches.value = _libraryBatches.value.mapNotNull { batch ->
            val remaining = batch.items.filter { it.id != itemId }
            if (remaining.isEmpty()) null else batch.copy(items = remaining)
        }
        viewModelScope.launch(Dispatchers.IO) {
            LibraryStore.saveLibrary(getApplication(), _libraryBatches.value)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val stored = LibraryStore.loadLibrary(app)
            if (stored.isNotEmpty()) {
                _libraryBatches.value = stored
            }
        }
    }
}
