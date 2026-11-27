package com.lightning.browser.ui.browser

import android.Manifest
import android.content.Context
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
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
import kotlinx.coroutines.delay
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.MorphLoader
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.theme.rememberLightningHaptics
import com.lightning.browser.ui.components.TonalIconButton
import com.lightning.browser.ui.home.ChipTone
import com.lightning.browser.ui.home.RecentPage
import com.lightning.browser.ui.settings.Bookmark
import com.lightning.browser.ui.settings.DnsProvider
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
            var targetContent = 'width=1280, initial-scale=$formattedScale';
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
    webView.settings.useWideViewPort = enabled
    webView.settings.loadWithOverviewMode = enabled
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
    dnsProvider: DnsProvider = DnsProvider.OFF,
    customDnsUrl: String = "",
    bypassTrusted: Boolean = false,
    onBlocked: () -> Unit = {},
    onDnsResolved: () -> Unit = {},
    focusSignal: Int = 0,
    onFocusConsumed: () -> Unit = {},
    findSignal: Int = 0,
    onFindConsumed: () -> Unit = {},
    chromeVisible: Boolean = true,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    recentSearches: List<String> = emptyList(),
    recentPages: List<RecentPage> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    onUpdateTab: (Int, BrowserTab) -> Unit,
    onRecordVisit: (String, String) -> Unit = { _, _ -> },
    onSearchSubmitted: (String) -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onPrintRequest: () -> Unit = {},
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTab = tabs.getOrNull(activeIndex)
    val keyboard = LocalSoftwareKeyboardController.current
    var editing by remember(activeIndex) { mutableStateOf(false) }
    var query by remember(activeIndex) { mutableStateOf("") }
    var finding by remember(activeIndex) { mutableStateOf(false) }
    var findQuery by remember(activeIndex) { mutableStateOf("") }
    var findActiveMatch by remember(activeIndex) { mutableIntStateOf(0) }
    var findTotalMatches by remember(activeIndex) { mutableIntStateOf(0) }
    val progress = remember { mutableStateMapOf<Int, Int>() }
    val scrollAccumulator = remember { intArrayOf(0) }
    val lastToggle = remember { longArrayOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    var duplicateDownload by remember { mutableStateOf<DuplicateDownloadPrompt?>(null) }
    var downloadPopup by remember { mutableStateOf<DownloadPopupState?>(null) }
    var waybackSnapshot by remember(activeIndex) { mutableStateOf<WaybackSnapshot?>(null) }
    val dnsEndpoint = remember(dnsProvider, customDnsUrl) {
        DohResolver.endpointFor(dnsProvider, customDnsUrl)
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

    fun enqueueChecked(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        val fileName = BrowserDownloads.fileNameFrom(contentDisposition, url, mimeType)
        if (BrowserDownloads.downloadExists(context, fileName)) {
            val suggested = BrowserDownloads.nextAvailableName(context, fileName)
            duplicateDownload = if (url.startsWith("data:", ignoreCase = true)) {
                DuplicateDownloadPrompt.DataUri(url, fileName, suggested, mimeType)
            } else {
                DuplicateDownloadPrompt.Http(url, userAgent, fileName, suggested, mimeType)
            }
        } else {
            val started = BrowserDownloads.enqueueWithFileName(context, url, userAgent, fileName, mimeType)
            if (started) {
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

    fun startDownload(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        if (url.startsWith("data:", ignoreCase = true)) {
            enqueueChecked(url, userAgent, contentDisposition, mimeType)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = PendingDownload(url, userAgent, contentDisposition, mimeType)
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enqueueChecked(url, userAgent, contentDisposition, mimeType)
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

    BackHandler(enabled = finding) { closeFind() }
    BackHandler(enabled = editing) { editing = false }

    val fullscreen = (webViews[activeIndex]?.tag as? WebConfig)?.fullscreen
    BackHandler(enabled = fullscreen?.showing == true) { fullscreen?.hide() }

    Column(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = chromeVisible || editing || finding,
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
            editing = editing,
            onQueryChange = { query = it },
            onEditStart = {
                editing = true
                query = activeTab?.url.orEmpty()
            },
            onCancel = { editing = false },
            onSubmit = {
                val text = query.trim()
                if (text.isNotEmpty() && tabs.isNotEmpty()) {
                    val url = toUrl(text, searchEngine)
                    onUpdateTab(activeIndex, tabs[activeIndex].copy(url = url, domain = hostOf(url)))
                    if (url == searchEngine.searchUrl(text)) onSearchSubmitted(text)
                    editing = false
                    keyboard?.hide()
                }
            },
            onRefresh = { webViews[activeIndex]?.reload() },
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
        Box(Modifier.weight(1f)) {
            if (activeTab != null && activeTab.url.isNotEmpty()) {
                key(activeTab.id) {
                BrowserPage(
                    tab = activeTab,
                    index = activeIndex,
                    adBlocking = adBlocking,
                    dnsEndpoint = dnsEndpoint,
                    bypassTrusted = bypassTrusted,
                    webViews = webViews,
                    loadedUrls = loadedUrls,
                    onProgress = { progress[activeIndex] = it },
                    onNavChange = { back, forward -> navStates[activeIndex] = TabNavState(back, forward) },
                    onScrolled = { scrollY, delta ->
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
                            loadedUrls[activeIndex] = url
                            val current = tabs.getOrNull(activeIndex) ?: activeTab
                            onUpdateTab(activeIndex, current.copy(url = url, domain = hostOf(url)))
                        }
                    },
                    onPageMeta = { title, domain, url ->
                        loadedUrls[activeIndex] = url
                        val current = tabs.getOrNull(activeIndex) ?: activeTab
                        onUpdateTab(activeIndex, current.copy(title = title, domain = domain, url = url))
                        if (!current.private) onRecordVisit(title, domain)
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
                    onHttpError = { failedUrl, statusCode ->
                        if (statusCode in listOf(404, 410, 502, 504)) {
                            WaybackResolver.checkAvailability(failedUrl) { result ->
                                if (result != null && (activeTab?.url == failedUrl || loadedUrls[activeIndex] == failedUrl)) {
                                    waybackSnapshot = result
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                }
            } else {
                EmptyBrowser()
            }
            if (editing) {
                Suggestions(
                    query = query.trim(),
                    recentSearches = recentSearches,
                    recentPages = recentPages,
                    bookmarks = bookmarks,
                    onSearch = { term ->
                        if (tabs.isNotEmpty()) {
                            val url = searchEngine.searchUrl(term)
                            onUpdateTab(activeIndex, tabs[activeIndex].copy(url = url, domain = hostOf(url)))
                            onSearchSubmitted(term)
                        }
                        editing = false
                        keyboard?.hide()
                    },
                    onOpenSuggestion = { domain ->
                        if (tabs.isNotEmpty()) {
                            val url = "https://$domain"
                            onUpdateTab(activeIndex, tabs[activeIndex].copy(url = url, domain = domain))
                        }
                        editing = false
                        keyboard?.hide()
                    },
                )
            }
            if (activeProgress < 100 && !editing) {
                LightningLoadingIndicator(Modifier.fillMaxSize())
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = waybackSnapshot != null && downloadPopup == null,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
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
                            )
                        ) {
                            showDownloadStarted(prompt.suggestedName)
                        }
                    }
                    is DuplicateDownloadPrompt.DataUri -> {
                        if (BrowserDownloads.saveDataOrBlob(
                                context,
                                prompt.dataUri,
                                prompt.suggestedName,
                                prompt.mimeType,
                            )
                        ) {
                            showDownloadFinished(prompt.suggestedName)
                        }
                    }
                    is DuplicateDownloadPrompt.BlobTemp -> {
                        if (BrowserDownloads.completePendingBlob(
                                context,
                                prompt.suggestedName,
                                prompt.mimeType,
                                prompt.tempFile,
                            )
                        ) {
                            showDownloadFinished(prompt.suggestedName)
                        }
                    }
                }
                duplicateDownload = null
            },
        )
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
                icon = Icons.Filled.FileDownload,
                shape = ExpressiveShapes.Cookie9,
                container = if (state.completed) colors.secondaryContainer else colors.primaryContainer,
                content = if (state.completed) colors.onSecondaryContainer else colors.onPrimaryContainer,
                size = 44.dp,
                iconSize = 22.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (state.completed) R.string.download_finished_title else R.string.download_started_title,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                if (state.completed) {
                    LinearProgressIndicator(
                        progress = { 1f },
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

@Composable
private fun BrowserPage(
    tab: BrowserTab,
    index: Int,
    adBlocking: Boolean,
    dnsEndpoint: String?,
    bypassTrusted: Boolean,
    webViews: SnapshotStateMap<Int, WebView>,
    loadedUrls: SnapshotStateMap<Int, String>,
    onProgress: (Int) -> Unit,
    onNavChange: (Boolean, Boolean) -> Unit,
    onUrlChange: (String) -> Unit,
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
                val config = WebConfig(tab, adBlocking, dnsEndpoint, bypassTrusted)
                tag = config
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                if (tab.private) {
                    settings.setSaveFormData(false)
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                }
                setBackgroundColor(colors.surface.toArgb())
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
                        if (!config.adBlocking || !request.isForMainFrame) return false
                        if (request.hasGesture() && !request.isRedirect) return false
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
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { config.onUrlChange(it) }
                        if (config.desktopMode && view != null) {
                            view.evaluateJavascript(desktopViewportScript(view), null)
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
                        }
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
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (config.desktopMode && view != null) {
                            view.evaluateJavascript(desktopViewportScript(view), null)
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
            }
        },
        update = { wv ->
            (wv.tag as? WebConfig)?.let { config ->
                val desktopChanged = config.desktopMode != tab.desktopMode
                config.tab = tab
                config.adBlocking = adBlocking
                config.dnsEndpoint = dnsEndpoint
                config.bypassTrusted = bypassTrusted
                config.onProgress = onProgress
                config.onNavChange = onNavChange
                config.onScrolled = onScrolled
                config.onUrlChange = onUrlChange
                config.onPageMeta = onPageMeta
                config.onDownload = onDownload
                config.onDownloadStart = onDownloadStart
                config.onDownloadComplete = onDownloadComplete
                config.onDuplicateBlob = onDuplicateBlob
                config.onBlocked = onBlocked
                config.onDnsResolved = onDnsResolved
                config.onHttpError = onHttpError
                config.onPrintRequest = onPrintRequest
                if (desktopChanged) {
                    applyDesktopMode(wv, config, tab.desktopMode)
                    if (tab.url.isNotEmpty()) {
                        wv.reload()
                    }
                }
            }
            wv.onResume()
            onNavChange(wv.canGoBack(), wv.canGoForward())
        },
        modifier = modifier,
    )
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
    onQueryChange: (String) -> Unit,
    onEditStart: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    onRefresh: () -> Unit,
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
                onClick = onEditStart,
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
    recentSearches: List<String>,
    recentPages: List<RecentPage>,
    bookmarks: List<Bookmark>,
    onSearch: (String) -> Unit,
    onOpenSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val matches = { text: String -> text.contains(query, ignoreCase = true) }
    val searches = if (query.isBlank()) recentSearches else recentSearches.filter(matches)
    val sites = (recentPages.map { Suggestion(it.title, it.domain, it.icon, it.tone) } +
        bookmarks.map { Suggestion(it.title, it.domain, it.icon, it.tone) })
        .distinctBy { it.domain }
        .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.domain.contains(query, ignoreCase = true) }

    val entries = buildList {
        if (query.isNotEmpty()) {
            add(
                Suggestion(
                    title = stringResource(R.string.browser_suggestion_search, query),
                    domain = "",
                    icon = Icons.Filled.Search,
                    tone = ChipTone.PRIMARY,
                ),
            )
        }
        searches.forEach { term ->
            add(
                Suggestion(
                    title = term,
                    domain = stringResource(R.string.browser_suggestion_search, term),
                    icon = Icons.Filled.Search,
                    tone = ChipTone.PRIMARY,
                ),
            )
        }
        addAll(sites)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp),
    ) {
        GroupedColumn(entries.size) { index, shape ->
            val entry = entries[index]
            val isSearchAction = index == 0 && query.isNotEmpty()
            SuggestionRow(
                icon = entry.icon,
                title = entry.title,
                subtitle = entry.domain,
                shape = shape,
                index = index,
                onClick = {
                    when {
                        isSearchAction -> onSearch(query)
                        entry.domain.isEmpty() || index < searches.size + (if (query.isEmpty()) 0 else 1) ->
                            onSearch(entry.title)
                        else -> onOpenSuggestion(entry.domain)
                    }
                },
            )
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

private data class Suggestion(
    val title: String,
    val domain: String,
    val icon: ImageVector,
    val tone: ChipTone,
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
) {
    class Http(
        val url: String,
        val userAgent: String?,
        existingName: String,
        suggestedName: String,
        val mimeType: String?,
    ) : DuplicateDownloadPrompt(existingName, suggestedName)

    class DataUri(
        val dataUri: String,
        existingName: String,
        suggestedName: String,
        val mimeType: String?,
    ) : DuplicateDownloadPrompt(existingName, suggestedName)

    class BlobTemp(
        existingName: String,
        suggestedName: String,
        val mimeType: String,
        val tempFile: File,
    ) : DuplicateDownloadPrompt(existingName, suggestedName)
}

private fun toUrl(text: String, engine: SearchEngine): String {
    val trimmed = text.trim()
    val hasScheme = trimmed.startsWith("http://") || trimmed.startsWith("https://")
    val looksLikeUrl = trimmed.contains(".") && !trimmed.contains(" ") && !hasScheme
    return when {
        hasScheme -> trimmed
        looksLikeUrl -> "https://$trimmed"
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
