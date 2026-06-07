package com.example.stash.download

/**
 * User-selectable quality presets (dynamic for both Audio and Video).
 */
enum class DownloadQuality(val label: String, val bitrateKbps: Int, val videoLabel: String) {
    LOW("Low Quality (128kbps)", 128, "Low Quality (360p)"),
    MID("Mid Quality (192kbps)", 192, "Mid Quality (720p)"),
    HIGH("High Quality (320kbps)", 320, "High Quality (1080p)");

    fun getLabelForFormat(format: DownloadFormat): String {
        return when (format) {
            DownloadFormat.MP4,
            DownloadFormat.YOUTUBE_VIDEO,
            DownloadFormat.INSTAGRAM_VIDEO,
            DownloadFormat.OTHER_VIDEO -> videoLabel
            DownloadFormat.AUTO -> when (this) {
                LOW -> "Low Quality (128kbps / 360p)"
                MID -> "Mid Quality (192kbps / 720p)"
                HIGH -> "High Quality (320kbps / 1080p)"
            }
            else -> label
        }
    }

    override fun toString(): String = label
}

/**
 * Download format — Auto-Detect, MP3/AAC audio, or MP4 video options.
 */
enum class DownloadFormat(val label: String, val extension: String, val ffmpegCodec: String) {
    AUTO("Auto-Detect", "", ""),
    MP3("MP3 Audio", "mp3", "libmp3lame"),
    AAC("AAC Audio", "m4a", "aac"),
    MP4("MP4 Video", "mp4", "copy"),
    YOUTUBE_VIDEO("YouTube Video (MP4)", "mp4", "copy"),
    INSTAGRAM_VIDEO("Instagram Video (MP4)", "mp4", "copy"),
    OTHER_VIDEO("Other Video (MP4)", "mp4", "copy");

    override fun toString(): String = label
}

/**
 * Represents a single download request submitted to the download queue.
 */
data class DownloadRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val trackInfo: com.example.stash.model.TrackInfo,
    val outputDir: String,
    val quality: DownloadQuality = DownloadQuality.HIGH,
    val format: DownloadFormat = DownloadFormat.MP3,
    val embedArtwork: Boolean = true
)

/**
 * Current state of a download in the queue.
 */
enum class DownloadState {
    QUEUED,
    SEARCHING,     // Searching YouTube for a match (Spotify tracks)
    DOWNLOADING,
    CONVERTING,
    MOVING,        // Moving converted file to final destination
    TAGGING,
    COMPLETE,
    FAILED,
    CANCELLED,
    PAUSED
}

/**
 * Observable state for a single download item in the queue.
 */
data class DownloadItem(
    val id: String,
    val trackInfo: com.example.stash.model.TrackInfo,
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Float = 0f,
    val speed: String? = null,
    val eta: Long? = null,
    val error: String? = null,
    val filePath: String? = null,
    val rawFilePath: String? = null  // Path to the raw downloaded file before conversion
)

/**
 * Sealed result class emitted by the download engine.
 */
sealed class DownloadResult {
    data class Success(
        val filePath: String,
        val trackInfo: com.example.stash.model.TrackInfo
    ) : DownloadResult()

    data class Error(
        val message: String,
        val trackInfo: com.example.stash.model.TrackInfo
    ) : DownloadResult()

    data class Progress(
        val percent: Float,
        val eta: Long,
        val speed: String
    ) : DownloadResult()
}

/**
 * Represents a batch of downloads (e.g. a single track or a playlist of tracks pasted by the user).
 */
data class DownloadBatch(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val items: List<DownloadItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val outputDir: String = ""  // User-selected output directory for this batch
) {
    // Computed properties for progress and overall state
    val totalTracks: Int get() = items.size
    
    val completedTracks: Int get() = items.count { it.state == DownloadState.COMPLETE }
    val failedTracks: Int get() = items.count { it.state == DownloadState.FAILED }
    val cancelledTracks: Int get() = items.count { it.state == DownloadState.CANCELLED }
    
    val progress: Float get() {
        if (items.isEmpty()) return 0f
        val sumProgress = items.sumOf { 
            when (it.state) {
                DownloadState.COMPLETE -> 1.0
                DownloadState.FAILED, DownloadState.CANCELLED -> 0.0
                else -> it.progress.toDouble()
            }
        }
        return (sumProgress / items.size).toFloat()
    }
    
    val state: DownloadState get() {
        if (items.isEmpty()) return DownloadState.QUEUED
        
        // If all items are complete, the batch is complete
        val states = items.map { it.state }
        if (states.all { it == DownloadState.COMPLETE }) return DownloadState.COMPLETE
        if (states.all { it == DownloadState.CANCELLED }) return DownloadState.CANCELLED
        
        // If any item is actively downloading, converting, tagging, moving, or searching
        if (states.any { 
            it == DownloadState.DOWNLOADING || 
            it == DownloadState.SEARCHING || 
            it == DownloadState.CONVERTING || 
            it == DownloadState.MOVING ||
            it == DownloadState.TAGGING 
        }) return DownloadState.DOWNLOADING
        
        // If any item is queued and we haven't finished everything
        if (states.any { it == DownloadState.QUEUED }) return DownloadState.QUEUED
        
        // If everything failed or was cancelled
        if (states.all { it == DownloadState.FAILED || it == DownloadState.CANCELLED }) return DownloadState.FAILED
        
        return DownloadState.FAILED
    }
}
