package com.example.stash.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Manages output file paths, directory creation, duplicate handling,
 * and MediaStore registration so downloaded files appear in the system music library.
 */
class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"

        /** Default download subfolder inside the device's Downloads directory. */
        const val DEFAULT_SUBFOLDER = "Stash"
    }

    /**
     * Returns the default download directory path.
     * Creates the directory if it doesn't exist.
     *
     * Path: `/storage/emulated/0/Download/Stash/`
     */
    fun getDefaultDownloadDir(): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val stashDir = File(downloadsDir, DEFAULT_SUBFOLDER)
        if (!stashDir.exists()) {
            stashDir.mkdirs()
        }
        return stashDir.absolutePath
    }

    /**
     * Generates a unique file path for the given track, avoiding duplicates.
     *
     * If `Artist - Title.mp3` already exists, produces:
     * - `Artist - Title (1).mp3`
     * - `Artist - Title (2).mp3`
     * - etc.
     *
     * @param outputDir Directory where the file will be saved.
     * @param baseName Desired filename without extension (from [TrackInfo.safeFileName]).
     * @param extension File extension (e.g., "mp3", "m4a").
     * @return A [File] with a unique name in the target directory.
     */
    fun getUniqueFile(outputDir: String, baseName: String, extension: String): File {
        val dir = File(outputDir)
        if (!dir.exists()) dir.mkdirs()

        var file = File(dir, "$baseName.$extension")
        var counter = 1

        while (file.exists()) {
            file = File(dir, "$baseName ($counter).$extension")
            counter++
        }

        return file
    }

    /**
     * Checks if a file with the given name already exists in the output directory.
     * Useful for skip-if-exists functionality.
     */
    fun fileExists(outputDir: String, baseName: String, extension: String): Boolean {
        return File(outputDir, "$baseName.$extension").exists()
    }

    /**
     * Registers a downloaded audio file with Android's MediaStore.
     *
     * This makes the file visible in:
     * - The device's built-in music player
     * - Google Files app
     * - Any app that scans the MediaStore
     *
     * @param filePath Absolute path to the downloaded file.
     * @param title Track title.
     * @param artist Artist name.
     * @param album Album name.
     * @param durationMs Duration in milliseconds.
     */
    fun registerInMediaStore(
        filePath: String,
        title: String,
        artist: String,
        album: String?,
        durationMs: Long
    ): Uri? {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w(TAG, "Cannot register non-existent file: $filePath")
                return null
            }

            val mimeType = when (file.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "m4a" -> "audio/mp4"
                "ogg" -> "audio/ogg"
                "opus" -> "audio/opus"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                else -> "audio/*"
            }

            val isVideo = mimeType.startsWith("video/")
            val contentUri = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_SUBFOLDER")
                put(MediaStore.MediaColumns.SIZE, file.length())

                if (!isVideo) {
                    put(MediaStore.Audio.AudioColumns.TITLE, title)
                    put(MediaStore.Audio.AudioColumns.ARTIST, artist)
                    if (album != null) {
                        put(MediaStore.Audio.AudioColumns.ALBUM, album)
                    }
                    if (durationMs > 0) {
                        put(MediaStore.Audio.AudioColumns.DURATION, durationMs)
                    }
                    put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1)
                }
            }

            val insertedUri = context.contentResolver.insert(contentUri, values)
            Log.d(TAG, "Registered in MediaStore: ${file.name} -> $insertedUri")
            return insertedUri

        } catch (e: Exception) {
            Log.w(TAG, "MediaStore registration failed (non-fatal): ${e.message}")
            return null
        }
    }

    /**
     * Deletes a downloaded file and removes it from MediaStore.
     */
    fun deleteFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted file: $filePath")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete file: $filePath", e)
        }
    }

    /**
     * Moves or copies a file from the temporary/cache directory to its final destination.
     */
    fun moveToFinalDestination(
        tempFilePath: String,
        trackInfo: com.example.stash.model.TrackInfo,
        extension: String
    ): String {
        val cacheFile = File(tempFilePath)
        if (!cacheFile.exists()) {
            throw java.io.IOException("Source file not found: $tempFilePath")
        }

        val prefs = context.getSharedPreferences("stash_prefs", Context.MODE_PRIVATE)
        val customUriStr = prefs.getString("download_folder_uri", null)

        if (customUriStr != null) {
            try {
                val uri = Uri.parse(customUriStr)
                val docDir = DocumentFile.fromTreeUri(context, uri)
                    ?: throw java.io.IOException("Failed to resolve custom download directory")

                // Create a unique filename in the SAF folder
                val baseName = trackInfo.safeFileName
                var docFile = docDir.createFile(getMimeType(extension), "$baseName.$extension")
                if (docFile == null) {
                    var counter = 1
                    while (docFile == null && counter < 100) {
                        docFile = docDir.createFile(getMimeType(extension), "$baseName ($counter).$extension")
                        counter++
                    }
                }

                val targetDocFile = docFile ?: throw java.io.IOException("Failed to create file in custom directory")

                // Copy stream
                context.contentResolver.openOutputStream(targetDocFile.uri)?.use { outStream ->
                    cacheFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                // Delete cache file
                cacheFile.delete()

                // Register in MediaStore
                registerUriInMediaStore(targetDocFile.uri, targetDocFile.name ?: "$baseName.$extension", trackInfo, extension)

                return targetDocFile.uri.toString()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save to custom directory, falling back to default", e)
            }
        }

        // Default: save to Downloads/Stash
        val defaultDir = getDefaultDownloadDir()
        val uniqueFile = getUniqueFile(defaultDir, trackInfo.safeFileName, extension)
        
        cacheFile.renameTo(uniqueFile)
        
        // Register in MediaStore
        val mediaUri = registerInMediaStore(
            filePath = uniqueFile.absolutePath,
            title = trackInfo.title,
            artist = trackInfo.artists.joinToString(", "),
            album = trackInfo.album,
            durationMs = trackInfo.durationMs
        )

        return mediaUri?.toString() ?: Uri.fromFile(uniqueFile).toString()
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            else -> "audio/*"
        }
    }

    private fun registerUriInMediaStore(uri: Uri, filename: String, trackInfo: com.example.stash.model.TrackInfo, extension: String) {
        try {
            val isVideo = extension.lowercase() == "mp4" || extension.lowercase() == "webm"
            val contentUri = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(extension))
                if (!isVideo) {
                    put(MediaStore.Audio.AudioColumns.TITLE, trackInfo.title)
                    put(MediaStore.Audio.AudioColumns.ARTIST, trackInfo.artists.joinToString(", "))
                    if (trackInfo.album != null) {
                        put(MediaStore.Audio.AudioColumns.ALBUM, trackInfo.album)
                    }
                    if (trackInfo.durationMs > 0) {
                        put(MediaStore.Audio.AudioColumns.DURATION, trackInfo.durationMs)
                    }
                    put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1)
                }
            }

            context.contentResolver.insert(contentUri, values)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register SAF URI in MediaStore: ${e.message}")
        }
    }
}
