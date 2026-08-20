package com.eurtlabs.stash.data.storage

import android.content.Context
import android.os.Environment
import android.util.Log
import com.eurtlabs.stash.data.downloader.LogManager
import com.eurtlabs.stash.data.model.DownloadBatch
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadItem
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.DownloadState
import com.eurtlabs.stash.data.model.Platform
import com.eurtlabs.stash.data.model.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object LibraryStore {

    private const val TAG = "LibraryStore"
    private const val FILE_NAME = "stash_library_history.json"

    suspend fun loadLibrary(context: Context): List<DownloadBatch> = withContext(Dispatchers.IO) {
        val persistedBatches = loadPersistedBatches(context).toMutableList()
        val existingFiles = scanDiskMedia(context)

        // Merge any newly discovered disk files not yet in persisted batches
        val knownPaths = persistedBatches.flatMap { it.items }.mapNotNull { it.finalFilePath }.toSet()
        val newDiskItems = existingFiles.filter { it.finalFilePath !in knownPaths }

        if (newDiskItems.isNotEmpty()) {
            val diskBatch = DownloadBatch(
                id = "disk_scan_batch",
                name = "Downloaded on Device",
                items = newDiskItems,
                outputDir = StorageManager.getDisplayStoragePath(context),
                isCompleted = true
            )
            persistedBatches.add(0, diskBatch)
        }

        LogManager.append(TAG, "Loaded ${persistedBatches.flatMap { it.items }.size} library tracks from storage")
        persistedBatches
    }

    suspend fun saveLibrary(context: Context, batches: List<DownloadBatch>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            batches.forEach { batch ->
                val completedItems = batch.items.filter { it.state == DownloadState.COMPLETED && !it.finalFilePath.isNullOrBlank() }
                if (completedItems.isNotEmpty()) {
                    val batchObj = JSONObject().apply {
                        put("id", batch.id)
                        put("name", batch.name)
                        put("outputDir", batch.outputDir)
                        put("format", batch.format.name)
                        put("quality", batch.quality.name)

                        val itemsArray = JSONArray()
                        completedItems.forEach { item ->
                            val itemObj = JSONObject().apply {
                                put("id", item.id)
                                put("trackId", item.trackInfo.id)
                                put("title", item.trackInfo.title)
                                put("artists", JSONArray(item.trackInfo.artists))
                                put("durationMs", item.trackInfo.durationMs)
                                put("albumArtUrl", item.trackInfo.albumArtUrl ?: "")
                                put("sourceUrl", item.trackInfo.sourceUrl)
                                put("safeFileName", item.trackInfo.safeFileName)
                                put("format", item.format.name)
                                put("quality", item.quality.name)
                                put("state", item.state.name)
                                put("finalFilePath", item.finalFilePath ?: "")
                            }
                            itemsArray.put(itemObj)
                        }
                        put("items", itemsArray)
                    }
                    jsonArray.put(batchObj)
                }
            }

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(jsonArray.toString(), Charsets.UTF_8)
            LogManager.append(TAG, "Persisted library state to ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save library history", e)
        }
    }

    private fun loadPersistedBatches(context: Context): List<DownloadBatch> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val jsonStr = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonStr)
            val batches = mutableListOf<DownloadBatch>()

            for (i in 0 until jsonArray.length()) {
                val batchObj = jsonArray.getJSONObject(i)
                val batchId = batchObj.optString("id", UUID.randomUUID().toString())
                val name = batchObj.optString("name", "Saved Batch")
                val outputDir = batchObj.optString("outputDir", StorageManager.getDisplayStoragePath(context))
                val batchFormatName = batchObj.optString("format", DownloadFormat.MP3.name)
                val batchFormat = runCatching { DownloadFormat.valueOf(batchFormatName) }.getOrDefault(DownloadFormat.MP3)
                val batchQualityName = batchObj.optString("quality", DownloadQuality.AUDIO_320K.name)
                val batchQuality = runCatching { DownloadQuality.valueOf(batchQualityName) }.getOrDefault(DownloadQuality.AUDIO_320K)

                val itemsArray = batchObj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<DownloadItem>()

                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val filePath = itemObj.optString("finalFilePath", "")
                    if (filePath.isNotEmpty() && File(filePath).exists()) {
                        val trackId = itemObj.optString("trackId", UUID.randomUUID().toString())
                        val trackTitle = itemObj.optString("title", "Media Item")
                        val artistsJson = itemObj.optJSONArray("artists") ?: JSONArray()
                        val artists = mutableListOf<String>()
                        for (k in 0 until artistsJson.length()) {
                            artists.add(artistsJson.getString(k))
                        }
                        if (artists.isEmpty()) artists.add("Unknown Artist")

                        val durationMs = itemObj.optLong("durationMs", 0L)
                        val albumArtUrl = itemObj.optString("albumArtUrl", "").ifBlank { null }
                        val sourceUrl = itemObj.optString("sourceUrl", "")
                        val safeFileName = itemObj.optString("safeFileName", trackTitle)

                        val formatName = itemObj.optString("format", DownloadFormat.MP3.name)
                        val format = runCatching { DownloadFormat.valueOf(formatName) }.getOrDefault(DownloadFormat.MP3)
                        val qualityName = itemObj.optString("quality", DownloadQuality.AUDIO_320K.name)
                        val quality = runCatching { DownloadQuality.valueOf(qualityName) }.getOrDefault(DownloadQuality.AUDIO_320K)

                        val track = TrackInfo(
                            id = trackId,
                            title = trackTitle,
                            artists = artists,
                            durationMs = durationMs,
                            albumArtUrl = albumArtUrl,
                            source = Platform.OTHER,
                            sourceUrl = sourceUrl,
                            safeFileName = safeFileName
                        )

                        items.add(
                            DownloadItem(
                                id = itemObj.optString("id", UUID.randomUUID().toString()),
                                batchId = batchId,
                                trackInfo = track,
                                quality = quality,
                                format = format,
                                state = DownloadState.COMPLETED,
                                progress = 1f,
                                finalFilePath = filePath
                            )
                        )
                    }
                }

                if (items.isNotEmpty()) {
                    batches.add(
                        DownloadBatch(
                            id = batchId,
                            name = name,
                            items = items,
                            outputDir = outputDir,
                            quality = batchQuality,
                            format = batchFormat,
                            isCompleted = true
                        )
                    )
                }
            }
            batches
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing persisted library", e)
            emptyList()
        }
    }

    private fun scanDiskMedia(context: Context): List<DownloadItem> {
        val targetDirs = mutableListOf<File>()

        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "Music")
        val stashDir = File(baseDir, "Stash")
        if (stashDir.exists()) targetDirs.add(stashDir)

        val publicMusicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Stash")
        if (publicMusicDir.exists()) targetDirs.add(publicMusicDir)

        val supportedExts = setOf("mp3", "m4a", "flac", "opus", "wav", "mp4", "mkv", "webm")
        val diskItems = mutableListOf<DownloadItem>()

        for (dir in targetDirs) {
            dir.listFiles()?.filter { it.isFile && it.length() > 0L && it.extension.lowercase() in supportedExts }?.forEach { file ->
                val nameWithoutExt = file.nameWithoutExtension
                val parts = nameWithoutExt.split(" - ", limit = 2)
                val artist = if (parts.size == 2) parts[0].trim() else "Stash Audio"
                val title = if (parts.size == 2) parts[1].trim() else nameWithoutExt

                val format = when (file.extension.lowercase()) {
                    "flac" -> DownloadFormat.FLAC
                    "wav" -> DownloadFormat.WAV
                    "m4a", "aac" -> DownloadFormat.AAC
                    "opus" -> DownloadFormat.OPUS
                    "mp4" -> DownloadFormat.MP4
                    "mkv" -> DownloadFormat.MKV
                    "webm" -> DownloadFormat.WEBM
                    else -> DownloadFormat.MP3
                }

                val track = TrackInfo(
                    id = file.name,
                    title = title,
                    artists = listOf(artist),
                    durationMs = 0L,
                    albumArtUrl = null,
                    source = Platform.OTHER,
                    sourceUrl = "",
                    safeFileName = nameWithoutExt
                )

                diskItems.add(
                    DownloadItem(
                        id = file.absolutePath,
                        batchId = "disk_scan_batch",
                        trackInfo = track,
                        quality = DownloadQuality.AUDIO_320K,
                        format = format,
                        state = DownloadState.COMPLETED,
                        progress = 1f,
                        finalFilePath = file.absolutePath
                    )
                )
            }
        }
        return diskItems
    }
}
