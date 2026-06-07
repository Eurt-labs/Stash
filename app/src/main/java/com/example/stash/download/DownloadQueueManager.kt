package com.example.stash.download

import com.example.stash.convert.ConversionEngine
import com.example.stash.storage.FileManager
import com.example.stash.tagger.MetadataTagger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages a strict 5-phase parallel download pipeline.
 *
 * Phase 1: FETCH    — Metadata already fetched by StashOrchestrator, saved to JSON manifest
 * Phase 2: DOWNLOAD — Download tracks concurrently from the manifest using yt-dlp (max 5 parallel)
 * Phase 3: CONVERT  — Convert downloaded files concurrently using FFmpeg (max 3 parallel)
 * Phase 4: MOVE     — Tag and move each converted file to the user's selected folder
 * Phase 5: CLEANUP  — Delete the temp JSON manifest and cache files
 *
 * Processing within a batch is parallel (multiple tracks download/convert at once).
 * Batches themselves are processed sequentially (one batch at a time).
 */
class DownloadQueueManager {
    companion object {
        private const val TAG = "DownloadQueue"
    }

    private val downloadEngine = DownloadEngine()
    private val conversionEngine = ConversionEngine()

    private val metadataTagger = MetadataTagger()
    private val fileManager = FileManager()
    private val manifestManager = ManifestManager()

    // Semaphores to limit active download and convert tasks to allow high-speed parallel execution
    private val downloadSemaphore = Semaphore(5)
    private val convertSemaphore = Semaphore(3)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── Observable state ──
    private val _batches = MutableStateFlow<Map<String, DownloadBatch>>(emptyMap())
    val batches: StateFlow<Map<String, DownloadBatch>> = _batches.asStateFlow()

    // Track active batch job for cancellation
    private var activeBatchJob: Job? = null
    private var activeBatchId: String? = null

    // Store request objects by track ID
    private val requestMap = java.util.Collections.synchronizedMap(mutableMapOf<String, DownloadRequest>())

    // Track raw file paths for each download item (populated during download phase)
    private val rawFilePaths = java.util.Collections.synchronizedMap(mutableMapOf<String, String>())

    // Track converted file paths (populated during convert phase)
    private val convertedFilePaths = java.util.Collections.synchronizedMap(mutableMapOf<String, String>())

    // Pending queue of batch IDs
    private val pendingBatchIds = java.util.Collections.synchronizedList(mutableListOf<String>())

    /**
     * Enqueues a batch of download requests.
     * Saves the track list to a temp JSON manifest, then starts processing.
     */
    fun enqueueBatch(
        batchName: String,
        requests: List<DownloadRequest>,
        outputDir: String
    ) {
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
            items = items,
            outputDir = outputDir
        )

        // Store requests for later lookup
        requests.forEach { requestMap[it.id] = it }

        // Phase 1: Save manifest to temp JSON
        val tracks = requests.map { it.trackInfo }
        manifestManager.saveManifest(batchId, tracks)

        _batches.update { currentBatches ->
            currentBatches.toMutableMap().apply {
                put(batchId, batch)
            }
        }
        pendingBatchIds.add(batchId)

