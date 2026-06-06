package com.example.stash.parser

import com.example.stash.model.ContentType
import com.example.stash.model.Platform

/**
 * Result of parsing a Spotify or YouTube URL.
 *
 * @property platform The detected platform (Spotify, YouTube, YouTube Music).
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
 * Parses Spotify and YouTube URLs into structured [ParsedLink] objects.
 *
 * Supports the following URL formats:
 *
 * **Spotify:**
 * - `https://open.spotify.com/track/{id}`
 * - `https://open.spotify.com/playlist/{id}`
 * - `https://open.spotify.com/album/{id}`
 * - `https://open.spotify.com/intl-xx/track/{id}` (intl variants)
 *
 * **YouTube:**
 * - `https://www.youtube.com/watch?v={id}`
 * - `https://youtu.be/{id}`
 * - `https://www.youtube.com/playlist?list={id}`
 * - `https://music.youtube.com/watch?v={id}`
 *
 * Query parameters (like `?si=...`) and fragments are stripped cleanly.
 */
object LinkParser {

    // ──────────────────────────────────────────────
    // Spotify patterns
    // ──────────────────────────────────────────────

    private val SPOTIFY_TRACK_REGEX = Regex(
        """https?://open\.spotify\.com/(?:intl-[a-z]{2}/)?track/([a-zA-Z0-9]+)"""
    )
    private val SPOTIFY_PLAYLIST_REGEX = Regex(
        """https?://open\.spotify\.com/(?:intl-[a-z]{2}/)?playlist/([a-zA-Z0-9]+)"""
    )
    private val SPOTIFY_ALBUM_REGEX = Regex(
        """https?://open\.spotify\.com/(?:intl-[a-z]{2}/)?album/([a-zA-Z0-9]+)"""
    )

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

    /** Instagram post/reel/tv URL: instagram.com/p/ID, instagram.com/reel/ID, instagram.com/tv/ID */
    private val INSTAGRAM_REGEX = Regex(
        """https?://(?:www\.)?instagram\.com/(?:p|reel|tv)/([a-zA-Z0-9_-]+)"""
    )

    /**
     * Parses a URL string and returns a [ParsedLink] if recognized, or null otherwise.
     */
    fun parse(url: String): ParsedLink? {
        val trimmedUrl = url.trim()

        // ── Spotify ──
        SPOTIFY_TRACK_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.SPOTIFY, ContentType.TRACK, match.groupValues[1], trimmedUrl)
        }
        SPOTIFY_PLAYLIST_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.SPOTIFY, ContentType.PLAYLIST, match.groupValues[1], trimmedUrl)
        }
        SPOTIFY_ALBUM_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.SPOTIFY, ContentType.ALBUM, match.groupValues[1], trimmedUrl)
        }

        // ── YouTube Music (check before regular YouTube to avoid false matches) ──
        YOUTUBE_MUSIC_PLAYLIST_REGEX.find(trimmedUrl)?.let { match ->
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

        // ── Instagram ──
        INSTAGRAM_REGEX.find(trimmedUrl)?.let { match ->
            return ParsedLink(Platform.INSTAGRAM, ContentType.VIDEO, match.groupValues[1], trimmedUrl)
        }

        return null
    }

    /**
     * Returns true if the given URL is a recognized Spotify or YouTube link.
     */
    fun isSupported(url: String): Boolean = parse(url) != null
}
