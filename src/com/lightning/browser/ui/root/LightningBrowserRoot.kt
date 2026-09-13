package com.lightning.browser.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightning.browser.ui.bar.LightningBottomBar
import com.lightning.browser.ui.home.HomeScreen
import com.lightning.browser.ui.tabs.BrowserTab
import com.lightning.browser.ui.theme.LightningShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.WordmarkTextStyle

enum class LightningScreen { HOME, TABS }

@Composable
fun LightningBrowserRoot() {
    var screen by rememberSaveable { mutableStateOf(LightningScreen.HOME) }
    val tabs = remember { mutableStateListOf<BrowserTab>() }

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
                LightningScreen.TABS -> TabsSummary(tabs)
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

// Placeholder shown until the real tab grid lands.
@Composable
private fun TabsSummary(tabs: SnapshotStateList<BrowserTab>) {
    val colors = LightningTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Surface(
            color = colors.surfaceContainer,
            shape = LightningShapes.small,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.FlashOn, null, tint = colors.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text("${tabs.size} tab(s)", style = WordmarkTextStyle, color = colors.onSurface)
            }
        }
    }
}

private val BAR_BOTTOM_PADDING = 80.dp