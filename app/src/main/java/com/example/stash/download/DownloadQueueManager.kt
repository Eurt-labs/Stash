package com.example.stash.download

import com.example.stash.convert.ConversionEngine
import com.example.stash.storage.FileManager
import com.example.stash.tagger.MetadataTagger
import com.example.stash.youtube.YouTubeSearchMatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages a strict 5-phase sequential download pipeline.
 *
 * Phase 1: FETCH    — Metadata already fetched by StashOrchestrator, saved to JSON manifest
 * Phase 2: DOWNLOAD — Download each track one-by-one from the manifest using yt-dlp
 * Phase 3: CONVERT  — Convert each downloaded file one-by-one using FFmpeg
 * Phase 4: MOVE     — Tag and move each converted file to the user's selected folder
 * Phase 5: CLEANUP  — Delete the temp JSON manifest and cache files
 *
 * All processing within a batch is sequential (one track at a time).
 * Batches themselves are also processed sequentially (one batch at a time).
 */
class DownloadQueueManager {
    companion object {
        private const val TAG = "DownloadQueue"
    }

    private val downloadEngine = DownloadEngine()
    private val conversionEngine = ConversionEngine()
    private val youtubeSearchMatcher = YouTubeSearchMatcher()
    private val metadataTagger = MetadataTagger()
    private val fileManager = FileManager()
    private val manifestManager = ManifestManager()

    // Semaphores to limit active download and convert tasks to 1 at a time for pipeline safety
    private val downloadSemaphore = Semaphore(1)
    private val convertSemaphore = Semaphore(1)

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
                        val request = requestMap[item.id]
                        if (request == null) {
                            System.err.println("No request found for track ${item.id}")
                            failItem(batchId, item.id, "No request found")
                            return@launch
                        }

                        // Step 1: Search (for Spotify tracks)
                        var downloadUrl = request.url
                        if (request.trackInfo.youtubeUrl == null &&
                            request.trackInfo.source == com.example.stash.model.Platform.SPOTIFY
                        ) {
                            updateItemState(batchId, request.id, DownloadState.SEARCHING)
                            try {
                                val matchedUrl = youtubeSearchMatcher.findBestMatch(request.trackInfo)
                                    ?: throw DownloadException("No YouTube match found for: ${request.trackInfo.displayName}")
                                downloadUrl = matchedUrl
                            } catch (e: CancellationException) {
                                updateItemState(batchId, request.id, DownloadState.CANCELLED)
                                throw e
                            } catch (e: Exception) {
                                System.err.println("YouTube search failed for: ${item.trackInfo.displayName}")
                                e.printStackTrace()
                                failItem(batchId, request.id, e.message ?: "YouTube search failed")
                                return@launch
                            }
                        } else if (request.trackInfo.youtubeUrl != null) {
                            downloadUrl = request.trackInfo.youtubeUrl
                        }

                        // Step 2: Download (constrained by downloadSemaphore)
                        val rawFilePath = try {
                            downloadSemaphore.withPermit {
                                if (!isActive) return@launch
                                updateItemState(batchId, request.id, DownloadState.DOWNLOADING)
                                val updatedRequest = request.copy(url = downloadUrl)
                                downloadEngine.download(updatedRequest) { percent, eta, speed ->
                                    updateItemProgress(batchId, request.id, percent / 100f, eta, speed.ifBlank { null })
                                }
                            }
                        } catch (e: CancellationException) {
                            updateItemState(batchId, request.id, DownloadState.CANCELLED)
                            throw e
                        } catch (e: Exception) {
                            System.err.println("Download failed for: ${item.trackInfo.displayName}")
                            e.printStackTrace()
                            failItem(batchId, request.id, e.message ?: "Download failed")
                            return@launch
                        }

                        rawFilePaths[request.id] = rawFilePath
                        println("Downloaded: ${request.trackInfo.displayName} → $rawFilePath")

