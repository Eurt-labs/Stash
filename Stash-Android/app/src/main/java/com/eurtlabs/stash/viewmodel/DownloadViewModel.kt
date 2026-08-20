package com.eurtlabs.stash.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eurtlabs.stash.data.downloader.YoutubeDLManager
import com.eurtlabs.stash.data.model.ColorTheme
import com.eurtlabs.stash.data.model.DownloadBatch
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadItem
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.DownloadState
import com.eurtlabs.stash.data.model.MediaType
import com.eurtlabs.stash.data.model.StashSettings
import com.eurtlabs.stash.data.model.TrackInfo
import com.eurtlabs.stash.data.parser.LinkParser
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

    private val _settings = MutableStateFlow(
        StashSettings(
            outputDir = YoutubeDLManager.getDefaultOutputDir(application).absolutePath,
            mediaType = MediaType.AUDIO,
            audioFormat = DownloadFormat.MP3,
            audioQuality = DownloadQuality.AUDIO_320K,
            videoFormat = DownloadFormat.MP4,
            videoQuality = DownloadQuality.VIDEO_1080P,
            theme = ColorTheme.OBSIDIAN
        )
    )
    val settings: StateFlow<StashSettings> = _settings.asStateFlow()

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

    fun parseAndEnqueue(input: String) {
        val parsed = LinkParser.parse(input) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isFetching.value = true
            _fetchingMessage.value = "Analyzing media link..."

            try {
                val tracks = YoutubeDLManager.extractMetadata(parsed.originalUrl)
                if (tracks.isNotEmpty()) {
                    enqueueBatch(
                        name = tracks.firstOrNull()?.title ?: "Download Batch",
                        tracks = tracks
                    )
                }
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error scraping metadata", e)
            } finally {
                _isFetching.value = false
                _fetchingMessage.value = ""
            }
        }
    }

    private fun enqueueBatch(name: String, tracks: List<TrackInfo>) {
        val currentSettings = _settings.value
        val currentFormat = currentSettings.format
        val currentQuality = currentSettings.quality
        val batchId = UUID.randomUUID().toString()

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

        val batch = DownloadBatch(
            id = batchId,
            name = name,
            items = items,
            outputDir = currentSettings.outputDir,
            quality = currentQuality,
            format = currentFormat
        )

        _batches.value = listOf(batch) + _batches.value
        processQueue()
    }

    private fun processQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val outputDir = YoutubeDLManager.getDefaultOutputDir(context)

            val currentBatches = _batches.value
            for (batch in currentBatches) {
                for (item in batch.items) {
                    if (item.state == DownloadState.QUEUED) {
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
                                    item.id,
                                    DownloadState.DOWNLOADING,
                                    progress,
                                    "Downloading: ${progress.toInt()}%$speedText$etaText"
                                )
                                DownloadForegroundService.updateProgress(context, item.trackInfo.title, progress.toInt())
                            }

                            updateItemState(item.id, DownloadState.TAGGING, 100f, "Embedding metadata...")
                            val finalFile = MediaTagger.tagAndScan(context, downloadedFile, item.trackInfo)

                            updateItemState(item.id, DownloadState.COMPLETED, 100f, "Completed", finalPath = finalFile.absolutePath)
                        } catch (e: Exception) {
                            Log.e("DownloadViewModel", "Failed to download item ${item.trackInfo.title}", e)
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

    fun removeBatch(batchId: String) {
        _batches.value = _batches.value.filter { it.id != batchId }
    }
}
