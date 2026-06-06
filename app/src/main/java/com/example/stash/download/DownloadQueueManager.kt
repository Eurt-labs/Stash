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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore

/**
 * Manages a batch-based download queue with sequential batch processing
 * and concurrent item processing (up to 7 parallel downloads per active batch).
 */
class DownloadQueueManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "DownloadQueue"
    }

    private val downloadEngine = DownloadEngine(context)
    private val youtubeSearchMatcher = YouTubeSearchMatcher(context)
    private val metadataTagger = MetadataTagger()
    private val fileManager = FileManager(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val downloadSemaphore = Semaphore(1) // 1 concurrent download (prevents heating)
    private val convertSemaphore = Semaphore(1)  // Strict limit of 1 conversion to prevent CPU starvation

    // ── Observable state ──
    private val _batches = MutableStateFlow<Map<String, DownloadBatch>>(emptyMap())
    val batches: StateFlow<Map<String, DownloadBatch>> = _batches.asStateFlow()

    // Track active jobs for cancellation
    private val activeJobs = java.util.Collections.synchronizedMap(mutableMapOf<String, Job>())
    
    // Store request objects by track ID
    private val requestMap = java.util.Collections.synchronizedMap(mutableMapOf<String, DownloadRequest>())

    // Pending queue of batch IDs
    private val pendingBatchIds = java.util.Collections.synchronizedList(mutableListOf<String>())
    
    // Active batch ID
    private var activeBatchId: String? = null

    private fun checkNextBatch() {
        synchronized(pendingBatchIds) {
            if (activeBatchId != null) return // Already running a batch
            if (pendingBatchIds.isEmpty()) return // No batches waiting
            
            val nextBatchId = pendingBatchIds.removeAt(0)
            activeBatchId = nextBatchId
            startBatchExecution(nextBatchId)
        }
    }

    private fun startBatchExecution(batchId: String) {
        val batch = _batches.value[batchId] ?: return
        
        Log.d(TAG, "Starting execution of batch '$batchId' (${batch.name}) with ${batch.totalTracks} tracks")
        
        // Mark all items in the batch as QUEUED (or preserve their current state)
        _batches.update { currentBatches ->
            val currentBatch = currentBatches[batchId] ?: return@update currentBatches
            val updatedItems = currentBatch.items.map { item ->
                if (item.state == DownloadState.QUEUED) item else item.copy(state = DownloadState.QUEUED)
            }
            currentBatches.toMutableMap().apply { put(batchId, currentBatch.copy(items = updatedItems)) }
        }

        // Launch execution for each track in the batch
        batch.items.forEach { item ->
            val request = requestMap[item.id]
            if (request != null) {
                val job = scope.launch {
                    try {
                        processDownload(request, batchId)
                    } finally {
                        activeJobs.remove(request.id)
                        checkBatchCompletion(batchId)
                    }
                }
                activeJobs[item.id] = job
            } else {
                Log.w(TAG, "No request found for track ${item.id}")
            }
        }
    }

    private fun checkBatchCompletion(batchId: String) {
        val batch = _batches.value[batchId] ?: return
        
        // A batch is complete when all its tracks are COMPLETE, FAILED, or CANCELLED
        val isFinished = batch.items.all {
            it.state == DownloadState.COMPLETE ||
            it.state == DownloadState.FAILED ||
            it.state == DownloadState.CANCELLED
        }
        
        if (isFinished) {
            Log.d(TAG, "Batch '$batchId' (${batch.name}) execution completed")
            synchronized(pendingBatchIds) {
                if (activeBatchId == batchId) {
                    activeBatchId = null
                }
            }
            // Trigger check for the next batch
            checkNextBatch()
        }
    }

    /**
     * Enqueues a batch of download requests.
     */
    fun enqueueBatch(batchName: String, requests: List<DownloadRequest>) {
        val batchId = java.util.UUID.randomUUID().toString()
        val items = requests.map { request ->
            DownloadItem(
                id = request.id,
                trackInfo = request.trackInfo,
                state = DownloadState.QUEUED
            )
        }
        val batch = DownloadBatch(
            id = batchId,
            name = batchName,
            items = items
        )

        requests.forEach { requestMap[it.id] = it }
        _batches.update { currentBatches ->
            currentBatches.toMutableMap().apply {
                put(batchId, batch)
            }
        }
        pendingBatchIds.add(batchId)
        
        checkNextBatch()
    }

    /**
     * Cancels a specific download item.
     */
    fun cancel(trackId: String) {
        // Find which batch contains this track
        val batchEntry = _batches.value.entries.find { entry ->
            entry.value.items.any { it.id == trackId }
        }
        if (batchEntry != null) {
            val batchId = batchEntry.key
            activeJobs[trackId]?.cancel()
            updateItemState(batchId, trackId, DownloadState.CANCELLED)
            checkBatchCompletion(batchId)
        }
    }

    /**
     * Cancels all batches and active downloads.
     */
    fun cancelAll() {
        pendingBatchIds.clear()
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        
        _batches.update { currentBatches ->
            currentBatches.mapValues { (_, batch) ->
                val updatedItems = batch.items.map { item ->
                    if (item.state != DownloadState.COMPLETE && item.state != DownloadState.FAILED) {
                        item.copy(state = DownloadState.CANCELLED)
                    } else {
                        item
                    }
                }
                batch.copy(items = updatedItems)
            }
        }
        activeBatchId = null
    }

    /**
     * Clears finished (completed/failed/cancelled) batches.
     */
    fun clearFinished() {
        _batches.update { currentBatches ->
            currentBatches.filter { (_, batch) ->
                batch.state == DownloadState.QUEUED || batch.state == DownloadState.DOWNLOADING
            }
        }
    }

    /**
     * Returns the count of active (non-finished) downloads.
     */
    fun activeCount(): Int {
        return _batches.value.values.sumOf { batch ->
            batch.items.count {
                it.state == DownloadState.DOWNLOADING ||
                it.state == DownloadState.SEARCHING ||
                it.state == DownloadState.CONVERTING ||
                it.state == DownloadState.TAGGING
            }
        }
    }

    /**
     * Returns the count of queued downloads.
     */
    fun queuedCount(): Int {
        return _batches.value.values.sumOf { batch ->
            batch.items.count { it.state == DownloadState.QUEUED }
        }
    }

    /**
     * Shuts down the manager.
     */
    fun shutdown() {
        cancelAll()
        scope.cancel()
    }

    // ── Internal download processing pipeline ──

    private suspend fun processDownload(request: DownloadRequest, batchId: String) {
        try {
            var downloadUrl = request.url

            // ── Step 1: Search ──
            if (request.trackInfo.youtubeUrl == null &&
                request.trackInfo.source == com.example.stash.model.Platform.SPOTIFY
            ) {
                downloadSemaphore.acquire()
                try {
                    updateItemState(batchId, request.id, DownloadState.SEARCHING)
                    val matchedUrl = youtubeSearchMatcher.findBestMatch(request.trackInfo)
                        ?: throw DownloadException("No YouTube match found for: ${request.trackInfo.displayName}")
                    downloadUrl = matchedUrl
                } finally {
                    downloadSemaphore.release()
                }
            } else if (request.trackInfo.youtubeUrl != null) {
                downloadUrl = request.trackInfo.youtubeUrl
            }

            // ── Step 2: Download ──
            downloadSemaphore.acquire()
            val rawFilePath: String
            try {
                updateItemState(batchId, request.id, DownloadState.DOWNLOADING)
                val updatedRequest = request.copy(url = downloadUrl)
                rawFilePath = downloadEngine.download(updatedRequest) { percent, eta, speed ->
                    updateItemProgress(batchId, request.id, percent / 100f, eta, speed.ifBlank { null })
                }
            } finally {
                downloadSemaphore.release()
            }

            // ── Step 3: Convert ──
            updateItemState(batchId, request.id, DownloadState.CONVERTING)
            convertSemaphore.acquire()
            val convertedFilePath: String
            try {
                convertedFilePath = if (!request.format.isVideo) {
                    downloadEngine.convertAudio(rawFilePath, request.format, request.quality)
                } else {
                    rawFilePath
                }
            } finally {
                convertSemaphore.release()
            }

            // ── Step 4: Tag ──
            updateItemState(batchId, request.id, DownloadState.TAGGING)

            try {
                metadataTagger.tagFile(convertedFilePath, request.trackInfo)
            } catch (e: Exception) {
                Log.w(TAG, "Metadata tagging failed (non-fatal): ${e.message}")
            }

            // ── Step 5: Move ──
            val finalPath = withContext(Dispatchers.IO) {
                fileManager.moveToFinalDestination(convertedFilePath, request.trackInfo, request.format.extension)
            }

            // ── Step 6: Complete ──
            completeItem(batchId, request.id, finalPath)

            Log.d(TAG, "Download complete: $finalPath")

        } catch (e: CancellationException) {
            updateItemState(batchId, request.id, DownloadState.CANCELLED)
            throw e

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${request.trackInfo.displayName}", e)
            failItem(batchId, request.id, e.message ?: "Unknown error")
        }
    }

    // ── State management helpers ──

    private fun getItem(batchId: String, trackId: String): DownloadItem? {
        return _batches.value[batchId]?.items?.find { it.id == trackId }
    }

    private fun updateItemProgress(batchId: String, trackId: String, progress: Float, eta: Long, speed: String?) {
        _batches.update { currentBatches ->
            val batch = currentBatches[batchId] ?: return@update currentBatches
            val updatedItems = batch.items.map {
                if (it.id == trackId) {
                    it.copy(progress = progress, eta = eta, speed = speed)
                } else it
            }
            currentBatches.toMutableMap().apply { put(batchId, batch.copy(items = updatedItems)) }
        }
    }

    private fun updateItemState(batchId: String, trackId: String, state: DownloadState) {
        _batches.update { currentBatches ->
            val batch = currentBatches[batchId] ?: return@update currentBatches
            val updatedItems = batch.items.map {
                if (it.id == trackId) it.copy(state = state) else it
            }
            currentBatches.toMutableMap().apply { put(batchId, batch.copy(items = updatedItems)) }
        }
    }

    private fun completeItem(batchId: String, trackId: String, filePath: String) {
        _batches.update { currentBatches ->
            val batch = currentBatches[batchId] ?: return@update currentBatches
            val updatedItems = batch.items.map {
                if (it.id == trackId) {
                    it.copy(state = DownloadState.COMPLETE, progress = 1f, filePath = filePath)
                } else it
            }
            currentBatches.toMutableMap().apply { put(batchId, batch.copy(items = updatedItems)) }
        }
    }

    private fun failItem(batchId: String, trackId: String, errorMsg: String) {
        _batches.update { currentBatches ->
            val batch = currentBatches[batchId] ?: return@update currentBatches
            val updatedItems = batch.items.map {
                if (it.id == trackId) {
                    it.copy(state = DownloadState.FAILED, error = errorMsg)
                } else it
            }
            currentBatches.toMutableMap().apply { put(batchId, batch.copy(items = updatedItems)) }
        }
    }
}
