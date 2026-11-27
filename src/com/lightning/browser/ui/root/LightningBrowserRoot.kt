package com.lightning.browser.ui.root

import android.app.Activity
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lightning.browser.IncognitoActivity
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.bar.LightningBottomBar
import com.lightning.browser.ui.bar.QuickArcMenu
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.browser.BrowserScreen
import com.lightning.browser.ui.browser.TabNavState
import com.lightning.browser.ui.home.HomeScreen
import com.lightning.browser.ui.home.PinnedSite
import com.lightning.browser.ui.home.RecentPage
import com.lightning.browser.ui.siteIcon
import com.lightning.browser.ui.siteTone
import com.lightning.browser.ui.settings.AboutScreen
import com.lightning.browser.ui.settings.AppearanceScreen
import com.lightning.browser.ui.settings.Bookmark
import com.lightning.browser.ui.settings.BookmarksScreen
import com.lightning.browser.ui.settings.DnsProvider
import com.lightning.browser.ui.settings.DnsScreen
import com.lightning.browser.ui.settings.DownloadsScreen
import com.lightning.browser.ui.settings.HistoryEntry
import com.lightning.browser.ui.settings.HistoryScreen
import com.lightning.browser.ui.settings.PrivacyScreen
import com.lightning.browser.ui.settings.SearchEngine
import com.lightning.browser.ui.settings.SearchEngineScreen
import com.lightning.browser.ui.settings.SettingsRootScreen
import com.lightning.browser.ui.settings.SitePermissionsScreen
import com.lightning.browser.ui.settings.StartupBehavior
import com.lightning.browser.ui.settings.StartupScreen
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.tabs.TabSwitcherScreen
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.LocalHapticsEnabled
import com.lightning.browser.ui.theme.ThemeMode
import com.lightning.browser.ui.theme.rememberLightningHaptics
import kotlinx.coroutines.flow.distinctUntilChanged

enum class LightningScreen {
    HOME, TABS, SETTINGS, APPEARANCE, DNS, PRIVACY, SEARCH_ENGINE, DOWNLOADS, ABOUT, BOOKMARKS, HISTORY,
    SITE_PERMISSIONS, BROWSER, STARTUP
}

