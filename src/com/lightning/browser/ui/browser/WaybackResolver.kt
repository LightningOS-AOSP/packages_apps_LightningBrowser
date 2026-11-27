/*
 * Copyright (C) 2026 The LightningOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightning.browser.ui.browser

import android.os.Handler
import android.os.Looper
import android.util.LruCache
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

data class WaybackSnapshot(
    val originalUrl: String,
    val snapshotUrl: String,
    val formattedDate: String,
)

object WaybackResolver {

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cache = LruCache<String, WaybackSnapshot?>(50)

    fun checkAvailability(
        url: String,
        onResult: (WaybackSnapshot?) -> Unit,
    ) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            onResult(null)
            return
        }
        if (trimmed.contains("web.archive.org/web/", ignoreCase = true)) {
            onResult(null)
            return
        }

        synchronized(cache) {
            val cached = cache.get(trimmed)
            if (cached != null) {
                onResult(cached)
                return
            }
        }

        executor.execute {
            var conn: HttpURLConnection? = null
            try {
                val encodedUrl = URLEncoder.encode(trimmed, "UTF-8")
                val apiUrl = "https://archive.org/wayback/available?url=$encodedUrl"
                conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "LightningBrowser/1.1 (Android; Linux)")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(rawJson)
                    val snapshots = json.optJSONObject("archived_snapshots")
                    val closest = snapshots?.optJSONObject("closest")
                    val available = closest?.optBoolean("available", false) == true
                    val snapshotUrl = closest?.optString("url")
                    val timestamp = closest?.optString("timestamp")

                    if (available && !snapshotUrl.isNullOrBlank()) {
                        val httpsSnapshotUrl = if (snapshotUrl.startsWith("http://", ignoreCase = true)) {
                            "https://" + snapshotUrl.substring(7)
                        } else {
                            snapshotUrl
                        }
                        val formattedDate = formatTimestamp(timestamp)
                        val snapshot = WaybackSnapshot(
                            originalUrl = trimmed,
                            snapshotUrl = httpsSnapshotUrl,
                            formattedDate = formattedDate,
                        )
                        synchronized(cache) { cache.put(trimmed, snapshot) }
                        mainHandler.post { onResult(snapshot) }
                        return@execute
                    }
                }
            } catch (_: Exception) {
                // Ignore network/parsing issues
            } finally {
                runCatching { conn?.disconnect() }
            }
            mainHandler.post { onResult(null) }
        }
    }

    private fun formatTimestamp(rawTimestamp: String?): String {
        if (rawTimestamp.isNullOrBlank() || rawTimestamp.length < 8) return ""
        return runCatching {
            val parser = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            val date = parser.parse(rawTimestamp) ?: return@runCatching ""
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
            formatter.format(date)
        }.getOrElse {
            val year = rawTimestamp.take(4)
            val month = rawTimestamp.drop(4).take(2)
            "$month/$year"
        }
    }
}
