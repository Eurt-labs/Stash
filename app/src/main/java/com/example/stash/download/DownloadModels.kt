package com.example.stash.download

/**
 * User-selectable audio quality presets.
 */
enum class DownloadQuality(val label: String, val bitrateKbps: Int) {
    AUDIO_128("MP3 128kbps (Small)", 128),
    AUDIO_192("MP3 192kbps (Medium)", 192),
    AUDIO_256("MP3 256kbps (Good)", 256),
    AUDIO_320("MP3 320kbps (Best)", 320);

    override fun toString(): String = label
}

/**
 * Download format — audio-only or video with audio.
 */
enum class DownloadFormat(val label: String, val extension: String, val isVideo: Boolean = false) {
    MP3("MP3 Audio", "mp3"),
    M4A("M4A/AAC Audio", "m4a"),
    OGG("OGG Vorbis Audio", "ogg"),
    OPUS("Opus Audio", "opus"),
    VIDEO_360("Video 360p", "mp4", isVideo = true),
    VIDEO_720("Video 720p", "mp4", isVideo = true),
    VIDEO_1080("Video 1080p", "mp4", isVideo = true),
    VIDEO_BEST("Video Best Quality", "mp4", isVideo = true);

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
    val quality: DownloadQuality = DownloadQuality.AUDIO_320,
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
    val filePath: String? = null
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
    val timestamp: Long = System.currentTimeMillis()
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
        
        // If any item is actively downloading, converting, tagging, or searching, then the batch is downloading
        if (states.any { 
            it == DownloadState.DOWNLOADING || 
            it == DownloadState.SEARCHING || 
            it == DownloadState.CONVERTING || 
            it == DownloadState.TAGGING 
        }) return DownloadState.DOWNLOADING
        
        // If any item is queued and we haven't finished everything
        if (states.any { it == DownloadState.QUEUED }) return DownloadState.QUEUED
        
        // If everything failed or was cancelled
        if (states.all { it == DownloadState.FAILED || it == DownloadState.CANCELLED }) return DownloadState.FAILED
        
        return DownloadState.FAILED
    }
}