@Composable
fun LightningBrowserRoot(
    incognito: Boolean = false,
    initialUrl: String? = null,
) {
    val context = LocalContext.current

    val initialTabs = remember {
        if (!incognito) {
            val saved = BrowserStore.loadTabs(context)
            if (saved.isNotEmpty()) saved else listOf(BrowserTab(title = "New tab", domain = "", private = false))
        } else {
            listOf(BrowserTab(title = "New tab", domain = "", private = true))
        }
    }
    val tabs = remember {
        mutableStateListOf<BrowserTab>().apply { addAll(initialTabs) }
    }
    val initialActiveIndex = remember {
        if (!incognito) {
            BrowserStore.loadActiveTab(context).coerceIn(0, (initialTabs.size - 1).coerceAtLeast(0))
        } else {
            0
        }
    }
    var activeIndex by rememberSaveable { mutableStateOf(initialActiveIndex) }
    var startupBehavior by remember { mutableStateOf(BrowserStore.loadStartupBehavior(context)) }
    var screen by rememberSaveable {
        mutableStateOf(
            if (startupBehavior == StartupBehavior.LAST_TAB &&
                initialTabs.getOrNull(initialActiveIndex)?.url?.isNotEmpty() == true
            ) {
                LightningScreen.BROWSER
            } else {
                LightningScreen.HOME
            }
        )
    }
    var addressFocusSignal by remember { mutableStateOf(0) }
    var findInPageSignal by remember { mutableStateOf(0) }
    val screenBackStack = remember { mutableStateListOf<LightningScreen>() }

    fun navigateTo(target: LightningScreen) {
        if (screen != target) {
            screenBackStack.add(screen)
            screen = target
        }
    }

    fun navigateBack(fallback: LightningScreen = LightningScreen.HOME) {
        screen = if (screenBackStack.isNotEmpty()) {
            screenBackStack.removeAt(screenBackStack.lastIndex)
        } else {
            fallback
        }
    }

    var themeMode by remember {
        mutableStateOf(if (incognito) ThemeMode.DARK else BrowserStore.loadThemeMode(context))
    }
    var searchEngine by remember { mutableStateOf(BrowserStore.loadSearchEngine(context)) }
    var dnsProvider by remember { mutableStateOf(BrowserStore.loadDnsProvider(context)) }
    var customDnsUrl by remember { mutableStateOf(BrowserStore.loadCustomDnsUrl(context)) }
    var bypassTrusted by remember { mutableStateOf(BrowserStore.loadBypassTrusted(context)) }
    var adBlocking by remember { mutableStateOf(BrowserStore.loadAdBlocking(context)) }
    var fingerprintLock by remember { mutableStateOf(BrowserStore.loadFingerprintLock(context)) }
    var hapticsEnabled by remember { mutableStateOf(BrowserStore.loadHapticFeedback(context)) }
    var blockedCount by remember { mutableIntStateOf(0) }
    var dnsResolvedCount by remember { mutableIntStateOf(0) }

    val navStates = remember { mutableStateMapOf<Int, TabNavState>() }
    val webViews = remember { mutableStateMapOf<Int, WebView>() }
    val loadedUrls = remember { mutableStateMapOf<Int, String>() }
    val tabThumbnails = remember {
        mutableStateMapOf<String, Bitmap>().apply {
            if (!incognito) {
                initialTabs.forEach { tab ->
                    BrowserStore.loadTabThumbnail(context, tab.id)?.let { bmp ->
                        put(tab.id, bmp)
                    }
                }
            }
        }
    }
    var chromeVisible by remember { mutableStateOf(true) }
    val bookmarks = remember {
        mutableStateListOf<Bookmark>().apply { addAll(BrowserStore.loadBookmarks(context)) }
    }
    val history = remember {
        mutableStateListOf<HistoryEntry>().apply { if (!incognito) addAll(BrowserStore.loadHistory(context)) }
    }
    val recentSearches = remember {
        mutableStateListOf<String>().apply { if (!incognito) addAll(BrowserStore.loadRecentSearches(context)) }
    }
    val pinnedSites = remember {
        mutableStateListOf<PinnedSite>().apply { if (!incognito) addAll(BrowserStore.loadPinnedSites(context)) }
    }
    var showMenu by remember { mutableStateOf(false) }
    var showQuickArc by remember { mutableStateOf(false) }
    var tabsReturnScreen by remember { mutableStateOf(LightningScreen.HOME) }
    var pendingBookmarkTab by remember { mutableStateOf<BrowserTab?>(null) }
    var authError by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(BrowserStore.loadFingerprintLock(context)) }
    var showClearData by remember { mutableStateOf(false) }
    var showAddPinned by remember { mutableStateOf(false) }
    var pinnedDraft by remember { mutableStateOf("") }
    var showDefaultBrowserPrompt by remember {
        mutableStateOf(
            if (!incognito && !BrowserStore.hasPromptedDefaultBrowser(context)) {
                !isDefaultBrowser(context)
            } else {
                false
            }
        )
    }

    LaunchedEffect(tabs) {
        snapshotFlow { tabs.toList() }.distinctUntilChanged().collect {
            if (!incognito) BrowserStore.saveTabs(context, it)
        }
    }
    LaunchedEffect(activeIndex) {
        if (!incognito) BrowserStore.saveActiveTab(context, activeIndex)
    }
    LaunchedEffect(startupBehavior) {
        if (!incognito) BrowserStore.saveStartupBehavior(context, startupBehavior)
    }
    LaunchedEffect(bookmarks) {
        snapshotFlow { bookmarks.toList() }.distinctUntilChanged().collect {
            if (!incognito) BrowserStore.saveBookmarks(context, it)
        }
    }
    LaunchedEffect(history) {
        snapshotFlow { history.toList() }.distinctUntilChanged().collect {
            if (!incognito) BrowserStore.saveHistory(context, it)
        }
    }
    LaunchedEffect(recentSearches) {
        snapshotFlow { recentSearches.toList() }.distinctUntilChanged().collect {
            if (!incognito) BrowserStore.saveRecentSearches(context, it)
        }
    }
    LaunchedEffect(pinnedSites) {
        snapshotFlow { pinnedSites.toList() }.distinctUntilChanged().collect {
            if (!incognito) BrowserStore.savePinnedSites(context, it)
        }
    }
    LaunchedEffect(themeMode) { if (!incognito) BrowserStore.saveThemeMode(context, themeMode) }
    LaunchedEffect(searchEngine) { if (!incognito) BrowserStore.saveSearchEngine(context, searchEngine) }
    LaunchedEffect(dnsProvider) { if (!incognito) BrowserStore.saveDnsProvider(context, dnsProvider) }
    LaunchedEffect(customDnsUrl) { if (!incognito) BrowserStore.saveCustomDnsUrl(context, customDnsUrl) }
    LaunchedEffect(bypassTrusted) { if (!incognito) BrowserStore.saveBypassTrusted(context, bypassTrusted) }
    LaunchedEffect(adBlocking) { if (!incognito) BrowserStore.saveAdBlocking(context, adBlocking) }
    LaunchedEffect(fingerprintLock) {
        if (!incognito) BrowserStore.saveFingerprintLock(context, fingerprintLock)
        if (fingerprintLock) locked = true
    }
    LaunchedEffect(hapticsEnabled) {
        if (!incognito) BrowserStore.saveHapticFeedback(context, hapticsEnabled)
    }

    fun captureTabThumbnail(index: Int) {
        val tab = tabs.getOrNull(index) ?: return
        if (tab.url.isEmpty()) return
        val wv = webViews[index] ?: return
        val w = wv.width
        val h = wv.height
        if (w <= 0 || h <= 0) return
        runCatching {
            val scale = (480f / w).coerceAtMost(0.5f)
            val bmpW = (w * scale).toInt().coerceAtLeast(1)
            val bmpH = (h * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.RGB_565)
            val canvas = Canvas(bmp)
            canvas.scale(scale, scale)
            wv.draw(canvas)
            tabThumbnails[tab.id] = bmp
            if (!incognito && !tab.private) {
                BrowserStore.saveTabThumbnail(context, tab.id, bmp)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (screen == LightningScreen.BROWSER) {
                    captureTabThumbnail(activeIndex)
                }
            }
            if (event == Lifecycle.Event.ON_STOP && fingerprintLock) locked = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun recordVisit(title: String, domain: String) {
        if (incognito || domain.isBlank()) return
        val now = System.currentTimeMillis()
        history.firstOrNull()?.let { first ->
            if (first.domain == domain && now - first.timestamp < 5 * 60_000L) {
                history[0] = first.copy(title = title, timestamp = now)
                return
            }
        }
        history.add(0, HistoryEntry(title, domain, siteIcon(domain), siteTone(domain), now))
    }

    fun recordSearch(term: String) {
        val trimmed = term.trim()
        if (incognito || trimmed.isBlank()) return
        recentSearches.find { it.equals(trimmed, ignoreCase = true) }?.let {
            recentSearches.remove(it)
        }
        recentSearches.add(0, trimmed)
    }

    fun toggleBookmark(tab: BrowserTab) {
        val existing = bookmarks.find { it.domain == tab.domain }
        if (existing != null) {
            bookmarks.remove(existing)
            Toast.makeText(context, R.string.bookmark_removed, Toast.LENGTH_SHORT).show()
        } else {
            pendingBookmarkTab = tab
        }
    }

    fun clearBrowsingData() {
        webViews.values.forEach { webView ->
            runCatching { webView.clearCache(true) }
            runCatching { webView.clearHistory() }
            runCatching { webView.clearFormData() }
        }
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
        runCatching { CookieManager.getInstance().flush() }
        runCatching { WebViewDatabase.getInstance(context).clearFormData() }
        runCatching { WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword() }
        runCatching { WebStorage.getInstance().deleteAllData() }
        history.clear()
        recentSearches.clear()
        Toast.makeText(context, R.string.privacy_cleared, Toast.LENGTH_SHORT).show()
    }

    fun copyUrl() {
        val url = tabs.getOrNull(activeIndex)?.url ?: return
        if (url.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("url", url))
        Toast.makeText(context, R.string.menu_copy_url_copied, Toast.LENGTH_SHORT).show()
    }

    fun shareUrl() {
        val url = tabs.getOrNull(activeIndex)?.url ?: return
        if (url.isBlank()) return
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, url)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.menu_share_chooser))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    val bookmarkFolders = bookmarks.map { it.folder }.filter { it.isNotBlank() }.distinct()

    val recentPages = history
        .distinctBy { it.domain }
        .take(3)
        .map { RecentPage(it.title, it.domain, it.icon, it.tone) }

    fun openSite(domain: String) {
        val url = if (domain.startsWith("http")) domain else "https://$domain"
        if (tabs.isEmpty()) {
            tabs.add(BrowserTab(title = domain, domain = hostOf(url), url = url, private = incognito))
            activeIndex = 0
        } else {
            val index = activeIndex.coerceIn(tabs.indices)
            tabs[index] = tabs[index].copy(url = url, domain = hostOf(url))
            activeIndex = index
        }
        screenBackStack.clear()
        screen = LightningScreen.BROWSER
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            openSite(initialUrl)
        }
    }

    val defaultBrowserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { }

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            locked = false
        } else {
            authError = true
        }
    }

    fun requestUnlock() {
        val activity = context as? Activity ?: return
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: run { authError = true; return }
        if (!keyguard.isDeviceSecure) {
            locked = false
            return
        }
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            context.getString(R.string.fingerprint_title),
            context.getString(R.string.fingerprint_subtitle),
        )
        if (intent != null) {
            authError = false
            authLauncher.launch(intent)
        } else {
            locked = false
        }
    }

    BackHandler(enabled = showQuickArc) {
        showQuickArc = false
    }

    BackHandler(enabled = !showQuickArc && screen != LightningScreen.HOME) {
        when (screen) {
            LightningScreen.BROWSER -> {
                val view = webViews[activeIndex]
                if (view?.canGoBack() == true) {
                    view.goBack()
                } else {
                    captureTabThumbnail(activeIndex)
                    screenBackStack.clear()
                    screen = LightningScreen.HOME
                }
            }
            LightningScreen.TABS -> {
                screenBackStack.clear()
                screen = if (tabs.getOrNull(activeIndex)?.url?.isNotEmpty() == true) {
                    LightningScreen.BROWSER
                } else {
                    LightningScreen.HOME
                }
            }
            LightningScreen.SETTINGS, LightningScreen.BOOKMARKS, LightningScreen.HISTORY -> {
                navigateBack(LightningScreen.HOME)
            }
            LightningScreen.SITE_PERMISSIONS -> navigateBack(LightningScreen.PRIVACY)
            else -> navigateBack(LightningScreen.SETTINGS)
        }
    }

    CompositionLocalProvider(LocalHapticsEnabled provides hapticsEnabled) {
        LightningTheme(themeMode = themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightningTheme.colors.surface),
        ) {
            LaunchedEffect(screen, activeIndex) { chromeVisible = true }
            val showBar = screen == LightningScreen.HOME ||
                screen == LightningScreen.TABS ||
                (screen == LightningScreen.BROWSER && chromeVisible)
            val barPadding by animateDpAsState(
                targetValue = if (showBar) BAR_BOTTOM_PADDING else 0.dp,
                animationSpec = tween(200),
                label = "barPadding",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(bottom = barPadding),
            ) {
                val keepBrowserMounted = screen == LightningScreen.BROWSER ||
                    (screen == LightningScreen.TABS && tabsReturnScreen == LightningScreen.BROWSER)
                if (keepBrowserMounted) {
                    BrowserScreen(
                        tabs = tabs,
                        activeIndex = activeIndex,
                        navStates = navStates,
                        webViews = webViews,
                        loadedUrls = loadedUrls,
                        adBlocking = adBlocking,
                        searchEngine = searchEngine,
                        dnsProvider = dnsProvider,
                        customDnsUrl = customDnsUrl,
                        bypassTrusted = bypassTrusted,
                        onBlocked = { blockedCount++ },
                        onDnsResolved = { dnsResolvedCount++ },
                        focusSignal = addressFocusSignal,
                        onFocusConsumed = { addressFocusSignal = 0 },
                        findSignal = findInPageSignal,
                        onFindConsumed = { findInPageSignal = 0 },
                        chromeVisible = chromeVisible,
                        onChromeVisibleChange = { chromeVisible = it },
                        recentSearches = recentSearches,
                        recentPages = recentPages,
                        bookmarks = bookmarks,
                        onUpdateTab = { index, tab -> if (index in tabs.indices) tabs[index] = tab },
                        onRecordVisit = { title, domain ->
                            recordVisit(title, domain)
                            webViews[activeIndex]?.postDelayed({
                                if (screen == LightningScreen.BROWSER) {
                                    captureTabThumbnail(activeIndex)
                                }
                            }, 600L)
                        },
                        onSearchSubmitted = { recordSearch(it) },
                        onOpenDownloads = { navigateTo(LightningScreen.DOWNLOADS) },
                        onBackHome = {
                            captureTabThumbnail(activeIndex)
                            screen = LightningScreen.HOME
                        },
                    )
                }
                AnimatedVisibility(
                    visible = screen != LightningScreen.BROWSER,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(140)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LightningTheme.colors.surface),
                    ) {
                        Crossfade(targetState = screen, animationSpec = tween(160), modifier = Modifier.fillMaxSize()) { s ->
                            when (s) {
                    LightningScreen.HOME -> HomeScreen(
                        recentPages = recentPages,
                        pinnedSites = pinnedSites,
                        onOpenSearch = {
                            addressFocusSignal++
                            screen = LightningScreen.BROWSER
                        },
                        onOpenSite = { openSite(it) },
                        onOpenProfile = { screen = LightningScreen.SETTINGS },
                        onAddPinned = {
                            pinnedDraft = ""
                            showAddPinned = true
                        },
                        incognito = incognito,
                        onRemovePinned = { site ->
                            pinnedSites.remove(site)
                            Toast.makeText(context, R.string.home_pinned_removed, Toast.LENGTH_SHORT).show()
                        },
                    )
                    LightningScreen.TABS -> TabSwitcherScreen(
                        tabs = tabs,
                        thumbnails = tabThumbnails,
                        onCloseTab = { index ->
                            if (index in tabs.indices) {
                                val removed = tabs.removeAt(index)
                                tabThumbnails.remove(removed.id)
                                if (!incognito && !removed.private) {
                                    BrowserStore.deleteTabThumbnail(context, removed.id)
                                }
                                webViews.remove(index)?.destroy()
                                navStates.remove(index)
                                loadedUrls.remove(index)
                                for (i in index until tabs.size) {
                                    webViews.remove(i + 1)?.let { webViews[i] = it }
                                    navStates.remove(i + 1)?.let { navStates[i] = it }
                                    loadedUrls.remove(i + 1)?.let { loadedUrls[i] = it }
                                }
                                activeIndex = when {
                                    tabs.isEmpty() -> 0
                                    activeIndex >= tabs.size -> tabs.size - 1
                                    index < activeIndex -> activeIndex - 1
                                    else -> activeIndex
                                }
                            }
                        },
                        onCloseAll = {
                            tabs.clear()
                            tabThumbnails.clear()
                            if (!incognito) {
                                BrowserStore.clearAllTabThumbnails(context)
                            }
                            webViews.values.forEach { runCatching { it.destroy() } }
                            webViews.clear()
                            navStates.clear()
                            loadedUrls.clear()
                            activeIndex = 0
                        },
                        onNewTab = {
                            tabs.add(BrowserTab(title = "New tab", domain = "", private = incognito))
                            activeIndex = tabs.size - 1
                        },
                        onOpenTab = {
                            activeIndex = it
                            screen = LightningScreen.BROWSER
                        },
                    )
                    LightningScreen.BROWSER -> Unit
                    LightningScreen.SETTINGS -> SettingsRootScreen(
                        startupBehavior = startupBehavior,
                        themeMode = themeMode,
                        searchEngine = searchEngine,
                        onOpenStartup = { navigateTo(LightningScreen.STARTUP) },
                        onOpenAppearance = { navigateTo(LightningScreen.APPEARANCE) },
                        onOpenPrivacy = { navigateTo(LightningScreen.PRIVACY) },
                        onOpenSearchEngine = { navigateTo(LightningScreen.SEARCH_ENGINE) },
                        onOpenDownloads = { navigateTo(LightningScreen.DOWNLOADS) },
                        onOpenAbout = { navigateTo(LightningScreen.ABOUT) },
                        onBack = { navigateBack(LightningScreen.HOME) },
                    )
                    LightningScreen.STARTUP -> StartupScreen(
                        startupBehavior = startupBehavior,
                        onStartupBehaviorSelect = { startupBehavior = it },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.SEARCH_ENGINE -> SearchEngineScreen(
                        engine = searchEngine,
                        onEngineSelect = { searchEngine = it },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.DOWNLOADS -> DownloadsScreen(
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.ABOUT -> AboutScreen(
                        onOpenWebsite = { openSite(it) },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.BOOKMARKS -> BookmarksScreen(
                        bookmarks = bookmarks,
                        onOpen = { openSite(it.domain) },
                        onDelete = { bookmarks.remove(it) },
                        onBack = { navigateBack(LightningScreen.HOME) },
                    )
                    LightningScreen.HISTORY -> HistoryScreen(
                        history = history,
                        onOpen = { openSite(it.domain) },
                        onDelete = { history.remove(it) },
                        onClear = { history.clear() },
                        onBack = { navigateBack(LightningScreen.HOME) },
                    )
                    LightningScreen.APPEARANCE -> AppearanceScreen(
                        themeMode = themeMode,
                        onThemeModeSelect = { themeMode = it },
                        hapticsEnabled = hapticsEnabled,
                        onHapticsChange = { hapticsEnabled = it },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.DNS -> DnsScreen(
                        provider = dnsProvider,
                        customUrl = customDnsUrl,
                        bypassTrusted = bypassTrusted,
                        resolvedCount = dnsResolvedCount,
                        onProviderSelect = { dnsProvider = it },
                        onCustomUrlChange = { customDnsUrl = it },
                        onBypassTrustedChange = { bypassTrusted = it },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.PRIVACY -> PrivacyScreen(
                        provider = dnsProvider,
                        adBlocking = adBlocking,
                        blockedCount = blockedCount,
                        fingerprintLock = fingerprintLock,
                        onAdBlockingChange = { adBlocking = it },
                        onFingerprintChange = { fingerprintLock = it },
                        onOpenDns = { navigateTo(LightningScreen.DNS) },
                        onOpenSitePermissions = { navigateTo(LightningScreen.SITE_PERMISSIONS) },
                        onClearData = { showClearData = true },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.SITE_PERMISSIONS -> SitePermissionsScreen(
                        onBack = { navigateBack(LightningScreen.PRIVACY) },
                    )
                    }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                AnimatedVisibility(
                    visible = showBar,
                    enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it }),
                ) {
                    val currentTab = tabs.getOrNull(activeIndex)
                    val isCurrentBookmarked = screen == LightningScreen.BROWSER &&
                        currentTab != null &&
                        !currentTab.private &&
                        currentTab.domain.isNotEmpty() &&
                        bookmarks.any { it.domain == currentTab.domain }
                    LightningBottomBar(
                        onBack = { webViews[activeIndex]?.goBack() },
                        onForward = { webViews[activeIndex]?.goForward() },
                        backEnabled = navStates[activeIndex]?.canGoBack == true,
                        forwardEnabled = navStates[activeIndex]?.canGoForward == true,
                        bookmarked = isCurrentBookmarked,
                        onNewTab = {
                            if (screen != LightningScreen.TABS) {
                                tabsReturnScreen = screen
                                if (screen == LightningScreen.BROWSER) {
                                    captureTabThumbnail(activeIndex)
                                }
                                screen = LightningScreen.TABS
                            } else {
                                screen = if (tabsReturnScreen == LightningScreen.BROWSER && tabs.isNotEmpty()) {
                                    LightningScreen.BROWSER
                                } else {
                                    LightningScreen.HOME
                                }
                            }
                        },
                        onLightningLongClick = { showQuickArc = true },
                        onBookmark = {
                            val tab = tabs.getOrNull(activeIndex)
                            if (screen == LightningScreen.BROWSER && tab != null &&
                                !tab.private && tab.domain.isNotEmpty()
                            ) {
                                toggleBookmark(tab)
                            } else {
                                screen = LightningScreen.BOOKMARKS
                            }
                        },
                        onMenu = { showMenu = true },
                    )
                }
            }
            QuickArcMenu(
                visible = showQuickArc,
                onDismiss = { showQuickArc = false },
                onHome = {
                    showQuickArc = false
                    if (screen == LightningScreen.BROWSER) {
                        captureTabThumbnail(activeIndex)
                    }
                    screenBackStack.clear()
                    screen = LightningScreen.HOME
                },
                onTabs = {
                    showQuickArc = false
                    if (screen != LightningScreen.TABS) {
                        tabsReturnScreen = screen
                        if (screen == LightningScreen.BROWSER) {
                            captureTabThumbnail(activeIndex)
                        }
                        screen = LightningScreen.TABS
                    }
                },
                onSettings = {
                    showQuickArc = false
                    if (screen == LightningScreen.BROWSER) {
                        captureTabThumbnail(activeIndex)
                    }
                    navigateTo(LightningScreen.SETTINGS)
                },
            )
            AppMenuSheet(
                visible = showMenu,
                onHide = { showMenu = false },
                onCopyUrl = { copyUrl() },
                onReload = { webViews[activeIndex]?.reload() },
                onShare = { shareUrl() },
                onFindInPage = { findInPageSignal++ },
                desktopMode = tabs.getOrNull(activeIndex)?.desktopMode == true,
                onToggleDesktop = {
                    val tab = tabs.getOrNull(activeIndex)
                    if (tab != null) {
                        tabs[activeIndex] = tab.copy(desktopMode = !tab.desktopMode)
                    }
                },
                onIncognito = { context.startActivity(Intent(context, IncognitoActivity::class.java)) },
                incognito = incognito,
                copyUrlVisible = screen == LightningScreen.BROWSER &&
                    tabs.getOrNull(activeIndex)?.url?.isNotEmpty() == true,
                onOpen = {
                    showMenu = false
                    navigateTo(it)
                },
            )
            pendingBookmarkTab?.let { tab ->
                BookmarkFolderDialog(
                    folders = bookmarkFolders,
                    onPick = { folder ->
                        bookmarks.add(0, Bookmark(tab.title, tab.domain, siteIcon(tab.domain), siteTone(tab.domain), folder))
                        pendingBookmarkTab = null
                        Toast.makeText(context, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { pendingBookmarkTab = null },
                )
            }
            if (showClearData) {
                val colors = LightningTheme.colors
                val haptics = rememberLightningHaptics()
                AlertDialog(
                    onDismissRequest = { showClearData = false },
                    title = {
                        Text(
                            text = stringResource(R.string.privacy_clear_confirm_title),
                            color = colors.onSurface,
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.privacy_clear_confirm_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearData = false }) {
                            Text(stringResource(R.string.dns_custom_cancel), color = colors.primary)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            haptics.reject()
                            showClearData = false
                            clearBrowsingData()
                        }) {
                            Text(stringResource(R.string.privacy_clear), color = colors.primary)
                        }
                    },
                )
            }
            if (showAddPinned) {
                val colors = LightningTheme.colors
                AlertDialog(
                    onDismissRequest = { showAddPinned = false },
                    title = {
                        Text(
                            text = stringResource(R.string.home_pinned_add_title),
                            color = colors.onSurface,
                        )
                    },
                    text = {
                        TextField(
                            value = pinnedDraft,
                            onValueChange = { pinnedDraft = it },
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.home_pinned_hint),
                                    color = colors.onSurfaceVariant,
                                )
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = colors.surfaceContainerHigh,
                                unfocusedContainerColor = colors.surfaceContainerHigh,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = colors.primary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPinned = false }) {
                            Text(stringResource(R.string.dns_custom_cancel), color = colors.primary)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val domain = pinnedDraft.trim()
                                    .removePrefix("https://")
                                    .removePrefix("http://")
                                    .substringBefore('/')
                                if (domain.isNotBlank() && domain.contains('.')) {
                                    if (pinnedSites.none { it.domain == domain }) {
                                        pinnedSites.add(PinnedSite(domain, siteIcon(domain), siteTone(domain), domain))
                                    }
                                    Toast.makeText(context, R.string.home_pinned_added, Toast.LENGTH_SHORT).show()
                                }
                                showAddPinned = false
                            },
                            enabled = pinnedDraft.isNotBlank(),
                        ) {
                            Text(stringResource(R.string.dns_custom_save), color = colors.primary)
                        }
                    },
                )
            }
            if (showDefaultBrowserPrompt) {
                DefaultBrowserPromptDialog(
                    onDismiss = {
                        BrowserStore.setDefaultBrowserPrompted(context, true)
                        showDefaultBrowserPrompt = false
                    },
                    onConfirm = {
                        BrowserStore.setDefaultBrowserPrompted(context, true)
                        showDefaultBrowserPrompt = false
                        openDefaultBrowserSettings(context, defaultBrowserLauncher)
                    },
                )
            }
            if (locked) {
                LockScreen(
                    error = authError,
                    onUnlock = { requestUnlock() },
                )
            }
        }
    }
    }
}

