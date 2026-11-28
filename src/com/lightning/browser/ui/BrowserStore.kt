package com.lightning.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lightning.browser.ui.home.PinnedSite
import com.lightning.browser.ui.settings.Bookmark
import com.lightning.browser.ui.settings.CustomSearchEngine
import com.lightning.browser.ui.settings.DnsProvider
import com.lightning.browser.ui.settings.HistoryEntry
import com.lightning.browser.ui.settings.SearchEngine
import com.lightning.browser.ui.settings.StartupBehavior
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.tabs.ClosedTab
import com.lightning.browser.ui.theme.ThemeMode
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors
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
    private const val KEY_ACTIVE_SEARCH_ENGINE_ID = "active_search_engine_id"
    private const val KEY_CUSTOM_SEARCH_ENGINES = "custom_search_engines"
    private const val KEY_DNS_PROVIDER = "dns_provider"
    private const val KEY_DNS_URL = "dns_url"
    private const val KEY_DNS_BYPASS = "dns_bypass"
    private const val KEY_AD_BLOCKING = "ad_blocking"
    private const val KEY_FINGERPRINT = "fingerprint_lock"
    private const val KEY_TABS = "open_tabs"
    private const val KEY_ACTIVE_TAB = "active_tab_index"
    private const val KEY_STARTUP = "startup_behavior"
    private const val KEY_HIDDEN_DOWNLOADS = "hidden_downloads"
    private const val KEY_DOWNLOADS = "browser_downloads"
    private const val KEY_DEFAULT_BROWSER_PROMPTED = "default_browser_prompted"
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    private const val KEY_READING_TIME = "reading_time"
    private const val KEY_QUICK_ARC = "quick_arc_menu"
    private const val KEY_TAB_PREVIEWS = "tab_previews"
    private const val KEY_CLIPBOARD_PROMPT = "clipboard_url_prompt"
    private const val KEY_WAYBACK_FALLBACK = "wayback_fallback"
    private const val KEY_DUPLICATE_TAB_WARNING = "duplicate_tab_warning"
    private const val KEY_RECENTLY_CLOSED_ENABLED = "recently_closed_enabled"
    private const val KEY_DL_SAVE_FOLDER = "dl_save_folder"
    private const val KEY_DL_CUSTOM_PATH = "dl_custom_path"
    private const val KEY_DL_ASK_BEFORE = "dl_ask_before"
    private const val KEY_DL_DELETE_PRIVATE = "dl_delete_private"
    private const val KEY_PRIVATE_DOWNLOADS = "pending_private_downloads"
    private const val KEY_DL_OTHER_ASK = "dl_other_ask"
    private const val KEY_DL_CONTENT_ACTIONS = "dl_content_actions"
    private const val KEY_DISMISSED_CLIP_TIMESTAMP = "dismissed_clip_timestamp"
    private const val KEY_CLOSED_TABS = "recently_closed_tabs"
    const val MAX_CLOSED_TABS = 10
    const val CLOSED_TAB_EXPIRY_MS = 24L * 60L * 60L * 1000L
    private const val MAX_HISTORY = 200
    private const val MAX_DOWNLOADS = 200
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
        prefs(context).edit()
            .putString(KEY_SEARCH_ENGINE, engine.name)
            .putString(KEY_ACTIVE_SEARCH_ENGINE_ID, engine.name)
            .apply()
    }

    fun loadActiveSearchEngineId(context: Context): String =
        prefs(context).getString(KEY_ACTIVE_SEARCH_ENGINE_ID, null)
            ?: prefs(context).getString(KEY_SEARCH_ENGINE, SearchEngine.GOOGLE.name)
            ?: SearchEngine.GOOGLE.name

    fun saveActiveSearchEngineId(context: Context, id: String) {
        val editor = prefs(context).edit().putString(KEY_ACTIVE_SEARCH_ENGINE_ID, id)
        SearchEngine.entries.firstOrNull { it.name == id }?.let {
            editor.putString(KEY_SEARCH_ENGINE, it.name)
        }
        editor.apply()
    }

    fun loadCustomSearchEngines(context: Context): List<CustomSearchEngine> {
        val raw = prefs(context).getString(KEY_CUSTOM_SEARCH_ENGINES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val list = mutableListOf<CustomSearchEngine>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CustomSearchEngine(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        searchUrlFormat = obj.getString("searchUrl"),
                        keyword = obj.optString("keyword", ""),
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun saveCustomSearchEngines(context: Context, engines: List<CustomSearchEngine>) {
        val array = JSONArray()
        engines.forEach { engine ->
            val obj = JSONObject()
            obj.put("id", engine.id)
            obj.put("name", engine.name)
            obj.put("searchUrl", engine.searchUrlFormat)
            obj.put("keyword", engine.keyword)
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_CUSTOM_SEARCH_ENGINES, array.toString()).apply()
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

    fun loadHapticFeedback(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAPTIC_FEEDBACK, true)

    fun saveHapticFeedback(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()
    }

    fun loadReadingTimeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_READING_TIME, false)

    fun saveReadingTimeEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_READING_TIME, value).apply()
    }

    fun loadQuickArcEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QUICK_ARC, true)

    fun saveQuickArcEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_QUICK_ARC, value).apply()
    }

    fun loadTabPreviewsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TAB_PREVIEWS, true)

    fun saveTabPreviewsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_TAB_PREVIEWS, value).apply()
    }

    fun loadClipboardPromptEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CLIPBOARD_PROMPT, true)

    fun saveClipboardPromptEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CLIPBOARD_PROMPT, value).apply()
    }

    fun loadWaybackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WAYBACK_FALLBACK, true)

    fun saveWaybackEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_WAYBACK_FALLBACK, value).apply()
    }

    fun loadDuplicateTabWarningEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DUPLICATE_TAB_WARNING, true)

    fun saveDuplicateTabWarningEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DUPLICATE_TAB_WARNING, value).apply()
    }

    fun loadRecentlyClosedEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RECENTLY_CLOSED_ENABLED, true)

    fun saveRecentlyClosedEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_RECENTLY_CLOSED_ENABLED, value).apply()
    }

    enum class DownloadFolder(val labelRes: Int, val relativePath: String) {
        DOWNLOADS(com.lightning.browser.R.string.dl_folder_downloads, "Download"),
        DOWNLOADS_LIGHTNING(com.lightning.browser.R.string.dl_folder_downloads_lightning, "Download/Lightning"),
        DOCUMENTS(com.lightning.browser.R.string.dl_folder_documents, "Documents"),
        MUSIC(com.lightning.browser.R.string.dl_folder_music, "Music"),
        MOVIES(com.lightning.browser.R.string.dl_folder_movies, "Movies"),
        PICTURES(com.lightning.browser.R.string.dl_folder_pictures, "Pictures"),
        CUSTOM(com.lightning.browser.R.string.dl_folder_custom, "Download"),
    }

    val ALLOWED_USER_DIR_ROOTS = listOf(
        "Download",
        "Documents",
        "Music",
        "Movies",
        "Pictures",
        "DCIM",
        "Podcasts",
        "Audiobooks",
    )

    data class PrivateDownloadRecord(
        val tabId: String,
        val fileName: String,
        val relativePath: String,
    )

    enum class ContentAction(val labelRes: Int) {
        ALWAYS_ASK(com.lightning.browser.R.string.dl_action_always_ask),
        SAVE_FILE(com.lightning.browser.R.string.dl_action_save_file),
        OPEN_IN_BROWSER(com.lightning.browser.R.string.dl_action_open_in_browser),
        OPEN_WITH_APP(com.lightning.browser.R.string.dl_action_open_with_app),
    }

    data class ContentPreference(
        val action: ContentAction,
        val packageName: String? = null,
        val className: String? = null,
        val appLabel: String? = null,
    )

    data class ContentTypeRule(
        val key: String,
        val title: String,
        val mimeOrScheme: String,
        val defaultAction: ContentAction,
        val supportedActions: List<ContentAction>,
    )

    val defaultContentTypeRules = listOf(
        ContentTypeRule(
            key = "avif",
            title = "AV1 Image File (AVIF)",
            mimeOrScheme = "image/avif",
            defaultAction = ContentAction.OPEN_IN_BROWSER,
            supportedActions = listOf(ContentAction.OPEN_IN_BROWSER, ContentAction.SAVE_FILE, ContentAction.ALWAYS_ASK),
        ),
        ContentTypeRule(
            key = "xml",
            title = "Extensible Markup Language (XML)",
            mimeOrScheme = "text/xml",
            defaultAction = ContentAction.SAVE_FILE,
            supportedActions = listOf(ContentAction.SAVE_FILE, ContentAction.OPEN_IN_BROWSER, ContentAction.ALWAYS_ASK),
        ),
        ContentTypeRule(
            key = "jxl",
            title = "JPEG XL Image File",
            mimeOrScheme = "image/jxl",
            defaultAction = ContentAction.SAVE_FILE,
            supportedActions = listOf(ContentAction.SAVE_FILE, ContentAction.ALWAYS_ASK),
        ),
        ContentTypeRule(
            key = "mailto",
            title = "mailto",
            mimeOrScheme = "mailto",
            defaultAction = ContentAction.OPEN_WITH_APP,
            supportedActions = listOf(ContentAction.ALWAYS_ASK),
        ),
        ContentTypeRule(
            key = "pdf",
            title = "Portable Document Format (PDF)",
            mimeOrScheme = "application/pdf",
            defaultAction = ContentAction.OPEN_IN_BROWSER,
            supportedActions = listOf(ContentAction.OPEN_IN_BROWSER, ContentAction.SAVE_FILE, ContentAction.ALWAYS_ASK),
        ),
        ContentTypeRule(
            key = "svg",
            title = "Scalable Vector Graphics (SVG)",
            mimeOrScheme = "image/svg+xml",
            defaultAction = ContentAction.SAVE_FILE,
            supportedActions = listOf(ContentAction.SAVE_FILE, ContentAction.OPEN_IN_BROWSER, ContentAction.ALWAYS_ASK),
        ),
        ContentTypeRule(
            key = "webp",
            title = "WebP Image",
            mimeOrScheme = "image/webp",
            defaultAction = ContentAction.OPEN_IN_BROWSER,
            supportedActions = listOf(ContentAction.OPEN_IN_BROWSER, ContentAction.SAVE_FILE, ContentAction.ALWAYS_ASK),
        ),
    )

    fun loadDownloadFolder(context: Context): DownloadFolder =
        DownloadFolder.entries.firstOrNull { it.name == prefs(context).getString(KEY_DL_SAVE_FOLDER, null) }
            ?: DownloadFolder.DOWNLOADS

    fun saveDownloadFolder(context: Context, folder: DownloadFolder) {
        prefs(context).edit().putString(KEY_DL_SAVE_FOLDER, folder.name).apply()
    }

    fun loadCustomDownloadPath(context: Context): String =
        prefs(context).getString(KEY_DL_CUSTOM_PATH, null)?.takeIf { isAllowedUserRelativePath(it) }
            ?: "Download"

    fun saveCustomDownloadPath(context: Context, relativePath: String) {
        if (isAllowedUserRelativePath(relativePath)) {
            prefs(context).edit().putString(KEY_DL_CUSTOM_PATH, relativePath.trim('/')).apply()
        }
    }

    fun effectiveDownloadRelativePath(context: Context): String {
        val folder = loadDownloadFolder(context)
        return if (folder == DownloadFolder.CUSTOM) loadCustomDownloadPath(context) else folder.relativePath
    }

    fun isAllowedUserRelativePath(path: String): Boolean {
        val clean = path.trim().trim('/')
        if (clean.isEmpty() || clean.contains("..")) return false
        return ALLOWED_USER_DIR_ROOTS.any { root ->
            clean.equals(root, ignoreCase = true) || clean.startsWith("$root/", ignoreCase = true)
        }
    }

    private fun privateDownloadsFile(context: Context): File =
        File(context.applicationContext.filesDir, "private_downloads.json")

    fun addPrivateDownload(context: Context, tabId: String, fileName: String, relativePath: String) {
        if (fileName.isBlank()) return
        val existing = loadPrivateDownloads(context).toMutableList()
        if (existing.none { it.tabId == tabId && it.fileName == fileName && it.relativePath == relativePath }) {
            existing.add(PrivateDownloadRecord(tabId, fileName, relativePath))
            savePrivateDownloads(context, existing)
        }
    }

    fun loadPrivateDownloads(context: Context): List<PrivateDownloadRecord> {
        val file = privateDownloadsFile(context)
        if (!file.exists()) return emptyList()
        val raw = runCatching { file.readText() }.getOrNull() ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        PrivateDownloadRecord(
                            tabId = obj.optString("tabId", ""),
                            fileName = obj.getString("fileName"),
                            relativePath = obj.optString("relativePath", "Download"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun removePrivateDownloadsForTab(context: Context, tabId: String): List<PrivateDownloadRecord> {
        val all = loadPrivateDownloads(context)
        val removed = all.filter { it.tabId == tabId }
        if (removed.isNotEmpty()) {
            savePrivateDownloads(context, all.filterNot { it.tabId == tabId })
        }
        return removed
    }

    fun popAllPrivateDownloads(context: Context): List<PrivateDownloadRecord> {
        val all = loadPrivateDownloads(context)
        runCatching { privateDownloadsFile(context).delete() }
        return all
    }

    private fun savePrivateDownloads(context: Context, list: List<PrivateDownloadRecord>) {
        val file = privateDownloadsFile(context)
        if (list.isEmpty()) {
            runCatching { file.delete() }
            return
        }
        val array = JSONArray()
        list.forEach { rec ->
            array.put(
                JSONObject()
                    .put("tabId", rec.tabId)
                    .put("fileName", rec.fileName)
                    .put("relativePath", rec.relativePath),
            )
        }
        runCatching { file.writeText(array.toString()) }
    }

    fun loadDownloadAskBefore(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DL_ASK_BEFORE, false)

    fun saveDownloadAskBefore(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DL_ASK_BEFORE, value).apply()
    }

    fun loadDeletePrivateDownloads(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DL_DELETE_PRIVATE, false)

    fun saveDeletePrivateDownloads(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DL_DELETE_PRIVATE, value).apply()
    }

    fun loadDownloadOtherAsk(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DL_OTHER_ASK, false)

    fun saveDownloadOtherAsk(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DL_OTHER_ASK, value).apply()
    }

    fun loadContentPreferences(context: Context): Map<String, ContentPreference> {
        val raw = prefs(context).getString(KEY_DL_CONTENT_ACTIONS, null)
        val overrides = mutableMapOf<String, ContentPreference>()
        if (raw != null) {
            runCatching {
                val obj = JSONObject(raw)
                obj.keys().forEach { key ->
                    val entry = obj.opt(key)
                    when (entry) {
                        is JSONObject -> {
                            val name = entry.optString("action")
                            val action = ContentAction.entries.firstOrNull { it.name == name }
                            if (action != null) {
                                overrides[key] = ContentPreference(
                                    action = action,
                                    packageName = entry.optString("package").ifBlank { null },
                                    className = entry.optString("class").ifBlank { null },
                                    appLabel = entry.optString("label").ifBlank { null },
                                )
                            }
                        }
                        is String -> {
                            ContentAction.entries.firstOrNull { it.name == entry }?.let { action ->
                                overrides[key] = ContentPreference(action = action)
                            }
                        }
                    }
                }
            }
        }
        return defaultContentTypeRules.associate { rule ->
            rule.key to (overrides[rule.key] ?: ContentPreference(rule.defaultAction))
        }
    }

    fun saveContentPreference(context: Context, key: String, pref: ContentPreference) {
        val current = loadContentPreferences(context).toMutableMap()
        current[key] = pref
        val obj = JSONObject()
        current.forEach { (k, v) ->
            obj.put(
                k,
                JSONObject()
                    .put("action", v.action.name)
                    .put("package", v.packageName.orEmpty())
                    .put("class", v.className.orEmpty())
                    .put("label", v.appLabel.orEmpty()),
            )
        }
        prefs(context).edit().putString(KEY_DL_CONTENT_ACTIONS, obj.toString()).apply()
    }

    fun loadTabs(context: Context): List<BrowserTab> {
        val raw = prefs(context).getString(KEY_TABS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val entry = array.getJSONObject(i)
                    val url = entry.optString("url", "")
                    val domain = entry.optString("domain", "")
                    val title = entry.optString("title", if (url.isEmpty()) "New tab" else domain)
                    val id = entry.optString("id", "").ifEmpty { UUID.randomUUID().toString() }
                    val desktopMode = entry.optBoolean("desktopMode", false)
                    add(BrowserTab(
                        title = title.ifEmpty { if (url.isEmpty()) "New tab" else domain },
                        domain = domain,
                        url = url,
                        private = false,
                        desktopMode = desktopMode,
                        id = id,
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveTabs(context: Context, tabs: List<BrowserTab>) {
        val nonPrivate = tabs.filter { !it.private }
        val array = JSONArray()
        nonPrivate.forEach { tab ->
            array.put(JSONObject()
                .put("title", tab.title)
                .put("domain", tab.domain)
                .put("url", tab.url)
                .put("desktopMode", tab.desktopMode)
                .put("id", tab.id))
        }
        prefs(context)
            .edit()
            .putString(KEY_TABS, array.toString())
            .apply()
    }

    private val diskExecutor = Executors.newSingleThreadExecutor()

    private fun thumbnailDir(context: Context): File =
        File(context.filesDir, "tab_thumbnails").apply { if (!exists()) mkdirs() }

    fun saveTabThumbnail(context: Context, tabId: String, bitmap: Bitmap) {
        if (tabId.isEmpty()) return
        diskExecutor.execute {
            runCatching {
                val file = File(thumbnailDir(context), "$tabId.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            }
        }
    }

    fun loadTabThumbnail(context: Context, tabId: String): Bitmap? {
        if (tabId.isEmpty()) return null
        return runCatching {
            val file = File(thumbnailDir(context), "$tabId.jpg")
            if (file.exists()) {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
            } else {
                null
            }
        }.getOrNull()
    }

    fun deleteTabThumbnail(context: Context, tabId: String) {
        if (tabId.isEmpty()) return
        diskExecutor.execute {
            runCatching {
                val file = File(thumbnailDir(context), "$tabId.jpg")
                if (file.exists()) file.delete()
            }
        }
    }

    fun clearAllTabThumbnails(context: Context) {
        diskExecutor.execute {
            runCatching {
                thumbnailDir(context).listFiles()?.forEach { it.delete() }
            }
        }
    }

    fun loadActiveTab(context: Context): Int =
        prefs(context).getInt(KEY_ACTIVE_TAB, 0)

    fun saveActiveTab(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY_ACTIVE_TAB, index).apply()
    }

    fun loadStartupBehavior(context: Context): StartupBehavior =
        StartupBehavior.entries.firstOrNull { it.name == prefs(context).getString(KEY_STARTUP, null) }
            ?: StartupBehavior.HOMEPAGE

    fun saveStartupBehavior(context: Context, behavior: StartupBehavior) {
        prefs(context).edit().putString(KEY_STARTUP, behavior.name).apply()
    }

    data class DownloadRecord(
        val title: String,
        val domain: String,
        val sizeBytes: Long,
        val timestamp: Long,
        val relativePath: String = "Download",
    )

    fun loadDownloads(context: Context): List<DownloadRecord> {
        val raw = prefs(context).getString(KEY_DOWNLOADS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val entry = array.getJSONObject(i)
                    add(DownloadRecord(
                        title = entry.getString("title"),
                        domain = entry.optString("domain", ""),
                        sizeBytes = entry.optLong("sizeBytes", 0L),
                        timestamp = entry.optLong("timestamp", 0L),
                        relativePath = entry.optString("relativePath", "Download"),
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addDownload(context: Context, record: DownloadRecord) {
        val current = loadDownloads(context).filterNot { it.title == record.title }
        val updated = (listOf(record) + current).take(MAX_DOWNLOADS)
        val array = JSONArray()
        updated.forEach { item ->
            array.put(JSONObject()
                .put("title", item.title)
                .put("domain", item.domain)
                .put("sizeBytes", item.sizeBytes)
                .put("timestamp", item.timestamp)
                .put("relativePath", item.relativePath))
        }
        prefs(context).edit().putString(KEY_DOWNLOADS, array.toString()).apply()
    }

    fun removeDownload(context: Context, title: String) {
        val updated = loadDownloads(context).filterNot { it.title == title }
        val array = JSONArray()
        updated.forEach { item ->
            array.put(JSONObject()
                .put("title", item.title)
                .put("domain", item.domain)
                .put("sizeBytes", item.sizeBytes)
                .put("timestamp", item.timestamp)
                .put("relativePath", item.relativePath))
        }
        prefs(context).edit().putString(KEY_DOWNLOADS, array.toString()).apply()
    }

    fun hasPromptedDefaultBrowser(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEFAULT_BROWSER_PROMPTED, false)

    fun setDefaultBrowserPrompted(context: Context, prompted: Boolean = true) {
        prefs(context).edit().putBoolean(KEY_DEFAULT_BROWSER_PROMPTED, prompted).apply()
    }

    fun loadHiddenDownloads(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_HIDDEN_DOWNLOADS, emptySet())?.toSet() ?: emptySet()

    fun hideDownload(context: Context, title: String) {
        val updated = loadHiddenDownloads(context).toMutableSet().apply { add(title) }
        prefs(context).edit().putStringSet(KEY_HIDDEN_DOWNLOADS, updated).apply()
    }

    fun unhideDownload(context: Context, title: String) {
        val current = loadHiddenDownloads(context)
        if (title in current) {
            val updated = current.toMutableSet().apply { remove(title) }
            prefs(context).edit().putStringSet(KEY_HIDDEN_DOWNLOADS, updated).apply()
        }
    }

    fun loadDismissedClipTimestamp(context: Context): Long? {
        val p = prefs(context)
        return if (p.contains(KEY_DISMISSED_CLIP_TIMESTAMP)) p.getLong(KEY_DISMISSED_CLIP_TIMESTAMP, 0L) else null
    }

    fun saveDismissedClipTimestamp(context: Context, timestamp: Long) {
        prefs(context).edit().putLong(KEY_DISMISSED_CLIP_TIMESTAMP, timestamp).apply()
    }

    fun loadClosedTabs(context: Context): List<ClosedTab> {
        val raw = prefs(context).getString(KEY_CLOSED_TABS, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val entry = array.getJSONObject(i)
                    val url = entry.optString("url", "")
                    val closedAt = entry.optLong("closedAt", 0L)
                    if (url.isNotBlank() && now - closedAt <= CLOSED_TAB_EXPIRY_MS) {
                        add(
                            ClosedTab(
                                id = entry.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                                title = entry.optString("title", ""),
                                domain = entry.optString("domain", ""),
                                url = url,
                                desktopMode = entry.optBoolean("desktopMode", false),
                                closedAt = closedAt,
                            ),
                        )
                    }
                }
            }.take(MAX_CLOSED_TABS)
        }.getOrDefault(emptyList())
    }

    fun saveClosedTabs(context: Context, tabs: List<ClosedTab>) {
        val now = System.currentTimeMillis()
        val array = JSONArray()
        tabs.asSequence()
            .filter { it.url.isNotBlank() && now - it.closedAt <= CLOSED_TAB_EXPIRY_MS }
            .take(MAX_CLOSED_TABS)
            .forEach { tab ->
                array.put(
                    JSONObject()
                        .put("id", tab.id)
                        .put("title", tab.title)
                        .put("domain", tab.domain)
                        .put("url", tab.url)
                        .put("desktopMode", tab.desktopMode)
                        .put("closedAt", tab.closedAt),
                )
            }
        prefs(context).edit().putString(KEY_CLOSED_TABS, array.toString()).apply()
    }
}