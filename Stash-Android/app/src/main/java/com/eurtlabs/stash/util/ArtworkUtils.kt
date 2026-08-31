package com.eurtlabs.stash.util

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ArtworkUtils {

    private const val TAG = "ArtworkUtils"
    private val artworkCache = LruCache<String, String>(250)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * Synchronously resolves high-res URL based on known patterns.
     */
    fun getHighResArtworkUrl(url: String?, videoId: String? = null): String {
        if (!videoId.isNullOrBlank()) {
            val cached = artworkCache.get(videoId)
            if (!cached.isNullOrBlank()) return cached
        }

        if (url.isNullOrBlank()) {
            return if (!videoId.isNullOrBlank()) "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg" else ""
        }

        // Google User Content (YouTube Music CDNs: lh3.googleusercontent.com, etc.)
        if (url.contains("googleusercontent.com") || url.contains("ggpht.com")) {
            val upgraded = url.replace(Regex("""=w\d+-h\d+.*"""), "=w1400-h1400-l95-rj")
            if (!videoId.isNullOrBlank()) artworkCache.put(videoId, upgraded)
            return upgraded
        }

        // YouTube video thumbnails
        if (url.contains("i.ytimg.com/vi/") || url.contains("ytimg.com/vi/")) {
            return url.replace("hqdefault.jpg", "maxresdefault.jpg")
                .replace("mqdefault.jpg", "maxresdefault.jpg")
                .replace("sddefault.jpg", "maxresdefault.jpg")
                .replace("default.jpg", "maxresdefault.jpg")
        }

        return url
    }

    /**
     * Asynchronously fetches Retina Studio Album Cover (1400x1400) from iTunes/Apple Music.
     */
    suspend fun fetchStudioArtwork(
        title: String,
        artist: String,
        videoId: String? = null,
        fallbackUrl: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (!videoId.isNullOrBlank()) {
            val cached = artworkCache.get(videoId)
            if (!cached.isNullOrBlank()) return@withContext cached
        }

        // 1. Clean Title and Artist for precise query matching
        val cleanTitle = title.replace(Regex("""(?i)\(official.*?\)|\[official.*?\]|ft\..*|feat\..*|video|lyrics|audio|remix"""), "").trim()
        val cleanArtist = artist.replace(Regex("""(?i) - Topic|VEVO"""), "").trim()

        if (cleanTitle.isNotBlank()) {
            try {
                val query = URLEncoder.encode("$cleanTitle $cleanArtist", "UTF-8")
                val request = Request.Builder()
                    .url("https://itunes.apple.com/search?term=$query&entity=song&limit=1")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val first = results.getJSONObject(0)
                        val rawArt = first.optString("artworkUrl100", "")
                        if (rawArt.isNotBlank()) {
                            // Upgrade iTunes 100x100 to 1400x1400 Retina Studio resolution
                            val studioArt = rawArt.replace("100x100bb.jpg", "1400x1400bb.jpg")
                                .replace("100x100bb.png", "1400x1400bb.png")
                            if (!videoId.isNullOrBlank()) {
                                artworkCache.put(videoId, studioArt)
                            }
                            Log.d(TAG, "Fetched iTunes Studio Artwork: $studioArt for '$title'")
                            return@withContext studioArt
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "iTunes artwork lookup error: ${e.message}")
            }
        }

        val highRes = getHighResArtworkUrl(fallbackUrl, videoId)
        if (!videoId.isNullOrBlank() && highRes.isNotBlank()) {
            artworkCache.put(videoId, highRes)
        }
        highRes
    }
}