private fun isDefaultBrowser(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER)) {
            return roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)
        }
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.resolveActivity(
            intent,
            android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
    }
    val currentDefaultPackage = resolveInfo?.activityInfo?.packageName
    return currentDefaultPackage == context.packageName
}

private fun openDefaultBrowserSettings(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
        if (roleManager != null &&
            roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER) &&
            !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)
        ) {
            val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER)
            runCatching { launcher.launch(intent) }.onSuccess { return }
        }
    }
    val settingsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(settingsIntent) }.onFailure {
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(appDetails) }
    }
}

@Composable
private fun DefaultBrowserPromptDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LightningTheme.colors
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        icon = {
            ShapedIcon(
                icon = Icons.Filled.Language,
                shape = ExpressiveShapes.Cookie9,
                container = colors.primaryContainer,
                content = colors.onPrimaryContainer,
                size = 48.dp,
                iconSize = 24.dp,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.default_browser_prompt_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.default_browser_prompt_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.default_browser_prompt_dismiss),
                    color = colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.default_browser_prompt_confirm),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}

@Composable
private fun LockScreen(error: Boolean, onUnlock: () -> Unit) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ShapedIcon(
                Icons.Filled.Lock,
                ExpressiveShapes.Cookie9,
                colors.primaryContainer,
                colors.onPrimaryContainer,
                size = 120.dp,
                iconSize = 56.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.fingerprint_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(if (error) R.string.fingerprint_error else R.string.fingerprint_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = if (error) MaterialTheme.colorScheme.error else colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    haptics.confirm()
                    onUnlock()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                shape = CircleShape,
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.fingerprint_unlock))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkFolderDialog(
    folders: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LightningTheme.colors
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    if (creating) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bookmarks_new_folder_title)) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.bookmarks_folder_hint),
                            color = colors.onSurfaceVariant,
                        )
                    },
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceContainerHigh,
                        unfocusedContainerColor = colors.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) {
                    Text(stringResource(R.string.dns_custom_cancel), color = colors.primary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onPick(newName.trim()) },
                    enabled = newName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.dns_custom_save), color = colors.primary)
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.bookmarks_add_to_folder)) },
            text = {
                GroupedColumn(folders.size + 2) { i, shape ->
                    when {
                        i == 0 -> FolderOption(
                            depth = 0,
                            label = stringResource(R.string.bookmarks_root),
                            onClick = { onPick("") },
                            shape = shape,
                            index = i,
                        )
                        i == folders.size + 1 -> FolderOption(
                            depth = 0,
                            label = stringResource(R.string.bookmarks_new_folder),
                            onClick = { creating = true },
                            shape = shape,
                            index = i,
                        )
                        else -> FolderOption(
                            depth = 0,
                            label = folders[i - 1],
                            onClick = { onPick(folders[i - 1]) },
                            shape = shape,
                            index = i,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dns_custom_cancel), color = colors.primary)
                }
            },
        )
    }
}

@Composable
private fun FolderOption(
    depth: Int,
    label: String,
    onClick: () -> Unit,
    shape: Shape = ExpressiveShapes.grouped(0, 1),
    index: Int = 0,
) {
    val colors = LightningTheme.colors
    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            ShapedIcon(
                Icons.Filled.Folder,
                ExpressiveShapes.cycle(index),
                colors.surfaceContainer,
                colors.onSurfaceVariant,
                size = 32.dp,
                iconSize = 18.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
        }
    }
}

private val BAR_BOTTOM_PADDING = 88.dp

private fun hostOf(url: String): String =
    runCatching { android.net.Uri.parse(url).host }.getOrNull() ?: ""