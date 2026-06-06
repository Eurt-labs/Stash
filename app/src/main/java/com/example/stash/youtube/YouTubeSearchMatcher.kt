package com.example.stash.youtube

import android.content.Context
import android.util.Log
import com.example.stash.model.TrackInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Matches Spotify tracks to YouTube videos using yt-dlp's built-in search.
 *
 * Algorithm:
 * 1. Construct search query: "Artist - Track Title"
 * 2. Use yt-dlp's `ytsearch5:` prefix to search YouTube for top 5 results
 * 3. Score each result by:
 *    - Duration match (within ±15 seconds = high score)
 *    - Title similarity
 *    - Channel name containing artist name
 * 4. Return the best-matching YouTube URL
 *
 * This avoids needing a YouTube Data API key.
 */
class YouTubeSearchMatcher(private val context: Context) {

    companion object {
        private const val TAG = "YTSearchMatcher"

        /** Maximum allowed duration difference in milliseconds (15 seconds). */
        private const val DURATION_TOLERANCE_MS = 15_000L

        /** Number of YouTube results to evaluate per search. */
        private const val SEARCH_RESULT_COUNT = 5
    }

    private val gson = Gson()

    /**
     * Searches YouTube for the best match for the given [TrackInfo].
     *
     * @param trackInfo Track metadata (title, artists, duration) to match against.
     * @return The YouTube video URL of the best match, or null if no suitable match is found.
     */
    suspend fun findBestMatch(trackInfo: TrackInfo): String? = withContext(Dispatchers.IO) {
        try {
            val searchQuery = buildSearchQuery(trackInfo)
            Log.d(TAG, "Searching YouTube for: $searchQuery")

            val candidates = searchYouTube(searchQuery)
            if (candidates.isEmpty()) {
                Log.w(TAG, "No YouTube results found for: $searchQuery")
                return@withContext null
            }

            val bestMatch = scoreCandidates(candidates, trackInfo)
            if (bestMatch != null) {
                Log.d(TAG, "Best match: ${bestMatch.title} (${bestMatch.url}) — score: ${bestMatch.score}")
                return@withContext bestMatch.url
            }

            // Fallback: if scoring fails, use the first result
            Log.w(TAG, "No scored match found, using first result: ${candidates.first().url}")
            return@withContext candidates.first().url

        } catch (e: Exception) {
            Log.e(TAG, "YouTube search failed for: ${trackInfo.displayName}", e)
            return@withContext null
        }
    }

    /**
     * Constructs the search query from track metadata.
     * Format: "Artist1, Artist2 - Track Title"
     */
    private fun buildSearchQuery(trackInfo: TrackInfo): String {
        val artists = trackInfo.artists.joinToString(", ")
        return "$artists - ${trackInfo.title}"
    }

    /**
     * Uses yt-dlp's ytsearch to find YouTube videos matching the query.
     */
    private fun searchYouTube(query: String): List<YouTubeCandidate> {
        val request = YoutubeDLRequest("ytsearch$SEARCH_RESULT_COUNT:$query")
        request.addOption("--dump-json")
        request.addOption("--flat-playlist")
        request.addOption("--no-download")
        request.addOption("--no-warnings")

        val response = YoutubeDL.getInstance().execute(request)
        val output = response.out ?: return emptyList()

        // yt-dlp outputs one JSON object per line for each result
        return output.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    val json = gson.fromJson(line, JsonObject::class.java)
                    YouTubeCandidate(
                        url = json.get("url")?.asString
                            ?: json.get("webpage_url")?.asString
                            ?: "https://www.youtube.com/watch?v=${json.get("id")?.asString}",
                        title = json.get("title")?.asString ?: "",
                        durationMs = (json.get("duration")?.asDouble?.times(1000))?.toLong() ?: 0L,
                        channel = json.get("channel")?.asString
                            ?: json.get("uploader")?.asString ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse search result line", e)
                    null
                }
            }
    }

    /**
     * Scores candidates and returns the best match, or null if none are acceptable.
     */
    private fun scoreCandidates(
        candidates: List<YouTubeCandidate>,
        trackInfo: TrackInfo
    ): ScoredCandidate? {
        return candidates.map { candidate ->
            var score = 0.0

            // ── Duration matching (most important) ──
            if (trackInfo.durationMs > 0 && candidate.durationMs > 0) {
                val durationDiff = abs(trackInfo.durationMs - candidate.durationMs)
                score += when {
                    durationDiff <= 3_000 -> 50.0   // Within 3s: perfect
                    durationDiff <= 10_000 -> 30.0  // Within 10s: good
                    durationDiff <= DURATION_TOLERANCE_MS -> 15.0 // Within 15s: acceptable
                    else -> -20.0                   // Too different: penalty
                }
            }

            // ── Title similarity ──
            val normalizedTrackTitle = trackInfo.title.lowercase().trim()
            val normalizedCandidateTitle = candidate.title.lowercase().trim()

            if (normalizedCandidateTitle.contains(normalizedTrackTitle)) {
                score += 25.0 // Title found in result
            } else {
                // Partial word match
                val trackWords = normalizedTrackTitle.split(Regex("\\s+")).filter { it.length > 2 }
                val matchingWords = trackWords.count { normalizedCandidateTitle.contains(it) }
                score += (matchingWords.toDouble() / trackWords.size.coerceAtLeast(1)) * 15.0
            }

            // ── Artist name in title or channel ──
            val primaryArtist = trackInfo.artists.firstOrNull()?.lowercase()?.trim() ?: ""
            if (primaryArtist.isNotEmpty()) {
                if (normalizedCandidateTitle.contains(primaryArtist)) {
                    score += 15.0
                }
                if (candidate.channel.lowercase().contains(primaryArtist)) {
                    score += 10.0
                }
            }

            // ── Penalize very long videos (likely compilations) ──
            if (trackInfo.durationMs > 0 && candidate.durationMs > trackInfo.durationMs * 2) {
                score -= 30.0
            }

            // ── Bonus: official-sounding channels ──
            val channelLower = candidate.channel.lowercase()
            if (channelLower.contains("official") || channelLower.contains("vevo") ||
                channelLower.contains("topic")) {
                score += 5.0
            }

            ScoredCandidate(candidate.url, candidate.title, score)
        }
            .filter { it.score > 0 } // Only accept positive scores
            .maxByOrNull { it.score }
    }

    /**
     * Internal candidate from YouTube search results.
     */
    private data class YouTubeCandidate(
        val url: String,
        val title: String,
        val durationMs: Long,
        val channel: String
    )

    /**
     * A candidate with a computed relevance score.
     */
    private data class ScoredCandidate(
        val url: String,
        val title: String,
        val score: Double
    )
}
