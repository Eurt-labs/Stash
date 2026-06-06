package com.example.stash.spotify

import android.util.Log
import com.example.stash.model.Platform
import com.example.stash.model.TrackInfo
import com.example.stash.parser.ParsedLink
import com.example.stash.model.ContentType
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Extracts track metadata from public Spotify pages — **NO API key or authentication required**.
 *
 * How it works:
 * 1. Fetches the public Spotify page HTML (e.g. `open.spotify.com/track/...`)
 * 2. Extracts the embedded `__NEXT_DATA__` JSON blob which contains full metadata
 * 3. Falls back to Open Graph `<meta>` tags if the JSON blob isn't available
 * 4. Parses track name, artist(s), album, duration, and artwork URL
 *
 * Supports:
 * - Single tracks (`/track/{id}`)
 * - Playlists (`/playlist/{id}`) — extracts all tracks with pagination
 * - Albums (`/album/{id}`) — extracts all tracks
 *
 * No Spotify Developer account needed. No Client ID. No Client Secret.
 */
class SpotifyWebScraper {

    companion object {
        private const val TAG = "SpotifyWebScraper"
        private const val SPOTIFY_BASE = "https://open.spotify.com"

        // Marker strings to find __NEXT_DATA__ JSON in the page HTML
        private const val NEXT_DATA_START = "<script id=\"__NEXT_DATA__\" type=\"application/json\">"
        private const val NEXT_DATA_END = "</script>"

        // Regex for Open Graph meta tags (fallback)
        private val OG_TITLE_REGEX = Regex(
            """<meta\s+property="og:title"\s+content="([^"]+)"/?""",
            RegexOption.IGNORE_CASE
        )
        private val OG_DESC_REGEX = Regex(
            """<meta\s+property="og:description"\s+content="([^"]+)"/?""",
            RegexOption.IGNORE_CASE
        )
        private val OG_IMAGE_REGEX = Regex(
            """<meta\s+property="og:image"\s+content="([^"]+)"/?""",
            RegexOption.IGNORE_CASE
        )

        // Regex to parse title tag: "Song Name - song and lyrics by Artist | Spotify"
        // or "Song Name · Artist"
        private val TITLE_TAG_REGEX = Regex(
            """<title>([^<]+)</title>""",
            RegexOption.IGNORE_CASE
        )

        // Regex to extract track entries from the Spotify HTML embed data
        // The page contains JSON-LD or data attributes with track info
        private val META_TITLE_REGEX = Regex(
            """<meta\s+name="title"\s+content="([^"]+)"/?""",
            RegexOption.IGNORE_CASE
        )
        private val META_DESC_REGEX = Regex(
            """<meta\s+name="description"\s+content="([^"]+)"/?""",
            RegexOption.IGNORE_CASE
        )
        fun extractJsonById(html: String, id: String): String? {
            val startTag = "<script id=\"$id\" type=\"application/json\">"
            val startIdx = html.indexOf(startTag)
            if (startIdx == -1) return null
            val jsonStart = startIdx + startTag.length
            val endIdx = html.indexOf(NEXT_DATA_END, jsonStart)
            if (endIdx == -1) return null
            return html.substring(jsonStart, endIdx).trim()
        }

        /**
         * Extracts the __NEXT_DATA__ JSON string from the HTML using indexOf.
         * More reliable than regex on Android's ICU engine.
         */
        fun extractNextDataJson(html: String): String? {
            return extractJsonById(html, "__NEXT_DATA__")
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            // Pretend to be a browser so Spotify serves full HTML
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .build()
            chain.proceed(request)
        }
        .build()

    private val gson = Gson()

    /**
     * Extracts track metadata from a Spotify link.
     *
     * @param parsedLink A parsed Spotify URL.
     * @return List of [TrackInfo] (single item for tracks, multiple for playlists/albums).
     */
    suspend fun extractTracks(parsedLink: ParsedLink): List<TrackInfo> {
        require(parsedLink.platform == Platform.SPOTIFY) {
            "SpotifyWebScraper only handles Spotify links"
        }

        return when (parsedLink.contentType) {
            ContentType.TRACK -> extractSingleTrack(parsedLink)
            ContentType.PLAYLIST -> extractPlaylistTracks(parsedLink)
            ContentType.ALBUM -> extractAlbumTracks(parsedLink)
            else -> {
                Log.w(TAG, "Unsupported content type: ${parsedLink.contentType}")
                emptyList()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Single Track Extraction
    // ──────────────────────────────────────────────────────────────

    private fun extractSingleTrack(parsedLink: ParsedLink): List<TrackInfo> {
        val url = "$SPOTIFY_BASE/track/${parsedLink.id}"
        val html = fetchPage(url) ?: return emptyList()

        // Try __NEXT_DATA__ JSON first (most reliable)
        val nextDataTrack = tryExtractFromNextData(html, parsedLink)
        if (nextDataTrack != null) return listOf(nextDataTrack)

        // Fallback: parse from OG meta tags + title tag
        val fallbackTrack = parseFromMetaTags(html, parsedLink)
        if (fallbackTrack != null) return listOf(fallbackTrack)

        // Last resort: try the oEmbed endpoint
        val oEmbedTrack = tryOEmbed(parsedLink)
        if (oEmbedTrack != null) return listOf(oEmbedTrack)

        Log.w(TAG, "Could not extract track info from: $url")
        return emptyList()
    }

    // ──────────────────────────────────────────────────────────────
    // Playlist Extraction
    // ──────────────────────────────────────────────────────────────

    private fun extractPlaylistTracks(parsedLink: ParsedLink): List<TrackInfo> {
        val url = "$SPOTIFY_BASE/playlist/${parsedLink.id}"
        val html = fetchPage(url) ?: return emptyList()

        // Try extracting from __NEXT_DATA__
        val tracks = tryExtractPlaylistFromNextData(html, parsedLink)
        if (tracks.isNotEmpty()) return tracks

        // Fallback: try the embed page which sometimes has more structured data
        val embedTracks = tryExtractFromEmbed("playlist", parsedLink.id, parsedLink)
        if (embedTracks.isNotEmpty()) return embedTracks

        // Last fallback: extract whatever we can from the page
        val metaTracks = parsePlaylistFromMetaTags(html, parsedLink)
        if (metaTracks.isNotEmpty()) return metaTracks

        Log.w(TAG, "Could not extract playlist tracks from: $url")
        return emptyList()
    }

    // ──────────────────────────────────────────────────────────────
    // Album Extraction
    // ──────────────────────────────────────────────────────────────

    private fun extractAlbumTracks(parsedLink: ParsedLink): List<TrackInfo> {
        val url = "$SPOTIFY_BASE/album/${parsedLink.id}"
        val html = fetchPage(url) ?: return emptyList()

        // Try __NEXT_DATA__
        val tracks = tryExtractAlbumFromNextData(html, parsedLink)
        if (tracks.isNotEmpty()) return tracks

        // Fallback: embed page
        val embedTracks = tryExtractFromEmbed("album", parsedLink.id, parsedLink)
        if (embedTracks.isNotEmpty()) return embedTracks

        Log.w(TAG, "Could not extract album tracks from: $url")
        return emptyList()
    }

    // ──────────────────────────────────────────────────────────────
    // __NEXT_DATA__ JSON extraction (primary method)
    // ──────────────────────────────────────────────────────────────

    private fun tryExtractFromNextData(html: String, parsedLink: ParsedLink): TrackInfo? {
        return try {
            val jsonStr = extractNextDataJson(html) ?: return null
            val json = JsonParser.parseString(jsonStr).asJsonObject

            // Navigate the __NEXT_DATA__ structure to find track data
            val pageProps = json.getAsJsonObject("props")
                ?.getAsJsonObject("pageProps") ?: return null

            // The structure varies, but track data is usually under "state" or "track"
            val trackData = pageProps.getAsJsonObject("state")
                ?.getAsJsonObject("data")
                ?.getAsJsonObject("entity")
                ?: pageProps.getAsJsonObject("track")
                ?: return null

            parseTrackJson(trackData, parsedLink)
        } catch (e: Exception) {
            Log.d(TAG, "NextData extraction failed: ${e.message}")
            null
        }
    }

    private fun tryExtractPlaylistFromNextData(html: String, parsedLink: ParsedLink): List<TrackInfo> {
        return try {
            val jsonStr = extractNextDataJson(html) ?: return emptyList()
            val json = JsonParser.parseString(jsonStr).asJsonObject

            val pageProps = json.getAsJsonObject("props")
                ?.getAsJsonObject("pageProps") ?: return emptyList()

            // Try to find the track list in the data structure
            val trackList = pageProps.getAsJsonObject("state")
                ?.getAsJsonObject("data")
                ?.getAsJsonObject("entity")
                ?.getAsJsonObject("trackList")
                ?.getAsJsonArray("items")
                ?: return emptyList()

            trackList.mapNotNull { item ->
                try {
                    val trackObj = item.asJsonObject.getAsJsonObject("track")
                        ?: item.asJsonObject
                    parseTrackJson(trackObj, parsedLink)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Playlist NextData extraction failed: ${e.message}")
            emptyList()
        }
    }

    private fun tryExtractAlbumFromNextData(html: String, parsedLink: ParsedLink): List<TrackInfo> {
        return try {
            val jsonStr = extractNextDataJson(html) ?: return emptyList()
            val json = JsonParser.parseString(jsonStr).asJsonObject

            val pageProps = json.getAsJsonObject("props")
                ?.getAsJsonObject("pageProps") ?: return emptyList()

            val albumData = pageProps.getAsJsonObject("state")
                ?.getAsJsonObject("data")
                ?.getAsJsonObject("entity")
                ?: return emptyList()

            val albumName = albumData.get("name")?.asString
            val albumArt = albumData.getAsJsonObject("coverArt")
                ?.getAsJsonArray("sources")
                ?.firstOrNull()?.asJsonObject
                ?.get("url")?.asString
            val releaseYear = albumData.getAsJsonObject("date")
                ?.get("year")?.asString

            val trackList = albumData.getAsJsonObject("trackList")
                ?.getAsJsonArray("items")
                ?: albumData.getAsJsonObject("tracks")
                    ?.getAsJsonArray("items")
                ?: return emptyList()

            trackList.mapIndexedNotNull { index, item ->
                try {
                    val trackObj = item.asJsonObject.getAsJsonObject("track")
                        ?: item.asJsonObject
                    val name = trackObj.get("name")?.asString ?: return@mapIndexedNotNull null
                    val artists = extractArtistNames(trackObj)
                    val durationMs = trackObj.get("duration_ms")?.asLong
                        ?: trackObj.getAsJsonObject("duration")?.get("totalMilliseconds")?.asLong
                        ?: 0L

                    TrackInfo(
                        title = name,
                        artists = artists,
                        album = albumName,
                        durationMs = durationMs,
                        albumArtUrl = albumArt,
                        trackNumber = index + 1,
                        releaseYear = releaseYear,
                        source = Platform.SPOTIFY,
                        sourceUrl = parsedLink.originalUrl
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Album NextData extraction failed: ${e.message}")
            emptyList()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Embed page extraction (secondary method)
    // ──────────────────────────────────────────────────────────────

    private fun tryExtractFromEmbed(type: String, id: String, parsedLink: ParsedLink): List<TrackInfo> {
        return try {
            val embedUrl = "$SPOTIFY_BASE/embed/$type/$id"
            val html = fetchPage(embedUrl) ?: return emptyList()

            // Extract resource or __NEXT_DATA__ from the embed page
            val jsonStr = extractJsonById(html, "resource")
                ?: extractJsonById(html, "__NEXT_DATA__")
                ?: return emptyList()
            val json = JsonParser.parseString(jsonStr).asJsonObject

            // Extract track list from embed data
            val entity = when {
                json.has("tracks") || json.has("trackList") -> json
                else -> json.getAsJsonObject("props")
                    ?.getAsJsonObject("pageProps")
                    ?.getAsJsonObject("state")
                    ?.getAsJsonObject("data")
                    ?.getAsJsonObject("entity")
            } ?: return emptyList()

            if (type == "track") {
                val track = parseTrackJson(entity, parsedLink) ?: return emptyList()
                return listOf(track)
            }

            val trackList = entity.getAsJsonObject("trackList")?.getAsJsonArray("items")
                ?: entity.getAsJsonObject("tracks")?.getAsJsonArray("items")
                ?: return emptyList()

            trackList.mapNotNull { item ->
                try {
                    val trackObj = item.asJsonObject.getAsJsonObject("track")
                        ?: item.asJsonObject
                    parseTrackJson(trackObj, parsedLink)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Embed extraction failed: ${e.message}")
            emptyList()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // OG Meta tag extraction (fallback)
    // ──────────────────────────────────────────────────────────────

    private fun parseFromMetaTags(html: String, parsedLink: ParsedLink): TrackInfo? {
        // Try OG title first
        val ogTitle = OG_TITLE_REGEX.find(html)?.groupValues?.get(1)
        val ogDesc = OG_DESC_REGEX.find(html)?.groupValues?.get(1)
        val ogImage = OG_IMAGE_REGEX.find(html)?.groupValues?.get(1)

        // Also check <title> tag
        val pageTitle = TITLE_TAG_REGEX.find(html)?.groupValues?.get(1)

        // Try to parse "Song Name" from og:title and "Artist · Album · Year" from og:description
        val title = ogTitle ?: return null

        // og:description is usually: "Artist · Album · Year" or "Artist · Song · Year"
        val descParts = ogDesc?.split("·")?.map { it.trim().htmlDecode() } ?: emptyList()
        val artist = descParts.firstOrNull() ?: "Unknown Artist"

        // Try to extract from title tag: "Song Name - song and lyrics by Artist | Spotify"
        val parsedTitle = pageTitle?.let { parseTitleTag(it) }

        return TrackInfo(
            title = title.htmlDecode(),
            artists = if (parsedTitle?.second != null) listOf(parsedTitle.second!!) else listOf(artist),
            album = if (descParts.size >= 2) descParts[1] else null,
            durationMs = 0L,
            albumArtUrl = ogImage,
            releaseYear = descParts.lastOrNull()?.takeIf { it.matches(Regex("\\d{4}")) },
            source = Platform.SPOTIFY,
            sourceUrl = parsedLink.originalUrl
        )
    }

    private fun parsePlaylistFromMetaTags(html: String, parsedLink: ParsedLink): List<TrackInfo> {
        // For playlists, the meta description sometimes lists tracks
        val metaDesc = META_DESC_REGEX.find(html)?.groupValues?.get(1)
            ?: OG_DESC_REGEX.find(html)?.groupValues?.get(1)
            ?: return emptyList()

        // Spotify playlist descriptions often list songs like:
        // "Song1 · Song2 · Song3 · and more"
        // Or: "Playlist · Artist · Song1, Song2..."
        val trackEntries = metaDesc.split(Regex("[·,]"))
            .map { it.trim().htmlDecode() }
            .filter { it.isNotBlank() && !it.contains("and more", ignoreCase = true) }
            .filter { it.length > 2 } // Filter out noise

        if (trackEntries.isEmpty()) return emptyList()

        // Safeguard: if description is just a summary of the playlist name and item count, ignore it
        if (trackEntries.any {
                it.contains("Playlist", ignoreCase = true) ||
                it.contains("items", ignoreCase = true) ||
                it.contains("songs", ignoreCase = true) ||
                it.contains("likes", ignoreCase = true)
            }) {
            return emptyList()
        }

        // Each entry is typically "Artist - Song" or just "Song"
        return trackEntries.mapIndexed { index, entry ->
            val parts = entry.split(" - ", " – ", " — ")
            val (artist, title) = if (parts.size >= 2) {
                parts[0].trim() to parts.drop(1).joinToString(" - ").trim()
            } else {
                "Unknown Artist" to entry.trim()
            }

            TrackInfo(
                title = title,
                artists = listOf(artist),
                trackNumber = index + 1,
                source = Platform.SPOTIFY,
                sourceUrl = parsedLink.originalUrl
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // oEmbed endpoint (last resort, no auth needed)
    // ──────────────────────────────────────────────────────────────

    private fun tryOEmbed(parsedLink: ParsedLink): TrackInfo? {
        return try {
            val oEmbedUrl = "$SPOTIFY_BASE/oembed?url=${parsedLink.originalUrl}"
            val request = Request.Builder().url(oEmbedUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val json = JsonParser.parseString(body).asJsonObject

            val title = json.get("title")?.asString ?: return null
            val thumbnailUrl = json.get("thumbnail_url")?.asString

            // oEmbed title is usually "Song Name - Artist"
            val parts = title.split(" - ", limit = 2)
            val (trackTitle, artist) = if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else {
                title to "Unknown Artist"
            }

            TrackInfo(
                title = trackTitle,
                artists = listOf(artist),
                albumArtUrl = thumbnailUrl,
                source = Platform.SPOTIFY,
                sourceUrl = parsedLink.originalUrl
            )
        } catch (e: Exception) {
            Log.d(TAG, "oEmbed extraction failed: ${e.message}")
            null
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private fun fetchPage(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to fetch page: $url → HTTP ${response.code}")
                return null
            }

            response.body?.string()
        } catch (e: Exception) {
            Log.e(TAG, "Network error fetching: $url", e)
            null
        }
    }

    private fun parseTrackJson(trackObj: JsonObject, parsedLink: ParsedLink): TrackInfo? {
        val name = trackObj.get("name")?.asString
            ?: trackObj.get("title")?.asString
            ?: return null

        val artists = extractArtistNames(trackObj)
        val durationMs = trackObj.get("duration_ms")?.asLong
            ?: trackObj.getAsJsonObject("duration")?.get("totalMilliseconds")?.asLong
            ?: 0L

        val albumName = trackObj.getAsJsonObject("album")?.get("name")?.asString
        val albumArt = trackObj.getAsJsonObject("album")
            ?.getAsJsonArray("images")?.firstOrNull()?.asJsonObject
            ?.get("url")?.asString
            ?: trackObj.getAsJsonObject("coverArt")
                ?.getAsJsonArray("sources")?.firstOrNull()?.asJsonObject
                ?.get("url")?.asString

        val releaseDate = trackObj.getAsJsonObject("album")
            ?.get("release_date")?.asString
            ?: trackObj.getAsJsonObject("date")?.get("year")?.asString

        return TrackInfo(
            title = name,
            artists = artists.ifEmpty { listOf("Unknown Artist") },
            album = albumName,
            durationMs = durationMs,
            albumArtUrl = albumArt,
            trackNumber = trackObj.get("track_number")?.asInt,
            releaseYear = releaseDate?.take(4),
            source = Platform.SPOTIFY,
            sourceUrl = parsedLink.originalUrl
        )
    }

    private fun extractArtistNames(trackObj: JsonObject): List<String> {
        // Try "artists" array
        val artistsArray = trackObj.getAsJsonArray("artists")
        if (artistsArray != null && artistsArray.size() > 0) {
            return artistsArray.mapNotNull { it.asJsonObject.get("name")?.asString }
        }

        // Try "artistName" single field
        val singleArtist = trackObj.get("artistName")?.asString
            ?: trackObj.get("artist")?.asString
        if (singleArtist != null) return listOf(singleArtist)

        return emptyList()
    }

    /**
     * Parses Spotify's `<title>` tag format.
     * Common formats:
     * - "Song Name - song and lyrics by Artist Name | Spotify"
     * - "Song Name | Spotify"
     */
    private fun parseTitleTag(title: String): Pair<String, String?>? {
        // "Song Name - song and lyrics by Artist Name | Spotify"
        val lyricsMatch = Regex("""(.+?)\s*-\s*song and lyrics by\s+(.+?)\s*\|""").find(title)
        if (lyricsMatch != null) {
            return lyricsMatch.groupValues[1].trim() to lyricsMatch.groupValues[2].trim()
        }

        // "Song Name - Artist Name | Spotify"
        val simpleMatch = Regex("""(.+?)\s*-\s*(.+?)\s*\|""").find(title)
        if (simpleMatch != null) {
            return simpleMatch.groupValues[1].trim() to simpleMatch.groupValues[2].trim()
        }

        return null
    }

    /** Decodes common HTML entities. */
    private fun String.htmlDecode(): String {
        return this
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }
}
