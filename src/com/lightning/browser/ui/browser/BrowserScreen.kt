package com.lightning.browser.ui.browser

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import org.json.JSONObject
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.MorphLoader
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.theme.rememberLightningHaptics
import com.lightning.browser.ui.components.TonalIconButton
import com.lightning.browser.ui.home.ChipTone
import com.lightning.browser.ui.home.PinnedSite
import com.lightning.browser.ui.home.RecentPage
import com.lightning.browser.ui.root.DuplicateTabWarning
import com.lightning.browser.ui.settings.Bookmark
import com.lightning.browser.ui.settings.CustomSearchEngine
import com.lightning.browser.ui.settings.DnsProvider
import com.lightning.browser.ui.settings.HistoryEntry
import com.lightning.browser.ui.settings.SearchEngine
import com.lightning.browser.ui.settings.searchUrl
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.tone
import java.io.ByteArrayInputStream
import java.io.File

private const val CHROME_SCROLL_THRESHOLD = 48
private const val CHROME_SETTLE_MS = 400L
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

private val IMAGE_FIT_SCRIPT = """
    (function() {
        if (!document || !document.contentType || !document.contentType.startsWith('image/')) return;
        var meta = document.querySelector('meta[name="viewport"]');
        if (!meta) {
            meta = document.createElement('meta');
            meta.name = 'viewport';
            if (document.head) document.head.appendChild(meta);
        }
        if (meta) {
            meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
        }
        var img = document.querySelector('body > img');
        if (img && document.body) {
            document.documentElement.style.height = '100%';
            document.body.style.margin = '0';
            document.body.style.minHeight = '100%';
            document.body.style.display = 'flex';
            document.body.style.alignItems = 'center';
            document.body.style.justifyContent = 'center';
            img.style.maxWidth = '100%';
            img.style.maxHeight = '100vh';
            img.style.width = 'auto';
            img.style.height = 'auto';
            img.style.objectFit = 'contain';
        }
    })();
""".trimIndent()

private val ENABLE_ZOOM_SCRIPT = """
    (function() {
        if (!document || (document.contentType && document.contentType.startsWith('image/'))) return;
        function allowZoom() {
            var metas = document.querySelectorAll('meta[name="viewport"]');
            for (var i = 0; i < metas.length; i++) {
                var m = metas[i];
                var c = m.getAttribute('content') || '';
                var updated = c
                    .replace(/user-scalable\s*=\s*(no|0)/gi, 'user-scalable=yes')
                    .replace(/maximum-scale\s*=\s*[0-9.]+/gi, 'maximum-scale=10.0')
                    .replace(/minimum-scale\s*=\s*[0-9.]+/gi, 'minimum-scale=0.25');
                if (!/user-scalable/i.test(updated)) {
                    updated += (updated ? ', ' : '') + 'user-scalable=yes';
                }
                if (!/maximum-scale/i.test(updated)) {
                    updated += (updated ? ', ' : '') + 'maximum-scale=10.0';
                }
                if (!/minimum-scale/i.test(updated)) {
                    updated += (updated ? ', ' : '') + 'minimum-scale=0.25';
                }
                if (c !== updated) {
                    m.setAttribute('content', updated);
                }
            }
        }
        allowZoom();
        if (!document.getElementById('_lightning_zoom_style')) {
            var style = document.createElement('style');
            style.id = '_lightning_zoom_style';
            style.textContent = 'html, body { touch-action: auto !important; }';
            var target = document.head || document.documentElement;
            if (target) target.appendChild(style);
        }
        if (!window._lightningZoomObserver && typeof MutationObserver !== 'undefined') {
            var root = document.head || document.documentElement;
            if (root) {
                window._lightningZoomObserver = new MutationObserver(allowZoom);
                window._lightningZoomObserver.observe(root, {
                    childList: true,
                    subtree: true,
                    attributes: true,
                    attributeFilter: ['content']
                });
            }
        }
    })();
""".trimIndent()

private fun desktopScale(webView: WebView): Float {
    val density = webView.resources.displayMetrics.density.coerceAtLeast(1f)
    val widthPx = if (webView.width > 0) webView.width else webView.resources.displayMetrics.widthPixels
    val viewDp = (widthPx / density).takeIf { it > 0f } ?: 390f
    return (viewDp / 1280f).coerceIn(0.2f, 1f)
}

private fun desktopViewportScript(webView: WebView): String {
    val formattedScale = String.format(java.util.Locale.US, "%.3f", desktopScale(webView))
    return """
        (function() {
            var targetContent = 'width=1280, initial-scale=$formattedScale, minimum-scale=0.1, maximum-scale=10.0, user-scalable=yes';
            function enforce() {
                var metas = document.querySelectorAll('meta[name="viewport"]');
                for (var i = 1; i < metas.length; i++) {
                    metas[i].remove();
                }
                var meta = metas[0];
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    var target = document.head || document.documentElement;
                    if (target) target.appendChild(meta);
                }
                if (meta && meta.getAttribute('content') !== targetContent) {
                    meta.setAttribute('content', targetContent);
                }
            }
            enforce();
            if (!window._lightningViewportObserver && typeof MutationObserver !== 'undefined') {
                var root = document.head || document.documentElement;
                if (root) {
                    window._lightningViewportObserver = new MutationObserver(enforce);
                    window._lightningViewportObserver.observe(root, {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        attributeFilter: ['content']
                    });
                }
            }
        })();
    """.trimIndent()
}

private fun applyDesktopMode(webView: WebView, config: WebConfig, enabled: Boolean) {
    val ua = if (enabled) DESKTOP_USER_AGENT else config.defaultUserAgent
    if (ua != null) {
        webView.settings.userAgentString = ua
    }
    webView.settings.useWideViewPort = true
    webView.settings.loadWithOverviewMode = true
    webView.settings.setSupportZoom(true)
    webView.settings.builtInZoomControls = true
    webView.settings.displayZoomControls = false
    webView.setInitialScale(if (enabled) (desktopScale(webView) * 100f).toInt().coerceIn(20, 100) else 0)
    config.userAgent = webView.settings.userAgentString
    config.desktopMode = enabled
}

data class TabNavState(val canGoBack: Boolean = false, val canGoForward: Boolean = false)

