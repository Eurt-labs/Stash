package com.example.stash.model

/**
 * Platform source of the content.
 */
enum class Platform {
    YOUTUBE,
    YOUTUBE_MUSIC,
    INSTAGRAM,
    OTHER
}

/**
 * Type of content identified from a link.
 */
enum class ContentType {
    TRACK,
    PLAYLIST,
    ALBUM,
    VIDEO
}

/**
 * Unified track metadata model used across all components of the download pipeline.
 * Populated from YouTube metadata extraction or other supported platforms.
 */
data class TrackInfo(
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val durationMs: Long = 0L,
    val albumArtUrl: String? = null,
    val trackNumber: Int? = null,
    val releaseYear: String? = null,
    val genre: String? = null,
    val source: Platform,
    val sourceUrl: String,
    val youtubeUrl: String? = null
) {
    /**
     * Returns a display-friendly string: "Artist1, Artist2 - Title"
     */
    val displayName: String
        get() = "${artists.joinToString(", ")} - $title"

    /**
     * Returns a filesystem-safe filename (without extension).
     */
    val safeFileName: String
        get() = displayName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(200) // Prevent exceeding filesystem limits
}
