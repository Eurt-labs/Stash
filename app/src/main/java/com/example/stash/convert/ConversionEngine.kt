package com.example.stash.convert

import com.example.stash.download.DownloadFormat
import com.example.stash.download.DownloadQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Audio conversion engine wrapping FFmpeg via ProcessBuilder.
 *
 * Converts raw downloaded audio files (typically .webm, .opus, .m4a from yt-dlp)
 * to the user's selected format (MP3/AAC) at the user's selected quality (bitrate).
 *
 * Processes one file at a time sequentially.
 */
class ConversionEngine {

    /**
     * Converts a raw audio file to the specified format and quality.
     *
     * @param inputPath Absolute path to the raw downloaded audio file.
     * @param format Target output format (MP3 or AAC).
     * @param quality Target audio quality (bitrate).
     * @param onProgress Optional callback for conversion progress (0.0 to 1.0).
     * @return Absolute path to the converted output file.
     * @throws ConversionException if conversion fails.
     */
    suspend fun convert(
        inputPath: String,
        format: DownloadFormat,
        quality: DownloadQuality,
        onProgress: ((Float) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            throw ConversionException("Input file not found: $inputPath")
        }

        // Build output path: same directory, same base name, new extension
        val outputFile = File(
            inputFile.parent,
            "${inputFile.nameWithoutExtension}.${format.extension}"
        )

        // If the input is already in the correct format, check if we just need to re-encode for quality
        val inputExtension = inputFile.extension.lowercase()
        if (inputExtension == format.extension) {
            // Already correct format — just rename/keep
            println("File already in ${format.extension} format, skipping conversion: ${inputFile.name}")
            return@withContext inputPath
        }

        println("Converting: ${inputFile.name} → ${outputFile.name} (${format.ffmpegCodec} @ ${quality.bitrateKbps}kbps)")

        // First, get the duration of the input file for progress tracking
        val durationSeconds = getDuration(inputPath)

        val cmd = mutableListOf(
            "ffmpeg",
            "-i", inputPath,
            "-y",                          // Overwrite output
            "-vn",                         // No video
            "-codec:a", format.ffmpegCodec,
            "-b:a", "${quality.bitrateKbps}k",
            "-ar", "44100",               // Standard sample rate
            "-ac", "2",                    // Stereo
            "-progress", "pipe:1",         // Output progress to stdout
            "-loglevel", "error",          // Only show errors on stderr
            outputFile.absolutePath
        )

        try {
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start()

            // Read progress from stdout
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("out_time_us=")) {
                        val timeUs = line.substringAfter("out_time_us=").toLongOrNull()
                        if (timeUs != null && durationSeconds > 0) {
                            val currentSeconds = timeUs / 1_000_000.0
                            val progress = (currentSeconds / durationSeconds).toFloat().coerceIn(0f, 1f)
                            onProgress?.invoke(progress)
                        }
                    }
                }
            }

            // Also drain stderr to prevent blocking
            val stderrOutput = process.errorStream.bufferedReader().readText()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw ConversionException(
                    "FFmpeg exited with code $exitCode: ${stderrOutput.take(500)}"
                )
            }

            if (!outputFile.exists()) {
                throw ConversionException("Conversion completed but output file not found: ${outputFile.absolutePath}")
            }

            // Delete the raw input file after successful conversion
            if (inputFile.absolutePath != outputFile.absolutePath) {
                inputFile.delete()
                println("Deleted raw file: ${inputFile.name}")
            }

            println("Conversion complete: ${outputFile.name} (${outputFile.length() / 1024}KB)")
            onProgress?.invoke(1f)

            return@withContext outputFile.absolutePath

        } catch (e: ConversionException) {
            throw e
        } catch (e: Exception) {
            System.err.println("Conversion failed: ${e.message}")
            e.printStackTrace()
            throw ConversionException("Conversion failed: ${e.message}", e)
        }
    }

    /**
     * Gets the duration of an audio file in seconds using ffprobe.
     * Returns 0 if duration cannot be determined.
     */
    private fun getDuration(filePath: String): Double {
        return try {
            val cmd = listOf(
                "ffprobe",
                "-v", "quiet",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            )

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            output.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            println("Could not determine duration: ${e.message}")
            0.0
        }
    }
}

class ConversionException(message: String, cause: Throwable? = null) : Exception(message, cause)
