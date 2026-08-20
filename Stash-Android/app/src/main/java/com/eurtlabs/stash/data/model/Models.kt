package com.eurtlabs.stash.data.model

import java.util.UUID

enum class NavigationTab(val label: String) {
    QUEUE("Queue"),
    SEARCH("Search"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

enum class MediaType(val label: String) {
    AUDIO("Music & Audio"),
    VIDEO("Video")
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

enum class DownloadQuality(val label: String, val valueOption: String, val isAudioOnly: Boolean) {
    // Audio Bitrates
    AUDIO_320K("320 kbps (Lossless / Ultra)", "320k", true),
    AUDIO_256K("256 kbps (High Quality)", "256k", true),
    AUDIO_192K("192 kbps (Medium Quality)", "192k", true),
    AUDIO_128K("128 kbps (Standard Quality)", "128k", true),

    // Video Resolutions
    VIDEO_4K("4K Ultra HD (2160p)", "bv*[height<=2160]+ba/b[height<=2160]/bv*+ba/b", false),
    VIDEO_2K("2K QHD (1440p)", "bv*[height<=1440]+ba/b[height<=1440]/bv*+ba/b", false),
    VIDEO_1080P("Full HD (1080p)", "bv*[height<=1080]+ba/b[height<=1080]/bv*+ba/b", false),
    VIDEO_720P("HD (720p)", "bv*[height<=720]+ba/b[height<=720]/bv*+ba/b", false),
    VIDEO_480P("SD (480p)", "bv*[height<=480]+ba/b[height<=480]/bv*+ba/b", false)
}

enum class DownloadFormat(val ext: String, val isAudioOnly: Boolean, val label: String) {
    // Audio formats
    MP3("mp3", true, "MP3"),
    AAC("m4a", true, "AAC"),
    FLAC("flac", true, "FLAC"),
    OPUS("opus", true, "OPUS"),
    WAV("wav", true, "WAV"),

    // Video formats
    MP4("mp4", false, "MP4"),
    MKV("mkv", false, "MKV"),
    WEBM("webm", false, "WEBM")
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
    val quality: DownloadQuality = DownloadQuality.AUDIO_320K,
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
    val quality: DownloadQuality = DownloadQuality.AUDIO_320K,
    val format: DownloadFormat = DownloadFormat.MP3,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

enum class SearchFilter(val label: String) {
    ALL("All"),
    MUSIC("Songs & Music"),
    ARTISTS("Artists"),
    VIDEOS("Videos")
}

data class SearchResultItem(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String = "",
    val thumbnailUrl: String? = null,
    val url: String,
    val isAudio: Boolean = true
)

data class StashSettings(
    val outputDir: String = "",
    val customDirUri: String? = null,
    val isFirstLaunchDone: Boolean = false,
    val mediaType: MediaType = MediaType.AUDIO,
    val audioFormat: DownloadFormat = DownloadFormat.MP3,
    val audioQuality: DownloadQuality = DownloadQuality.AUDIO_320K,
    val videoFormat: DownloadFormat = DownloadFormat.MP4,
    val videoQuality: DownloadQuality = DownloadQuality.VIDEO_1080P,
    val theme: ColorTheme = ColorTheme.OBSIDIAN
) {
    val format: DownloadFormat
        get() = if (mediaType == MediaType.AUDIO) audioFormat else videoFormat

    val quality: DownloadQuality
        get() = if (mediaType == MediaType.AUDIO) audioQuality else videoQuality
}
