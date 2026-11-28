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
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.delay
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
import com.lightning.browser.ui.browser.AppHandlerResolver
import com.lightning.browser.ui.browser.BrowserDownloads
import com.lightning.browser.ui.browser.BrowserScreen
import com.lightning.browser.ui.browser.ClipboardUrlHelper
import com.lightning.browser.ui.browser.PdfFilePrintAdapter
import com.lightning.browser.ui.browser.PdfPrint
import com.lightning.browser.ui.browser.PdfViewerScreen
import com.lightning.browser.ui.browser.PrintScreen
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
import com.lightning.browser.ui.settings.BrowsingScreen
import com.lightning.browser.ui.settings.ControlsScreen
import com.lightning.browser.ui.settings.CustomSearchEngine
import com.lightning.browser.ui.settings.DnsProvider
import com.lightning.browser.ui.settings.DnsScreen
import com.lightning.browser.ui.settings.DownloadSettingsScreen
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
import com.lightning.browser.ui.tabs.ClosedTab
import java.util.UUID
import com.lightning.browser.ui.tabs.RecentlyClosedScreen
import com.lightning.browser.ui.tabs.SavedPagesScreen
import com.lightning.browser.ui.tabs.TabSwitcherScreen
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.LocalHapticsEnabled
import com.lightning.browser.ui.theme.ThemeMode
import com.lightning.browser.ui.theme.rememberLightningHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class DuplicateTabWarning(
    val existingIndex: Int,
    val existingTabId: String,
    val title: String,
    val normalizedUrl: String,
    val forTabId: String = "",
    val createdTabId: String? = null,
)

enum class LightningScreen {
    HOME, TABS, SAVED_PAGES, RECENTLY_CLOSED, PRINT, PDF_VIEWER, SETTINGS, APPEARANCE, CONTROLS, BROWSING, DNS, PRIVACY, SEARCH_ENGINE, DOWNLOADS, DOWNLOAD_SETTINGS, ABOUT, BOOKMARKS, HISTORY,
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
    var editStartedFromHome by remember { mutableStateOf(false) }
    var clipboardPromptEnabled by remember { mutableStateOf(BrowserStore.loadClipboardPromptEnabled(context)) }
    var dismissedClipTimestamp by remember { mutableStateOf(BrowserStore.loadDismissedClipTimestamp(context)) }
    var clipboardHintPresent by remember { mutableStateOf(false) }
    var currentClipSig by remember { mutableStateOf(0L) }
    var checkedClipSig by remember { mutableStateOf<Long?>(null) }
    val windowInfo = LocalWindowInfo.current
    val clipboardLifecycleOwner = LocalLifecycleOwner.current

    fun dismissClipboard(sig: Long) {
        dismissedClipTimestamp = sig
        BrowserStore.saveDismissedClipTimestamp(context, sig)
    }

    fun refreshClipboardState(force: Boolean = false) {
        if (!clipboardPromptEnabled) {
            clipboardHintPresent = false
            return
        }
        val sig = ClipboardUrlHelper.clipSignature(context)
        if (!force && sig != 0L && sig == checkedClipSig) return
        currentClipSig = sig
        val expired = sig > 0L && (System.currentTimeMillis() - sig) > 10 * 60 * 1000L
        if (expired || sig == dismissedClipTimestamp) {
            clipboardHintPresent = false
            if (sig != 0L) checkedClipSig = sig
            return
        }
        val pending = ClipboardUrlHelper.isClassificationPending(context)
        clipboardHintPresent = ClipboardUrlHelper.hasClipboardUrlHint(context)
        if (!pending && sig != 0L) {
            checkedClipSig = sig
        }
    }

    LaunchedEffect(windowInfo.isWindowFocused, clipboardPromptEnabled) {
        if (windowInfo.isWindowFocused && clipboardPromptEnabled) {
            refreshClipboardState()
            if (ClipboardUrlHelper.isClassificationPending(context)) {
                delay(200L)
                refreshClipboardState(force = true)
            }
        }
    }

