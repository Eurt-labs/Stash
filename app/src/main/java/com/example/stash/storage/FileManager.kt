package com.example.stash.storage

import java.io.File

/**
 * Manages output file paths, directory creation, duplicate handling for Desktop.
 */
class FileManager {

    companion object {
        /** Default download subfolder inside the user's Downloads directory. */
        const val DEFAULT_SUBFOLDER = "Stash"
    }

    /**
     * Returns the default download directory path.
     * Creates the directory if it doesn't exist.
     *
     * Path: `C:\Users\username\Downloads\Stash` (or equivalent on macOS/Linux)
     */
    fun getDefaultDownloadDir(): String {
        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
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

    fun fileExists(outputDir: String, baseName: String, extension: String): Boolean {
        return File(outputDir, "$baseName.$extension").exists()
    }

    fun deleteFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
                println("Deleted file: $filePath")
            }
        } catch (e: Exception) {
            System.err.println("Failed to delete file: $filePath")
            e.printStackTrace()
        }
    }

    /**
     * Moves a file from the temporary/cache directory to its final destination.
     *
     * @param tempFilePath Absolute path to the temp/converted file.
     * @param trackInfo Track metadata for generating the filename.
     * @param extension File extension (e.g. "mp3", "m4a").
     * @param destinationDir The user-selected output directory. If not provided, uses the default.
     * @return The absolute path to the final destination file.
     */
    fun moveToFinalDestination(
        tempFilePath: String,
        trackInfo: com.example.stash.model.TrackInfo,
        extension: String,
        destinationDir: String? = null
    ): String {
        val cacheFile = File(tempFilePath)
        if (!cacheFile.exists()) {
            throw java.io.IOException("Source file not found: $tempFilePath")
        }

        val outputDir = destinationDir ?: getDefaultDownloadDir()
        val uniqueFile = getUniqueFile(outputDir, trackInfo.safeFileName, extension)
        
        val success = cacheFile.renameTo(uniqueFile)
        if (!success) {
            // fallback to copy and delete
            cacheFile.copyTo(uniqueFile, overwrite = true)
            cacheFile.delete()
        }
        
        return uniqueFile.absolutePath
    }

    /**
     * Cleans up the cache directory by deleting all files within it.
     * The directory itself is preserved.
     */
    fun cleanupCacheDir() {
        val userHome = System.getProperty("user.home")
        val cacheDir = File(userHome, ".stash_cache")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                try {
                    if (file.isFile) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    System.err.println("Failed to delete cache file: ${file.name}")
                }
            }
            println("Cache directory cleaned up")
        }
    }
}