                        // Step 3: Convert (constrained by convertSemaphore)
                        val convertedPath = try {
                            convertSemaphore.withPermit {
                                if (!isActive) return@launch
                                updateItemState(batchId, item.id, DownloadState.CONVERTING)
                                updateItemProgress(batchId, item.id, 0f, null, "Converting...")
                                conversionEngine.convert(
                                    inputPath = rawFilePath,
                                    format = request.format,
                                    quality = request.quality
                                ) { progress ->
                                    updateItemProgress(batchId, item.id, progress, null, "Converting...")
                                }
                            }
                        } catch (e: CancellationException) {
                            updateItemState(batchId, item.id, DownloadState.CANCELLED)
                            throw e
                        } catch (e: Exception) {
                            System.err.println("Conversion failed for: ${item.trackInfo.displayName}")
                            e.printStackTrace()
                            failItem(batchId, item.id, e.message ?: "Conversion failed")
                            return@launch
                        }

                        convertedFilePaths[request.id] = convertedPath
                        println("Converted: ${request.trackInfo.displayName} → $convertedPath")

                        // Step 4: Tag & Move
                        try {
                            if (!isActive) return@launch
                            updateItemState(batchId, item.id, DownloadState.TAGGING)
                            try {
                                metadataTagger.tagFile(convertedPath, request.trackInfo)
                            } catch (e: Exception) {
                                System.err.println("Metadata tagging failed (non-fatal): ${e.message}")
                            }

                            updateItemState(batchId, item.id, DownloadState.MOVING)
                            val outputDir = batch.outputDir.ifBlank { fileManager.getDefaultDownloadDir() }
                            val subfolderName = request.trackInfo.album?.trim()?.takeIf { it.isNotBlank() } ?: batch.name
                            val finalPath = withContext(Dispatchers.IO) {
                                fileManager.moveToFinalDestination(convertedPath, request.trackInfo, request.format.extension, outputDir, subfolderName)
                            }

                            completeItem(batchId, item.id, finalPath)
                            println("Moved: ${request.trackInfo.displayName} → $finalPath")
                        } catch (e: CancellationException) {
                            updateItemState(batchId, item.id, DownloadState.CANCELLED)
                            throw e
                        } catch (e: Exception) {
                            System.err.println("Move failed for: ${item.trackInfo.displayName}")
                            e.printStackTrace()
                            failItem(batchId, item.id, e.message ?: "Move failed")
                        }
                    }
                }

                jobs.joinAll()

                // ════════════════════════════════════════════
                // PHASE 5: CLEANUP — delete temp JSON + cache
                // ════════════════════════════════════════════
                println("\n── Phase 5: CLEANUP ──")
                manifestManager.cleanupBatch(batchId)

                // Clean up tracking maps for this batch
                val finalBatch = _batches.value[batchId]
                finalBatch?.items?.forEach { item ->
                    rawFilePaths.remove(item.id)
                    convertedFilePaths.remove(item.id)
                    requestMap.remove(item.id)
                }

                println("\n═══════════════════════════════════════════════════")
                println("Batch '${batch.name}' completed!")
                println("═══════════════════════════════════════════════════\n")

            } catch (e: CancellationException) {
                println("Batch '$batchId' was cancelled")
                // Mark remaining queued items as cancelled
                val cancelledBatch = _batches.value[batchId]
                cancelledBatch?.items?.forEach { item ->
                    if (item.state != DownloadState.COMPLETE && item.state != DownloadState.FAILED) {
                        updateItemState(batchId, item.id, DownloadState.CANCELLED)
                    }
                }
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

    // ══════════════════════════════════════════════════════
    // Phase 2: Download one track
    // ══════════════════════════════════════════════════════

    private suspend fun processDownloadPhase(request: DownloadRequest, batchId: String) {
        var downloadUrl = request.url

        // Step 2a: Search YouTube if needed (for Spotify tracks)
        if (request.trackInfo.youtubeUrl == null &&
            request.trackInfo.source == com.example.stash.model.Platform.SPOTIFY
        ) {
            updateItemState(batchId, request.id, DownloadState.SEARCHING)
            val matchedUrl = youtubeSearchMatcher.findBestMatch(request.trackInfo)
                ?: throw DownloadException("No YouTube match found for: ${request.trackInfo.displayName}")
            downloadUrl = matchedUrl
        } else if (request.trackInfo.youtubeUrl != null) {
            downloadUrl = request.trackInfo.youtubeUrl
        }

        // Step 2b: Download raw audio
        updateItemState(batchId, request.id, DownloadState.DOWNLOADING)
        val updatedRequest = request.copy(url = downloadUrl)
        val rawFilePath = downloadEngine.download(updatedRequest) { percent, eta, speed ->
            updateItemProgress(batchId, request.id, percent / 100f, eta, speed.ifBlank { null })
        }

        // Store the raw file path for the convert phase
        rawFilePaths[request.id] = rawFilePath
        println("Downloaded: ${request.trackInfo.displayName} → $rawFilePath")
    }

    // ══════════════════════════════════════════════════════
    // Phase 3: Convert one track
    // ══════════════════════════════════════════════════════

    private suspend fun processConvertPhase(
        trackId: String,
        rawPath: String,
        request: DownloadRequest,
        batchId: String
    ) {
        updateItemState(batchId, trackId, DownloadState.CONVERTING)
        updateItemProgress(batchId, trackId, 0f, null, null)

        val convertedPath = conversionEngine.convert(
            inputPath = rawPath,
            format = request.format,
            quality = request.quality
        ) { progress ->
            updateItemProgress(batchId, trackId, progress, null, "Converting...")
        }

        convertedFilePaths[trackId] = convertedPath
        println("Converted: ${request.trackInfo.displayName} → $convertedPath")
    }

    // ══════════════════════════════════════════════════════
    // Phase 4: Tag and move one track
    // ══════════════════════════════════════════════════════

    private suspend fun processMovePhase(
        trackId: String,
        convertedPath: String,
        request: DownloadRequest,
        batchId: String
    ) {
        // Step 4a: Tag the file with metadata
        updateItemState(batchId, trackId, DownloadState.TAGGING)
        try {
            metadataTagger.tagFile(convertedPath, request.trackInfo)
        } catch (e: Exception) {
            System.err.println("Metadata tagging failed (non-fatal): ${e.message}")
        }

        // Step 4b: Move to final destination
        updateItemState(batchId, trackId, DownloadState.MOVING)

        // Get the output directory from the batch
        val batch = _batches.value[batchId]
        val outputDir = batch?.outputDir ?: fileManager.getDefaultDownloadDir()
        val batchName = batch?.name ?: "Stash Playlist"
        val subfolderName = request.trackInfo.album?.trim()?.takeIf { it.isNotBlank() } ?: batchName

        val finalPath = withContext(Dispatchers.IO) {
            fileManager.moveToFinalDestination(convertedPath, request.trackInfo, request.format.extension, outputDir, subfolderName)
        }

        // Mark as complete
        completeItem(batchId, trackId, finalPath)
        println("Moved: ${request.trackInfo.displayName} → $finalPath")
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

        // If this is the active batch, cancel the job
        if (activeBatchId == batchId) {
            activeBatchJob?.cancel()
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

        // Clean up
        manifestManager.cleanupBatch(batchId)

        synchronized(pendingBatchIds) {
            pendingBatchIds.remove(batchId)
            if (activeBatchId == batchId) {
                activeBatchId = null
            }
        }
        checkNextBatch()

        println("Batch '$batchId' cancelled")
    }

    /**
     * Cancels all batches and active downloads, then cleans up cache.
     */
    fun cancelAll() {
        activeBatchJob?.cancel()
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

        // Clear the cache directory immediately upon stopping all
        fileManager.cleanupCacheDir()
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
                it.state == DownloadState.SEARCHING ||
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
    }
}