    DisposableEffect(clipboardLifecycleOwner, context, clipboardPromptEnabled) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            if (!clipboardPromptEnabled) return@OnPrimaryClipChangedListener
            checkedClipSig = null
            val sig = ClipboardUrlHelper.clipSignature(context)
            if (windowInfo.isWindowFocused) {
                // Link was copied inside Lightning Browser itself; do not prompt with a banner.
                dismissClipboard(sig)
                currentClipSig = sig
                clipboardHintPresent = false
                if (sig != 0L) checkedClipSig = sig
            } else {
                refreshClipboardState(force = true)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && clipboardPromptEnabled) {
                refreshClipboardState()
            }
        }
        clipboard?.addPrimaryClipChangedListener(clipListener)
        clipboardLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            clipboard?.removePrimaryClipChangedListener(clipListener)
            clipboardLifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val showClipboardBanner = clipboardPromptEnabled && clipboardHintPresent && currentClipSig != dismissedClipTimestamp

    LaunchedEffect(showClipboardBanner, currentClipSig) {
        if (showClipboardBanner) {
            delay(12_000L)
            dismissClipboard(currentClipSig)
        }
    }
    var browserEverOpened by remember { mutableStateOf(screen == LightningScreen.BROWSER) }
    var findInPageSignal by remember { mutableStateOf(0) }

    LaunchedEffect(screen) {
        if (screen == LightningScreen.BROWSER) {
            browserEverOpened = true
        }
    }

    LaunchedEffect(Unit) {
        android.os.Looper.myQueue().addIdleHandler {
            runCatching { android.webkit.WebSettings.getDefaultUserAgent(context) }
            false
        }
    }
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
    var customSearchEngines by remember { mutableStateOf(BrowserStore.loadCustomSearchEngines(context)) }
    var activeSearchEngineId by remember { mutableStateOf(BrowserStore.loadActiveSearchEngineId(context)) }
    var searchEngine by remember { mutableStateOf(BrowserStore.loadSearchEngine(context)) }
    val activeCustomEngine = customSearchEngines.firstOrNull { it.id == activeSearchEngineId }
    var dnsProvider by remember { mutableStateOf(BrowserStore.loadDnsProvider(context)) }
    var customDnsUrl by remember { mutableStateOf(BrowserStore.loadCustomDnsUrl(context)) }
    var bypassTrusted by remember { mutableStateOf(BrowserStore.loadBypassTrusted(context)) }
    var adBlocking by remember { mutableStateOf(BrowserStore.loadAdBlocking(context)) }
    var fingerprintLock by remember { mutableStateOf(BrowserStore.loadFingerprintLock(context)) }
    var hapticsEnabled by remember { mutableStateOf(BrowserStore.loadHapticFeedback(context)) }
    var quickArcEnabled by remember { mutableStateOf(BrowserStore.loadQuickArcEnabled(context)) }
    var tabPreviewsEnabled by remember { mutableStateOf(BrowserStore.loadTabPreviewsEnabled(context)) }
    var readingTimeEnabled by remember { mutableStateOf(BrowserStore.loadReadingTimeEnabled(context)) }
    var waybackEnabled by remember { mutableStateOf(BrowserStore.loadWaybackEnabled(context)) }
    var duplicateTabWarningEnabled by remember { mutableStateOf(BrowserStore.loadDuplicateTabWarningEnabled(context)) }
    var recentlyClosedEnabled by remember { mutableStateOf(BrowserStore.loadRecentlyClosedEnabled(context)) }
    var downloadFolder by remember { mutableStateOf(BrowserStore.loadDownloadFolder(context)) }
    var customDownloadPath by remember { mutableStateOf(BrowserStore.loadCustomDownloadPath(context)) }
    var askBeforeDownload by remember { mutableStateOf(BrowserStore.loadDownloadAskBefore(context)) }
    var deletePrivateDownloads by remember { mutableStateOf(BrowserStore.loadDeletePrivateDownloads(context)) }
    var otherFilesAsk by remember { mutableStateOf(BrowserStore.loadDownloadOtherAsk(context)) }
    val contentPreferences = remember {
        mutableStateMapOf<String, BrowserStore.ContentPreference>().apply {
            putAll(BrowserStore.loadContentPreferences(context))
        }
    }
    val privateDownloadedFiles = remember { mutableStateListOf<String>() }
    var pdfTitle by remember { mutableStateOf("") }
    var pdfUrl by remember { mutableStateOf<String?>(null) }
    var pdfUserAgent by remember { mutableStateOf<String?>(null) }
    var pdfFile by remember { mutableStateOf<java.io.File?>(null) }
    var pdfLoading by remember { mutableStateOf(false) }
    var pdfError by remember { mutableStateOf(false) }
    var pdfInitialPage by remember { mutableIntStateOf(1) }
    var pdfCurrentPage by remember { mutableIntStateOf(1) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var pdfNightMode by remember { mutableStateOf(BrowserStore.loadPdfNightMode(context)) }
    var printPdfFile by remember { mutableStateOf<java.io.File?>(null) }
    var printPdfPageCount by remember { mutableIntStateOf(0) }
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
    val closedTabs = remember {
        mutableStateListOf<ClosedTab>().apply { if (!incognito) addAll(BrowserStore.loadClosedTabs(context)) }
    }
    var showMenu by remember { mutableStateOf(false) }
    var printPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showQuickArc by remember { mutableStateOf(false) }
    var tabsReturnScreen by remember { mutableStateOf(LightningScreen.HOME) }
    var pendingBookmarkTab by remember { mutableStateOf<BrowserTab?>(null) }
    var authError by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(BrowserStore.loadFingerprintLock(context)) }
    var showClearData by remember { mutableStateOf(false) }
    var showAddPinned by remember { mutableStateOf(false) }
    var pinnedDraft by remember { mutableStateOf("") }
    var duplicateTabWarning by remember { mutableStateOf<DuplicateTabWarning?>(null) }
    val suppressedDuplicateKeys = remember { HashSet<String>() }
    val freshTabIds = remember { HashSet<String>() }
    val closingTabIds = remember { mutableStateListOf<String>() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    fun showBanner(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }
    fun showBanner(resId: Int) = showBanner(context.getString(resId))

    LaunchedEffect(duplicateTabWarning) {
        val current = duplicateTabWarning ?: return@LaunchedEffect
        delay(8_000L)
        if (duplicateTabWarning == current) {
            if (current.forTabId.isNotEmpty()) {
                suppressedDuplicateKeys.add("${current.forTabId}|${current.normalizedUrl}")
            }
            duplicateTabWarning = null
        }
    }
    LaunchedEffect(activeIndex) {
        val activeId = tabs.getOrNull(activeIndex)?.id
        val current = duplicateTabWarning
        if (current != null && current.forTabId.isNotEmpty() && current.forTabId != activeId) {
            duplicateTabWarning = null
        }
    }
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
    LaunchedEffect(closedTabs) {
        snapshotFlow { closedTabs.toList() }.distinctUntilChanged().collect {
            if (!incognito) BrowserStore.saveClosedTabs(context, it)
        }
    }
    LaunchedEffect(themeMode) { if (!incognito) BrowserStore.saveThemeMode(context, themeMode) }
    LaunchedEffect(customSearchEngines) { if (!incognito) BrowserStore.saveCustomSearchEngines(context, customSearchEngines) }
    LaunchedEffect(activeSearchEngineId) { if (!incognito) BrowserStore.saveActiveSearchEngineId(context, activeSearchEngineId) }
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
    LaunchedEffect(quickArcEnabled) {
        if (!incognito) BrowserStore.saveQuickArcEnabled(context, quickArcEnabled)
        if (!quickArcEnabled) showQuickArc = false
    }
    LaunchedEffect(tabPreviewsEnabled) {
        if (!incognito) BrowserStore.saveTabPreviewsEnabled(context, tabPreviewsEnabled)
        if (!tabPreviewsEnabled) tabThumbnails.clear()
    }
    LaunchedEffect(readingTimeEnabled) {
        if (!incognito) BrowserStore.saveReadingTimeEnabled(context, readingTimeEnabled)
    }
    LaunchedEffect(clipboardPromptEnabled) {
        if (!incognito) BrowserStore.saveClipboardPromptEnabled(context, clipboardPromptEnabled)
        if (!clipboardPromptEnabled) clipboardHintPresent = false
    }
    LaunchedEffect(waybackEnabled) {
        if (!incognito) BrowserStore.saveWaybackEnabled(context, waybackEnabled)
    }
    LaunchedEffect(duplicateTabWarningEnabled) {
        if (!incognito) BrowserStore.saveDuplicateTabWarningEnabled(context, duplicateTabWarningEnabled)
        if (!duplicateTabWarningEnabled) duplicateTabWarning = null
    }
    LaunchedEffect(recentlyClosedEnabled) {
        if (!incognito) BrowserStore.saveRecentlyClosedEnabled(context, recentlyClosedEnabled)
    }
    LaunchedEffect(downloadFolder) {
        if (!incognito) BrowserStore.saveDownloadFolder(context, downloadFolder)
    }
    LaunchedEffect(customDownloadPath) {
        if (!incognito) BrowserStore.saveCustomDownloadPath(context, customDownloadPath)
    }
    LaunchedEffect(askBeforeDownload) {
        if (!incognito) BrowserStore.saveDownloadAskBefore(context, askBeforeDownload)
    }
    LaunchedEffect(deletePrivateDownloads) {
        if (!incognito) BrowserStore.saveDeletePrivateDownloads(context, deletePrivateDownloads)
    }
    LaunchedEffect(otherFilesAsk) {
        if (!incognito) BrowserStore.saveDownloadOtherAsk(context, otherFilesAsk)
    }
    DisposableEffect(incognito) {
        onDispose {
            if (incognito && deletePrivateDownloads) {
                BrowserStore.popAllPrivateDownloads(context).forEach { rec ->
                    BrowserDownloads.deleteByFileName(context, rec.fileName, rec.relativePath)
                }
                privateDownloadedFiles.clear()
            }
        }
    }

    fun captureTabThumbnail(index: Int) {
        if (!tabPreviewsEnabled) return
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
            showBanner(R.string.bookmark_removed)
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
        showBanner(R.string.privacy_cleared)
    }

    fun copyUrl() {
        val url = tabs.getOrNull(activeIndex)?.url ?: return
        if (url.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("url", url))
        showBanner(R.string.menu_copy_url_copied)
    }

    fun savePageAsPdf(
        mediaSize: PrintAttributes.MediaSize,
        monochrome: Boolean,
        selectedPages: Set<Int>,
    ) {
        val webView = webViews[activeIndex] ?: return
        val tab = tabs.getOrNull(activeIndex) ?: return
        if (webView.width <= 0 || webView.height <= 0) return

        val widthPt = (mediaSize.widthMils * 72f / 1000f).toInt().coerceAtLeast(100)
        val heightPt = (mediaSize.heightMils * 72f / 1000f).toInt().coerceAtLeast(100)
        val fullContentPx = (webView.contentHeight * webView.scale).toInt().coerceAtLeast(webView.height)
        val scale = widthPt.toFloat() / webView.width.toFloat()
        val slicePx = (heightPt / scale).coerceAtLeast(1f)
        val totalPages = kotlin.math.ceil(fullContentPx / slicePx).toInt().coerceIn(1, 50)
        val pagesToWrite = selectedPages.filter { it in 1..totalPages }.sorted()
            .ifEmpty { listOf(1) }

        val pdf = android.graphics.pdf.PdfDocument()
        val monoPaint = if (monochrome) {
            android.graphics.Paint().apply {
                colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix().apply { setSaturation(0f) },
                )
            }
        } else {
            null
        }

        pagesToWrite.forEachIndexed { idx, pageNumber ->
            val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(widthPt, heightPt, idx + 1).create()
            val page = pdf.startPage(info)
            val canvas = page.canvas
            val layer = if (monoPaint != null) {
                canvas.saveLayer(0f, 0f, widthPt.toFloat(), heightPt.toFloat(), monoPaint)
            } else {
                canvas.save()
            }
            canvas.scale(scale, scale)
            canvas.translate(0f, -((pageNumber - 1) * slicePx))
            webView.draw(canvas)
            canvas.restoreToCount(layer)
            pdf.finishPage(page)
        }

        val baos = java.io.ByteArrayOutputStream()
        runCatching { pdf.writeTo(baos) }
        pdf.close()

        val bytes = baos.toByteArray()
        if (bytes.isEmpty()) {
            showBanner(R.string.menu_save_offline_failed)
            return
        }
        BrowserDownloads.savePdfBytes(context, tab.title, tab.domain, bytes) { savedName ->
            val message = if (savedName != null) {
                context.getString(R.string.menu_save_offline_saved, savedName)
            } else {
                context.getString(R.string.menu_save_offline_failed)
            }
            showBanner(message)
        }
    }

    fun printCurrentPage(attributes: PrintAttributes = PrintAttributes.Builder().build()) {
        val webView = webViews[activeIndex] ?: return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = tabs.getOrNull(activeIndex)?.title
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name)
        printManager.print(
            jobName,
            webView.createPrintDocumentAdapter(jobName),
            attributes,
        )
    }

    fun openPrintSheet() {
        printPdfFile = null
        printPdfPageCount = 0
        val wv = webViews[activeIndex]
        printPreviewBitmap = if (wv != null && wv.width > 0 && wv.height > 0) {
            runCatching {
                val scale = (360f / wv.width).coerceAtMost(0.5f)
                val fullHeightPx = (wv.contentHeight * wv.scale).toInt().coerceIn(wv.height, 4096)
                val w = (wv.width * scale).toInt().coerceAtLeast(1)
                val h = (fullHeightPx * scale).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.scale(scale, scale)
                    wv.draw(canvas)
                }
            }.getOrNull()
        } else {
            null
        }
        navigateTo(LightningScreen.PRINT)
    }

    fun openPdfPrintSheet(file: java.io.File) {
        printPdfFile = file
        printPdfPageCount = pdfPageCount.coerceAtLeast(1)
        printPreviewBitmap = null
        pdfInitialPage = pdfCurrentPage.coerceAtLeast(1)
        navigateTo(LightningScreen.PRINT)
    }

    fun startPdfDownload(url: String, userAgent: String?, title: String, resetPage: Boolean) {
        pdfFile?.let { runCatching { it.delete() } }
        pdfUrl = url
        pdfUserAgent = userAgent
        pdfTitle = title
        pdfFile = null
        pdfLoading = true
        pdfError = false
        pdfPageCount = 0
        if (resetPage) {
            val savedPage = if (incognito) {
                1
            } else {
                BrowserStore.loadPdfPosition(context, BrowserStore.pdfPositionKey(url, title))
            }
            pdfInitialPage = savedPage
            pdfCurrentPage = savedPage
        }
        BrowserDownloads.downloadToCacheFile(context, url, userAgent, title) { file ->
            pdfLoading = false
            if (file != null) {
                pdfFile = file
            } else {
                pdfError = true
            }
        }
    }

    fun saveOfflinePage() {
        val webView = webViews[activeIndex] ?: return
        val tab = tabs.getOrNull(activeIndex) ?: return
        val tempFile = java.io.File(context.cacheDir, "offline_${System.currentTimeMillis()}.mhtml")
        webView.saveWebArchive(tempFile.absolutePath, false) { savedPath ->
            val file = savedPath?.let { java.io.File(it) }
            if (file != null && file.exists()) {
                BrowserDownloads.saveOfflineArchive(context, tab.title, tab.domain, file) { savedName ->
                    val message = if (savedName != null) {
                        context.getString(R.string.menu_save_offline_saved, savedName)
                    } else {
                        context.getString(R.string.menu_save_offline_failed)
                    }
                    showBanner(message)
                }
            } else {
                runCatching { tempFile.delete() }
                showBanner(R.string.menu_save_offline_failed)
            }
        }
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

    fun normalizeHostForDup(rawHost: String): String {
        var host = rawHost.lowercase()
        while (host.startsWith("www.") || host.startsWith("m.") || host.startsWith("mobile.")) {
            host = when {
                host.startsWith("www.") -> host.removePrefix("www.")
                host.startsWith("m.") -> host.removePrefix("m.")
                else -> host.removePrefix("mobile.")
            }
        }
        host = host.replace(
            Regex("^([a-z-]+)\\.m\\.(wikipedia|wiktionary|wikiquote|wikibooks|wikisource|wikinews|wikiversity|wikivoyage)\\.org$"),
            "$1.$2.org",
        )
        return host
    }

    fun normalizeQueryForDup(uri: Uri, host: String, path: String): String {
        val rawQuery = uri.encodedQuery ?: return ""
        val ignored = buildSet {
            add("utm_source"); add("utm_medium"); add("utm_campaign")
            add("utm_term"); add("utm_content"); add("fbclid"); add("gclid"); add("si")
            if (host == "search.brave.com" && path == "/search" && uri.getQueryParameter("q") != null) {
                add("source")
            }
        }
        val kept = rawQuery.split("&")
            .filter { it.isNotEmpty() }
            .mapNotNull { part ->
                val eq = part.indexOf('=')
                val rawKey = if (eq >= 0) part.substring(0, eq) else part
                val key = Uri.decode(rawKey.replace("+", " "))
                if (key in ignored) return@mapNotNull null
                val rawVal = if (eq >= 0) part.substring(eq + 1) else ""
                val value = Uri.decode(rawVal.replace("+", " "))
                if (eq >= 0) "${Uri.encode(key)}=${Uri.encode(value)}" else Uri.encode(key)
            }
            .sorted()
        return if (kept.isEmpty()) "" else "?${kept.joinToString("&")}"
    }

    fun normalizeTabUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "about:blank") return ""
        return runCatching {
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme?.lowercase().orEmpty()
            val rawHost = uri.host.orEmpty()
            if (scheme.isNotEmpty() && rawHost.isNotEmpty()) {
                val host = normalizeHostForDup(rawHost)
                val path = uri.path.orEmpty().let { if (it == "/") "" else it.trimEnd('/') }
                val query = normalizeQueryForDup(uri, host, path)
                val prefix = if (scheme == "http" || scheme == "https") "" else "$scheme://"
                "$prefix$host$path$query"
            } else {
                trimmed.trimEnd('/').lowercase()
            }
        }.getOrDefault(trimmed.trimEnd('/').lowercase())
    }

    fun checkDuplicateTab(url: String, targetIndex: Int, createdTabId: String? = null) {
        val targetTab = tabs.getOrNull(targetIndex)
        val targetTabId = targetTab?.id.orEmpty()
        if (!duplicateTabWarningEnabled) {
            if (duplicateTabWarning?.forTabId == targetTabId) duplicateTabWarning = null
            return
        }
        val norm = normalizeTabUrl(url)
        if (norm.isEmpty()) {
            if (duplicateTabWarning?.forTabId == targetTabId) duplicateTabWarning = null
            return
        }
        val suppressKey = "$targetTabId|$norm"
        if (targetTabId.isNotEmpty() && suppressKey in suppressedDuplicateKeys) {
            if (duplicateTabWarning?.forTabId == targetTabId) duplicateTabWarning = null
            return
        }
        val targetPrivate = targetTab?.private ?: incognito
        val match = tabs.withIndex().firstOrNull { (i, tab) ->
            i != targetIndex &&
                tab.id !in closingTabIds &&
                tab.private == targetPrivate &&
                normalizeTabUrl(tab.url) == norm
        }
        if (match != null) {
            duplicateTabWarning = DuplicateTabWarning(
                existingIndex = match.index,
                existingTabId = match.value.id,
                title = match.value.title.ifBlank { match.value.domain.ifBlank { url } },
                normalizedUrl = norm,
                forTabId = targetTabId,
                createdTabId = createdTabId,
            )
        } else if (duplicateTabWarning?.forTabId == targetTabId) {
            duplicateTabWarning = null
        }
    }

    fun recordClosedTab(tab: BrowserTab) {
        if (!recentlyClosedEnabled || incognito || tab.private || tab.url.isBlank()) return
        val now = System.currentTimeMillis()
        closedTabs.removeAll { it.url == tab.url || now - it.closedAt > BrowserStore.CLOSED_TAB_EXPIRY_MS }
        closedTabs.add(
            0,
            ClosedTab(
                title = tab.title,
                domain = tab.domain,
                url = tab.url,
                desktopMode = tab.desktopMode,
                closedAt = now,
            ),
        )
        while (closedTabs.size > BrowserStore.MAX_CLOSED_TABS) {
            closedTabs.removeAt(closedTabs.lastIndex)
        }
    }

    fun cleanupPrivateTabDownloads(tabId: String) {
        if (incognito && deletePrivateDownloads) {
            BrowserStore.removePrivateDownloadsForTab(context, tabId).forEach { rec ->
                BrowserDownloads.deleteByFileName(context, rec.fileName, rec.relativePath)
            }
        }
    }

    fun removeTabAt(index: Int) {
        if (index !in tabs.indices) return
        val removed = tabs.removeAt(index)
        cleanupPrivateTabDownloads(removed.id)
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
    }

    fun openSite(domain: String) {
        val url = if (domain.startsWith("http")) domain else "https://$domain"
        var createdId: String? = null
        val targetIdx = if (tabs.isEmpty()) {
            val tab = BrowserTab(title = domain, domain = hostOf(url), url = url, private = incognito)
            createdId = tab.id
            tabs.add(tab)
            0
        } else {
            val index = activeIndex.coerceIn(tabs.indices)
            tabs[index] = tabs[index].copy(url = url, domain = hostOf(url))
            index
        }
        activeIndex = targetIdx
        checkDuplicateTab(url, targetIdx, createdId)
        screenBackStack.clear()
        screen = LightningScreen.BROWSER
    }

    fun openInNewTab(rawUrl: String) {
        val url = if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
        val host = hostOf(url)
        val existingEmpty = tabs.indexOfFirst { it.url.isEmpty() }
        var createdId: String? = null
        val targetIdx = if (existingEmpty >= 0) {
            tabs[existingEmpty] = tabs[existingEmpty].copy(title = host, domain = host, url = url)
            existingEmpty
        } else {
            val tab = BrowserTab(title = host, domain = host, url = url, private = incognito)
            createdId = tab.id
            tabs.add(tab)
            tabs.size - 1
        }
        activeIndex = targetIdx
        checkDuplicateTab(url, targetIdx, createdId)
        screenBackStack.clear()
        screen = LightningScreen.BROWSER
    }

    fun openSavedPage(title: String, uri: String) {
        val existingEmpty = tabs.indexOfFirst { it.url.isEmpty() }
        var createdId: String? = null
        val targetIdx = if (existingEmpty >= 0) {
            tabs[existingEmpty] = tabs[existingEmpty].copy(title = title, domain = "Saved offline", url = uri)
            existingEmpty
        } else {
            val tab = BrowserTab(title = title, domain = "Saved offline", url = uri, private = incognito)
            createdId = tab.id
            tabs.add(tab)
            tabs.size - 1
        }
        activeIndex = targetIdx
        checkDuplicateTab(uri, targetIdx, createdId)
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

    BackHandler(enabled = incognito && !showQuickArc && screen == LightningScreen.HOME) {
        (context as? android.app.Activity)?.finish()
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
                screen = if (tabsReturnScreen == LightningScreen.BROWSER) {
                    if (tabs.getOrNull(activeIndex)?.url?.isNotEmpty() == true) {
                        LightningScreen.BROWSER
                    } else {
                        LightningScreen.HOME
                    }
                } else {
                    tabsReturnScreen
                }
            }
            LightningScreen.SETTINGS, LightningScreen.BOOKMARKS, LightningScreen.HISTORY -> {
                navigateBack(LightningScreen.HOME)
            }
            LightningScreen.SITE_PERMISSIONS -> navigateBack(LightningScreen.PRIVACY)
            LightningScreen.SAVED_PAGES, LightningScreen.RECENTLY_CLOSED -> navigateBack(LightningScreen.TABS)
            LightningScreen.PRINT -> {
                val returnToPdf = printPdfFile != null
                printPdfFile = null
                printPdfPageCount = 0
                navigateBack(if (returnToPdf) LightningScreen.PDF_VIEWER else LightningScreen.BROWSER)
            }
            LightningScreen.PDF_VIEWER -> {
                pdfFile?.let { runCatching { it.delete() } }
                pdfFile = null
                pdfUrl = null
                pdfUserAgent = null
                navigateBack(LightningScreen.BROWSER)
            }
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
                val keepBrowserMounted = screen == LightningScreen.BROWSER || browserEverOpened
                if (keepBrowserMounted) {
                    BrowserScreen(
                        tabs = tabs,
                        activeIndex = activeIndex,
                        navStates = navStates,
                        webViews = webViews,
                        loadedUrls = loadedUrls,
                        adBlocking = adBlocking,
                        searchEngine = searchEngine,
                        customSearchEngines = customSearchEngines,
                        activeCustomEngine = activeCustomEngine,
                        dnsProvider = dnsProvider,
                        customDnsUrl = customDnsUrl,
                        bypassTrusted = bypassTrusted,
                        onBlocked = { blockedCount++ },
                        onDnsResolved = { dnsResolvedCount++ },
                        focusSignal = addressFocusSignal,
                        onFocusConsumed = { addressFocusSignal = 0 },
                        browserVisible = screen == LightningScreen.BROWSER,
                        editStartedFromHome = editStartedFromHome,
                        onClearEditFromHome = { editStartedFromHome = false },
                        onCancelFromHome = {
                            editStartedFromHome = false
                            screen = LightningScreen.HOME
                        },
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
                        onOpenInNewTab = { openInNewTab(it) },
                        duplicateTabWarning = duplicateTabWarning,
                        onCheckDuplicateTab = { url -> checkDuplicateTab(url, activeIndex) },
                        onSwitchDuplicateTab = {
                            val warning = duplicateTabWarning
                            duplicateTabWarning = null
                            if (warning != null) {
                                val toRemoveId = warning.createdTabId
                                    ?: tabs.getOrNull(activeIndex)?.id?.takeIf { it in freshTabIds }
                                val target = tabs.indexOfFirst { it.id == warning.existingTabId }
                                if (target >= 0) {
                                    activeIndex = target
                                }
                                if (toRemoveId != null && toRemoveId != warning.existingTabId) {
                                    freshTabIds.remove(toRemoveId)
                                    val idxNow = tabs.indexOfFirst { it.id == toRemoveId }
                                    if (idxNow >= 0) {
                                        webViews[idxNow]?.stopLoading()
                                    }
                                    if (toRemoveId !in closingTabIds) {
                                        closingTabIds.add(toRemoveId)
                                    }
                                    coroutineScope.launch {
                                        delay(260L)
                                        closingTabIds.remove(toRemoveId)
                                        val activeId = tabs.getOrNull(activeIndex)?.id
                                        val cIdx = tabs.indexOfFirst { it.id == toRemoveId }
                                        if (cIdx >= 0 && tabs.size > 1) {
                                            removeTabAt(cIdx)
                                            if (activeId != null) {
                                                val updatedActive = tabs.indexOfFirst { it.id == activeId }
                                                if (updatedActive >= 0) activeIndex = updatedActive
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onDismissDuplicateTab = {
                            duplicateTabWarning?.let { warning ->
                                val key = if (warning.forTabId.isNotEmpty()) {
                                    "${warning.forTabId}|${warning.normalizedUrl}"
                                } else {
                                    warning.normalizedUrl
                                }
                                suppressedDuplicateKeys.add(key)
                            }
                            tabs.getOrNull(activeIndex)?.id?.let { freshTabIds.remove(it) }
                            duplicateTabWarning = null
                        },
                        showClipboardBanner = showClipboardBanner,
                        dismissedClipTimestamp = dismissedClipTimestamp,
                        onOpenClipboardBanner = {
                            val url = ClipboardUrlHelper.readClipboardUrl(context)
                            dismissClipboard(currentClipSig)
                            if (url != null) {
                                openInNewTab(url)
                            }
                        },
                        onDismissClipboardBanner = { dismissClipboard(currentClipSig) },
                        onDismissClipSig = { dismissClipboard(it) },
                        onPrintRequest = { openPrintSheet() },
                        readingTimeEnabled = readingTimeEnabled,
                        clipboardPromptEnabled = clipboardPromptEnabled,
                        waybackEnabled = waybackEnabled,
                        onDownloadSaved = { name, relPath ->
                            if (incognito && name.isNotBlank()) {
                                privateDownloadedFiles.add(name)
                                BrowserStore.addPrivateDownload(
                                    context,
                                    tabs.getOrNull(activeIndex)?.id.orEmpty(),
                                    name,
                                    relPath,
                                )
                            }
                        },
                        onShowMessage = ::showBanner,
                        onOpenPdf = { url, userAgent, title ->
                            navigateTo(LightningScreen.PDF_VIEWER)
                            startPdfDownload(url, userAgent, title, resetPage = true)
                        },
                        onBackHome = {
                            captureTabThumbnail(activeIndex)
                            screen = LightningScreen.HOME
                        },
                    )
                }
                AnimatedVisibility(
                    visible = screen != LightningScreen.BROWSER,
                    enter = EnterTransition.None,
                    exit = fadeOut(tween(60)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LightningTheme.colors.surface)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent().changes.forEach { it.consume() }
                                    }
                                }
                            },
                    ) {
                        Crossfade(targetState = screen, animationSpec = tween(90), modifier = Modifier.fillMaxSize()) { s ->
                            when (s) {
                    LightningScreen.HOME -> HomeScreen(
                        recentPages = recentPages,
                        pinnedSites = pinnedSites,
                        onOpenSearch = {
                            editStartedFromHome = true
                            addressFocusSignal++
                            screen = LightningScreen.BROWSER
                        },
                        onOpenSite = { openSite(it) },
                        showClipboardBanner = showClipboardBanner,
                        onOpenClipboardBanner = {
                            val url = ClipboardUrlHelper.readClipboardUrl(context)
                            dismissClipboard(currentClipSig)
                            if (url != null) {
                                openInNewTab(url)
                            }
                        },
                        onDismissClipboardBanner = { dismissClipboard(currentClipSig) },
                        onOpenProfile = { screen = LightningScreen.SETTINGS },
                        onAddPinned = {
                            pinnedDraft = ""
                            showAddPinned = true
                        },
                        incognito = incognito,
                        onRemovePinned = { site ->
                            pinnedSites.remove(site)
                            showBanner(R.string.home_pinned_removed)
                        },
                    )
                    LightningScreen.TABS -> TabSwitcherScreen(
                        tabs = tabs,
                        thumbnails = if (tabPreviewsEnabled) tabThumbnails else emptyMap(),
                        onCloseTab = { index ->
                            if (index in tabs.indices) {
                                val removed = tabs.removeAt(index)
                                cleanupPrivateTabDownloads(removed.id)
                                recordClosedTab(removed)
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
                            if (incognito && deletePrivateDownloads) {
                                BrowserStore.popAllPrivateDownloads(context).forEach { rec ->
                                    BrowserDownloads.deleteByFileName(context, rec.fileName, rec.relativePath)
                                }
                                privateDownloadedFiles.clear()
                            }
                            tabs.forEach { recordClosedTab(it) }
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
                            val newTab = BrowserTab(title = "New tab", domain = "", private = incognito)
                            freshTabIds.add(newTab.id)
                            tabs.add(newTab)
                            activeIndex = tabs.size - 1
                        },
                        onOpenTab = {
                            activeIndex = it
                            screen = LightningScreen.BROWSER
                        },
                    )
                    LightningScreen.SAVED_PAGES -> SavedPagesScreen(
                        onOpenPage = { title, uri -> openSavedPage(title, uri) },
                        onBack = { navigateBack(LightningScreen.TABS) },
                    )
                    LightningScreen.RECENTLY_CLOSED -> RecentlyClosedScreen(
                        closedTabs = closedTabs,
                        onRestoreTab = { closed ->
                            closedTabs.removeAll { it.id == closed.id }
                            val restored = BrowserTab(
                                title = closed.title,
                                domain = closed.domain,
                                url = closed.url,
                                desktopMode = closed.desktopMode,
                                private = incognito,
                            )
                            tabs.add(restored)
                            val targetIdx = tabs.size - 1
                            activeIndex = targetIdx
                            checkDuplicateTab(closed.url, targetIdx, restored.id)
                            screen = LightningScreen.BROWSER
                        },
                        onClearAll = {
                            closedTabs.clear()
                        },
                        onBack = { navigateBack(LightningScreen.TABS) },
                    )
                    LightningScreen.PRINT -> {
                        val activePdf = printPdfFile
                        val pdfDomain = pdfUrl?.let { runCatching { android.net.Uri.parse(it).host }.getOrNull() }.orEmpty()
                        PrintScreen(
                            pageTitle = if (activePdf != null) pdfTitle.ifBlank { "PDF" } else tabs.getOrNull(activeIndex)?.title.orEmpty(),
                            pageDomain = if (activePdf != null) pdfDomain else tabs.getOrNull(activeIndex)?.domain.orEmpty(),
                            previewBitmap = printPreviewBitmap,
                            pdfFile = activePdf,
                            pdfPageCount = printPdfPageCount,
                            onBack = {
                                val returnToPdf = printPdfFile != null
                                printPdfFile = null
                                printPdfPageCount = 0
                                navigateBack(if (returnToPdf) LightningScreen.PDF_VIEWER else LightningScreen.BROWSER)
                            },
                            onSavePdf = { mediaSize, mono, pages ->
                                val file = printPdfFile
                                if (file != null) {
                                    PdfPrint.exportPdfToDownloads(
                                        context = context,
                                        scope = coroutineScope,
                                        pdfFile = file,
                                        title = pdfTitle.ifBlank { "document" },
                                        domain = pdfDomain,
                                        mediaSize = mediaSize,
                                        monochrome = mono,
                                        selectedPages = pages,
                                    ) { savedName ->
                                        if (savedName != null && incognito) {
                                            privateDownloadedFiles.add(savedName)
                                            BrowserStore.addPrivateDownload(
                                                context,
                                                tabs.getOrNull(activeIndex)?.id.orEmpty(),
                                                savedName,
                                                BrowserStore.effectiveDownloadRelativePath(context),
                                            )
                                        }
                                        val message = if (savedName != null) {
                                            context.getString(R.string.menu_save_offline_saved, savedName)
                                        } else {
                                            context.getString(R.string.menu_save_offline_failed)
                                        }
                                        showBanner(message)
                                    }
                                } else {
                                    savePageAsPdf(mediaSize, mono, pages)
                                }
                            },
                            onPrint = { attrs, pages ->
                                val file = printPdfFile
                                if (file != null) {
                                    val pm = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                    if (pm != null) {
                                        val jobName = pdfTitle.ifBlank { context.getString(R.string.app_name) }
                                        pm.print(jobName, PdfFilePrintAdapter(file, jobName, pages), attrs)
                                    }
                                } else {
                                    printCurrentPage(attrs)
                                }
                            },
                        )
                    }
                    LightningScreen.PDF_VIEWER -> PdfViewerScreen(
                        title = pdfTitle,
                        sourceUrl = pdfUrl,
                        pdfFile = pdfFile,
                        isLoading = pdfLoading,
                        loadError = pdfError,
                        initialPage = pdfInitialPage,
                        nightMode = pdfNightMode,
                        onToggleNightMode = {
                            val next = !pdfNightMode
                            pdfNightMode = next
                            BrowserStore.savePdfNightMode(context, next)
                        },
                        onDocumentLoaded = { count ->
                            pdfPageCount = count
                        },
                        onPageChanged = { page ->
                            pdfCurrentPage = page
                            if (!incognito) {
                                val key = BrowserStore.pdfPositionKey(pdfUrl, pdfTitle)
                                BrowserStore.savePdfPosition(context, key, page)
                            }
                        },
                        onSaveToDownloads = {
                            val file = pdfFile ?: return@PdfViewerScreen
                            val name = pdfTitle.ifBlank { "document.pdf" }
                            val relPath = BrowserStore.effectiveDownloadRelativePath(context)
                            BrowserDownloads.saveCachedFileToDownloads(context, name, "application/pdf", file, relPath) { ok ->
                                if (ok && incognito) {
                                    privateDownloadedFiles.add(name)
                                    BrowserStore.addPrivateDownload(
                                        context,
                                        tabs.getOrNull(activeIndex)?.id.orEmpty(),
                                        name,
                                        relPath,
                                    )
                                }
                                val msg = if (ok) {
                                    context.getString(R.string.pdf_viewer_saved, name)
                                } else {
                                    context.getString(R.string.menu_save_offline_failed)
                                }
                                showBanner(msg)
                            }
                        },
                        onShare = {
                            val file = pdfFile ?: return@PdfViewerScreen
                            runCatching {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.pdf_viewer_share)))
                            }.onFailure {
                                showBanner(R.string.dl_no_external_apps)
                            }
                        },
                        onOpenExternal = {
                            val file = pdfFile ?: return@PdfViewerScreen
                            val pref = contentPreferences["pdf"]
                            AppHandlerResolver.launchFile(
                                context,
                                file,
                                "application/pdf",
                                pref?.packageName,
                                pref?.className,
                            )
                        },
                        onPrint = {
                            val file = pdfFile ?: return@PdfViewerScreen
                            openPdfPrintSheet(file)
                        },
                        onRetry = {
                            val url = pdfUrl ?: return@PdfViewerScreen
                            startPdfDownload(url, pdfUserAgent, pdfTitle, resetPage = false)
                        },
                        onBack = {
                            pdfFile?.let { runCatching { it.delete() } }
                            pdfFile = null
                            pdfUrl = null
                            pdfUserAgent = null
                            navigateBack(LightningScreen.BROWSER)
                        },
                    )
                    LightningScreen.BROWSER -> Unit
                    LightningScreen.SETTINGS -> SettingsRootScreen(
                        startupBehavior = startupBehavior,
                        themeMode = themeMode,
                        searchEngine = searchEngine,
                        searchEngineSummary = activeCustomEngine?.name ?: stringResource(searchEngine.labelRes),
                        onOpenStartup = { navigateTo(LightningScreen.STARTUP) },
                        onOpenControls = { navigateTo(LightningScreen.CONTROLS) },
                        onOpenBrowsing = { navigateTo(LightningScreen.BROWSING) },
                        onOpenAppearance = { navigateTo(LightningScreen.APPEARANCE) },
                        onOpenPrivacy = { navigateTo(LightningScreen.PRIVACY) },
                        onOpenSearchEngine = { navigateTo(LightningScreen.SEARCH_ENGINE) },
                        onOpenDownloads = { navigateTo(LightningScreen.DOWNLOAD_SETTINGS) },
                        onOpenAbout = { navigateTo(LightningScreen.ABOUT) },
                        onBack = { navigateBack(LightningScreen.HOME) },
                    )
                    LightningScreen.CONTROLS -> ControlsScreen(
                        hapticsEnabled = hapticsEnabled,
                        quickArcEnabled = quickArcEnabled,
                        onHapticsChange = { hapticsEnabled = it },
                        onQuickArcChange = { quickArcEnabled = it },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.BROWSING -> BrowsingScreen(
                        duplicateTabWarningEnabled = duplicateTabWarningEnabled,
                        recentlyClosedEnabled = recentlyClosedEnabled,
                        waybackEnabled = waybackEnabled,
                        onDuplicateTabWarningChange = { duplicateTabWarningEnabled = it },
                        onRecentlyClosedChange = { recentlyClosedEnabled = it },
                        onWaybackChange = { waybackEnabled = it },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.STARTUP -> StartupScreen(
                        startupBehavior = startupBehavior,
                        onStartupBehaviorSelect = { startupBehavior = it },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.SEARCH_ENGINE -> SearchEngineScreen(
                        activeEngineId = activeSearchEngineId,
                        builtInEngine = searchEngine,
                        customEngines = customSearchEngines,
                        onSelectBuiltIn = { engine ->
                            searchEngine = engine
                            activeSearchEngineId = engine.name
                        },
                        onSelectCustom = { custom ->
                            activeSearchEngineId = custom.id
                        },
                        onAddCustomEngine = { name, url, keyword ->
                            val newEngine = CustomSearchEngine(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                searchUrlFormat = url,
                                keyword = keyword,
                            )
                            customSearchEngines = customSearchEngines + newEngine
                        },
                        onUpdateCustomEngine = { updated ->
                            customSearchEngines = customSearchEngines.map { if (it.id == updated.id) updated else it }
                        },
                        onDeleteCustomEngine = { id ->
                            customSearchEngines = customSearchEngines.filterNot { it.id == id }
                            if (activeSearchEngineId == id) {
                                activeSearchEngineId = SearchEngine.GOOGLE.name
                                searchEngine = SearchEngine.GOOGLE
                            }
                        },
                        onBack = { navigateBack(LightningScreen.SETTINGS) },
                    )
                    LightningScreen.DOWNLOADS -> DownloadsScreen(
                        onBack = { navigateBack(LightningScreen.HOME) },
                    )
                    LightningScreen.DOWNLOAD_SETTINGS -> DownloadSettingsScreen(
                        saveFolder = downloadFolder,
                        customDownloadPath = customDownloadPath,
                        askBeforeDownload = askBeforeDownload,
                        deletePrivateOnClose = deletePrivateDownloads,
                        otherFilesAsk = otherFilesAsk,
                        contentPreferences = contentPreferences,
                        onSaveFolderChange = { downloadFolder = it },
                        onCustomDownloadPathChange = { customDownloadPath = it },
                        onAskBeforeDownloadChange = { askBeforeDownload = it },
                        onDeletePrivateOnCloseChange = { deletePrivateDownloads = it },
                        onOtherFilesAskChange = { otherFilesAsk = it },
                        onContentPreferenceChange = { key, pref ->
                            contentPreferences[key] = pref
                            if (!incognito) BrowserStore.saveContentPreference(context, key, pref)
                        },
                        onShowMessage = ::showBanner,
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
                        tabPreviewsEnabled = tabPreviewsEnabled,
                        onTabPreviewsChange = { tabPreviewsEnabled = it },
                        readingTimeEnabled = readingTimeEnabled,
                        onReadingTimeChange = { readingTimeEnabled = it },
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
                        clipboardPromptEnabled = clipboardPromptEnabled,
                        onAdBlockingChange = { adBlocking = it },
                        onFingerprintChange = { fingerprintLock = it },
                        onClipboardPromptChange = { clipboardPromptEnabled = it },
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
                                screen = if (tabsReturnScreen == LightningScreen.BROWSER) {
                                    if (tabs.isNotEmpty()) LightningScreen.BROWSER else LightningScreen.HOME
                                } else {
                                    tabsReturnScreen
                                }
                            }
                        },
                        onLightningLongClick = if (quickArcEnabled) ({ showQuickArc = true }) else null,
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
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = if (showBar) 80.dp else 16.dp),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = LightningTheme.colors.surfaceContainerHigh,
                    contentColor = LightningTheme.colors.onSurface,
                    actionColor = LightningTheme.colors.primary,
                )
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
                onPrint = { openPrintSheet() },
                onSaveOffline = { saveOfflinePage() },
                onIncognito = { context.startActivity(Intent(context, IncognitoActivity::class.java)) },
                incognito = incognito,
                copyUrlVisible = screen == LightningScreen.BROWSER &&
                    tabs.getOrNull(activeIndex)?.url?.isNotEmpty() == true,
                savedPagesVisible = screen == LightningScreen.TABS,
                recentlyClosedVisible = screen == LightningScreen.TABS && recentlyClosedEnabled && !incognito,
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
                        showBanner(R.string.bookmark_added)
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
                                    showBanner(R.string.home_pinned_added)
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