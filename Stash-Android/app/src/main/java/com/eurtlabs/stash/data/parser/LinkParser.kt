package com.eurtlabs.stash.data.parser

import com.eurtlabs.stash.data.model.ContentType
import com.eurtlabs.stash.data.model.ParsedLink
import com.eurtlabs.stash.data.model.Platform
import java.security.MessageDigest
import java.util.regex.Pattern

object LinkParser {

    private val YOUTUBE_VIDEO_REGEX = Pattern.compile("https?://(?:www\\.)?youtube\\.com/watch\\?(?:.*?[&])?v=([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE)
    private val YOUTUBE_SHORT_REGEX = Pattern.compile("https?://(?:www\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE)
    private val YOUTU_BE_REGEX = Pattern.compile("https?://youtu\\.be/([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE)
    private val YOUTUBE_PLAYLIST_REGEX = Pattern.compile("https?://(?:www\\.)?youtube\\.com/playlist\\?(?:.*?[&])?list=([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE)
    private val YOUTUBE_MUSIC_REGEX = Pattern.compile("https?://music\\.youtube\\.com/watch\\?(?:.*?[&])?v=([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE)
    private val YOUTUBE_MUSIC_PLAYLIST_REGEX = Pattern.compile("https?://music\\.youtube\\.com/playlist\\?(?:.*?[&])?list=([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE)
    private val YOUTUBE_MUSIC_ALBUM_REGEX = Pattern.compile("https?://music\\.youtube\\.com/(?:album|browse)/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE)
    private val YOUTUBE_CHANNEL_REGEX = Pattern.compile("https?://(?:music\\.|www\\.)?youtube\\.com/(?:@|channel/|c/)([a-zA-Z0-9_.-]+)", Pattern.CASE_INSENSITIVE)
    private val PLAYLIST_PARAM_REGEX = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE)

    fun parse(inputUrl: String?): ParsedLink? {
        val trimmed = inputUrl?.trim() ?: return null
        if (trimmed.isEmpty()) return null

        // 1. YouTube / YouTube Music Playlist (Query param check)
        val playlistMatcher = PLAYLIST_PARAM_REGEX.matcher(trimmed)
        if (playlistMatcher.find()) {
            val isMusic = trimmed.contains("music.youtube.com")
            return ParsedLink(
                platform = if (isMusic) Platform.YOUTUBE_MUSIC else Platform.YOUTUBE,
                contentType = ContentType.PLAYLIST,
                id = playlistMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        // 2. YouTube Music Album / Playlist
        val ytMusicPlaylistMatcher = YOUTUBE_MUSIC_PLAYLIST_REGEX.matcher(trimmed)
        if (ytMusicPlaylistMatcher.find()) {
            return ParsedLink(
                platform = Platform.YOUTUBE_MUSIC,
                contentType = ContentType.PLAYLIST,
                id = ytMusicPlaylistMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        val ytMusicAlbumMatcher = YOUTUBE_MUSIC_ALBUM_REGEX.matcher(trimmed)
        if (ytMusicAlbumMatcher.find()) {
            return ParsedLink(
                platform = Platform.YOUTUBE_MUSIC,
                contentType = ContentType.ALBUM,
                id = ytMusicAlbumMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        // 3. YouTube Music Track
        val ytMusicTrackMatcher = YOUTUBE_MUSIC_REGEX.matcher(trimmed)
        if (ytMusicTrackMatcher.find()) {
            return ParsedLink(
                platform = Platform.YOUTUBE_MUSIC,
                contentType = ContentType.TRACK,
                id = ytMusicTrackMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        // 4. Standard YouTube Playlist
        val ytPlaylistMatcher = YOUTUBE_PLAYLIST_REGEX.matcher(trimmed)
        if (ytPlaylistMatcher.find()) {
            return ParsedLink(
                platform = Platform.YOUTUBE,
                contentType = ContentType.PLAYLIST,
                id = ytPlaylistMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        // 5. YouTube Shorts / Watch / youtu.be
        val ytShortMatcher = YOUTUBE_SHORT_REGEX.matcher(trimmed)
        if (ytShortMatcher.find()) {
            return ParsedLink(
                platform = Platform.YOUTUBE,
                contentType = ContentType.VIDEO,
                id = ytShortMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        val youtuBeMatcher = YOUTU_BE_REGEX.matcher(trimmed)
        if (youtuBeMatcher.find()) {
            return ParsedLink(
                platform = Platform.YOUTUBE,
                contentType = ContentType.VIDEO,
                id = youtuBeMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        val ytVideoMatcher = YOUTUBE_VIDEO_REGEX.matcher(trimmed)
        if (ytVideoMatcher.find()) {
            return ParsedLink(
                platform = Platform.YOUTUBE,
                contentType = ContentType.VIDEO,
                id = ytVideoMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        // 6. YouTube Channel / Artist
        val ytChannelMatcher = YOUTUBE_CHANNEL_REGEX.matcher(trimmed)
        if (ytChannelMatcher.find()) {
            val isMusic = trimmed.contains("music.youtube.com")
            return ParsedLink(
                platform = if (isMusic) Platform.YOUTUBE_MUSIC else Platform.YOUTUBE,
                contentType = ContentType.PLAYLIST,
                id = ytChannelMatcher.group(1) ?: "",
                originalUrl = trimmed
            )
        }

        // 7. Generic URL fallback (only valid http/https URLs)
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            // Validate it looks like an actual URL (has a domain)
            val domainMatch = Regex("^https?://[a-zA-Z0-9]").containsMatchIn(trimmed)
            if (domainMatch) {
                val hashId = md5(trimmed).take(12)
                return ParsedLink(
                    platform = Platform.OTHER,
                    contentType = ContentType.VIDEO,
                    id = hashId,
                    originalUrl = trimmed
                )
            }
        }

        // 8. Reject file paths, random non-URL text
        // Only allow input that doesn't contain backslashes or look like a local path
        if (trimmed.contains("\\") || trimmed.contains(":/") || trimmed.startsWith("/") || trimmed.startsWith("C:")) {
            return null
        }

        // 9. Plain text query — search fallback (only for clean query strings)
        if (trimmed.length in 2..120 && !trimmed.contains("://")) {
            return ParsedLink(
                platform = Platform.YOUTUBE,
                contentType = ContentType.TRACK,
                id = trimmed,
                originalUrl = "ytsearch1:$trimmed"
            )
        }

        return null
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
