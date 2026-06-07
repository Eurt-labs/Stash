package com.example.stash.download

import com.example.stash.model.Platform
import com.example.stash.model.TrackInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Manages temporary JSON manifest files that store fetched track metadata.
 *
 * Each batch of tracks (from a link paste) gets its own JSON manifest file
 * stored in `~/.stash_cache/`. The manifest is created after fetching metadata
 * and deleted after all downloads, conversions, and moves are complete.
 *
 * File format: `~/.stash_cache/{batchId}_manifest.json`
 */
class ManifestManager {

    companion object {
        private const val CACHE_DIR_NAME = ".stash_cache"
        private const val MANIFEST_SUFFIX = "_manifest.json"
    }

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Returns the cache directory path, creating it if necessary.
     */
    fun getCacheDir(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Saves a list of tracks to a JSON manifest file for the given batch.
     *
     * @param batchId Unique identifier for the batch.
     * @param tracks List of track metadata to persist.
     * @return The absolute path to the created manifest file.
     */
    fun saveManifest(batchId: String, tracks: List<TrackInfo>): String {
        val cacheDir = getCacheDir()
        val manifestFile = File(cacheDir, "$batchId$MANIFEST_SUFFIX")

        // Convert to serializable format
        val serializableTracks = tracks.map { track ->
            SerializableTrack(
                title = track.title,
                artists = track.artists,
                album = track.album,
                durationMs = track.durationMs,
                albumArtUrl = track.albumArtUrl,
                trackNumber = track.trackNumber,
                releaseYear = track.releaseYear,
                genre = track.genre,
                source = track.source.name,
                sourceUrl = track.sourceUrl,
                youtubeUrl = track.youtubeUrl
            )
        }

        manifestFile.writeText(gson.toJson(serializableTracks))
        println("Saved manifest: ${manifestFile.absolutePath} (${tracks.size} tracks)")
        return manifestFile.absolutePath
    }

    /**
     * Loads tracks from a JSON manifest file for the given batch.
     *
     * @param batchId Unique identifier for the batch.
     * @return List of track metadata, or empty list if manifest doesn't exist.
     */
    fun loadManifest(batchId: String): List<TrackInfo> {
        val cacheDir = getCacheDir()
        val manifestFile = File(cacheDir, "$batchId$MANIFEST_SUFFIX")

        if (!manifestFile.exists()) {
            println("Manifest not found: ${manifestFile.absolutePath}")
            return emptyList()
        }

        return try {
            val jsonStr = manifestFile.readText()
            val type = object : TypeToken<List<SerializableTrack>>() {}.type
            val serializableTracks: List<SerializableTrack> = gson.fromJson(jsonStr, type)

            serializableTracks.map { st ->
                TrackInfo(
                    title = st.title,
                    artists = st.artists,
                    album = st.album,
                    durationMs = st.durationMs,
                    albumArtUrl = st.albumArtUrl,
                    trackNumber = st.trackNumber,
                    releaseYear = st.releaseYear,
                    genre = st.genre,
                    source = try { Platform.valueOf(st.source) } catch (e: Exception) { Platform.SPOTIFY },
                    sourceUrl = st.sourceUrl,
                    youtubeUrl = st.youtubeUrl
                )
            }
        } catch (e: Exception) {
            System.err.println("Failed to load manifest: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Deletes the manifest file for the given batch.
     *
     * @param batchId Unique identifier for the batch.
     */
    fun deleteManifest(batchId: String) {
        val cacheDir = getCacheDir()
        val manifestFile = File(cacheDir, "$batchId$MANIFEST_SUFFIX")

        if (manifestFile.exists()) {
            manifestFile.delete()
            println("Deleted manifest: ${manifestFile.absolutePath}")
        }
    }

    /**
     * Cleans up all temporary files for a batch (raw downloads + manifest).
     *
     * @param batchId Unique identifier for the batch.
     */
    fun cleanupBatch(batchId: String) {
        deleteManifest(batchId)

        // Delete any remaining raw files in the cache directory
        // (they should already be deleted by ConversionEngine, but this is a safety net)
        val cacheDir = getCacheDir()
        val batchFiles = cacheDir.listFiles()?.filter { 
            it.isFile && !it.name.endsWith(MANIFEST_SUFFIX) 
        }
        
        if (batchFiles != null && batchFiles.isNotEmpty()) {
            println("Cleaning up ${batchFiles.size} remaining cache files for batch $batchId")
            batchFiles.forEach { file ->
                try {
                    file.delete()
                } catch (e: Exception) {
                    System.err.println("Failed to delete cache file: ${file.name}")
                }
            }
        }
    }

    /**
     * Internal serializable representation of TrackInfo for JSON persistence.
     * Uses primitive types only for clean Gson serialization.
     */
    private data class SerializableTrack(
        val title: String,
        val artists: List<String>,
        val album: String? = null,
        val durationMs: Long = 0L,
        val albumArtUrl: String? = null,
        val trackNumber: Int? = null,
        val releaseYear: String? = null,
        val genre: String? = null,
        val source: String,
        val sourceUrl: String,
        val youtubeUrl: String? = null
    )
}
