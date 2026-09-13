package com.lightning.browser.ui

import android.content.Context
import com.lightning.browser.ui.home.PinnedSite
import com.lightning.browser.ui.settings.Bookmark
import com.lightning.browser.ui.settings.DnsProvider
import com.lightning.browser.ui.settings.HistoryEntry
import com.lightning.browser.ui.settings.SearchEngine
import com.lightning.browser.ui.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

// Bookmarks, history, recent searches, pinned sites and browser settings all
// live in SharedPreferences as JSON. Icons and tones are derived from the
// domain via SiteMeta, so only the plain fields need to be persisted.
object BrowserStore {

    private const val PREFS = "lightning_browser_data"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val KEY_HISTORY = "history"
    private const val KEY_SEARCHES = "recent_searches"
    private const val KEY_PINNED = "pinned_sites"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_SEARCH_ENGINE = "search_engine"
    private const val KEY_DNS_PROVIDER = "dns_provider"
    private const val KEY_DNS_URL = "dns_url"
    private const val KEY_DNS_BYPASS = "dns_bypass"
    private const val KEY_AD_BLOCKING = "ad_blocking"
    private const val KEY_FINGERPRINT = "fingerprint_lock"
    private const val MAX_HISTORY = 200
    private const val MAX_SEARCHES = 10
    private const val MAX_PINNED = 8

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadBookmarks(context: Context): List<Bookmark> {
        val raw = prefs(context).getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val entry = array.getJSONObject(i)
                    val domain = entry.getString("domain")
                    add(Bookmark(
                        title = entry.getString("title"),
                        domain = domain,
                        icon = siteIcon(domain),
                        tone = siteTone(domain),
                        folder = entry.optString("folder", ""),
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveBookmarks(context: Context, bookmarks: List<Bookmark>) {
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            array.put(JSONObject()
                .put("title", bookmark.title)
                .put("domain", bookmark.domain)
                .put("folder", bookmark.folder))
        }
        prefs(context)
            .edit()
            .putString(KEY_BOOKMARKS, array.toString())
            .apply()
    }

    fun loadHistory(context: Context): List<HistoryEntry> {
        val raw = prefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val entry = array.getJSONObject(i)
                    val domain = entry.getString("domain")
                    add(HistoryEntry(
                        title = entry.getString("title"),
                        domain = domain,
                        icon = siteIcon(domain),
                        tone = siteTone(domain),
                        timestamp = entry.optLong("timestamp", 0L),
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveHistory(context: Context, history: List<HistoryEntry>) {
        val array = JSONArray()
        history.take(MAX_HISTORY).forEach { item ->
            array.put(JSONObject()
                .put("title", item.title)
                .put("domain", item.domain)
                .put("timestamp", item.timestamp))
        }
        prefs(context)
            .edit()
            .putString(KEY_HISTORY, array.toString())
            .apply()
    }

    fun loadRecentSearches(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_SEARCHES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(array.getString(i))
            }
        }.getOrDefault(emptyList())
    }

    fun saveRecentSearches(context: Context, searches: List<String>) {
        val array = JSONArray()
        searches.take(MAX_SEARCHES).forEach { array.put(it) }
        prefs(context)
            .edit()
            .putString(KEY_SEARCHES, array.toString())
            .apply()
    }

    fun loadPinnedSites(context: Context): List<PinnedSite> {
        val raw = prefs(context).getString(KEY_PINNED, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val entry = array.getJSONObject(i)
                    val domain = entry.getString("domain")
                    add(PinnedSite(
                        title = entry.optString("title", domain.ifEmpty { "?" }),
                        domain = domain,
                        icon = siteIcon(domain),
                        tone = siteTone(domain),
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun savePinnedSites(context: Context, sites: List<PinnedSite>) {
        val array = JSONArray()
        sites.take(MAX_PINNED).forEach { site ->
            array.put(JSONObject()
                .put("title", site.title)
                .put("domain", site.domain))
        }
        prefs(context)
            .edit()
            .putString(KEY_PINNED, array.toString())
            .apply()
    }

    fun loadThemeMode(context: Context): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == prefs(context).getString(KEY_THEME, null) }
            ?: ThemeMode.SYSTEM

    fun saveThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    fun loadSearchEngine(context: Context): SearchEngine =
        SearchEngine.entries.firstOrNull { it.name == prefs(context).getString(KEY_SEARCH_ENGINE, null) }
            ?: SearchEngine.GOOGLE

    fun saveSearchEngine(context: Context, engine: SearchEngine) {
        prefs(context).edit().putString(KEY_SEARCH_ENGINE, engine.name).apply()
    }

    fun loadDnsProvider(context: Context): DnsProvider =
        DnsProvider.entries.firstOrNull { it.name == prefs(context).getString(KEY_DNS_PROVIDER, null) }
            ?: DnsProvider.OFF

    fun saveDnsProvider(context: Context, provider: DnsProvider) {
        prefs(context).edit().putString(KEY_DNS_PROVIDER, provider.name).apply()
    }

    fun loadCustomDnsUrl(context: Context): String =
        prefs(context).getString(KEY_DNS_URL, "") ?: ""

    fun saveCustomDnsUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_DNS_URL, url).apply()
    }

    fun loadBypassTrusted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DNS_BYPASS, false)

    fun saveBypassTrusted(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DNS_BYPASS, value).apply()
    }

    fun loadAdBlocking(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AD_BLOCKING, true)

    fun saveAdBlocking(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_AD_BLOCKING, value).apply()
    }

    fun loadFingerprintLock(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FINGERPRINT, false)

    fun saveFingerprintLock(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_FINGERPRINT, value).apply()
    }
}