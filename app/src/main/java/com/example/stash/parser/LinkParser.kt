package com.example.stash.parser

import com.example.stash.model.ContentType
import com.example.stash.model.Platform

/**
 * Result of parsing a YouTube, YouTube Music, Instagram, or generic URL.
 *
 * @property platform The detected platform (YouTube, YouTube Music, Instagram, or Other).
 * @property contentType The type of content (track, playlist, album, video).
 * @property id The extracted content ID from the URL.
 * @property originalUrl The original URL that was parsed.
 */
data class ParsedLink(
    val platform: Platform,
    val contentType: ContentType,
    val id: String,
    val originalUrl: String
)

/**
 * Parses YouTube, Instagram, and generic URLs into structured [ParsedLink] objects.
 *
 * Supports the following URL formats:
 *
 * **YouTube:**
 * - `https://www.youtube.com/watch?v={id}`
 * - `https://youtu.be/{id}`
 * - `https://www.youtube.com/playlist?list={id}`
 * - `https://music.youtube.com/watch?v={id}`
 *
 * **Instagram:**
 * - `https://www.instagram.com/p/{id}`
 * - `https://www.instagram.com/reel/{id}`
 *
 * Query parameters (like `?si=...`) and fragments are stripped cleanly.
 */
object LinkParser {


    // ──────────────────────────────────────────────
    // YouTube patterns
    // ──────────────────────────────────────────────

    /** Standard YouTube watch URL: youtube.com/watch?v=VIDEO_ID */
    private val YOUTUBE_VIDEO_REGEX = Regex(
        """https?://(?:www\.)?youtube\.com/watch\?.*?v=([a-zA-Z0-9_-]{11})"""
    )

    /** Short YouTube URL: youtu.be/VIDEO_ID */
    private val YOUTUBE_SHORT_REGEX = Regex(
        """https?://youtu\.be/([a-zA-Z0-9_-]{11})"""
    )

    /** YouTube playlist URL: youtube.com/playlist?list=PLAYLIST_ID */
    private val YOUTUBE_PLAYLIST_REGEX = Regex(
        """https?://(?:www\.)?youtube\.com/playlist\?.*?list=([a-zA-Z0-9_-]+)"""
    )

    /** YouTube Music watch URL: music.youtube.com/watch?v=VIDEO_ID */
    private val YOUTUBE_MUSIC_REGEX = Regex(
        """https?://music\.youtube\.com/watch\?.*?v=([a-zA-Z0-9_-]{11})"""
    )

    /** YouTube Music playlist: music.youtube.com/playlist?list=PLAYLIST_ID */
    private val YOUTUBE_MUSIC_PLAYLIST_REGEX = Regex(
        """https?://music\.youtube\.com/playlist\?.*?list=([a-zA-Z0-9_-]+)"""
    )

    /** YouTube Music album: music.youtube.com/album/ALBUM_ID */
    private val YOUTUBE_MUSIC_ALBUM_REGEX = Regex(
        """https?://music\.youtube\.com/album/([a-zA-Z0-9_-]+)"""
    )

    /** YouTube/YouTube Music channel/artist URL */
    private val YOUTUBE_CHANNEL_REGEX = Regex(
        """https?://(?:music\.|www\.)?youtube\.com/(?:@|channel/|c/)([a-zA-Z0-9_.-]+)"""
    )

    /** Instagram post/reel/tv URL: instagram.com/p/ID, instagram.com/reel/ID, instagram.com/tv/ID */
    private val INSTAGRAM_REGEX = Regex(
        """https?://(?:www\.)?instagram\.com/(?:p|reel|tv)/([a-zA-Z0-9_-]+)"""
    )

    /**
     * Parses a URL string and returns a [ParsedLink] if recognized, or null otherwise.
     */
    fun parse(url: String): ParsedLink? {
        val trimmedUrl = url.trim()


        // ── YouTube Music (check before regular YouTube to avoid false matches) ──
        YOUTUBE_MUSIC_PLAYLIST_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.YOUTUBE_MUSIC, ContentType.PLAYLIST, match.groupValues[1], trimmedUrl)
        }
        YOUTUBE_MUSIC_ALBUM_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.YOUTUBE_MUSIC, ContentType.PLAYLIST, match.groupValues[1], trimmedUrl)
        }
        YOUTUBE_MUSIC_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.YOUTUBE_MUSIC, ContentType.TRACK, match.groupValues[1], trimmedUrl)
        }

        // ── YouTube ──
        YOUTUBE_PLAYLIST_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.YOUTUBE, ContentType.PLAYLIST, match.groupValues[1], trimmedUrl)
        }
        YOUTUBE_VIDEO_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.YOUTUBE, ContentType.VIDEO, match.groupValues[1], trimmedUrl)
        }
        YOUTUBE_SHORT_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.YOUTUBE, ContentType.VIDEO, match.groupValues[1], trimmedUrl)
        }

        // ── YouTube Channel ──
        YOUTUBE_CHANNEL_REGEX.find(trimmedUrl)?.let { match ->
            val platform = if (trimmedUrl.contains("music.youtube.com")) {
                Platform.YOUTUBE_MUSIC
            } else {
                Platform.YOUTUBE
            }
            return ParsedLink(platform, ContentType.PLAYLIST, match.groupValues[1], trimmedUrl)
        }

        // ── Instagram ──
        INSTAGRAM_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.INSTAGRAM, ContentType.VIDEO, match.groupValues[1], trimmedUrl)
        }

        // ── Generic Fallback (any other http/https link) ──
        if (trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true)) {
            val simpleId = java.util.UUID.nameUUIDFromBytes(trimmedUrl.toByteArray()).toString()
            return ParsedLink(Platform.OTHER, ContentType.VIDEO, simpleId, trimmedUrl)
        }

        // ── Artist / Query Search Fallback ──
        if (trimmedUrl.isNotBlank() && !trimmedUrl.contains("://")) {
            return ParsedLink(Platform.YOUTUBE, ContentType.PLAYLIST, trimmedUrl, "ytsearch150:$trimmedUrl music.youtube.com")
        }

        return null
    }

    /**
     * Returns true if the given URL is a recognized link.
     */
    fun isSupported(url: String): Boolean = parse(url) != null
}
