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
import com.lightning.browser.ui.settings.SettingsRootScreen
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.tabs.TabSwitcherScreen
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.ThemeMode

enum class LightningScreen { HOME, TABS, SETTINGS }

@Composable
fun LightningBrowserRoot() {
    var screen by rememberSaveable { mutableStateOf(LightningScreen.HOME) }
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    val tabs = remember {
        mutableStateListOf(
            BrowserTab(title = "LightningOS", domain = "lightning-web-web.vercel.app"),
            BrowserTab(title = "Docs", domain = "lightning-web-web.vercel.app", private = true),
        )
    }

    BackHandler(enabled = screen != LightningScreen.HOME) {
        screen = when (screen) {
            LightningScreen.TABS, LightningScreen.SETTINGS -> LightningScreen.HOME
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
                        onOpenAppearance = {},
                        onOpenPrivacy = {},
                        onBack = { screen = LightningScreen.HOME },
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
                    onMenu = { screen = LightningScreen.SETTINGS },
                )
            }
        }
    }
}

private val BAR_BOTTOM_PADDING = 80.dp