@Composable
fun BrowserScreen(
    tabs: SnapshotStateList<BrowserTab>,
    activeIndex: Int,
    navStates: SnapshotStateMap<Int, TabNavState>,
    webViews: SnapshotStateMap<Int, WebView>,
    loadedUrls: SnapshotStateMap<Int, String>,
    adBlocking: Boolean,
    searchEngine: SearchEngine,
    customSearchEngines: List<CustomSearchEngine> = emptyList(),
    activeCustomEngine: CustomSearchEngine? = null,
    dnsProvider: DnsProvider = DnsProvider.OFF,
    customDnsUrl: String = "",
    bypassTrusted: Boolean = false,
    onBlocked: () -> Unit = {},
    onDnsResolved: () -> Unit = {},
    focusSignal: Int = 0,
    onFocusConsumed: () -> Unit = {},
    browserVisible: Boolean = true,
    editStartedFromHome: Boolean = false,
    onClearEditFromHome: () -> Unit = {},
    onCancelFromHome: () -> Unit = {},
    findSignal: Int = 0,
    onFindConsumed: () -> Unit = {},
    chromeVisible: Boolean = true,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    barScrollBehavior: BrowserStore.BarScrollBehavior = BrowserStore.BarScrollBehavior.HIDE_BOTH,
    recentSearches: List<String> = emptyList(),
    recentPages: List<RecentPage> = emptyList(),
    history: List<HistoryEntry> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    pinnedSites: List<PinnedSite> = emptyList(),
    incognito: Boolean = false,
    suggestionSettings: BrowserStore.SearchSuggestionSettings = BrowserStore.SearchSuggestionSettings(),
    onSwitchTab: (Int) -> Unit = {},
    onUpdateTab: (Int, BrowserTab) -> Unit,
    onRecordVisit: (String, String) -> Unit = { _, _ -> },
    onSearchSubmitted: (String) -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenInNewTab: (String) -> Unit = {},
    duplicateTabWarning: DuplicateTabWarning? = null,
    onCheckDuplicateTab: (String) -> Unit = {},
    onSwitchDuplicateTab: () -> Unit = {},
    onDismissDuplicateTab: () -> Unit = {},
    showClipboardBanner: Boolean = false,
    dismissedClipTimestamp: Long? = null,
    onOpenClipboardBanner: () -> Unit = {},
    onDismissClipboardBanner: () -> Unit = {},
    onDismissClipSig: (Long) -> Unit = {},
    onPrintRequest: () -> Unit = {},
    readingTimeEnabled: Boolean = false,
    clipboardPromptEnabled: Boolean = true,
    waybackEnabled: Boolean = true,
    onDownloadSaved: (fileName: String, relativePath: String) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit = {},
    onOpenPdf: (url: String, userAgent: String?, title: String) -> Unit = { _, _, _ -> },
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTab = tabs.getOrNull(activeIndex)
    val keyboard = LocalSoftwareKeyboardController.current
    var editing by remember(activeIndex) { mutableStateOf(false) }
    val isEditing = editing || focusSignal > 0
    var query by remember(activeIndex) { mutableStateOf("") }
    var finding by remember(activeIndex) { mutableStateOf(false) }
    var findQuery by remember(activeIndex) { mutableStateOf("") }
    var findActiveMatch by remember(activeIndex) { mutableIntStateOf(0) }
    var findTotalMatches by remember(activeIndex) { mutableIntStateOf(0) }
    val progress = remember { mutableStateMapOf<Int, Int>() }
    val readingTimes = remember { mutableStateMapOf<String, ReadingTime?>() }
    val scrollAccumulator = remember { intArrayOf(0) }
    val lastToggle = remember { longArrayOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    var downloadConfirmPrompt by remember { mutableStateOf<PendingDownload?>(null) }
    var mailtoPromptUri by remember { mutableStateOf<Uri?>(null) }
    var duplicateDownload by remember { mutableStateOf<DuplicateDownloadPrompt?>(null) }
    var downloadPopup by remember { mutableStateOf<DownloadPopupState?>(null) }
    var waybackSnapshot by remember(activeIndex) { mutableStateOf<WaybackSnapshot?>(null) }
    var clipboardUrl by remember(activeIndex) { mutableStateOf<String?>(null) }
    var liveSearchSuggestions by remember(activeIndex) { mutableStateOf<List<String>>(emptyList()) }
    val isPrivateContext = incognito || (activeTab?.private == true)
    val allowLiveSuggestions = isEditing &&
        browserVisible &&
        suggestionSettings.showSearchSuggestions &&
        (!isPrivateContext || suggestionSettings.searchSuggestionsInPrivate)

    LaunchedEffect(query, allowLiveSuggestions, searchEngine, activeCustomEngine) {
        val trimmed = query.trim()
        if (!allowLiveSuggestions || trimmed.isEmpty()) {
            if (!suggestionSettings.showSearchSuggestions || (isPrivateContext && !suggestionSettings.searchSuggestionsInPrivate) || trimmed.isEmpty()) {
                liveSearchSuggestions = emptyList()
            }
            return@LaunchedEffect
        }
        delay(160L)
        liveSearchSuggestions = SearchSuggestionsClient.fetchSuggestions(
            query = trimmed,
            builtInEngine = searchEngine,
            customEngine = activeCustomEngine,
        )
    }
    val dnsEndpoint = remember(dnsProvider, customDnsUrl) {
        DohResolver.endpointFor(dnsProvider, customDnsUrl)
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(browserVisible) {
        if (!browserVisible) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(readingTimeEnabled) {
        if (!readingTimeEnabled) {
            readingTimes.clear()
        }
    }

    LaunchedEffect(waybackEnabled) {
        if (!waybackEnabled) {
            waybackSnapshot = null
        }
    }

    LaunchedEffect(isEditing, activeIndex, dismissedClipTimestamp, clipboardPromptEnabled) {
        if (isEditing && clipboardPromptEnabled) {
            val sig = ClipboardUrlHelper.clipSignature(context)
            if (sig == dismissedClipTimestamp) {
                clipboardUrl = null
            } else {
                val candidate = ClipboardUrlHelper.readClipboardUrl(context)
                val currentUrl = activeTab?.url.orEmpty()
                val loadedUrl = loadedUrls[activeIndex].orEmpty()
                clipboardUrl = if (candidate != null && candidate != currentUrl && candidate != loadedUrl) {
                    candidate
                } else {
                    null
                }
            }
        } else {
            clipboardUrl = null
        }
    }

    LaunchedEffect(downloadPopup) {
        val current = downloadPopup ?: return@LaunchedEffect
        delay(if (current.completed) 3_500L else 6_000L)
        if (downloadPopup?.id == current.id) {
            downloadPopup = null
        }
    }

    fun showDownloadStarted(name: String) {
        downloadPopup = DownloadPopupState(fileName = name, completed = false)
    }

    fun showDownloadFinished(name: String) {
        downloadPopup = DownloadPopupState(fileName = name, completed = true)
    }

    LaunchedEffect(Unit) {
        BrowserDownloads.completedEvents.collect { completedName ->
            showDownloadFinished(completedName)
        }
    }

    fun enqueueChecked(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        overrideFileName: String? = null,
        overrideRelativePath: String? = null,
    ) {
        val fileName = overrideFileName?.takeIf { it.isNotBlank() }
            ?: BrowserDownloads.fileNameFrom(contentDisposition, url, mimeType)
        val relPath = overrideRelativePath ?: BrowserStore.effectiveDownloadRelativePath(context)
        if (BrowserDownloads.downloadExists(context, fileName, relPath)) {
            val suggested = BrowserDownloads.nextAvailableName(context, fileName, relPath)
            duplicateDownload = if (url.startsWith("data:", ignoreCase = true)) {
                DuplicateDownloadPrompt.DataUri(url, fileName, suggested, mimeType, relPath)
            } else {
                DuplicateDownloadPrompt.Http(url, userAgent, fileName, suggested, mimeType, relPath)
            }
        } else {
            val started = BrowserDownloads.enqueueWithFileName(context, url, userAgent, fileName, mimeType, relPath)
            if (started) {
                onDownloadSaved(fileName, relPath)
                if (url.startsWith("data:", ignoreCase = true)) {
                    showDownloadFinished(fileName)
                } else {
                    showDownloadStarted(fileName)
                }
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val request = pendingDownload
        pendingDownload = null
        if (granted && request != null) {
            enqueueChecked(request.url, request.userAgent, request.contentDisposition, request.mimeType)
        }
    }

    fun proceedDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        overrideFileName: String? = null,
        overrideRelativePath: String? = null,
    ) {
        if (url.startsWith("data:", ignoreCase = true)) {
            enqueueChecked(url, userAgent, contentDisposition, mimeType, overrideFileName, overrideRelativePath)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = PendingDownload(url, userAgent, contentDisposition, mimeType)
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enqueueChecked(url, userAgent, contentDisposition, mimeType, overrideFileName, overrideRelativePath)
        }
    }

    fun startDownload(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        val fileName = BrowserDownloads.fileNameFrom(contentDisposition, url, mimeType)
        val pref = BrowserDownloads.resolvePreferenceForDownload(context, url, mimeType, fileName)
        val isPdf = mimeType?.contains("pdf", ignoreCase = true) == true ||
            fileName.endsWith(".pdf", ignoreCase = true)
        val effectiveMime = mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: BrowserDownloads.mimeTypeFromFileName(fileName)
            ?: "*/*"
        Log.d("LightningDL", "startDownload url=$url file=$fileName mime=$effectiveMime action=${pref.action} pkg=${pref.packageName}")
        when (pref.action) {
            BrowserStore.ContentAction.OPEN_IN_BROWSER -> {
                if (isPdf) {
                    onOpenPdf(url, userAgent, fileName)
                } else {
                    BrowserDownloads.downloadToCacheFile(context, url, userAgent, fileName) { file ->
                        if (file != null) {
                            webViews[activeIndex]?.loadUrl(Uri.fromFile(file).toString())
                        } else {
                            proceedDownload(url, userAgent, contentDisposition, mimeType)
                        }
                    }
                }
            }
            BrowserStore.ContentAction.OPEN_WITH_APP -> {
                BrowserDownloads.downloadToCacheFile(context, url, userAgent, fileName) { file ->
                    if (file == null) {
                        Log.w("LightningDL", "downloadToCacheFile returned null for $url")
                        onShowMessage(context.getString(R.string.menu_save_offline_failed))
                        return@downloadToCacheFile
                    }
                    val opened = AppHandlerResolver.launchFile(
                        context,
                        file,
                        effectiveMime,
                        pref.packageName,
                        pref.className,
                    )
                    if (!opened) {
                        onShowMessage(context.getString(R.string.dl_no_external_apps))
                    }
                }
            }
            BrowserStore.ContentAction.ALWAYS_ASK -> {
                downloadConfirmPrompt = PendingDownload(url, userAgent, contentDisposition, mimeType)
            }
            BrowserStore.ContentAction.SAVE_FILE -> {
                if (BrowserStore.loadDownloadAskBefore(context)) {
                    downloadConfirmPrompt = PendingDownload(url, userAgent, contentDisposition, mimeType)
                } else {
                    proceedDownload(url, userAgent, contentDisposition, mimeType)
                }
            }
        }
    }

    LaunchedEffect(focusSignal) {
        if (focusSignal > 0) {
            finding = false
            webViews[activeIndex]?.clearMatches()
            editing = true
            query = ""
            onFocusConsumed()
        }
    }

    LaunchedEffect(findSignal) {
        if (findSignal > 0) {
            editing = false
            finding = true
            findQuery = ""
            findActiveMatch = 0
            findTotalMatches = 0
            onChromeVisibleChange(true)
            onFindConsumed()
        }
    }

    fun closeFind() {
        finding = false
        findQuery = ""
        findActiveMatch = 0
        findTotalMatches = 0
        webViews[activeIndex]?.clearMatches()
        keyboard?.hide()
    }

    fun cancelEdit() {
        keyboard?.hide()
        if (editStartedFromHome) {
            onCancelFromHome()
        } else {
            editing = false
        }
    }

    BackHandler(enabled = finding) { closeFind() }
    BackHandler(enabled = isEditing) { cancelEdit() }

    val fullscreen = (webViews[activeIndex]?.tag as? WebConfig)?.fullscreen
    BackHandler(enabled = fullscreen?.showing == true) { fullscreen?.hide() }

    val topBarVisible = when (barScrollBehavior) {
        BrowserStore.BarScrollBehavior.ALWAYS_VISIBLE,
        BrowserStore.BarScrollBehavior.TOP_ALWAYS -> true
        BrowserStore.BarScrollBehavior.HIDE_BOTH,
        BrowserStore.BarScrollBehavior.BOTTOM_ALWAYS -> chromeVisible
    }

    Column(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = topBarVisible || isEditing || finding,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
        ) {
        if (finding) {
            FindInPageRow(
                query = findQuery,
                activeMatch = findActiveMatch,
                totalMatches = findTotalMatches,
                onQueryChange = { text ->
                    findQuery = text
                    val webView = webViews[activeIndex]
                    if (webView != null) {
                        webView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                            findActiveMatch = activeMatchOrdinal
                            findTotalMatches = numberOfMatches
                        }
                        if (text.isEmpty()) {
                            webView.clearMatches()
                            findActiveMatch = 0
                            findTotalMatches = 0
                        } else {
                            webView.findAllAsync(text)
                        }
                    }
                },
                onPrev = { webViews[activeIndex]?.findNext(false) },
                onNext = { webViews[activeIndex]?.findNext(true) },
                onClose = { closeFind() },
            )
        } else {
        UrlRow(
            address = displayAddress(activeTab?.url.orEmpty(), activeTab?.domain.orEmpty()),
            query = query,
            editing = isEditing && browserVisible,
            browserVisible = browserVisible,
            onQueryChange = { query = it },
            onEditStart = {
                if (!browserVisible) return@UrlRow
                onClearEditFromHome()
                editing = true
                query = activeTab?.url.orEmpty()
            },
            onCancel = { cancelEdit() },
            onSubmit = {
                val text = query.trim()
                if (text.isNotEmpty() && tabs.isNotEmpty()) {
                    val url = toUrl(text, searchEngine, customSearchEngines, activeCustomEngine)
                    onUpdateTab(activeIndex, tabs[activeIndex].copy(url = url, domain = hostOf(url)))
                    val activeSearchUrl = activeCustomEngine?.searchUrl(text) ?: searchEngine.searchUrl(text)
                    if (url == activeSearchUrl || customSearchEngines.any { it.searchUrl(text) == url }) {
                        onSearchSubmitted(text)
                    }
                    onCheckDuplicateTab(url)
                    onClearEditFromHome()
                    editing = false
                    keyboard?.hide()
                }
            },
            onRefresh = { webViews[activeIndex]?.reload() },
            readingMinutes = if (readingTimeEnabled) activeTab?.id?.let { readingTimes[it]?.minutes } else null,
        )
        }
        }
        val activeProgress = progress[activeIndex] ?: 100
        if (activeProgress < 100) {
            LinearProgressIndicator(
                progress = { activeProgress / 100f },
                color = LightningTheme.colors.primary,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(3.dp),
            )
        }
        var previousTabId by remember { mutableStateOf(activeTab?.id) }
        var outgoingTabId by remember { mutableStateOf<String?>(null) }
        val incomingAlpha = remember { Animatable(1f) }
        val currentTabId = activeTab?.id
        val isTransitionPending = currentTabId != null &&
            previousTabId != null &&
            currentTabId != previousTabId &&
            activeTab?.url?.isNotEmpty() == true &&
            tabs.any { it.id == previousTabId && it.url.isNotEmpty() }

        LaunchedEffect(currentTabId) {
            if (currentTabId != null && previousTabId != null && currentTabId != previousTabId) {
                val prevHasPage = tabs.any { it.id == previousTabId && it.url.isNotEmpty() }
                val nextHasPage = activeTab?.url?.isNotEmpty() == true
                if (prevHasPage && nextHasPage) {
                    outgoingTabId = previousTabId
                    incomingAlpha.snapTo(0.005f)
                    previousTabId = currentTabId
                    // Allow 2 frames for the target WebView to wake and draw before fading in.
                    delay(32L)
                    incomingAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                    outgoingTabId = null
                } else {
                    outgoingTabId = null
                    incomingAlpha.snapTo(1f)
                    previousTabId = currentTabId
                }
            } else {
                outgoingTabId = null
                incomingAlpha.snapTo(1f)
                previousTabId = currentTabId
            }
        }

        Box(Modifier.weight(1f)) {
            if (activeTab == null || activeTab.url.isEmpty() || (isEditing && webViews[activeIndex] == null)) {
                EmptyBrowser()
            }
            tabs.forEachIndexed { idx, tab ->
                val isActive = idx == activeIndex
                val isOutgoing = (tab.id == outgoingTabId) || (isTransitionPending && tab.id == previousTabId)
                val isVisibleLayer = isActive || isOutgoing
                val shouldMount = tab.url.isNotEmpty() && (!isEditing || webViews[idx] != null) &&
                    (isVisibleLayer || webViews[idx] != null)
                if (shouldMount) {
                    key(tab.id) {
                        val layerZ = when {
                            isActive -> 1f
                            isOutgoing -> 0.5f
                            else -> 0f
                        }
                        BrowserPage(
                            tab = tab,
                            index = idx,
                            isActive = isActive,
                            isVisibleLayer = isVisibleLayer,
                            adBlocking = adBlocking,
                            dnsEndpoint = dnsEndpoint,
                            bypassTrusted = bypassTrusted,
                            readingTimeEnabled = readingTimeEnabled,
                            webViews = webViews,
                            loadedUrls = loadedUrls,
                            onProgress = { progress[idx] = it },
                            onReadingTime = { estimate ->
                                readingTimes[tab.id] = estimate
                            },
                            onNavChange = { back, forward ->
                                val next = TabNavState(back, forward)
                                if (navStates[idx] != next) navStates[idx] = next
                            },
                            onScrolled = { scrollY, delta ->
                                if (!isActive) return@BrowserPage
                                if (barScrollBehavior == BrowserStore.BarScrollBehavior.ALWAYS_VISIBLE) return@BrowserPage
                                val now = System.currentTimeMillis()
                                if (now - lastToggle[0] > CHROME_SETTLE_MS && delta != 0) {
                                    if (scrollY <= 0) {
                                        scrollAccumulator[0] = 0
                                        if (!chromeVisible) {
                                            lastToggle[0] = now
                                            onChromeVisibleChange(true)
                                        }
                                    } else {
                                        if ((delta > 0) != (scrollAccumulator[0] > 0)) scrollAccumulator[0] = 0
                                        scrollAccumulator[0] += delta
                                        if (scrollAccumulator[0] > CHROME_SCROLL_THRESHOLD && chromeVisible) {
                                            scrollAccumulator[0] = 0
                                            lastToggle[0] = now
                                            onChromeVisibleChange(false)
                                        } else if (scrollAccumulator[0] < -CHROME_SCROLL_THRESHOLD && !chromeVisible) {
                                            scrollAccumulator[0] = 0
                                            lastToggle[0] = now
                                            onChromeVisibleChange(true)
                                        }
                                    }
                                }
                            },
                            onUrlChange = { url ->
                                if (url.isNotBlank() && url != "about:blank") {
                                    val prev = loadedUrls[idx]
                                    loadedUrls[idx] = url
                                    val current = tabs.getOrNull(idx) ?: tab
                                    val isLocal = url.startsWith("content://") || url.startsWith("file://")
                                    val domain = if (isLocal) current.domain else hostOf(url)
                                    onUpdateTab(idx, current.copy(url = url, domain = domain))
                                    if (isActive && !isLocal && prev != url) {
                                        onCheckDuplicateTab(url)
                                    }
                                }
                            },
                            onDuplicateCheckCandidate = { candidate ->
                                val isLocal = candidate.startsWith("content://") || candidate.startsWith("file://")
                                if (isActive && !isLocal) {
                                    onCheckDuplicateTab(candidate)
                                }
                            },
                            onPageMeta = { title, domain, url ->
                                loadedUrls[idx] = url
                                val current = tabs.getOrNull(idx) ?: tab
                                val isLocal = url.startsWith("content://") || url.startsWith("file://")
                                val resolvedDomain = if (isLocal) current.domain else domain
                                val resolvedTitle = if (isLocal && title.isBlank()) current.title else title
                                onUpdateTab(idx, current.copy(title = resolvedTitle, domain = resolvedDomain, url = url))
                                if (!current.private && !isLocal) onRecordVisit(title, domain)
                            },
                            onDownload = ::startDownload,
                            onDownloadStart = ::showDownloadStarted,
                            onDownloadComplete = ::showDownloadFinished,
                            onDuplicateBlob = { existing, suggested, mime, temp ->
                                downloadPopup = null
                                duplicateDownload = DuplicateDownloadPrompt.BlobTemp(existing, suggested, mime, temp)
                            },
                            onBlocked = onBlocked,
                            onDnsResolved = onDnsResolved,
                            onPrintRequest = onPrintRequest,
                            onMailtoPrompt = { uri -> mailtoPromptUri = uri },
                            onHttpError = { failedUrl, statusCode ->
                                if (waybackEnabled && isActive && statusCode in listOf(404, 410, 502, 504)) {
                                    WaybackResolver.checkAvailability(failedUrl) { result ->
                                        if (waybackEnabled && result != null && (activeTab?.url == failedUrl || loadedUrls[idx] == failedUrl)) {
                                            waybackSnapshot = result
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(layerZ)
                                .graphicsLayer {
                                    alpha = when {
                                        isActive -> if (isTransitionPending) 0.005f else incomingAlpha.value
                                        isOutgoing -> 1f
                                        else -> 0f
                                    }
                                },
                        )
                    }
                }
            }
            if (isEditing && browserVisible) {
                Suggestions(
                    query = query.trim(),
                    clipboardUrl = clipboardUrl,
                    recentSearches = recentSearches,
                    liveSearchSuggestions = liveSearchSuggestions,
                    recentPages = recentPages,
                    history = history,
                    bookmarks = bookmarks,
                    pinnedSites = pinnedSites,
                    tabs = tabs,
                    activeIndex = activeIndex,
                    activeEngine = searchEngine,
                    activeCustomEngine = activeCustomEngine,
                    customSearchEngines = customSearchEngines,
                    suggestionSettings = suggestionSettings,
                    isPrivateContext = isPrivateContext,
                    onOpenClipboardUrl = { url ->
                        onDismissClipSig(ClipboardUrlHelper.clipSignature(context))
                        onClearEditFromHome()
                        editing = false
                        keyboard?.hide()
                        onOpenInNewTab(url)
                    },
                    onFillClipboardUrl = { url ->
                        query = url
                    },
                    onDismissClipboardUrl = {
                        onDismissClipSig(ClipboardUrlHelper.clipSignature(context))
                        clipboardUrl = null
                    },
                    onSearch = { term ->
                        if (tabs.isNotEmpty()) {
                            val url = activeCustomEngine?.searchUrl(term) ?: searchEngine.searchUrl(term)
                            onUpdateTab(activeIndex, tabs[activeIndex].copy(url = url, domain = hostOf(url)))
                            onSearchSubmitted(term)
                            onCheckDuplicateTab(url)
                        }
                        onClearEditFromHome()
                        editing = false
                        keyboard?.hide()
                    },
                    onSearchWithUrl = { term, url ->
                        if (tabs.isNotEmpty()) {
                            onUpdateTab(activeIndex, tabs[activeIndex].copy(url = url, domain = hostOf(url)))
                            onSearchSubmitted(term)
                            onCheckDuplicateTab(url)
                        }
                        onClearEditFromHome()
                        editing = false
                        keyboard?.hide()
                    },
                    onOpenSuggestion = { domain ->
                        if (tabs.isNotEmpty()) {
                            val url = "https://$domain"
                            onUpdateTab(activeIndex, tabs[activeIndex].copy(url = url, domain = domain))
                            onCheckDuplicateTab(url)
                        }
                        onClearEditFromHome()
                        editing = false
                        keyboard?.hide()
                    },
                    onSwitchTab = { tabIndex ->
                        onSwitchTab(tabIndex)
                        onClearEditFromHome()
                        editing = false
                        keyboard?.hide()
                    },
                    modifier = Modifier.zIndex(2f),
                )
            }
            if (activeProgress < 100 && !isEditing) {
                LightningLoadingIndicator(Modifier.fillMaxSize().zIndex(2f))
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = duplicateTabWarning != null && !isEditing && !finding && waybackSnapshot == null && downloadPopup == null,
                enter = slideInVertically(tween(170)) { it / 2 } + fadeIn(tween(150)),
                exit = slideOutVertically(tween(150)) { it / 2 } + fadeOut(tween(140)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                duplicateTabWarning?.let { warning ->
                    DuplicateTabBanner(
                        title = warning.title,
                        onSwitch = onSwitchDuplicateTab,
                        onDismiss = onDismissDuplicateTab,
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showClipboardBanner && duplicateTabWarning == null && !isEditing && !finding && waybackSnapshot == null && downloadPopup == null,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                ClipboardUrlFloatingBanner(
                    onOpen = onOpenClipboardBanner,
                    onDismiss = onDismissClipboardBanner,
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = waybackSnapshot != null && downloadPopup == null,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                waybackSnapshot?.let { snapshot ->
                    WaybackPromptBanner(
                        snapshot = snapshot,
                        onLoadSnapshot = {
                            waybackSnapshot = null
                            if (activeTab != null) {
                                loadedUrls[activeIndex] = snapshot.snapshotUrl
                                onUpdateTab(
                                    activeIndex,
                                    activeTab.copy(
                                        url = snapshot.snapshotUrl,
                                        domain = hostOf(snapshot.snapshotUrl),
                                    ),
                                )
                                webViews[activeIndex]?.loadUrl(snapshot.snapshotUrl)
                            }
                        },
                        onDismiss = { waybackSnapshot = null },
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = downloadPopup != null,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                downloadPopup?.let { popup ->
                    DownloadStatusPopup(
                        state = popup,
                        onView = {
                            downloadPopup = null
                            onOpenDownloads()
                        },
                        onDismiss = { downloadPopup = null },
                    )
                }
            }
        }
    }

    duplicateDownload?.let { prompt ->
        DuplicateDownloadDialog(
            prompt = prompt,
            onDismiss = {
                if (prompt is DuplicateDownloadPrompt.BlobTemp) runCatching { prompt.tempFile.delete() }
                duplicateDownload = null
            },
            onConfirm = {
                when (prompt) {
                    is DuplicateDownloadPrompt.Http -> {
                        if (BrowserDownloads.enqueueWithFileName(
                                context,
                                prompt.url,
                                prompt.userAgent,
                                prompt.suggestedName,
                                prompt.mimeType,
                                prompt.relativePath,
                            )
                        ) {
                            onDownloadSaved(prompt.suggestedName, prompt.relativePath)
                            showDownloadStarted(prompt.suggestedName)
                        }
                    }
                    is DuplicateDownloadPrompt.DataUri -> {
                        if (BrowserDownloads.saveDataOrBlob(
                                context,
                                prompt.dataUri,
                                prompt.suggestedName,
                                prompt.mimeType,
                                prompt.relativePath,
                            )
                        ) {
                            onDownloadSaved(prompt.suggestedName, prompt.relativePath)
                            showDownloadFinished(prompt.suggestedName)
                        }
                    }
                    is DuplicateDownloadPrompt.BlobTemp -> {
                        if (BrowserDownloads.completePendingBlob(
                                context,
                                prompt.suggestedName,
                                prompt.mimeType,
                                prompt.tempFile,
                                prompt.relativePath,
                            )
                        ) {
                            onDownloadSaved(prompt.suggestedName, prompt.relativePath)
                            showDownloadFinished(prompt.suggestedName)
                        }
                    }
                }
                duplicateDownload = null
            },
        )
    }

    downloadConfirmPrompt?.let { pending ->
        val colors = LightningTheme.colors
        val initialName = remember(pending) {
            BrowserDownloads.fileNameFrom(pending.contentDisposition, pending.url, pending.mimeType)
        }
        var draftName by remember(pending) { mutableStateOf(initialName) }
        var selectedRelativePath by remember(pending) {
            mutableStateOf(BrowserStore.effectiveDownloadRelativePath(context))
        }
        val promptFolderPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                val rel = BrowserDownloads.extractAllowedRelativePath(uri)
                if (rel != null) {
                    selectedRelativePath = rel
                } else {
                    onShowMessage(context.getString(R.string.dl_folder_invalid))
                }
            }
        }
        val quickPaths = remember {
            listOf("Download", "Download/Lightning", "Documents", "Music", "Movies", "Pictures")
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { downloadConfirmPrompt = null },
            title = { Text(stringResource(R.string.dl_prompt_title), color = colors.onSurface) },
            text = {
                Column {
                    TextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.dl_prompt_filename)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dl_save_to_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant,
                            )
                            Text(
                                text = selectedRelativePath,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { promptFolderPicker.launch(null) }) {
                            Text(stringResource(R.string.dl_folder_browse), color = colors.primary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    ) {
                        quickPaths.forEach { path ->
                            val isSelected = selectedRelativePath.equals(path, ignoreCase = true)
                            Surface(
                                onClick = { selectedRelativePath = path },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { downloadConfirmPrompt = null }) {
                    Text(stringResource(R.string.dns_custom_cancel), color = colors.primary)
                }
            },
            confirmButton = {
                Row {
                    if (pending.url.startsWith("http", ignoreCase = true) || pending.url.startsWith("data:", ignoreCase = true)) {
                        TextButton(onClick = {
                            val name = draftName.trim().ifEmpty { initialName }
                            val isPdf = pending.mimeType?.contains("pdf", ignoreCase = true) == true ||
                                name.endsWith(".pdf", ignoreCase = true)
                            downloadConfirmPrompt = null
                            if (isPdf) {
                                onOpenPdf(pending.url, pending.userAgent, name)
                            } else {
                                val effectiveMime = pending.mimeType?.takeIf {
                                    it.isNotBlank() && it != "application/octet-stream"
                                } ?: BrowserDownloads.mimeTypeFromFileName(name) ?: "*/*"
                                BrowserDownloads.downloadToCacheFile(context, pending.url, pending.userAgent, name) { file ->
                                    if (file != null) {
                                        AppHandlerResolver.launchFile(context, file, effectiveMime)
                                    }
                                }
                            }
                        }) {
                            Text(stringResource(R.string.dl_prompt_open), color = colors.primary)
                        }
                    }
                    TextButton(onClick = {
                        val finalName = draftName.trim().ifEmpty { initialName }
                        downloadConfirmPrompt = null
                        proceedDownload(
                            pending.url,
                            pending.userAgent,
                            pending.contentDisposition,
                            pending.mimeType,
                            finalName,
                            selectedRelativePath,
                        )
                    }) {
                        Text(stringResource(R.string.dl_prompt_save), color = colors.primary)
                    }
                }
            },
        )
    }

    mailtoPromptUri?.let { uri ->
        val colors = LightningTheme.colors
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mailtoPromptUri = null },
            title = { Text(stringResource(R.string.dl_prompt_open_mailto_title), color = colors.onSurface) },
            text = {
                Text(
                    text = stringResource(R.string.dl_prompt_open_mailto_body, uri.schemeSpecificPart.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            },
            dismissButton = {
                TextButton(onClick = { mailtoPromptUri = null }) {
                    Text(stringResource(R.string.dns_custom_cancel), color = colors.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mailtoPromptUri = null
                    val pref = BrowserStore.loadContentPreferences(context)["mailto"]
                    AppHandlerResolver.launchMailto(context, uri, pref?.packageName, pref?.className)
                }) {
                    Text(stringResource(R.string.dl_prompt_open), color = colors.primary)
                }
            },
        )
    }
}

@Composable
private fun DuplicateTabBanner(
    title: String,
    onSwitch: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Surface(
        onClick = {
            haptics.confirm()
            onSwitch()
        },
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            ShapedIcon(
                icon = Icons.Filled.Tab,
                shape = ExpressiveShapes.Cookie9,
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                size = 44.dp,
                iconSize = 22.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.duplicate_tab_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = {
                    haptics.confirm()
                    onSwitch()
                },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier.height(36.dp),
            ) {
                Text(
                    text = stringResource(R.string.duplicate_tab_switch),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.clipboard_url_dismiss),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ClipboardUrlFloatingBanner(
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Surface(
        onClick = {
            haptics.confirm()
            onOpen()
        },
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            ShapedIcon(
                icon = Icons.Filled.Link,
                shape = ExpressiveShapes.Cookie9,
                container = colors.primaryContainer,
                content = colors.onPrimaryContainer,
                size = 44.dp,
                iconSize = 22.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.clipboard_url_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.clipboard_url_home_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = {
                    haptics.confirm()
                    onOpen()
                },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier.height(36.dp),
            ) {
                Text(
                    text = stringResource(R.string.clipboard_url_open),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.clipboard_url_dismiss),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun WaybackPromptBanner(
    snapshot: WaybackSnapshot,
    onLoadSnapshot: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            ShapedIcon(
                icon = Icons.Filled.History,
                shape = ExpressiveShapes.Cookie9,
                container = colors.tertiaryContainer,
                content = colors.onTertiaryContainer,
                size = 44.dp,
                iconSize = 22.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.wayback_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (snapshot.formattedDate.isNotEmpty()) {
                        stringResource(R.string.wayback_message_with_date, snapshot.formattedDate)
                    } else {
                        stringResource(R.string.wayback_message)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    haptics.confirm()
                    onLoadSnapshot()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.wayback_load_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadStatusPopup(
    state: DownloadPopupState,
    onView: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LightningTheme.colors
    val iconScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (state.completed) 1.08f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
        ),
        label = "downloadIconScale",
    )
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (state.completed) 1f else 0.35f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "downloadCompleteProgress",
    )
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) {
                ShapedIcon(
                    icon = if (state.completed) Icons.Filled.CheckCircle else Icons.Filled.FileDownload,
                    shape = ExpressiveShapes.Cookie9,
                    container = if (state.completed) colors.secondaryContainer else colors.primaryContainer,
                    content = if (state.completed) colors.onSecondaryContainer else colors.onPrimaryContainer,
                    size = 44.dp,
                    iconSize = 22.dp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                androidx.compose.animation.Crossfade(
                    targetState = state.completed,
                    animationSpec = tween(220),
                    label = "downloadTitleCrossfade",
                ) { done ->
                    Text(
                        text = stringResource(
                            if (done) R.string.download_finished_title else R.string.download_started_title,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.animation.Crossfade(
                    targetState = state.completed,
                    animationSpec = tween(220),
                    label = "downloadProgressCrossfade",
                ) { done ->
                    if (done) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            color = colors.primary,
                            trackColor = colors.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round,
                            gapSize = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        LinearProgressIndicator(
                            color = colors.primary,
                            trackColor = colors.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onView,
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(R.string.download_view),
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DuplicateDownloadDialog(
    prompt: DuplicateDownloadPrompt,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LightningTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                ShapedIcon(
                    icon = Icons.Filled.FileDownload,
                    shape = ExpressiveShapes.Clover4,
                    container = colors.secondaryContainer,
                    content = colors.onSecondaryContainer,
                    size = 52.dp,
                    iconSize = 24.dp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.download_exists_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.download_exists_message, prompt.existingName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = prompt.suggestedName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = stringResource(R.string.download_exists_cancel),
                            color = colors.onSurface,
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.download_exists_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun LightningLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MorphLoader(description = stringResource(R.string.browser_loading))
    }
}

@Keep
class BlobDownloadBridge(
    private val context: Context,
    private val onStart: (fileName: String) -> Unit,
    private val onComplete: (fileName: String) -> Unit,
    private val onDuplicate: (existingName: String, suggestedName: String, mimeType: String, tempFile: File) -> Unit,
) {
    @JavascriptInterface
    fun receiveBlobChunk(
        downloadId: String,
        fileName: String,
        mimeType: String,
        base64Chunk: String,
        isLast: Boolean,
    ) {
        BrowserDownloads.saveBlobChunk(
            context,
            downloadId,
            fileName,
            mimeType,
            base64Chunk,
            isLast,
            onStart = onStart,
            onComplete = onComplete,
            onDuplicate = onDuplicate,
        )
    }

    @JavascriptInterface
    fun receiveBlobData(base64Data: String, fileName: String, mimeType: String) {
        BrowserDownloads.saveDataOrBlob(context, base64Data, fileName, mimeType)
    }
}

private const val DOWNLOAD_INTERCEPT_SCRIPT = """
(function() {
    if (window._lightningDownloadHooked) return;
    window._lightningDownloadHooked = true;

    if (window.showSaveFilePicker) {
        try {
            delete window.showSaveFilePicker;
        } catch(e) {
            window.showSaveFilePicker = undefined;
        }
    }

    var recentDownloads = {};

    function isDuplicate(url) {
        var now = Date.now();
        if (recentDownloads[url] && (now - recentDownloads[url] < 2000)) {
            return true;
        }
        recentDownloads[url] = now;
        return false;
    }

    function sendBlobChunks(blob, name, mime) {
        if (!blob) return;
        var chunkSize = 512 * 1024;
        var totalSize = blob.size || 0;
        var downloadId = 'dl_' + Date.now() + '_' + Math.random().toString(36).substring(2);
        var finalMime = mime || blob.type || '';

        if (totalSize === 0) {
            if (window.LightningBlobBridge && window.LightningBlobBridge.receiveBlobChunk) {
                window.LightningBlobBridge.receiveBlobChunk(downloadId, name || '', finalMime, '', true);
            }
            return;
        }

        var offset = 0;
        function readNext() {
            if (offset >= totalSize) return;
            var slice = blob.slice(offset, Math.min(offset + chunkSize, totalSize));
            var reader = new FileReader();
            reader.onload = function(e) {
                var dataUrl = e.target.result || '';
                var commaIdx = dataUrl.indexOf(',');
                var base64 = commaIdx >= 0 ? dataUrl.substring(commaIdx + 1) : dataUrl;
                offset += slice.size;
                var isLast = (offset >= totalSize);
                if (window.LightningBlobBridge && window.LightningBlobBridge.receiveBlobChunk) {
                    window.LightningBlobBridge.receiveBlobChunk(downloadId, name || '', finalMime, base64, isLast);
                }
                if (!isLast) {
                    readNext();
                }
            };
            reader.onerror = function(err) {
                console.error('Failed reading blob slice:', err);
            };
            reader.readAsDataURL(slice);
        }
        readNext();
    }

    function isEmbeddedElement(url) {
        if (!url) return false;
        try {
            var tags = ['iframe', 'frame', 'embed', 'object', 'video', 'audio', 'img', 'source'];
            for (var t = 0; t < tags.length; t++) {
                var els = document.getElementsByTagName(tags[t]);
                for (var i = 0; i < els.length; i++) {
                    var el = els[i];
                    var src = el.src || el.getAttribute('src') || el.data || el.currentSrc;
                    if (src === url) return true;
                }
            }
        } catch(e) {}
        return false;
    }

    function processBlobOrData(url, name, fallbackMime, fromDownloadListener) {
        if (!url) return;
        if (fromDownloadListener && isEmbeddedElement(url)) {
            return;
        }
        if (isDuplicate(url)) return;

        var effectiveName = name || window._lastDownloadFileName || '';
        var effectiveMime = fallbackMime || (window._blobTypes && window._blobTypes[url]) || '';

        if (url.startsWith('data:')) {
            try {
                fetch(url).then(function(res) {
                    return res.blob();
                }).then(function(blob) {
                    sendBlobChunks(blob, effectiveName, effectiveMime || blob.type);
                }).catch(function() {
                    var commaIdx = url.indexOf(',');
                    var base64 = commaIdx >= 0 ? url.substring(commaIdx + 1) : url;
                    var header = commaIdx >= 0 ? url.substring(5, commaIdx) : '';
                    var mime = header.split(';')[0] || effectiveMime;
                    if (window.LightningBlobBridge && window.LightningBlobBridge.receiveBlobChunk) {
                        var downloadId = 'dl_' + Date.now();
                        window.LightningBlobBridge.receiveBlobChunk(downloadId, effectiveName, mime, base64, true);
                    }
                });
            } catch(e) {
                var commaIdx = url.indexOf(',');
                var base64 = commaIdx >= 0 ? url.substring(commaIdx + 1) : url;
                if (window.LightningBlobBridge && window.LightningBlobBridge.receiveBlobChunk) {
                    var downloadId = 'dl_' + Date.now();
                    window.LightningBlobBridge.receiveBlobChunk(downloadId, effectiveName, effectiveMime, base64, true);
                }
            }
            return;
        }

        try {
            fetch(url).then(function(res) {
                return res.blob();
            }).then(function(blob) {
                sendBlobChunks(blob, effectiveName, effectiveMime || blob.type);
            }).catch(function(e) {
                fetchViaXhr(url, effectiveName, effectiveMime);
            });
        } catch(e) {
            fetchViaXhr(url, effectiveName, effectiveMime);
        }
    }

    function fetchViaXhr(url, effectiveName, effectiveMime) {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', url, true);
        xhr.responseType = 'blob';
        xhr.onload = function() {
            if (this.status === 200 || this.status === 0) {
                var blob = this.response;
                sendBlobChunks(blob, effectiveName, effectiveMime || (blob && blob.type));
            }
        };
        xhr.onerror = function() {
            console.error('Failed to fetch blob via XHR:', url);
        };
        xhr.send();
    }

    window._lightningProcessBlob = processBlobOrData;

    var origClick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function() {
        var href = this.getAttribute('href') || this.href;
        var name = this.getAttribute('download') || this.download;
        var hasDownloadAttr = this.hasAttribute('download') || (typeof this.download === 'string' && this.download !== '');
        if (name) {
            window._lastDownloadFileName = name;
        }
        if (href && (href.startsWith('blob:') || href.startsWith('data:'))) {
            if (hasDownloadAttr || name) {
                processBlobOrData(href, name, '', false);
                return;
            }
        }
        return origClick.apply(this, arguments);
    };

    document.addEventListener('click', function(e) {
        var el = e.target;
        while (el && el.tagName !== 'A') {
            el = el.parentElement;
        }
        if (el && el.tagName === 'A') {
            var href = el.getAttribute('href') || el.href;
            var name = el.getAttribute('download') || el.download;
            var hasDownloadAttr = el.hasAttribute('download') || (typeof el.download === 'string' && el.download !== '');
            if (name) {
                window._lastDownloadFileName = name;
            }
            if (href && (href.startsWith('blob:') || href.startsWith('data:'))) {
                if (hasDownloadAttr || name) {
                    e.preventDefault();
                    e.stopPropagation();
                    processBlobOrData(href, name, '', false);
                }
            }
        }
    }, true);

    if (window.URL && window.URL.createObjectURL) {
        var origCreateObjectURL = window.URL.createObjectURL;
        window.URL.createObjectURL = function(blob) {
            var url = origCreateObjectURL.apply(this, arguments);
            if (url && blob && blob.type) {
                window._blobTypes = window._blobTypes || {};
                window._blobTypes[url] = blob.type;
            }
            return url;
        };
    }
})();
"""

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun BrowserPage(
    tab: BrowserTab,
    index: Int,
    isActive: Boolean,
    isVisibleLayer: Boolean,
    adBlocking: Boolean,
    dnsEndpoint: String?,
    bypassTrusted: Boolean,
    readingTimeEnabled: Boolean,
    webViews: SnapshotStateMap<Int, WebView>,
    loadedUrls: SnapshotStateMap<Int, String>,
    onProgress: (Int) -> Unit,
    onReadingTime: (ReadingTime?) -> Unit,
    onNavChange: (Boolean, Boolean) -> Unit,
    onUrlChange: (String) -> Unit,
    onDuplicateCheckCandidate: (String) -> Unit,
    onScrolled: (Int, Int) -> Unit,
    onPageMeta: (String, String, String) -> Unit,
    onDownload: (String, String?, String?, String?) -> Unit,
    onDownloadStart: (String) -> Unit,
    onDownloadComplete: (String) -> Unit,
    onDuplicateBlob: (String, String, String, File) -> Unit,
    onBlocked: () -> Unit,
    onDnsResolved: () -> Unit,
    onHttpError: (String, Int) -> Unit,
    onPrintRequest: () -> Unit,
    onMailtoPrompt: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    AndroidView(
        factory = { context ->
            (webViews[index] ?: WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                val config = WebConfig(tab, adBlocking, dnsEndpoint, bypassTrusted).apply {
                    this.isActive = isActive
                    this.readingTimeEnabled = readingTimeEnabled
                }
                tag = config
                setOnTouchListener { _, _ -> !config.isActive }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowContentAccess = true
                settings.allowFileAccess = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                if (tab.private) {
                    settings.setSaveFormData(false)
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                }
                setBackgroundColor(android.graphics.Color.WHITE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    isForceDarkAllowed = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    settings.isAlgorithmicDarkeningAllowed = false
                }
                config.defaultUserAgent = settings.userAgentString
                applyDesktopMode(this, config, tab.desktopMode)
                val fullscreen = FullscreenVideo(context)
                config.fullscreen = fullscreen
                addJavascriptInterface(
                    BlobDownloadBridge(
                        context = context,
                        onStart = { name -> config.onDownloadStart(name) },
                        onComplete = { name -> config.onDownloadComplete(name) },
                        onDuplicate = { existing, suggested, mime, temp ->
                            config.onDuplicateBlob(existing, suggested, mime, temp)
                        },
                    ),
                    "LightningBlobBridge",
                )
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        @Keep
                        fun print() {
                            post { config.onPrintRequest() }
                        }
                    },
                    "LightningPrintBridge",
                )
                setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    config.onScrolled(scrollY, scrollY - oldScrollY)
                }
                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    if (url.startsWith("blob:", ignoreCase = true)) {
                        val fallbackName = BrowserDownloads.fileNameFrom(contentDisposition, url, mimeType)
                        val jsonUrl = JSONObject.quote(url)
                        val jsonFallbackName = JSONObject.quote(fallbackName)
                        val jsonMime = JSONObject.quote(mimeType ?: "")
                        val js = """
                            (function() {
                                if (window._lightningProcessBlob) {
                                    window._lightningProcessBlob($jsonUrl, $jsonFallbackName, $jsonMime, true);
                                }
                            })();
                        """.trimIndent()
                        post { evaluateJavascript(js, null) }
                    } else {
                        config.onDownload(url, userAgent, contentDisposition, mimeType)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        config.onProgress(newProgress)
                        if (newProgress >= 25 && view != null && !config.desktopMode) {
                            view.evaluateJavascript(ENABLE_ZOOM_SCRIPT, null)
                        }
                    }

                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        result?.confirm()
                        return true
                    }

                    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        result?.confirm()
                        return true
                    }

                    override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                        result?.confirm(defaultValue ?: "")
                        return true
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (view == null || callback == null) {
                            callback?.onCustomViewHidden()
                            return
                        }
                        fullscreen.show(view, callback)
                    }

                    override fun onHideCustomView() {
                        fullscreen.hide()
                    }
                }
                webViewClient = object : WebViewClient() {
                    private var lastReadingTimeUrl: String? = null

                    private fun scheduleReadingTime(view: WebView?, pageUrl: String?) {
                        if (!config.readingTimeEnabled || view == null || pageUrl == null) return
                        fun measure(retry: Boolean) {
                            if (!config.readingTimeEnabled || view.url != pageUrl) return
                            view.evaluateJavascript(ReadingTimeEstimator.SCRIPT) { raw ->
                                if (!config.readingTimeEnabled || view.url != pageUrl) return@evaluateJavascript
                                val estimate = ReadingTimeEstimator.parse(raw)
                                if (estimate != null) {
                                    config.onReadingTime(estimate)
                                } else if (retry) {
                                    // Give SPAs and lazy-loaded articles one more chance.
                                    view.postDelayed({ measure(false) }, 1_800L)
                                }
                            }
                        }
                        view.postDelayed({ measure(true) }, 700L)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url ?: return null
                        val host = url.host ?: return null
                        if (config.adBlocking &&
                            (AdBlocker.shouldBlock(url, hostOf(config.tab.url), request.isForMainFrame, context) ||
                                YoutubeAdBlock.isAdRequest(url))
                        ) {
                            config.onBlocked()
                            return WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        if (config.adBlocking && request.isForMainFrame && request.method == "GET" &&
                            YoutubeAdBlock.isYoutubeHost(host)
                        ) {
                            val page = YoutubeAdBlock.page(
                                url.toString(),
                                config.userAgent ?: view?.settings?.userAgentString,
                                buildEffectHeaders(request),
                                config.dnsEndpoint,
                            )
                            if (page != null) return page
                        }
                        val endpoint = config.dnsEndpoint ?: return null
                        if (url.scheme != "https") return null
                        val method = request.method
                        if (method != "GET" && method != "HEAD") return null
                        if (request.requestHeaders.containsKey("Range")) return null
                        val accept = request.requestHeaders["Accept"]
                        if (accept != null && accept.contains("text/event-stream", ignoreCase = true)) return null
                        if (host.isIpAddress()) return null
                        if (config.bypassTrusted && (host.isLocalHost() || host.isPrivateRange())) return null
                        val ua = config.userAgent ?: return null
                        val responseHeaders = buildEffectHeaders(request)
                        Log.d("LightningDoH", "intercept $method ${url} -> $host")
                        val response = PinnedHttp.get(
                            url = url.toString(),
                            dnsEndpoint = endpoint,
                            userAgent = ua,
                            headers = responseHeaders,
                            method = method,
                            followRedirects = !request.isForMainFrame,
                        ) ?: return null
                        config.onDnsResolved()
                        if (response.statusCode in 300..399) {
                            val location = response.headers["location"] ?: return null
                            val nextUrl = runCatching {
                                java.net.URL(java.net.URL(url.toString()), location).toString()
                            }.getOrNull() ?: return null
                            val quoted = JSONObject.quote(nextUrl)
                            val html = "<!doctype html><script>location.replace($quoted);</script>"
                            Log.d("LightningDoH", "redirect $host -> $nextUrl")
                            return WebResourceResponse(
                                "text/html",
                                "UTF-8",
                                200,
                                "OK",
                                emptyMap(),
                                ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)),
                            )
                        }
                        Log.d("LightningDoH", "served $host via pinned TLS")
                        if (request.isForMainFrame && response.statusCode in listOf(404, 410, 502, 504)) {
                            config.onHttpError(url.toString(), response.statusCode)
                        }
                        if (response.statusCode >= 400 && response.body.isEmpty()) return null
                        val (mime, charset) = parseContentType(response.headers["content-type"])
                        return WebResourceResponse(
                            mime,
                            charset,
                            response.statusCode,
                            "OK",
                            response.headers,
                            ByteArrayInputStream(response.body),
                        )
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val target = request?.url ?: return false
                        if (target.scheme.equals("mailto", ignoreCase = true)) {
                            val pref = BrowserStore.loadContentPreferences(context)["mailto"]
                            if (pref?.action == BrowserStore.ContentAction.ALWAYS_ASK) {
                                view?.post { config.onMailtoPrompt(target) }
                            } else {
                                AppHandlerResolver.launchMailto(context, target, pref?.packageName, pref?.className)
                            }
                            return true
                        }
                        if (target.scheme.equals("intent", ignoreCase = true)) {
                            return handleIntentUri(context, view, target.toString())
                        }
                        val lowerScheme = target.scheme.orEmpty().lowercase()
                        if (lowerScheme in setOf("tel", "sms", "smsto", "geo", "market")) {
                            runCatching {
                                val viewIntent = Intent(Intent.ACTION_VIEW, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (context.packageManager.resolveActivity(viewIntent, 0) != null) {
                                    context.startActivity(viewIntent)
                                }
                            }
                            return true
                        }
                        if (request.isForMainFrame && (target.scheme.equals("http", true) || target.scheme.equals("https", true))) {
                            val lastSeg = target.lastPathSegment.orEmpty()
                            val ext = lastSeg.substringAfterLast('.', "").lowercase()
                            val ruleKey = when (ext) {
                                "pdf" -> "pdf"
                                "jpg", "jpeg", "png", "gif", "bmp", "heic", "heif", "ico" -> "jpeg"
                                "jxl" -> "jxl"
                                "avif" -> "avif"
                                "webp" -> "webp"
                                "svg" -> "svg"
                                "xml" -> "xml"
                                else -> null
                            }
                            if (ruleKey != null) {
                                val pref = BrowserStore.loadContentPreferences(context)[ruleKey]
                                if (pref != null && pref.action != BrowserStore.ContentAction.OPEN_IN_BROWSER) {
                                    val mime = BrowserDownloads.mimeTypeFromFileName(lastSeg)
                                    val ua = view?.settings?.userAgentString
                                    view?.post { config.onDownload(target.toString(), ua, null, mime) }
                                    return true
                                }
                            }
                        }
                        if (config.adBlocking && request.isForMainFrame && !(request.hasGesture() && !request.isRedirect)) {
                            val host = target.host.orEmpty()
                            if (AdBlocker.isAdHost(host, context)) {
                                config.onBlocked()
                                return true
                            }
                            val scheme = target.scheme.orEmpty()
                            if (scheme != "http" && scheme != "https" && scheme != "about" && scheme != "blob" && scheme != "data") {
                                config.onBlocked()
                                return true
                            }
                        }
                        if (request.isForMainFrame && !request.isRedirect) {
                            view?.post { config.onDuplicateCheckCandidate(target.toString()) }
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (url != null && (url.startsWith("http://", true) || url.startsWith("https://", true))) {
                            val parsed = runCatching { Uri.parse(url) }.getOrNull()
                            val lastSeg = parsed?.lastPathSegment.orEmpty()
                            val ext = lastSeg.substringAfterLast('.', "").lowercase()
                            if (ext in setOf("pdf", "jxl", "avif", "webp", "svg", "xml", "jpg", "jpeg", "png", "gif", "bmp", "heic", "heif", "ico")) {
                                val mime = BrowserDownloads.mimeTypeFromFileName(lastSeg)
                                val pref = BrowserDownloads.resolvePreferenceForDownload(context, url, mime, lastSeg)
                                if (pref.action != BrowserStore.ContentAction.OPEN_IN_BROWSER) {
                                    Log.d("LightningDL", "onPageStarted intercept url=$url ext=$ext action=${pref.action}")
                                    val ua = view?.settings?.userAgentString
                                    view?.stopLoading()
                                    if (view?.canGoBack() == true) view.goBack()
                                    view?.post { config.onDownload(url, ua, null, mime) }
                                    return
                                }
                            }
                        }
                        lastReadingTimeUrl = url
                        config.onReadingTime(null)
                        url?.let { config.onUrlChange(it) }
                        config.onNavChange(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                        if (config.desktopMode && view != null) {
                            view.evaluateJavascript(desktopViewportScript(view), null)
                        } else {
                            view?.evaluateJavascript(ENABLE_ZOOM_SCRIPT, null)
                        }
                        view?.evaluateJavascript(IMAGE_FIT_SCRIPT, null)
                        if (url != null && (url.startsWith("http://", true) || url.startsWith("https://", true))) {
                            view?.evaluateJavascript("document && document.contentType ? document.contentType : ''") { raw ->
                                val ct = raw?.trim('"')?.lowercase().orEmpty()
                                if (ct.startsWith("image/") || ct == "application/pdf") {
                                    val fileName = BrowserDownloads.fileNameFrom(null, url, ct)
                                    val pref = BrowserDownloads.resolvePreferenceForDownload(context, url, ct, fileName)
                                    if (pref.action == BrowserStore.ContentAction.OPEN_WITH_APP) {
                                        Log.d("LightningDL", "onPageFinished intercept contentType=$ct url=$url")
                                        val ua = view.settings.userAgentString
                                        if (view.canGoBack()) view.goBack()
                                        config.onDownload(url, ua, null, ct)
                                    }
                                }
                            }
                        }
                        view?.evaluateJavascript(DOWNLOAD_INTERCEPT_SCRIPT, null)
                        view?.evaluateJavascript(
                            "window.print = function() { if (window.LightningPrintBridge) window.LightningPrintBridge.print(); };",
                            null,
                        )
                        if (config.adBlocking) view?.evaluateJavascript(CosmeticFilter.SCRIPT, null)
                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        if (config.desktopMode && view != null) {
                            view.evaluateJavascript(desktopViewportScript(view), null)
                        } else {
                            view?.evaluateJavascript(ENABLE_ZOOM_SCRIPT, null)
                        }
                        view?.post { config.onNavChange(view.canGoBack(), view.canGoForward()) }
                        view?.invalidate()
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true && errorResponse != null) {
                            val reqUrl = request.url?.toString().orEmpty()
                            config.onHttpError(reqUrl, errorResponse.statusCode)
                        }
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        url?.let { config.onUrlChange(it) }
                        config.onNavChange(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                        view?.post { config.onNavChange(view.canGoBack(), view.canGoForward()) }
                        if (!isReload && url != null && url != lastReadingTimeUrl) {
                            lastReadingTimeUrl = url
                            config.onReadingTime(null)
                            scheduleReadingTime(view, url)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        lastReadingTimeUrl = url
                        scheduleReadingTime(view, url)
                        if (config.desktopMode && view != null) {
                            view.evaluateJavascript(desktopViewportScript(view), null)
                        } else {
                            view?.evaluateJavascript(ENABLE_ZOOM_SCRIPT, null)
                        }
                        view?.evaluateJavascript(IMAGE_FIT_SCRIPT, null)
                        if (url != null && (url.startsWith("http://", true) || url.startsWith("https://", true))) {
                            view?.evaluateJavascript("document && document.contentType ? document.contentType : ''") { raw ->
                                val ct = raw?.trim('"')?.lowercase().orEmpty()
                                if (ct.startsWith("image/") || ct == "application/pdf") {
                                    val fileName = BrowserDownloads.fileNameFrom(null, url, ct)
                                    val pref = BrowserDownloads.resolvePreferenceForDownload(context, url, ct, fileName)
                                    if (pref.action == BrowserStore.ContentAction.OPEN_WITH_APP) {
                                        Log.d("LightningDL", "onPageFinished intercept contentType=$ct url=$url")
                                        val ua = view.settings.userAgentString
                                        if (view.canGoBack()) view.goBack()
                                        config.onDownload(url, ua, null, ct)
                                    }
                                }
                            }
                        }
                        view?.evaluateJavascript(DOWNLOAD_INTERCEPT_SCRIPT, null)
                        view?.evaluateJavascript(
                            "window.print = function() { if (window.LightningPrintBridge) window.LightningPrintBridge.print(); };",
                            null,
                        )
                        if (config.adBlocking) view?.evaluateJavascript(CosmeticFilter.SCRIPT, null)
                        val current = config.tab
                        val host = runCatching { Uri.parse(url).host }.getOrNull()?.takeIf { it.isNotEmpty() } ?: current.domain
                        config.onPageMeta(
                            view?.title?.takeIf { it.isNotBlank() } ?: current.title,
                            host,
                            url ?: current.url,
                        )
                        config.onNavChange(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                    }
                }
            }.also {
                webViews[index] = it
                if (tab.url.isNotEmpty() && loadedUrls[index] != tab.url) {
                    loadedUrls[index] = tab.url
                    it.loadUrl(tab.url)
                }
            }).also { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.visibility = if (isVisibleLayer) View.VISIBLE else View.INVISIBLE
                if (isActive) wv.onResume() else if (!isVisibleLayer) wv.onPause()
            }
        },
        update = { wv ->
            val targetVis = if (isVisibleLayer) View.VISIBLE else View.INVISIBLE
            if (wv.visibility != targetVis) {
                wv.visibility = targetVis
                if (targetVis == View.VISIBLE) {
                    wv.invalidate()
                }
            }
            if (isActive) {
                wv.onResume()
            } else if (!isVisibleLayer) {
                wv.onPause()
            }
            (wv.tag as? WebConfig)?.let { config ->
                val desktopChanged = config.desktopMode != tab.desktopMode
                config.tab = tab
                config.isActive = isActive
                config.adBlocking = adBlocking
                config.dnsEndpoint = dnsEndpoint
                config.bypassTrusted = bypassTrusted
                config.readingTimeEnabled = readingTimeEnabled
                config.onProgress = onProgress
                config.onReadingTime = onReadingTime
                config.onNavChange = onNavChange
                config.onScrolled = onScrolled
                config.onUrlChange = onUrlChange
                config.onDuplicateCheckCandidate = onDuplicateCheckCandidate
                config.onPageMeta = onPageMeta
                config.onDownload = onDownload
                config.onDownloadStart = onDownloadStart
                config.onDownloadComplete = onDownloadComplete
                config.onDuplicateBlob = onDuplicateBlob
                config.onBlocked = onBlocked
                config.onDnsResolved = onDnsResolved
                config.onHttpError = onHttpError
                config.onPrintRequest = onPrintRequest
                config.onMailtoPrompt = onMailtoPrompt
                if (desktopChanged) {
                    applyDesktopMode(wv, config, tab.desktopMode)
                    if (tab.url.isNotEmpty()) {
                        wv.reload()
                    }
                }
            }
        },
        modifier = modifier,
    )
    LaunchedEffect(index, tab.id, isActive, webViews[index]) {
        webViews[index]?.let { wv ->
            onNavChange(wv.canGoBack(), wv.canGoForward())
        }
    }
    LaunchedEffect(index, tab.url, webViews[index]) {
        val view = webViews[index] ?: return@LaunchedEffect
        val target = tab.url
        if (target.isNotEmpty() && loadedUrls[index] != target) {
            loadedUrls[index] = target
            view.loadUrl(target)
            view.invalidate()
        }
    }
}

private fun buildEffectHeaders(request: WebResourceRequest): Map<String, String> {
    val headers = LinkedHashMap<String, String>()
    request.requestHeaders.forEach { (key, value) ->
        val lower = key.lowercase()
        if (lower in setOf(
                "host",
                "connection",
                "user-agent",
                "accept-encoding",
                "cookie",
                "range",
                "content-length",
                "content-type",
            )
        ) {
            return@forEach
        }
        headers[key] = value
    }
    if (headers.none { it.key.equals("accept", true) }) headers["Accept"] = "*/*"
    CookieManager.getInstance().getCookie(request.url.toString())?.let { cookie ->
        headers["Cookie"] = cookie
    }
    return headers
}

private fun parseContentType(contentType: String?): Pair<String, String?> {
    if (contentType.isNullOrBlank()) return "text/html" to null
    val parts = contentType.split(";")
    val mime = parts.first().trim().ifBlank { "text/html" }
    val charset = parts.drop(1)
        .firstOrNull { it.trim().startsWith("charset=", ignoreCase = true) }
        ?.substringAfter("=")
        ?.trim()
        ?.removeSurrounding("\"")
    return mime to charset
}

private fun String.isIpAddress(): Boolean {
    if (count { it == ':' } >= 2) return true
    if (count { it == '.' } == 3) {
        val parts = split('.')
        return parts.all { part ->
            part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull() in 0..255
        }
    }
    return false
}

private fun String.isLocalHost(): Boolean =
    this == "localhost" || this == "localhost.localdomain" || endsWith(".localhost")

private fun String.isPrivateRange(): Boolean = when {
    startsWith("10.") -> true
    startsWith("192.168.") -> true
    startsWith("127.") -> true
    startsWith("172.") -> runCatching {
        val second = substringAfter(".").substringBefore(".").toInt()
        second in 16..31
    }.getOrDefault(false)
    else -> false
}

@Composable
private fun UrlRow(
    address: String,
    query: String,
    editing: Boolean,
    browserVisible: Boolean = true,
    onQueryChange: (String) -> Unit,
    onEditStart: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    onRefresh: () -> Unit,
    readingMinutes: Int? = null,
) {
    val colors = LightningTheme.colors
    val focusRequester = remember { FocusRequester() }
    var field by remember(editing) {
        mutableStateOf(TextFieldValue(query, TextRange(0, query.length)))
    }
    LaunchedEffect(query) {
        if (field.text != query) field = TextFieldValue(query, TextRange(query.length))
    }
    if (editing) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        if (editing) {
            TextField(
                value = field,
                onValueChange = {
                    field = it
                    onQueryChange(it.text)
                },
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.home_search_placeholder),
                        color = colors.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = colors.onSurfaceVariant)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.browser_clear_query),
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerHigh,
                    unfocusedContainerColor = colors.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colors.primary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.width(2.dp))
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.dns_custom_cancel),
                    color = colors.primary,
                )
            }
        } else {
            Surface(
                onClick = { if (browserVisible) onEditStart() },
                enabled = browserVisible,
                color = colors.surfaceContainerHigh,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 6.dp),
                ) {
                    ShapedIcon(
                        Icons.Filled.FlashOn,
                        ExpressiveShapes.Cookie9,
                        colors.primaryContainer,
                        colors.onPrimaryContainer,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = address.ifEmpty { stringResource(R.string.home_search_placeholder) },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (address.isNotEmpty()) colors.onSurface else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (readingMinutes != null) {
                        Surface(
                            color = colors.primaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.browser_reading_time, readingMinutes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onPrimaryContainer,
                                    maxLines = 1,
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    if (address.isNotEmpty()) {
                        TonalIconButton(
                            Icons.Filled.Refresh,
                            stringResource(R.string.browser_refresh),
                            onRefresh,
                            size = 40.dp,
                            iconSize = 20.dp,
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FindInPageRow(
    query: String,
    activeMatch: Int,
    totalMatches: Int,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LightningTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.find_in_page_placeholder),
                    color = colors.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, null, tint = colors.onSurfaceVariant)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Text(
                        text = if (totalMatches > 0) "${activeMatch + 1}/$totalMatches" else "0/0",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (totalMatches > 0) colors.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = colors.onSurface,
                fontWeight = FontWeight.Medium,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (totalMatches > 0) onNext() }),
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainerHigh,
                unfocusedContainerColor = colors.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = colors.primary,
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        Spacer(Modifier.width(4.dp))
        TonalIconButton(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = stringResource(R.string.find_in_page_prev),
            onClick = onPrev,
            enabled = totalMatches > 0,
            size = 40.dp,
            iconSize = 22.dp,
        )
        TonalIconButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.find_in_page_next),
            onClick = onNext,
            enabled = totalMatches > 0,
            size = 40.dp,
            iconSize = 22.dp,
        )
        TonalIconButton(
            icon = Icons.Filled.Close,
            contentDescription = stringResource(R.string.find_in_page_close),
            onClick = onClose,
            size = 40.dp,
            iconSize = 20.dp,
        )
    }
}

@Composable
private fun Suggestions(
    query: String,
    clipboardUrl: String?,
    recentSearches: List<String>,
    liveSearchSuggestions: List<String>,
    recentPages: List<RecentPage>,
    history: List<HistoryEntry>,
    bookmarks: List<Bookmark>,
    pinnedSites: List<PinnedSite>,
    tabs: List<BrowserTab>,
    activeIndex: Int,
    activeEngine: SearchEngine,
    activeCustomEngine: CustomSearchEngine?,
    customSearchEngines: List<CustomSearchEngine>,
    suggestionSettings: BrowserStore.SearchSuggestionSettings,
    isPrivateContext: Boolean,
    onOpenClipboardUrl: (String) -> Unit,
    onFillClipboardUrl: (String) -> Unit,
    onDismissClipboardUrl: () -> Unit,
    onSearch: (String) -> Unit,
    onSearchWithUrl: (String, String) -> Unit,
    onOpenSuggestion: (String) -> Unit,
    onSwitchTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val matches = { text: String -> query.isBlank() || text.contains(query, ignoreCase = true) }

    val activeEngineLabel = activeCustomEngine?.name ?: stringResource(activeEngine.labelRes)
    val recentSearchLabel = stringResource(R.string.suggestion_recent_search)

    val directSearchEntry = if (query.isNotEmpty()) {
        listOf(
            Suggestion(
                title = stringResource(R.string.browser_suggestion_search, query),
                domain = activeEngineLabel,
                icon = Icons.Filled.Search,
                tone = ChipTone.PRIMARY,
                action = SuggestionAction.Search(query),
            ),
        )
    } else {
        emptyList()
    }

    val recentSearchItems = if (suggestionSettings.suggestRecentSearches) {
        recentSearches
            .filter { matches(it) && !it.equals(query, ignoreCase = true) }
            .take(5)
            .map { term ->
                Suggestion(
                    title = term,
                    domain = recentSearchLabel,
                    icon = Icons.Filled.History,
                    tone = ChipTone.PRIMARY,
                    action = SuggestionAction.Search(term),
                )
            }
    } else {
        emptyList()
    }

    val liveSearchItems = if (
        suggestionSettings.showSearchSuggestions &&
        (!isPrivateContext || suggestionSettings.searchSuggestionsInPrivate) &&
        query.isNotEmpty()
    ) {
        val seenTerms = recentSearchItems.map { it.title.lowercase() }.toSet()
        liveSearchSuggestions
            .filter { it.lowercase() !in seenTerms && !it.equals(query, ignoreCase = true) }
            .map { term ->
                Suggestion(
                    title = term,
                    domain = stringResource(R.string.browser_suggestion_search, term),
                    icon = Icons.Filled.Search,
                    tone = ChipTone.PRIMARY,
                    action = SuggestionAction.Search(term),
                )
            }
    } else {
        emptyList()
    }

    val openTabItems = if (suggestionSettings.suggestOpenTabs) {
        tabs.mapIndexedNotNull { idx, tab ->
            if (idx == activeIndex || tab.url.isBlank()) return@mapIndexedNotNull null
            if (!matches(tab.title) && !matches(tab.domain) && !matches(tab.url)) return@mapIndexedNotNull null
            Suggestion(
                title = tab.title.ifBlank { tab.domain.ifBlank { tab.url } },
                domain = stringResource(R.string.suggestion_switch_to_tab, tab.domain.ifBlank { tab.url }),
                icon = Icons.Filled.Tab,
                tone = ChipTone.SECONDARY,
                action = SuggestionAction.SwitchTab(idx),
            )
        }
    } else {
        emptyList()
    }

    val bookmarkItems = if (suggestionSettings.suggestBookmarks) {
        bookmarks
            .filter { matches(it.title) || matches(it.domain) }
            .map {
                Suggestion(
                    title = it.title,
                    domain = stringResource(R.string.suggestion_bookmark, it.domain),
                    icon = Icons.Filled.Bookmark,
                    tone = it.tone,
                    action = SuggestionAction.OpenDomain(it.domain),
                )
            }
    } else {
        emptyList()
    }

    val shortcutItems = if (suggestionSettings.suggestShortcuts) {
        pinnedSites
            .filter { matches(it.title) || matches(it.domain) }
            .map {
                Suggestion(
                    title = it.title,
                    domain = stringResource(R.string.suggestion_shortcut, it.domain),
                    icon = it.icon,
                    tone = it.tone,
                    action = SuggestionAction.OpenDomain(it.domain),
                )
            }
    } else {
        emptyList()
    }

    val historyItems = if (suggestionSettings.suggestHistory) {
        val rawHistory = if (history.isNotEmpty()) {
            history.map { Triple(it.title, it.domain, it.icon to it.tone) }
        } else {
            recentPages.map { Triple(it.title, it.domain, it.icon to it.tone) }
        }
        rawHistory
            .distinctBy { it.second }
            .filter { (title, domain, _) -> matches(title) || matches(domain) }
            .take(8)
            .map { (title, domain, iconTone) ->
                Suggestion(
                    title = title,
                    domain = domain,
                    icon = iconTone.first,
                    tone = iconTone.second,
                    action = SuggestionAction.OpenDomain(domain),
                )
            }
    } else {
        emptyList()
    }

    val searchGroup = recentSearchItems + liveSearchItems
    val localGroup = (openTabItems + bookmarkItems + shortcutItems + historyItems)
        .distinctBy {
            when (val act = it.action) {
                is SuggestionAction.SwitchTab -> "tab:${act.tabIndex}"
                is SuggestionAction.OpenDomain -> "domain:${act.domain}"
                else -> it.title
            }
        }

    val entries = buildList {
        addAll(directSearchEntry)
        if (suggestionSettings.searchSuggestionsBeforeHistory) {
            addAll(searchGroup)
            addAll(localGroup)
        } else {
            addAll(localGroup)
            addAll(searchGroup)
        }
    }

    val engineOptions: List<Pair<String, String>> = if (suggestionSettings.suggestSearchEngines && query.isNotEmpty()) {
        buildList {
            SearchEngine.entries.forEach { engine ->
                if (activeCustomEngine != null || engine != activeEngine) {
                    add(stringResource(engine.labelRes) to engine.searchUrl(query))
                }
            }
            customSearchEngines.forEach { custom ->
                if (custom.id != activeCustomEngine?.id) {
                    add(custom.name to custom.searchUrl(query))
                }
            }
        }
    } else {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 16.dp),
    ) {
        if (clipboardUrl != null && suggestionSettings.suggestClipboard) {
            ClipboardUrlBanner(
                url = clipboardUrl,
                onOpen = { onOpenClipboardUrl(clipboardUrl) },
                onFill = { onFillClipboardUrl(clipboardUrl) },
                onDismiss = onDismissClipboardUrl,
            )
            Spacer(Modifier.height(12.dp))
        }
        if (engineOptions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.suggestion_search_engines_header),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
            ) {
                engineOptions.forEach { (label, url) ->
                    Surface(
                        onClick = { onSearchWithUrl(query, url) },
                        color = colors.surfaceContainerHigh,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurface,
                            )
                        }
                    }
                }
            }
        }
        if (entries.isNotEmpty()) {
            GroupedColumn(entries.size) { index, shape ->
                val entry = entries[index]
                SuggestionRow(
                    icon = entry.icon,
                    title = entry.title,
                    subtitle = entry.domain,
                    shape = shape,
                    index = index,
                    onClick = {
                        when (val act = entry.action) {
                            is SuggestionAction.Search -> onSearch(act.term)
                            is SuggestionAction.OpenDomain -> onOpenSuggestion(act.domain)
                            is SuggestionAction.SwitchTab -> onSwitchTab(act.tabIndex)
                            is SuggestionAction.SearchWithUrl -> onSearchWithUrl(act.query, act.url)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ClipboardUrlBanner(
    url: String,
    onOpen: () -> Unit,
    onFill: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Surface(
        onClick = {
            haptics.confirm()
            onOpen()
        },
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHighest,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        ) {
            ShapedIcon(
                icon = Icons.Filled.Link,
                shape = ExpressiveShapes.Cookie9,
                container = colors.primaryContainer,
                content = colors.onPrimaryContainer,
                size = 40.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.clipboard_url_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = {
                    haptics.click()
                    onFill()
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.NorthWest,
                    contentDescription = stringResource(R.string.clipboard_url_fill),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Button(
                onClick = {
                    haptics.confirm()
                    onOpen()
                },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier.height(36.dp),
            ) {
                Text(
                    text = stringResource(R.string.clipboard_url_open),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.clipboard_url_dismiss),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    shape: androidx.compose.ui.graphics.Shape,
    index: Int,
    onClick: () -> Unit,
) {
    val colors = LightningTheme.colors
    val tone = colors.tone(index)
    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            ShapedIcon(
                icon,
                ExpressiveShapes.cycle(index),
                tone.container,
                tone.content,
                size = 40.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBrowser() {
    val colors = LightningTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ShapedIcon(
            Icons.Filled.FlashOn,
            ExpressiveShapes.Cookie9,
            colors.primaryContainer,
            colors.onPrimaryContainer,
            size = 96.dp,
            iconSize = 44.dp,
            contentDescription = stringResource(R.string.home_search_placeholder),
        )
    }
}

private sealed class SuggestionAction {
    data class Search(val term: String) : SuggestionAction()
    data class OpenDomain(val domain: String) : SuggestionAction()
    data class SwitchTab(val tabIndex: Int) : SuggestionAction()
    data class SearchWithUrl(val query: String, val url: String) : SuggestionAction()
}

private data class Suggestion(
    val title: String,
    val domain: String,
    val icon: ImageVector,
    val tone: ChipTone,
    val action: SuggestionAction,
)

private data class DownloadPopupState(
    val fileName: String,
    val completed: Boolean,
    val id: Long = System.nanoTime(),
)

private data class PendingDownload(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimeType: String?,
)

private sealed class DuplicateDownloadPrompt(
    val existingName: String,
    val suggestedName: String,
    val relativePath: String = "Download",
) {
    class Http(
        val url: String,
        val userAgent: String?,
        existingName: String,
        suggestedName: String,
        val mimeType: String?,
        relativePath: String = "Download",
    ) : DuplicateDownloadPrompt(existingName, suggestedName, relativePath)

    class DataUri(
        val dataUri: String,
        existingName: String,
        suggestedName: String,
        val mimeType: String?,
        relativePath: String = "Download",
    ) : DuplicateDownloadPrompt(existingName, suggestedName, relativePath)

    class BlobTemp(
        existingName: String,
        suggestedName: String,
        val mimeType: String,
        val tempFile: File,
        relativePath: String = "Download",
    ) : DuplicateDownloadPrompt(existingName, suggestedName, relativePath)
}

private fun findKeywordMatch(
    text: String,
    customEngines: List<CustomSearchEngine>,
): Pair<CustomSearchEngine, String>? {
    val trimmed = text.trim()
    for (engine in customEngines) {
        val rawKw = engine.keyword.trim()
        if (rawKw.isEmpty()) continue
        val variants = listOf(
            rawKw,
            rawKw.removePrefix("@"),
            "@" + rawKw.removePrefix("@"),
        ).filter { it.isNotEmpty() }.distinct()

        for (v in variants) {
            if (trimmed.equals(v, ignoreCase = true)) {
                return engine to ""
            }
            if (trimmed.startsWith("$v ", ignoreCase = true)) {
                val query = trimmed.substring(v.length + 1).trim()
                return engine to query
            }
        }
    }
    return null
}

private fun toUrl(
    text: String,
    engine: SearchEngine,
    customEngines: List<CustomSearchEngine> = emptyList(),
    activeCustomEngine: CustomSearchEngine? = null,
): String {
    val trimmed = text.trim()
    val keywordMatch = findKeywordMatch(trimmed, customEngines)
    if (keywordMatch != null) {
        val (matchedEngine, query) = keywordMatch
        return matchedEngine.searchUrl(query)
    }
    val hasScheme = trimmed.startsWith("http://") || trimmed.startsWith("https://")
    val looksLikeUrl = trimmed.contains(".") && !trimmed.contains(" ") && !hasScheme
    return when {
        hasScheme -> trimmed
        looksLikeUrl -> "https://$trimmed"
        activeCustomEngine != null -> activeCustomEngine.searchUrl(trimmed)
        else -> engine.searchUrl(trimmed)
    }
}

private fun hostOf(url: String): String =
    runCatching { Uri.parse(url).host }.getOrNull() ?: ""

private fun displayAddress(url: String, domain: String): String {
    if (url.isBlank()) return domain
    return url.substringAfter("://")
        .removePrefix("www.")
        .trimEnd('/')
}

private fun handleIntentUri(context: Context, view: WebView?, uriString: String): Boolean {
    val intent = runCatching { Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME) }.getOrNull()
        ?: return false
    intent.addCategory(Intent.CATEGORY_BROWSABLE)
    intent.selector = null
    if (intent.`package` == context.packageName && intent.component != null) {
        intent.component = null
    }
    val launched = runCatching {
        val resolved = context.packageManager.resolveActivity(intent, 0)
        if (resolved != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            false
        }
    }.getOrDefault(false)
    if (launched) return true

    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
    if (!fallbackUrl.isNullOrBlank() &&
        (fallbackUrl.startsWith("http://", true) || fallbackUrl.startsWith("https://", true))
    ) {
        view?.post { view.loadUrl(fallbackUrl) }
    }
    return true
}
