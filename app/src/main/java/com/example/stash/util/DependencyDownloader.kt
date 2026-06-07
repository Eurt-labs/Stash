package com.example.stash.util

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object DependencyDownloader {
    private val client = OkHttpClient()

    /**
     * Downloads and installs yt-dlp, ffmpeg, and ffprobe.
     * Reports progress and status updates to onProgress.
     * returns true if successful.
     */
    fun downloadDependencies(
        onProgress: (String, Float) -> Unit
    ): Boolean {
        val targetDir = getTargetDirectory()
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        try {
            // 1. Download yt-dlp.exe
            onProgress("Downloading yt-dlp...", 0.05f)
            val ytDlpFile = File(targetDir, "yt-dlp.exe")
            downloadFile("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe", ytDlpFile) { progress ->
                onProgress("Downloading yt-dlp (${(progress * 100).toInt()}%)...", 0.05f + progress * 0.35f)
            }

            // 2. Download ffmpeg-release-essentials.zip
            onProgress("Downloading ffmpeg & ffprobe...", 0.40f)
            val ffmpegZipFile = File(targetDir, "ffmpeg.zip")
            downloadFile("https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip", ffmpegZipFile) { progress ->
                onProgress("Downloading ffmpeg & ffprobe (${(progress * 100).toInt()}%)...", 0.40f + progress * 0.40f)
            }

            // 3. Extract ffmpeg.exe and ffprobe.exe
            onProgress("Extracting ffmpeg & ffprobe...", 0.85f)
            extractExecutablesFromZip(ffmpegZipFile, targetDir)
            
            // Delete the temporary zip file
            ffmpegZipFile.delete()

            onProgress("Dependencies installed successfully!", 1.0f)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress("Installation failed: ${e.message}", -1.0f)
            return false
        }
    }

    private fun getTargetDirectory(): File {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            return File(resourcesDir)
        }
        val devDir = File("app-resources/windows")
        if (devDir.exists() || devDir.parentFile.exists()) {
            return devDir
        }
        return File("../app-resources/windows")
    }

    private fun downloadFile(url: String, destination: File, onProgress: (Float) -> Unit) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download file from $url: HTTP ${response.code}")
            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            
            body.byteStream().use { inputStream ->
                FileOutputStream(destination).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalBytesRead.toFloat() / totalBytes)
                        }
                    }
                }
            }
        }
    }

    private fun extractExecutablesFromZip(zipFile: File, targetDir: File) {
        zipFile.inputStream().use { fileInput ->
            ZipInputStream(fileInput).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name.endsWith("ffmpeg.exe") || name.endsWith("ffprobe.exe")) {
                        val simpleName = File(name).name
                        val outputFile = File(targetDir, simpleName)
                        FileOutputStream(outputFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zipInput.read(buffer).also { len = it } > 0) {
                                outputStream.write(buffer, 0, len)
                            }
                        }
                    }
                    zipInput.closeEntry()
                    entry = zipInput.nextEntry
                }
            }
        }
    }
}
