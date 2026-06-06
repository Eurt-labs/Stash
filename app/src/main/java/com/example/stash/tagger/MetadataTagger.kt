package com.example.stash.tagger

import android.util.Log
import com.example.stash.model.TrackInfo
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Writes ID3v2.4 metadata tags to downloaded audio files.
 *
 * Tags written:
 * - Title, Artist, Album, Year, Track Number, Genre
 * - Embedded album artwork (JPEG) fetched from the album art URL
 *
 * Supports:
 * - MP3 files: Full ID3v2.4 tagging via mp3agic
 * - M4A/OGG/Opus: Metadata is handled by FFmpeg during conversion (not this class)
 */
class MetadataTagger {

    companion object {
        private const val TAG = "MetadataTagger"
        private const val MAX_ART_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB max artwork
    }

    private val httpClient = OkHttpClient()

    /**
     * Tags the file at [filePath] with metadata from [trackInfo].
     *
     * For MP3 files, writes ID3v2.4 tags including embedded album art.
     * For other formats, this is a no-op (handled by FFmpeg/yt-dlp during conversion).
     *
     * @param filePath Absolute path to the downloaded audio file.
     * @param trackInfo Metadata to embed.
     * @throws IOException if the file cannot be read or written.
     */
    fun tagFile(filePath: String, trackInfo: TrackInfo) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.w(TAG, "File does not exist: $filePath")
            return
        }

        val extension = file.extension.lowercase()

        when (extension) {
            "mp3" -> tagMp3(file, trackInfo)
            else -> {
                Log.d(TAG, "Skipping tagging for format: $extension (handled by converter)")
            }
        }
    }

    /**
     * Tags an MP3 file with ID3v2.4 metadata using mp3agic.
     */
    private fun tagMp3(file: File, trackInfo: TrackInfo) {
        try {
            val mp3File = Mp3File(file)

            val tag = ID3v24Tag().apply {
                title = trackInfo.title
                artist = trackInfo.artists.joinToString(", ")
                albumArtist = trackInfo.artists.firstOrNull() ?: ""
                album = trackInfo.album ?: ""
                year = trackInfo.releaseYear ?: ""
                track = trackInfo.trackNumber?.toString() ?: ""
                genreDescription = trackInfo.genre ?: ""
            }

            // Embed album artwork
            trackInfo.albumArtUrl?.let { artUrl ->
                try {
                    val artBytes = downloadArtwork(artUrl)
                    if (artBytes != null && artBytes.isNotEmpty()) {
                        tag.setAlbumImage(artBytes, "image/jpeg")
                        Log.d(TAG, "Embedded album art (${artBytes.size} bytes)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to download album artwork: ${e.message}")
                }
            }

            mp3File.id3v2Tag = tag

            // mp3agic requires writing to a new file, then replacing
            val tempFile = File(file.parent, "${file.nameWithoutExtension}_tagged.${file.extension}")
            mp3File.save(tempFile.absolutePath)

            // Replace original with tagged version
            if (file.delete() && tempFile.renameTo(file)) {
                Log.d(TAG, "Tagged successfully: ${file.name}")
            } else {
                // Fallback: copy temp over original
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
                Log.d(TAG, "Tagged (fallback copy): ${file.name}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to tag MP3: ${file.name}", e)
            throw IOException("MP3 tagging failed: ${e.message}", e)
        }
    }

    /**
     * Downloads album artwork from a URL.
     * Returns the raw JPEG bytes, or null on failure.
     */
    private fun downloadArtwork(url: String): ByteArray? {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "Artwork download failed: HTTP ${response.code}")
                return null
            }

            val body = response.body ?: return null
            val bytes = body.bytes()

            if (bytes.size > MAX_ART_SIZE_BYTES) {
                Log.w(TAG, "Artwork too large (${bytes.size} bytes), skipping")
                return null
            }

            bytes

        } catch (e: Exception) {
            Log.w(TAG, "Artwork download error: ${e.message}")
            null
        }
    }
}