        checkNextBatch()
    }

    /**
     * Checks if a new batch can start processing.
     */
    private fun checkNextBatch() {
        synchronized(pendingBatchIds) {
            if (activeBatchId != null) return // Already running a batch
            if (pendingBatchIds.isEmpty()) return // No batches waiting

            val nextBatchId = pendingBatchIds.removeAt(0)
            activeBatchId = nextBatchId
            startBatchExecution(nextBatchId)
        }
    }

    private val pausedTrackIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val activeJobs = java.util.Collections.synchronizedMap(mutableMapOf<String, Job>())

    /**
     * Starts the strict 5-phase sequential pipeline for a batch.
     */
    private fun startBatchExecution(batchId: String) {
        val batch = _batches.value[batchId] ?: return

        println("═══════════════════════════════════════════════════")
        println("Starting batch '$batchId' (${batch.name}) — ${batch.totalTracks} tracks")
        println("Output directory: ${batch.outputDir}")
        println("═══════════════════════════════════════════════════")

        activeBatchJob = scope.launch {
            try {
                val jobs = batch.items.map { item ->
                    launch {
                        executeTrack(batchId, item.id)
                    }
                }

                jobs.joinAll()

                val remainingItems = _batches.value[batchId]?.items ?: emptyList()
                val isFullyDone = remainingItems.all {
                    it.state == DownloadState.COMPLETE ||
                    it.state == DownloadState.FAILED ||
                    it.state == DownloadState.CANCELLED
                }

                if (isFullyDone) {
                    println("\n── Phase 5: CLEANUP ──")
                    manifestManager.cleanupBatch(batchId)
                    remainingItems.forEach { item ->
                        rawFilePaths.remove(item.id)
                        convertedFilePaths.remove(item.id)
                        requestMap.remove(item.id)
                    }
                }

                println("\n═══════════════════════════════════════════════════")
                println("Batch '${batch.name}' completed!")
                println("═══════════════════════════════════════════════════\n")

            } catch (e: CancellationException) {
                println("Batch '$batchId' was cancelled")
                // Mark remaining queued items as cancelled
                val cancelledBatch = _batches.value[batchId]
                cancelledBatch?.items?.forEach { item ->
                    if (item.state != DownloadState.COMPLETE && item.state != DownloadState.FAILED && item.state != DownloadState.PAUSED) {
                        updateItemState(batchId, item.id, DownloadState.CANCELLED)
                    }
                }
                manifestManager.cleanupBatch(batchId)
            } finally {
                synchronized(pendingBatchIds) {
                    if (activeBatchId == batchId) {
                        activeBatchId = null
                    }
                }
                activeBatchJob = null
                checkNextBatch()
            }
        }
    }

    private suspend fun executeTrack(batchId: String, trackId: String) {
        val request = requestMap[trackId] ?: return
        val downloadUrl = request.url

        val currentJob = coroutineContext[Job]
        if (currentJob != null) {
            activeJobs[trackId] = currentJob
        }

        try {
            // Step 2: Download (constrained by downloadSemaphore)
            val rawFilePath = try {
                downloadSemaphore.withPermit {
                    if (pausedTrackIds.contains(trackId)) {
                        updateItemState(batchId, trackId, DownloadState.PAUSED)
                        return
                    }
                    updateItemState(batchId, trackId, DownloadState.DOWNLOADING)
                    val updatedRequest = request.copy(url = downloadUrl)
                    downloadEngine.download(updatedRequest) { percent, eta, speed ->
                        updateItemProgress(batchId, trackId, percent / 100f, eta, speed.ifBlank { null })
                    }
                }
            } catch (e: CancellationException) {
                if (pausedTrackIds.contains(trackId)) {
                    updateItemState(batchId, trackId, DownloadState.PAUSED)
                } else {
                    updateItemState(batchId, trackId, DownloadState.CANCELLED)
                }
                throw e
            } catch (e: Exception) {
                System.err.println("Download failed for: ${request.trackInfo.displayName}")
                e.printStackTrace()
                failItem(batchId, trackId, e.message ?: "Download failed")
                return
            }

            rawFilePaths[trackId] = rawFilePath
            println("Downloaded: ${request.trackInfo.displayName} -> $rawFilePath")

            // Step 3: Convert (constrained by convertSemaphore)
            val convertedPath = try {
                convertSemaphore.withPermit {
                    if (pausedTrackIds.contains(trackId)) {
                        updateItemState(batchId, trackId, DownloadState.PAUSED)
                        return
                    }
                    updateItemState(batchId, trackId, DownloadState.CONVERTING)
                    updateItemProgress(batchId, trackId, 0f, null, "Converting...")
                    conversionEngine.convert(
                        inputPath = rawFilePath,
                        format = request.format,
                        quality = request.quality
                    ) { progress ->
                        updateItemProgress(batchId, trackId, progress, null, "Converting...")
                    }
                }
            } catch (e: CancellationException) {
                if (pausedTrackIds.contains(trackId)) {
                    updateItemState(batchId, trackId, DownloadState.PAUSED)
                } else {
                    updateItemState(batchId, trackId, DownloadState.CANCELLED)
                }
                throw e
            } catch (e: Exception) {
                System.err.println("Conversion failed for: ${request.trackInfo.displayName}")
                e.printStackTrace()
                failItem(batchId, trackId, e.message ?: "Conversion failed")
                return
            }

            convertedFilePaths[trackId] = convertedPath
            println("Converted: ${request.trackInfo.displayName} -> $convertedPath")

            // Step 4: Tag & Move
            try {
                if (pausedTrackIds.contains(trackId)) {
                    updateItemState(batchId, trackId, DownloadState.PAUSED)
                    return
                }
                updateItemState(batchId, trackId, DownloadState.TAGGING)
                try {
                    metadataTagger.tagFile(convertedPath, request.trackInfo)
                } catch (e: Exception) {
                    System.err.println("Metadata tagging failed (non-fatal): ${e.message}")
                }

                updateItemState(batchId, trackId, DownloadState.MOVING)
                val batch = _batches.value[batchId] ?: return
                val outputDir = batch.outputDir.ifBlank { fileManager.getDefaultDownloadDir() }
                val isSearchOrChannel = request.trackInfo.sourceUrl.startsWith("ytsearch") ||
                        request.trackInfo.sourceUrl.contains("/@") ||
                        request.trackInfo.sourceUrl.contains("/channel/") ||
                        request.trackInfo.sourceUrl.contains("/c/")
                val subfolderName = if (request.isIndividualTrack) {
                    null
                } else if (isSearchOrChannel) {
                    batch.name
                } else {
                    request.trackInfo.album?.trim()?.takeIf { it.isNotBlank() } ?: batch.name
                }
                val finalPath = withContext(Dispatchers.IO) {
                    fileManager.moveToFinalDestination(convertedPath, request.trackInfo, request.format.extension, outputDir, subfolderName)
                }

                completeItem(batchId, trackId, finalPath)
                println("Moved: ${request.trackInfo.displayName} -> $finalPath")
            } catch (e: CancellationException) {
                if (pausedTrackIds.contains(trackId)) {
                    updateItemState(batchId, trackId, DownloadState.PAUSED)
                } else {
                    updateItemState(batchId, trackId, DownloadState.CANCELLED)
                }
                throw e
            } catch (e: Exception) {
                System.err.println("Move failed for: ${request.trackInfo.displayName}")
                e.printStackTrace()
                failItem(batchId, trackId, e.message ?: "Move failed")
            }
        } finally {
            activeJobs.remove(trackId)
        }
    }

    /**
     * Pauses a specific download item.
     */
    fun pauseTrack(trackId: String) {
        pausedTrackIds.add(trackId)
        val batchEntry = _batches.value.entries.find { entry ->
            entry.value.items.any { it.id == trackId }
        } ?: return
        val batchId = batchEntry.key

        // Cancel the active job
        val activeJob = activeJobs[trackId]
        if (activeJob != null) {
            activeJob.cancel()
        } else {
            // If it's queued but not active yet, set state to PAUSED directly
            updateItemState(batchId, trackId, DownloadState.PAUSED)
        }
    }

    /**
     * Resumes a specific download item.
     */
    fun resumeTrack(trackId: String) {
        pausedTrackIds.remove(trackId)
        val batchEntry = _batches.value.entries.find { entry ->
            entry.value.items.any { it.id == trackId }
        } ?: return
        val batchId = batchEntry.key

        updateItemState(batchId, trackId, DownloadState.QUEUED)

        // Launch executeTrack in the background scope
        scope.launch {
            executeTrack(batchId, trackId)
        }
    }

    private fun checkAndCleanupBatch(batchId: String) {
        val batch = _batches.value[batchId] ?: return
        val isFullyDone = batch.items.all {
            it.state == DownloadState.COMPLETE ||
            it.state == DownloadState.FAILED ||
            it.state == DownloadState.CANCELLED
        }
        if (isFullyDone) {
            println("\n── Phase 5: CLEANUP (Asynchronous) for batch '$batchId' ──")
            manifestManager.cleanupBatch(batchId)
            batch.items.forEach { item ->
                rawFilePaths.remove(item.id)
                convertedFilePaths.remove(item.id)
                requestMap.remove(item.id)
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // Cancellation
    // ══════════════════════════════════════════════════════

    /**
     * Cancels a specific download item.
     */
    fun cancel(trackId: String) {
        val batchEntry = _batches.value.entries.find { entry ->
            entry.value.items.any { it.id == trackId }
        }
        if (batchEntry != null) {
            updateItemState(batchEntry.key, trackId, DownloadState.CANCELLED)
        }
    }

    /**
     * Cancels all tracks within a specific batch only.
     */
    fun cancelBatch(batchId: String) {
        val batch = _batches.value[batchId] ?: return

        val jobToCancel = if (activeBatchId == batchId) activeBatchJob else null
        if (activeBatchId == batchId) {
            activeBatchJob = null
        }

        // Mark non-finished items as cancelled
        _batches.update { currentBatches ->
            val currentBatch = currentBatches[batchId] ?: return@update currentBatches
            val updatedItems = currentBatch.items.map { item ->
                if (item.state != DownloadState.COMPLETE && item.state != DownloadState.FAILED) {
                    item.copy(state = DownloadState.CANCELLED)
                } else {
                    item
                }
            }
            currentBatches.toMutableMap().apply { put(batchId, currentBatch.copy(items = updatedItems)) }
        }

        synchronized(pendingBatchIds) {
            pendingBatchIds.remove(batchId)
            if (activeBatchId == batchId) {
                activeBatchId = null
            }
        }

        scope.launch {
            if (jobToCancel != null) {
                try {
                    jobToCancel.cancelAndJoin()
                } catch (e: Exception) {
                    System.err.println("Error joining cancelled batch job: ${e.message}")
                }
            }
            // Clean up files after processes have terminated and released handles
            manifestManager.cleanupBatch(batchId)
        }

        checkNextBatch()

        println("Batch '$batchId' cancelled")
    }

    /**
     * Cancels all batches and active downloads, then cleans up cache.
     */
    fun cancelAll() {
        val jobToCancel = activeBatchJob
        activeBatchJob = null
        pendingBatchIds.clear()

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

        scope.launch {
            if (jobToCancel != null) {
                try {
                    jobToCancel.cancelAndJoin()
                } catch (e: Exception) {
                    System.err.println("Error joining cancelled batch job: ${e.message}")
                }
            }
            // Clear the cache directory safely after processes have terminated and released handles
            fileManager.cleanupCacheDir()
        }
    }

    /**
     * Clears finished (completed/failed/cancelled) batches from the UI.
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
                it.state == DownloadState.CONVERTING ||
                it.state == DownloadState.MOVING ||
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

    // ── State management helpers ──

    private fun updateItemProgress(batchId: String, trackId: String, progress: Float, eta: Long?, speed: String?) {
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
                    it.copy(state = DownloadState.COMPLETE, progress = 1f, speed = null, eta = null, filePath = filePath)
                } else it
            }
            currentBatches.toMutableMap().apply { put(batchId, batch.copy(items = updatedItems)) }
        }
        checkAndCleanupBatch(batchId)
    }

    private fun failItem(batchId: String, trackId: String, errorMsg: String) {
        _batches.update { currentBatches ->
            val batch = currentBatches[batchId] ?: return@update currentBatches
            val updatedItems = batch.items.map {
                if (it.id == trackId) {
                    it.copy(state = DownloadState.FAILED, error = errorMsg, speed = null, eta = null)
                } else it
            }
            currentBatches.toMutableMap().apply { put(batchId, batch.copy(items = updatedItems)) }
        }
        checkAndCleanupBatch(batchId)
    }
}
}
