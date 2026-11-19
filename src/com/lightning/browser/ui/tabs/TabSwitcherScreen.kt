package com.lightning.browser.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun TabSwitcherScreen(
    tabs: SnapshotStateList<BrowserTab>,
    onCloseTab: (Int) -> Unit = {},
    onCloseAll: () -> Unit = {},
    onNewTab: () -> Unit = {},
    onOpenTab: (Int) -> Unit = {},
) {
    val colors = LightningTheme.colors
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.tabs_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = CircleShape,
                    color = colors.primaryContainer,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = tabs.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCloseAll) {
                    Text(
                        text = stringResource(R.string.tabs_close_all),
                        style = EyebrowTextStyle,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            if (tabs.isEmpty()) {
                EmptyTabs()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp, top = 8.dp, end = 20.dp, bottom = 96.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(tabs.size) { index ->
                        val tab = tabs[index]
                        TabCard(
                            tab = tab,
                            index = index,
                            onClose = { onCloseTab(index) },
                            onOpen = { onOpenTab(index) },
                        )
                    }
                }
            }
        }

        LargeFloatingActionButton(
            onClick = onNewTab,
            shape = ExpressiveShapes.Cookie9,
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.bar_new_tab),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun EmptyTabs() {
    val colors = LightningTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No tabs yet",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun TabCard(tab: BrowserTab, index: Int, onClose: () -> Unit, onOpen: () -> Unit) {
    val colors = LightningTheme.colors
    // Private tabs get the violet secondary wash so they stand out from the
    // normal surface ones.
    val cardColor = if (tab.private) colors.secondaryContainer else colors.surfaceContainerHigh
    val onCardColor = if (tab.private) colors.onSecondaryContainer else colors.onSurface

    Surface(
        onClick = onOpen,
        shape = ExpressiveShapes.diagonal(index, big = 32, small = 10),
        color = cardColor,
        contentColor = onCardColor,
    ) {
        Column {
            // Thumbnail placeholder until real WebView captures land.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(colors.surfaceContainerHighest),
            ) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = null,
                    tint = if (tab.private) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp),
                )
                Surface(
                    onClick = onClose,
                    color = colors.surface.copy(alpha = 0.85f),
                    shape = ExpressiveShapes.Burst8,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.tabs_close_tab),
                            tint = colors.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                ShapedIcon(
                    Icons.Filled.FlashOn,
                    ExpressiveShapes.Cookie9,
                    colors.primaryContainer,
                    colors.onPrimaryContainer,
                    size = 28.dp,
                    iconSize = 14.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                    if (tab.domain.isNotEmpty()) {
                        Text(
                            text = tab.domain,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}