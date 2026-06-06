package com.example.stash.download

import android.content.Context
import android.util.Log
import com.example.stash.storage.FileManager
import com.example.stash.tagger.MetadataTagger
import com.example.stash.youtube.YouTubeSearchMatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore

/**
 * Manages a concurrent download queue with progress tracking.
 *
 * Features:
 * - Parallel downloads with configurable concurrency limit (default: 3)
 * - Per-item state tracking: QUEUED → SEARCHING → DOWNLOADING → CONVERTING → TAGGING → COMPLETE
 * - Observable state via [StateFlow] for UI binding
 * - Pause, resume, cancel, and retry per item or all items
 *
 * Usage:
 * ```
 * val queueManager = DownloadQueueManager(context)
 * queueManager.enqueue(request)
 * queueManager.downloadItems.collect { items -> updateUI(items) }
 * ```
 */
class DownloadQueueManager(
    private val context: Context,
    private val maxConcurrentDownloads: Int = 3
) {
    companion object {
        private const val TAG = "DownloadQueue"
    }

    private val downloadEngine = DownloadEngine(context)
    private val youtubeSearchMatcher = YouTubeSearchMatcher(context)
    private val metadataTagger = MetadataTagger()
    private val fileManager = FileManager(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val semaphore = Semaphore(maxConcurrentDownloads)

    // ── Observable state ──
    private val _downloadItems = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    val downloadItems: StateFlow<Map<String, DownloadItem>> = _downloadItems.asStateFlow()

    // Track active download jobs for cancellation
    private val activeJobs = mutableMapOf<String, Job>()

    /**
     * Enqueues a single download request.
     * The download will start automatically when a slot is available.
     */
    fun enqueue(request: DownloadRequest) {
        val item = DownloadItem(
            id = request.id,
            trackInfo = request.trackInfo,
            state = DownloadState.QUEUED
        )
        updateItem(item)

        val job = scope.launch {
            semaphore.acquire()
            try {
                processDownload(request)
            } finally {
                semaphore.release()
                activeJobs.remove(request.id)
            }
        }
        activeJobs[request.id] = job
    }

    /**
     * Enqueues multiple download requests at once.
     */
    fun enqueueAll(requests: List<DownloadRequest>) {
        requests.forEach { enqueue(it) }
    }

    /**
     * Cancels a specific download.
     */
    fun cancel(id: String) {
        activeJobs[id]?.cancel()
        updateItemState(id, DownloadState.CANCELLED)
    }

    /**
     * Cancels all active and queued downloads.
     */
    fun cancelAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        _downloadItems.value = _downloadItems.value.mapValues { (_, item) ->
            if (item.state != DownloadState.COMPLETE && item.state != DownloadState.FAILED) {
                item.copy(state = DownloadState.CANCELLED)
            } else {
                item
            }
        }
    }

    /**
     * Retries a failed or cancelled download.
     */
    fun retry(request: DownloadRequest) {
        cancel(request.id) // Cancel any existing attempt
        enqueue(request)
    }

    /**
     * Clears completed, failed, and cancelled items from the queue.
     */
    fun clearFinished() {
        _downloadItems.value = _downloadItems.value.filter { (_, item) ->
            item.state == DownloadState.QUEUED ||
            item.state == DownloadState.SEARCHING ||
            item.state == DownloadState.DOWNLOADING ||
            item.state == DownloadState.CONVERTING ||
            item.state == DownloadState.TAGGING
        }
    }

    /**
     * Returns the count of active (non-finished) downloads.
     */
    fun activeCount(): Int {
        return _downloadItems.value.count { (_, item) ->
            item.state == DownloadState.DOWNLOADING ||
            item.state == DownloadState.SEARCHING ||
            item.state == DownloadState.CONVERTING ||
            item.state == DownloadState.TAGGING
        }
    }

    /**
     * Returns the count of queued downloads waiting for a slot.
     */
    fun queuedCount(): Int {
        return _downloadItems.value.count { it.value.state == DownloadState.QUEUED }
    }

    /**
     * Cancels all downloads and cleans up resources.
     */
    fun shutdown() {
        cancelAll()
        scope.cancel()
    }

    // ──────────────────────────────────────────────────────────────
    // Internal download processing pipeline
    // ──────────────────────────────────────────────────────────────

    private suspend fun processDownload(request: DownloadRequest) {
        try {
            var downloadUrl = request.url

            // ── Step 1: If this is a Spotify track, search YouTube for a match ──
            if (request.trackInfo.youtubeUrl == null &&
                request.trackInfo.source == com.example.stash.model.Platform.SPOTIFY
            ) {
                updateItemState(request.id, DownloadState.SEARCHING)
                val matchedUrl = youtubeSearchMatcher.findBestMatch(request.trackInfo)
                    ?: throw DownloadException("No YouTube match found for: ${request.trackInfo.displayName}")
                downloadUrl = matchedUrl
            } else if (request.trackInfo.youtubeUrl != null) {
                downloadUrl = request.trackInfo.youtubeUrl
            }

            // ── Step 2: Download ──
            updateItemState(request.id, DownloadState.DOWNLOADING)

            val updatedRequest = request.copy(url = downloadUrl)
            val filePath = downloadEngine.download(updatedRequest) { percent, eta, speed ->
                updateItem(
                    getItem(request.id)?.copy(
                        progress = percent / 100f,
                        eta = eta,
                        speed = speed.ifBlank { null }
                    ) ?: return@download
                )
            }

            // ── Step 3: Tag metadata ──
            updateItemState(request.id, DownloadState.TAGGING)

            try {
                metadataTagger.tagFile(filePath, request.trackInfo)
            } catch (e: Exception) {
                Log.w(TAG, "Metadata tagging failed (non-fatal): ${e.message}")
            }

            // ── Step 4: Move to final destination (SAF or default folder) ──
            val finalPath = withContext(Dispatchers.IO) {
                fileManager.moveToFinalDestination(filePath, request.trackInfo, request.format.extension)
            }

            // ── Step 5: Complete ──
            updateItem(
                getItem(request.id)?.copy(
                    state = DownloadState.COMPLETE,
                    progress = 1f,
                    filePath = finalPath
                ) ?: return
            )

            Log.d(TAG, "Download complete: $filePath")

        } catch (e: CancellationException) {
            updateItemState(request.id, DownloadState.CANCELLED)
            throw e // Re-throw to let coroutine cleanup happen

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${request.trackInfo.displayName}", e)
            updateItem(
                getItem(request.id)?.copy(
                    state = DownloadState.FAILED,
                    error = e.message ?: "Unknown error"
                ) ?: return
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // State management helpers
    // ──────────────────────────────────────────────────────────────

    private fun updateItem(item: DownloadItem) {
        _downloadItems.value = _downloadItems.value.toMutableMap().apply {
            put(item.id, item)
        }
    }

    private fun updateItemState(id: String, state: DownloadState) {
        getItem(id)?.let { item ->
            updateItem(item.copy(state = state))
        }
    }

    private fun getItem(id: String): DownloadItem? = _downloadItems.value[id]
}
