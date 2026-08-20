package com.eurtlabs.stash.data.model

import java.util.UUID

enum class NavigationTab(val label: String) {
    QUEUE("Queue"),
    SEARCH("Search"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

enum class Platform {
    YOUTUBE,
    YOUTUBE_MUSIC,
    OTHER
}

enum class ContentType {
    TRACK,
    PLAYLIST,
    ALBUM,
    VIDEO
}

enum class DownloadQuality(val label: String) {
    QUALITY_4K("4K Ultra HD (2160p)"),
    QUALITY_2K("2K QHD (1440p)"),
    HIGH("High (320kbps / 1080p)"),
    MID("Medium (192kbps / 720p)"),
    LOW("Standard (128kbps / 480p)")
}

enum class DownloadFormat(val ext: String, val isAudioOnly: Boolean) {
    AUTO("mp3", true),
    MP3("mp3", true),
    AAC("m4a", true),
    FLAC("flac", true),
    OPUS("opus", true),
    WAV("wav", true),
    MP4("mp4", false)
}

enum class DownloadState {
    IDLE,
    QUEUED,
    FETCHING,
    DOWNLOADING,
    CONVERTING,
    TAGGING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class ColorTheme(val displayName: String, val subtitle: String) {
    OBSIDIAN("Obsidian OLED", "Pure Pitch Black & Sharp White"),
    TITANIUM("Titanium Slate", "Refined Monochrome Silver"),
    GRAPHITE("Graphite Carbon", "Industrial Matte Charcoal"),
    NORD("Nord Frost", "Subtle Arctic Slate"),
    SAGE("Sage Minimal", "Muted Botanical Stone"),
    ESPRESSO("Warm Espresso", "Architectural Mocha & Champagne"),
    MIDNIGHT("Midnight Navy", "Deep Studio Obsidian")
}

data class TrackInfo(
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val durationMs: Long = 0L,
    val albumArtUrl: String? = null,
    val source: Platform = Platform.YOUTUBE,
    val sourceUrl: String,
    val youtubeUrl: String? = null,
    val releaseYear: String? = null,
    val trackNumber: Int? = null,
    val genre: String? = null,
    val safeFileName: String
)

data class ParsedLink(
    val platform: Platform,
    val contentType: ContentType,
    val id: String,
    val originalUrl: String
)

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val batchId: String,
    val trackInfo: TrackInfo,
    val quality: DownloadQuality = DownloadQuality.HIGH,
    val format: DownloadFormat = DownloadFormat.MP3,
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Float = 0f,
    val speed: String = "",
    val eta: String = "",
    val statusMessage: String = "In queue",
    val errorMessage: String? = null,
    val finalFilePath: String? = null
)

data class DownloadBatch(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val items: List<DownloadItem>,
    val outputDir: String,
    val quality: DownloadQuality = DownloadQuality.HIGH,
    val format: DownloadFormat = DownloadFormat.MP3,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

data class StashSettings(
    val outputDir: String = "",
    val quality: DownloadQuality = DownloadQuality.HIGH,
    val format: DownloadFormat = DownloadFormat.MP3,
    val theme: ColorTheme = ColorTheme.OBSIDIAN
)
