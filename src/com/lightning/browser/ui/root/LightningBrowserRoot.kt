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
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.tabs.TabSwitcherScreen
import com.lightning.browser.ui.theme.LightningTheme

enum class LightningScreen { HOME, TABS }

@Composable
fun LightningBrowserRoot() {
    var screen by rememberSaveable { mutableStateOf(LightningScreen.HOME) }
    val tabs = remember {
        mutableStateListOf(
            BrowserTab(title = "LightningOS", domain = "lightning-web-web.vercel.app"),
            BrowserTab(title = "Docs", domain = "lightning-web-web.vercel.app", private = true),
        )
    }

    BackHandler(enabled = screen == LightningScreen.TABS) {
        screen = LightningScreen.HOME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightningTheme.colors.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = BAR_BOTTOM_PADDING),
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
            }
        }
        LightningBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onNewTab = {
                tabs.add(BrowserTab(title = "New tab", domain = ""))
                screen = LightningScreen.TABS
            },
        )
    }
}

private val BAR_BOTTOM_PADDING = 80.dp