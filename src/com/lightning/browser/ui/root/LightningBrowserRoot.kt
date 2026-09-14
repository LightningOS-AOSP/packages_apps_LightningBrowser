package com.lightning.browser.ui.root

import android.app.Activity
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.bar.LightningBottomBar
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
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.tabs.TabSwitcherScreen
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.ThemeMode
import kotlinx.coroutines.flow.distinctUntilChanged

enum class LightningScreen {
    HOME, TABS, SETTINGS, APPEARANCE, DNS, PRIVACY, SEARCH_ENGINE, DOWNLOADS, ABOUT, BOOKMARKS, HISTORY,
    SITE_PERMISSIONS, BROWSER
}

@Composable
fun LightningBrowserRoot() {
    var screen by rememberSaveable { mutableStateOf(LightningScreen.HOME) }
    var activeIndex by rememberSaveable { mutableStateOf(0) }
    var addressFocusSignal by remember { mutableStateOf(0) }
    val context = LocalContext.current

    var themeMode by remember { mutableStateOf(BrowserStore.loadThemeMode(context)) }
    var searchEngine by remember { mutableStateOf(BrowserStore.loadSearchEngine(context)) }
    var dnsProvider by remember { mutableStateOf(BrowserStore.loadDnsProvider(context)) }
    var customDnsUrl by remember { mutableStateOf(BrowserStore.loadCustomDnsUrl(context)) }
    var bypassTrusted by remember { mutableStateOf(BrowserStore.loadBypassTrusted(context)) }
    var adBlocking by remember { mutableStateOf(BrowserStore.loadAdBlocking(context)) }
    var fingerprintLock by remember { mutableStateOf(BrowserStore.loadFingerprintLock(context)) }
    var blockedCount by remember { mutableIntStateOf(0) }
    var dnsResolvedCount by remember { mutableIntStateOf(0) }

    val tabs = remember {
        mutableStateListOf(BrowserTab(title = "New tab", domain = ""))
    }
    val navStates = remember { mutableStateMapOf<Int, TabNavState>() }
    val webViews = remember { mutableStateMapOf<Int, WebView>() }
    val loadedUrls = remember { mutableStateMapOf<Int, String>() }
    val bookmarks = remember {
        mutableStateListOf<Bookmark>().apply { addAll(BrowserStore.loadBookmarks(context)) }
    }
    val history = remember {
        mutableStateListOf<HistoryEntry>().apply { addAll(BrowserStore.loadHistory(context)) }
    }
    val recentSearches = remember {
        mutableStateListOf<String>().apply { addAll(BrowserStore.loadRecentSearches(context)) }
    }
    val pinnedSites = remember {
        mutableStateListOf<PinnedSite>().apply { addAll(BrowserStore.loadPinnedSites(context)) }
    }
    var showMenu by remember { mutableStateOf(false) }
    var pendingBookmarkTab by remember { mutableStateOf<BrowserTab?>(null) }
    var authError by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(BrowserStore.loadFingerprintLock(context)) }
    var showClearData by remember { mutableStateOf(false) }
    var showAddPinned by remember { mutableStateOf(false) }
    var pinnedDraft by remember { mutableStateOf("") }

    LaunchedEffect(bookmarks) {
        snapshotFlow { bookmarks.toList() }.distinctUntilChanged().collect {
            BrowserStore.saveBookmarks(context, it)
        }
    }
    LaunchedEffect(history) {
        snapshotFlow { history.toList() }.distinctUntilChanged().collect {
            BrowserStore.saveHistory(context, it)
        }
    }
    LaunchedEffect(recentSearches) {
        snapshotFlow { recentSearches.toList() }.distinctUntilChanged().collect {
            BrowserStore.saveRecentSearches(context, it)
        }
    }
    LaunchedEffect(pinnedSites) {
        snapshotFlow { pinnedSites.toList() }.distinctUntilChanged().collect {
            BrowserStore.savePinnedSites(context, it)
        }
    }
    LaunchedEffect(themeMode) { BrowserStore.saveThemeMode(context, themeMode) }
    LaunchedEffect(searchEngine) { BrowserStore.saveSearchEngine(context, searchEngine) }
    LaunchedEffect(dnsProvider) { BrowserStore.saveDnsProvider(context, dnsProvider) }
    LaunchedEffect(customDnsUrl) { BrowserStore.saveCustomDnsUrl(context, customDnsUrl) }
    LaunchedEffect(bypassTrusted) { BrowserStore.saveBypassTrusted(context, bypassTrusted) }
    LaunchedEffect(adBlocking) { BrowserStore.saveAdBlocking(context, adBlocking) }
    LaunchedEffect(fingerprintLock) {
        BrowserStore.saveFingerprintLock(context, fingerprintLock)
        if (fingerprintLock) locked = true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && fingerprintLock) locked = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun recordVisit(title: String, domain: String) {
        if (domain.isBlank()) return
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
        if (trimmed.isBlank()) return
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

    val bookmarkFolders = bookmarks.map { it.folder }.filter { it.isNotBlank() }.distinct()

    val recentPages = history
        .distinctBy { it.domain }
        .take(3)
        .map { RecentPage(it.title, it.domain, it.icon, it.tone) }

    fun openSite(domain: String) {
        val url = if (domain.startsWith("http")) domain else "https://$domain"
        if (tabs.isEmpty()) {
            tabs.add(BrowserTab(title = domain, domain = hostOf(url), url = url))
            activeIndex = 0
        } else {
            val index = activeIndex.coerceIn(tabs.indices)
            tabs[index] = tabs[index].copy(url = url, domain = hostOf(url))
            activeIndex = index
        }
        screen = LightningScreen.BROWSER
    }

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

    BackHandler(enabled = screen != LightningScreen.HOME) {
        when (screen) {
            LightningScreen.BROWSER -> {
                val view = webViews[activeIndex]
                if (view?.canGoBack() == true) view.goBack() else screen = LightningScreen.HOME
            }
            LightningScreen.TABS, LightningScreen.SETTINGS -> screen = LightningScreen.HOME
            LightningScreen.BOOKMARKS, LightningScreen.HISTORY -> screen = LightningScreen.HOME
            LightningScreen.SITE_PERMISSIONS -> screen = LightningScreen.PRIVACY
            else -> screen = LightningScreen.SETTINGS
        }
    }

    LightningTheme(themeMode = themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightningTheme.colors.surface),
        ) {
            val showBar = screen == LightningScreen.HOME ||
                screen == LightningScreen.TABS ||
                screen == LightningScreen.BROWSER
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(bottom = if (showBar) BAR_BOTTOM_PADDING else 0.dp),
            ) {
                Crossfade(targetState = screen, animationSpec = tween(200), modifier = Modifier.fillMaxSize()) { s ->
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
                        onRemovePinned = { site ->
                            pinnedSites.remove(site)
                            Toast.makeText(context, R.string.home_pinned_removed, Toast.LENGTH_SHORT).show()
                        },
                    )
                    LightningScreen.TABS -> TabSwitcherScreen(
                        tabs = tabs,
                        onCloseTab = { tabs.removeAt(it) },
                        onCloseAll = { tabs.clear() },
                        onNewTab = {
                            tabs.add(BrowserTab(title = "New tab", domain = ""))
                            activeIndex = tabs.size - 1
                        },
                        onOpenTab = {
                            activeIndex = it
                            screen = LightningScreen.BROWSER
                        },
                    )
                    LightningScreen.BROWSER -> BrowserScreen(
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
                        recentSearches = recentSearches,
                        recentPages = recentPages,
                        bookmarks = bookmarks,
                        onUpdateTab = { index, tab -> tabs[index] = tab },
                        onRecordVisit = { title, domain -> recordVisit(title, domain) },
                        onSearchSubmitted = { recordSearch(it) },
                        onBackHome = { screen = LightningScreen.HOME },
                    )
                    LightningScreen.SETTINGS -> SettingsRootScreen(
                        themeMode = themeMode,
                        searchEngine = searchEngine,
                        onOpenAppearance = { screen = LightningScreen.APPEARANCE },
                        onOpenPrivacy = { screen = LightningScreen.PRIVACY },
                        onOpenSearchEngine = { screen = LightningScreen.SEARCH_ENGINE },
                        onOpenDownloads = { screen = LightningScreen.DOWNLOADS },
                        onOpenAbout = { screen = LightningScreen.ABOUT },
                        onBack = { screen = LightningScreen.HOME },
                    )
                    LightningScreen.SEARCH_ENGINE -> SearchEngineScreen(
                        engine = searchEngine,
                        onEngineSelect = { searchEngine = it },
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                    LightningScreen.DOWNLOADS -> DownloadsScreen(
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                    LightningScreen.ABOUT -> AboutScreen(
                        onOpenWebsite = { openSite(it) },
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                    LightningScreen.BOOKMARKS -> BookmarksScreen(
                        bookmarks = bookmarks,
                        onOpen = { openSite(it.domain) },
                        onDelete = { bookmarks.remove(it) },
                        onBack = { screen = LightningScreen.HOME },
                    )
                    LightningScreen.HISTORY -> HistoryScreen(
                        history = history,
                        onDelete = { history.remove(it) },
                        onClear = { history.clear() },
                        onBack = { screen = LightningScreen.HOME },
                    )
                    LightningScreen.APPEARANCE -> AppearanceScreen(
                        themeMode = themeMode,
                        onThemeModeSelect = { themeMode = it },
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                    LightningScreen.DNS -> DnsScreen(
                        provider = dnsProvider,
                        customUrl = customDnsUrl,
                        bypassTrusted = bypassTrusted,
                        resolvedCount = dnsResolvedCount,
                        onProviderSelect = { dnsProvider = it },
                        onCustomUrlChange = { customDnsUrl = it },
                        onBypassTrustedChange = { bypassTrusted = it },
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                    LightningScreen.PRIVACY -> PrivacyScreen(
                        provider = dnsProvider,
                        adBlocking = adBlocking,
                        blockedCount = blockedCount,
                        fingerprintLock = fingerprintLock,
                        onAdBlockingChange = { adBlocking = it },
                        onFingerprintChange = { fingerprintLock = it },
                        onOpenDns = { screen = LightningScreen.DNS },
                        onOpenSitePermissions = { screen = LightningScreen.SITE_PERMISSIONS },
                        onClearData = { showClearData = true },
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                    LightningScreen.SITE_PERMISSIONS -> SitePermissionsScreen(
                        onBack = { screen = LightningScreen.PRIVACY },
                    )
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
                    LightningBottomBar(
                        onBack = { webViews[activeIndex]?.goBack() },
                        onForward = { webViews[activeIndex]?.goForward() },
                        backEnabled = navStates[activeIndex]?.canGoBack == true,
                        forwardEnabled = navStates[activeIndex]?.canGoForward == true,
                        onNewTab = {
                            tabs.add(BrowserTab(title = "New tab", domain = ""))
                            activeIndex = tabs.size - 1
                            screen = LightningScreen.TABS
                        },
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
            AppMenuSheet(
                visible = showMenu,
                onHide = { showMenu = false },
                onCopyUrl = { copyUrl() },
                copyUrlVisible = screen == LightningScreen.BROWSER &&
                    tabs.getOrNull(activeIndex)?.url?.isNotEmpty() == true,
                onOpen = {
                    showMenu = false
                    screen = it
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
                            shape = RoundedCornerShape(14.dp),
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
            if (locked) {
                LockScreen(
                    error = authError,
                    onUnlock = { requestUnlock() },
                )
            }
        }
    }
}

@Composable
private fun LockScreen(error: Boolean, onUnlock: () -> Unit) {
    val colors = LightningTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = colors.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
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
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                shape = RoundedCornerShape(50),
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
                    shape = RoundedCornerShape(14.dp),
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
            title = { Text(stringResource(R.string.bookmarks_add_to_folder)) },
            text = {
                Column {
                    FolderOption(
                        depth = 0,
                        label = stringResource(R.string.bookmarks_root),
                        onClick = { onPick("") },
                    )
                    folders.forEach { folder ->
                        FolderOption(
                            depth = 0,
                            label = folder,
                            onClick = { onPick(folder) },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    FolderOption(
                        depth = 0,
                        label = stringResource(R.string.bookmarks_new_folder),
                        onClick = { creating = true },
                    )
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
private fun FolderOption(depth: Int, label: String, onClick: () -> Unit) {
    val colors = LightningTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
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

private val BAR_BOTTOM_PADDING = 72.dp

private fun hostOf(url: String): String =
    runCatching { android.net.Uri.parse(url).host }.getOrNull() ?: ""