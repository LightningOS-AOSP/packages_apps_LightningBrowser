package com.lightning.browser.ui.browser

import com.lightning.browser.ui.settings.CustomSearchEngine
import com.lightning.browser.ui.settings.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SearchSuggestionsClient {

    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    private const val WIKIPEDIA_USER_AGENT = "LightningBrowser/1.0 (Android)"

    suspend fun fetchSuggestions(
        query: String,
        builtInEngine: SearchEngine,
        customEngine: CustomSearchEngine?,
    ): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        if (looksLikeExplicitUri(trimmed)) return@withContext emptyList()

        val endpoint = suggestEndpointFor(builtInEngine, customEngine) ?: return@withContext emptyList()
        val userAgent = if (isWikipediaEndpoint(endpoint)) WIKIPEDIA_USER_AGENT else BROWSER_USER_AGENT

        val primary = fetchFromEndpoint(endpoint, trimmed, userAgent)
        if (primary.isNotEmpty()) return@withContext primary

        // Fallback to DuckDuckGo autocomplete when rate-limited (e.g. Brave 429)
        if (endpoint.contains("search.brave.com")) {
            return@withContext fetchFromEndpoint(
                "https://duckduckgo.com/ac/?q=%s&type=list",
                trimmed,
                BROWSER_USER_AGENT,
            )
        }
        emptyList()
    }

    private fun fetchFromEndpoint(endpoint: String, query: String, userAgent: String): List<String> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val requestUrl = endpoint.replace("%s", encoded)
        return runCatching {
            val conn = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", userAgent)
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching emptyList()
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseOpenSearchSuggestions(body, query)
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    private fun looksLikeExplicitUri(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("://") || lower.startsWith("about:") || lower.startsWith("file:")
    }

    private fun isWikipediaEndpoint(endpoint: String): Boolean =
        endpoint.contains("wikipedia.org")

    private fun suggestEndpointFor(
        builtInEngine: SearchEngine,
        customEngine: CustomSearchEngine?,
    ): String? {
        if (customEngine != null) {
            val fmt = customEngine.searchUrlFormat.lowercase()
            return when {
                "google." in fmt -> "https://www.google.com/complete/search?client=firefox&q=%s"
                "duckduckgo." in fmt -> "https://duckduckgo.com/ac/?q=%s&type=list"
                "bing." in fmt -> "https://api.bing.com/osjson.aspx?query=%s"
                "search.brave." in fmt -> "https://search.brave.com/api/suggest?q=%s"
                "wikipedia.org" in fmt -> "https://en.wikipedia.org/w/api.php?action=opensearch&search=%s&limit=6"
                else -> null
            }
        }
        return when (builtInEngine) {
            SearchEngine.GOOGLE -> "https://www.google.com/complete/search?client=firefox&q=%s"
            SearchEngine.DUCKDUCKGO -> "https://duckduckgo.com/ac/?q=%s&type=list"
            SearchEngine.BING -> "https://api.bing.com/osjson.aspx?query=%s"
            SearchEngine.BRAVE -> "https://search.brave.com/api/suggest?q=%s"
            SearchEngine.WIKIPEDIA -> "https://en.wikipedia.org/w/api.php?action=opensearch&search=%s&limit=6"
            SearchEngine.PERPLEXITY -> "https://duckduckgo.com/ac/?q=%s&type=list"
        }
    }

    private fun parseOpenSearchSuggestions(json: String, originalQuery: String): List<String> {
        val root = JSONArray(json)
        val items = root.optJSONArray(1) ?: return emptyList()
        val results = ArrayList<String>(6)
        for (i in 0 until items.length()) {
            val text = items.optString(i).trim()
            if (text.isNotEmpty() && !text.equals(originalQuery, ignoreCase = true) && text !in results) {
                results.add(text)
                if (results.size >= 6) break
            }
        }
        return results
    }
}
