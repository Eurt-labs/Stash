package com.eurtlabs.stash.data.downloader

import android.webkit.CookieManager
import com.eurtlabs.stash.data.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object YoutubeLibraryFetcher {

    suspend fun fetchLibrary(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val urlString = when (query) {
            ":ythistory" -> "https://m.youtube.com/feed/history"
            ":ytwatchlater" -> "https://m.youtube.com/playlist?list=WL"
            ":ytfav" -> "https://m.youtube.com/playlist?list=LL"
            else -> return@withContext emptyList()
        }

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.163 Mobile Safari/537.36")
            
            // Inject the raw, un-filtered cookies from the logged-in Webview to access private data
            val rawCookies = CookieManager.getInstance().getCookie("https://youtube.com")
            if (!rawCookies.isNullOrBlank()) {
                connection.setRequestProperty("Cookie", rawCookies)
            }

            if (connection.responseCode != 200) {
                LogManager.append("LibraryFetcher", "HTTP Error ${connection.responseCode} for $query")
                return@withContext emptyList()
            }

            val html = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            return@withContext parseHtmlForVideos(html)

        } catch (e: Exception) {
            LogManager.append("LibraryFetcher", "Error fetching library: ${e.message}")
            return@withContext emptyList()
        }
    }

    private fun parseHtmlForVideos(html: String): List<SearchResultItem> {
        val results = mutableListOf<SearchResultItem>()
        
        // Extract ytInitialData JSON object from the raw HTML using string manipulation
        val searchString = "var ytInitialData = "
        val startIndex = html.indexOf(searchString)
        if (startIndex == -1) return results
        
        val jsonStart = startIndex + searchString.length
        
        // Find the matching closing brace for the JSON object
        var openBraces = 0
        var jsonEnd = -1
        for (i in jsonStart until html.length) {
            val char = html[i]
            if (char == '{') openBraces++
            if (char == '}') {
                openBraces--
                if (openBraces == 0) {
                    jsonEnd = i + 1
                    break
                }
            }
        }
        
        if (jsonEnd == -1) return results
        
        val jsonString = html.substring(jsonStart, jsonEnd)
        
        try {
            val jsonObject = JSONObject(jsonString)
            // Due to YouTube's deeply nested and dynamic schema, we will recursively search the JSON for video renderers
            extractVideosRecursively(jsonObject, results)
        } catch (e: Exception) {
            LogManager.append("LibraryFetcher", "Failed to parse ytInitialData JSON: ${e.message}")
        }
        
        // Remove duplicates since YouTube sometimes includes the same video multiple times in the JSON blob
        return results.distinctBy { it.id }
    }

    private fun extractVideosRecursively(json: Any, results: MutableList<SearchResultItem>) {
        if (json is JSONObject) {
            if (json.has("videoId") && json.has("title")) {
                try {
                    val videoId = json.getString("videoId")
                    
                    // Parse title
                    val titleObj = json.optJSONObject("title")
                    val title = if (titleObj?.has("runs") == true) {
                        titleObj.getJSONArray("runs").getJSONObject(0).getString("text")
                    } else if (titleObj?.has("simpleText") == true) {
                        titleObj.getString("simpleText")
                    } else {
                        "Unknown Title"
                    }
                    
                    // Parse artist (author)
                    var artist = "Unknown Artist"
                    if (json.has("shortBylineText")) {
                        val bylineObj = json.getJSONObject("shortBylineText")
                        if (bylineObj.has("runs")) {
                            artist = bylineObj.getJSONArray("runs").getJSONObject(0).getString("text")
                        }
                    } else if (json.has("longBylineText")) {
                        val bylineObj = json.getJSONObject("longBylineText")
                        if (bylineObj.has("runs")) {
                            artist = bylineObj.getJSONArray("runs").getJSONObject(0).getString("text")
                        }
                    }
                    
                    // Parse duration
                    var durationText = ""
                    if (json.has("lengthText")) {
                        val lengthObj = json.getJSONObject("lengthText")
                        if (lengthObj.has("simpleText")) {
                            durationText = lengthObj.getString("simpleText")
                        }
                    }

                    results.add(
                        SearchResultItem(
                            id = videoId,
                            title = title,
                            artist = artist,
                            durationText = durationText,
                            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                            url = "https://youtube.com/watch?v=$videoId",
                            isAudio = true // Default all library items to audio processing queue initially
                        )
                    )
                } catch (e: Exception) {
                    // Ignore malformed objects and continue
                }
            }
            
            // Recursively check all keys in this object
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                extractVideosRecursively(json.get(key), results)
            }
        } else if (json is org.json.JSONArray) {
            for (i in 0 until json.length()) {
                extractVideosRecursively(json.get(i), results)
            }
        }
    }
}
