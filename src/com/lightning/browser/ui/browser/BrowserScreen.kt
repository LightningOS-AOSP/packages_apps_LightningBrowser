package com.lightning.browser.ui.browser

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.MorphLoader
import com.lightning.browser.ui.components.ShapedIcon
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

private const val CHROME_SCROLL_THRESHOLD = 48
private const val CHROME_SETTLE_MS = 400L

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
    chromeVisible: Boolean = true,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    recentSearches: List<String> = emptyList(),
    recentPages: List<RecentPage> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    onUpdateTab: (Int, BrowserTab) -> Unit,
    onRecordVisit: (String, String) -> Unit = { _, _ -> },
    onSearchSubmitted: (String) -> Unit = {},
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTab = tabs.getOrNull(activeIndex)
    val keyboard = LocalSoftwareKeyboardController.current
    var editing by rememberSaveable(activeIndex) { mutableStateOf(false) }
    var query by rememberSaveable(activeIndex) { mutableStateOf("") }
    val progress = remember { mutableStateMapOf<Int, Int>() }
    val scrollAccumulator = remember { intArrayOf(0) }
    val lastToggle = remember { longArrayOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    val dnsEndpoint = remember(dnsProvider, customDnsUrl) {
        DohResolver.endpointFor(dnsProvider, customDnsUrl)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val request = pendingDownload
        pendingDownload = null
        if (granted && request != null) {
            BrowserDownloads.enqueue(context, request.url, request.userAgent, request.contentDisposition, request.mimeType)
        }
    }

    fun startDownload(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = PendingDownload(url, userAgent, contentDisposition, mimeType)
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            BrowserDownloads.enqueue(context, url, userAgent, contentDisposition, mimeType)
        }
    }

    LaunchedEffect(focusSignal, activeIndex) {
        if (focusSignal > 0) {
            editing = true
            query = ""
        }
    }

    BackHandler(enabled = editing) { editing = false }

    val fullscreen = (webViews[activeIndex]?.tag as? WebConfig)?.fullscreen
    BackHandler(enabled = fullscreen?.showing == true) { fullscreen?.hide() }

    Column(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = chromeVisible || editing,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
        ) {
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
                            onUpdateTab(activeIndex, activeTab.copy(url = url, domain = hostOf(url)))
                        }
                    },
                    onPageMeta = { title, domain, url ->
                        loadedUrls[activeIndex] = url
                        onUpdateTab(activeIndex, activeTab.copy(title = title, domain = domain, url = url))
                        if (!activeTab.private) onRecordVisit(title, domain)
                    },
                    onDownload = ::startDownload,
                    onBlocked = onBlocked,
                    onDnsResolved = onDnsResolved,
                    modifier = Modifier.fillMaxSize(),
                )
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
        }
    }
}

@Composable
private fun LightningLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MorphLoader(description = stringResource(R.string.browser_loading))
    }
}

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
    onBlocked: () -> Unit,
    onDnsResolved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    AndroidView(
        factory = { context ->
            webViews[index] ?: WebView(context).apply {
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
                config.userAgent = settings.userAgentString
                val fullscreen = FullscreenVideo(context)
                config.fullscreen = fullscreen
                setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    config.onScrolled(scrollY, scrollY - oldScrollY)
                }
                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    config.onDownload(url, userAgent, contentDisposition, mimeType)
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        config.onProgress(newProgress)
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
                        val response = PinnedHttp.get(url.toString(), endpoint, ua, responseHeaders)
                            ?: return null
                        config.onDnsResolved()
                        Log.d("LightningDoH", "served $host via pinned TLS")
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
                        if (scheme != "http" && scheme != "https" && scheme != "about") {
                            config.onBlocked()
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { config.onUrlChange(it) }
                        if (config.adBlocking) view?.evaluateJavascript(CosmeticFilter.SCRIPT, null)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        url?.let { config.onUrlChange(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
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
            }.also { webViews[index] = it }
        },
        update = { wv ->
            (wv.tag as? WebConfig)?.let { config ->
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
                config.onBlocked = onBlocked
                config.onDnsResolved = onDnsResolved
            }
            wv.onResume()
            onNavChange(wv.canGoBack(), wv.canGoForward())
        },
        modifier = modifier,
    )
    LaunchedEffect(index, tab.url) {
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

private data class PendingDownload(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimeType: String?,
)

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
