package com.example.stash.youtube

import com.example.stash.model.TrackInfo
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import kotlin.math.abs

/**
 * Matches Spotify tracks to YouTube videos using yt-dlp's built-in search.
 */
class YouTubeSearchMatcher {

    companion object {
        private const val DURATION_TOLERANCE_MS = 15_000L
        private const val SEARCH_RESULT_COUNT = 5
    }

    private val gson = Gson()

    suspend fun findBestMatch(trackInfo: TrackInfo): String? = withContext(Dispatchers.IO) {
        try {
            val searchQuery = buildSearchQuery(trackInfo)
            println("Searching YouTube for: $searchQuery")

            val candidates = searchYouTube(searchQuery)
            if (candidates.isEmpty()) {
                println("No YouTube results found for: $searchQuery")
                return@withContext null
            }

            val bestMatch = scoreCandidates(candidates, trackInfo)
            if (bestMatch != null) {
                println("Best match: ${bestMatch.title} (${bestMatch.url}) — score: ${bestMatch.score}")
                return@withContext bestMatch.url
            }

            // Fallback: if scoring fails, use the first result
            println("No scored match found, using first result: ${candidates.first().url}")
            return@withContext candidates.first().url

        } catch (e: Exception) {
            System.err.println("YouTube search failed for: ${trackInfo.displayName}")
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun buildSearchQuery(trackInfo: TrackInfo): String {
        val primaryArtist = trackInfo.artists.firstOrNull() ?: ""
        return if (primaryArtist.isNotEmpty()) {
            "$primaryArtist ${trackInfo.title}"
        } else {
            trackInfo.title
        }
    }

    private fun searchYouTube(query: String): List<YouTubeCandidate> {
        // Try searching YouTube Music first
        var candidates = executeSearch("ytmsearch$SEARCH_RESULT_COUNT:$query")
        if (candidates.isEmpty()) {
            println("No YouTube Music results found, falling back to standard YouTube search")
            candidates = executeSearch("ytsearch$SEARCH_RESULT_COUNT:$query")
        }
        return candidates
    }

    private fun executeSearch(searchQuery: String): List<YouTubeCandidate> {
        val cmd = listOf(
            "yt-dlp",
            "--dump-json",
            "--flat-playlist",
            "--no-download",
            "--no-warnings",
            searchQuery
        )

        return try {
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val lines = InputStreamReader(process.inputStream).readLines()
            process.waitFor()

            lines.filter { it.isNotBlank() && it.startsWith("{") }
                .mapNotNull { line ->
                    try {
                        val json = gson.fromJson(line, JsonObject::class.java)
                        val rawId = json.get("id")?.asString ?: ""
                        val resolvedUrl = if (rawId.isNotEmpty()) {
                            "https://www.youtube.com/watch?v=$rawId"
                        } else {
                            val rawUrl = json.get("url")?.asString
                                ?: json.get("webpage_url")?.asString
                                ?: ""
                            if (rawUrl.startsWith("http")) rawUrl else "https://www.youtube.com/watch?v=$rawUrl"
                        }
                        YouTubeCandidate(
                            url = resolvedUrl,
                            title = json.get("title")?.asString ?: "",
                            durationMs = (json.get("duration")?.asDouble?.times(1000))?.toLong() ?: 0L,
                            channel = json.get("channel")?.asString
                                ?: json.get("uploader")?.asString ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            System.err.println("Search failed for: $searchQuery")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun scoreCandidates(
        candidates: List<YouTubeCandidate>,
        trackInfo: TrackInfo
    ): ScoredCandidate? {
        return candidates.map { candidate ->
            var score = 0.0

            if (trackInfo.durationMs > 0 && candidate.durationMs > 0) {
                val durationDiff = abs(trackInfo.durationMs - candidate.durationMs)
                score += when {
                    durationDiff <= 3_000 -> 50.0
                    durationDiff <= 10_000 -> 30.0
                    durationDiff <= DURATION_TOLERANCE_MS -> 15.0
                    else -> -20.0
                }
            }

            val normalizedTrackTitle = trackInfo.title.lowercase().trim()
            val normalizedCandidateTitle = candidate.title.lowercase().trim()

            if (normalizedCandidateTitle.contains(normalizedTrackTitle)) {
                score += 25.0
            } else {
                val trackWords = normalizedTrackTitle.split(Regex("\\s+")).filter { it.length > 2 }
                val matchingWords = trackWords.count { normalizedCandidateTitle.contains(it) }
                score += (matchingWords.toDouble() / trackWords.size.coerceAtLeast(1)) * 15.0
            }

            val primaryArtist = trackInfo.artists.firstOrNull()?.lowercase()?.trim() ?: ""
            if (primaryArtist.isNotEmpty()) {
                if (normalizedCandidateTitle.contains(primaryArtist)) {
                    score += 15.0
                }
                if (candidate.channel.lowercase().contains(primaryArtist)) {
                    score += 10.0
                }
            }

            if (trackInfo.durationMs > 0 && candidate.durationMs > trackInfo.durationMs * 2) {
                score -= 30.0
            }

            val channelLower = candidate.channel.lowercase()
            if (channelLower.contains("official") || channelLower.contains("vevo") ||
                channelLower.contains("topic")) {
                score += 5.0
            }

            ScoredCandidate(candidate.url, candidate.title, score)
        }
            .filter { it.score > 0 }
            .maxByOrNull { it.score }
    }

    private data class YouTubeCandidate(
        val url: String,
        val title: String,
        val durationMs: Long,
        val channel: String
    )

    private data class ScoredCandidate(
        val url: String,
        val title: String,
        val score: Double
    )
}
