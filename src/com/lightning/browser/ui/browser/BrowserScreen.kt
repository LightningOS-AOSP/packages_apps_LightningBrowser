package com.lightning.browser.ui.browser

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lightning.browser.R
import com.lightning.browser.ui.home.ChipTone
import com.lightning.browser.ui.home.RecentPage
import com.lightning.browser.ui.settings.Bookmark
import com.lightning.browser.ui.settings.DnsProvider
import com.lightning.browser.ui.settings.SearchEngine
import com.lightning.browser.ui.settings.searchUrl
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.theme.LightningColors
import com.lightning.browser.ui.theme.LightningTheme
import java.io.ByteArrayInputStream

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

    Column(modifier.fillMaxSize()) {
        UrlRow(
            domain = activeTab?.domain.orEmpty(),
            query = query,
            editing = editing,
            onQueryChange = { query = it },
            onEditStart = {
                editing = true
                query = ""
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
    val colors = LightningTheme.colors
    val transition = rememberInfiniteTransition()
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    val s = 0.8f + 0.2f * pulse
                    scaleX = s
                    scaleY = s
                    alpha = 0.10f + 0.35f * pulse
                }
                .background(
                    brush = Brush.radialGradient(
                        listOf(colors.primary.copy(alpha = 0.55f), Color.Transparent),
                    ),
                    shape = CircleShape,
                ),
        )
        Icon(
            imageVector = Icons.Filled.FlashOn,
            contentDescription = stringResource(R.string.browser_loading),
            tint = colors.primary,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    val s = 0.85f + 0.15f * pulse
                    scaleX = s
                    scaleY = s
                    alpha = 0.4f + 0.6f * pulse
                },
        )
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
    onPageMeta: (String, String, String) -> Unit,
    onDownload: (String, String?, String?, String?) -> Unit,
    onBlocked: () -> Unit,
    onDnsResolved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    var viewUserAgent by remember { mutableStateOf<String?>(null) }
    AndroidView(
        factory = { context ->
            webViews[index] ?: WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                if (tab.private) {
                    settings.setSaveFormData(false)
                    settings.setSupportZoom(false)
                }
                setBackgroundColor(colors.surface.toArgb())
                viewUserAgent = settings.userAgentString
                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    onDownload(url, userAgent, contentDisposition, mimeType)
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress)
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url ?: return null
                        val host = url.host ?: return null
                        if (adBlocking && isAdHost(host, context)) {
                            onBlocked()
                            return WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        if (dnsEndpoint == null) return null
                        if (url.scheme != "https") return null
                        val method = request.method
                        if (method != "GET" && method != "HEAD") return null
                        if (request.requestHeaders.containsKey("Range")) return null
                        val accept = request.requestHeaders["Accept"]
                        if (accept != null && accept.contains("text/event-stream", ignoreCase = true)) return null
                        if (host.isIpAddress()) return null
                        if (bypassTrusted && (host.isLocalHost() || host.isPrivateRange())) return null
                        val ua = viewUserAgent ?: return null
                        val responseHeaders = buildEffectHeaders(request)
                        Log.d("LightningDoH", "intercept $method ${url} -> $host")
                        val response = PinnedHttp.get(url.toString(), dnsEndpoint, ua, responseHeaders)
                            ?: return null
                        onDnsResolved()
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

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val host = runCatching { Uri.parse(url).host }.getOrNull()?.takeIf { it.isNotEmpty() } ?: tab.domain
                        onPageMeta(
                            view?.title?.takeIf { it.isNotBlank() } ?: tab.title,
                            host,
                            url ?: tab.url,
                        )
                        onNavChange(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                    }
                }
            }.also { webViews[index] = it }
        },
        update = { wv ->
            wv.onResume()
            if (tab.private) {
                CookieManager.getInstance().setAcceptCookie(false)
            }
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
    domain: String,
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
                value = query,
                onValueChange = onQueryChange,
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
                shape = RoundedCornerShape(18.dp),
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
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 4.dp),
                ) {
                    Text(
                        text = domain.ifEmpty { stringResource(R.string.home_search_placeholder) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (domain.isNotEmpty()) colors.onSurface else colors.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (domain.isNotEmpty()) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.browser_refresh),
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        if (query.isNotEmpty()) {
            Surface(
                onClick = { onSearch(query) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Surface(
                        color = colors.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.browser_suggestion_search, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        searches.forEach { term ->
            SuggestionRow(
                icon = Icons.Filled.Search,
                title = term,
                subtitle = stringResource(R.string.browser_suggestion_search, term),
                tone = ChipTone.PRIMARY,
                onClick = { onSearch(term) },
                colors = colors,
            )
            Spacer(Modifier.height(4.dp))
        }
        if (searches.isNotEmpty() && sites.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
        }
        sites.forEach { entry ->
            SuggestionRow(
                icon = entry.icon,
                title = entry.title,
                subtitle = entry.domain,
                tone = entry.tone,
                onClick = { onOpenSuggestion(entry.domain) },
                colors = colors,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tone: ChipTone,
    onClick: () -> Unit,
    colors: LightningColors,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            val (container, onContainer) = colors.chipColors(tone)
            Surface(
                color = container,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptyBrowser() {
    val colors = LightningTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.home_search_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
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

private fun LightningColors.chipColors(tone: ChipTone): Pair<Color, Color> =
    when (tone) {
        ChipTone.PRIMARY -> primaryContainer to onPrimaryContainer
        ChipTone.SECONDARY -> secondaryContainer to onSecondaryContainer
        ChipTone.TERTIARY -> tertiaryContainer to onTertiaryContainer
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

// Adblock hosts, loaded lazily from assets/adblock_hosts.txt (StevenBlack
// ad+tracking list plus curated hosts). Matched as a suffix walk so that a
// host like ads.foo.example.com also hits the entry for example.com.
private object AdBlockHosts {
    private var loaded: Set<String>? = null

    fun hosts(context: Context): Set<String> {
        loaded?.let { return it }
        synchronized(this) {
            loaded?.let { return it }
            val set = HashSet<String>()
            runCatching {
                context.assets.open("adblock_hosts.txt").bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val host = line.trim()
                        if (host.isNotEmpty() && !host.startsWith("#")) {
                            set.add(host)
                        }
                    }
                }
            }
            loaded = set
            return set
        }
    }
}

private fun isAdHost(host: String, context: Context): Boolean {
    val hosts = AdBlockHosts.hosts(context)
    var current = host
    while (current.isNotEmpty()) {
        if (current in hosts) return true
        val dot = current.indexOf('.')
        if (dot == -1) break
        current = current.substring(dot + 1)
    }
    return false
}