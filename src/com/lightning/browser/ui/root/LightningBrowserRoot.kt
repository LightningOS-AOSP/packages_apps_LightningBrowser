package com.lightning.browser.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightning.browser.ui.bar.LightningBottomBar
import com.lightning.browser.ui.home.HomeScreen
import com.lightning.browser.ui.settings.AboutScreen
import com.lightning.browser.ui.settings.AppearanceScreen
import com.lightning.browser.ui.settings.Bookmark
import com.lightning.browser.ui.settings.BookmarksScreen
import com.lightning.browser.ui.settings.DnsProvider
import com.lightning.browser.ui.settings.DnsScreen
import com.lightning.browser.ui.settings.DownloadsScreen
import com.lightning.browser.ui.settings.PrivacyScreen
import com.lightning.browser.ui.settings.SearchEngine
import com.lightning.browser.ui.settings.SearchEngineScreen
import com.lightning.browser.ui.settings.SettingsRootScreen
import com.lightning.browser.ui.settings.sampleBookmarks
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.tabs.TabSwitcherScreen
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.ThemeMode

enum class LightningScreen { HOME, TABS, SETTINGS, APPEARANCE, DNS, PRIVACY, SEARCH_ENGINE, DOWNLOADS, ABOUT, BOOKMARKS }

@Composable
fun LightningBrowserRoot() {
    var screen by rememberSaveable { mutableStateOf(LightningScreen.HOME) }
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    var searchEngine by rememberSaveable { mutableStateOf(SearchEngine.GOOGLE) }
    var dnsProvider by rememberSaveable { mutableStateOf(DnsProvider.CLOUDFLARE) }
    var customDnsUrl by rememberSaveable { mutableStateOf("") }
    var bypassTrusted by rememberSaveable { mutableStateOf(false) }
    var adBlocking by rememberSaveable { mutableStateOf(true) }
    var fingerprintLock by rememberSaveable { mutableStateOf(false) }
    val tabs = remember {
        mutableStateListOf(
            BrowserTab(title = "LightningOS", domain = "lightning-web-web.vercel.app"),
            BrowserTab(title = "Docs", domain = "lightning-web-web.vercel.app", private = true),
        )
    }
    val bookmarks = remember {
        mutableStateListOf<Bookmark>().apply { addAll(sampleBookmarks) }
    }

    BackHandler(enabled = screen != LightningScreen.HOME) {
        screen = when (screen) {
            LightningScreen.TABS, LightningScreen.SETTINGS -> LightningScreen.HOME
            LightningScreen.BOOKMARKS -> LightningScreen.HOME
            else -> LightningScreen.SETTINGS
        }
    }

    LightningTheme(themeMode = themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightningTheme.colors.surface),
        ) {
            val showBar = screen == LightningScreen.HOME || screen == LightningScreen.TABS
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showBar) BAR_BOTTOM_PADDING else 0.dp),
            ) {
                when (screen) {
                    LightningScreen.HOME -> HomeScreen()
                    LightningScreen.TABS -> TabSwitcherScreen(
                        tabs = tabs,
                        onCloseTab = { tabs.removeAt(it) },
                        onCloseAll = { tabs.clear() },
                        onNewTab = { tabs.add(BrowserTab(title = "New tab", domain = "")) },
                        onOpenTab = { screen = LightningScreen.HOME },
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
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                    LightningScreen.BOOKMARKS -> BookmarksScreen(
                        bookmarks = bookmarks,
                        onDelete = { bookmarks.remove(it) },
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
                        onProviderSelect = { dnsProvider = it },
                        onCustomUrlChange = { customDnsUrl = it },
                        onBypassTrustedChange = { bypassTrusted = it },
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                    LightningScreen.PRIVACY -> PrivacyScreen(
                        provider = dnsProvider,
                        adBlocking = adBlocking,
                        fingerprintLock = fingerprintLock,
                        onAdBlockingChange = { adBlocking = it },
                        onFingerprintChange = { fingerprintLock = it },
                        onOpenDns = { screen = LightningScreen.DNS },
                        onBack = { screen = LightningScreen.SETTINGS },
                    )
                }
            }
            if (showBar) {
                LightningBottomBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onNewTab = {
                        tabs.add(BrowserTab(title = "New tab", domain = ""))
                        screen = LightningScreen.TABS
                    },
                    onBookmark = { screen = LightningScreen.BOOKMARKS },
                    onMenu = { screen = LightningScreen.SETTINGS },
                )
            }
        }
    }
}

private val BAR_BOTTOM_PADDING = 80.dp