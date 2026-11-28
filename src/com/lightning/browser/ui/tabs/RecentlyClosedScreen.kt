package com.lightning.browser.ui.tabs

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.settings.SettingsHeader
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics
import com.lightning.browser.ui.theme.tone

@Composable
fun RecentlyClosedScreen(
    closedTabs: List<ClosedTab>,
    onRestoreTab: (ClosedTab) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    var searchQuery by remember { mutableStateOf("") }
    val queryTrimmed = searchQuery.trim()
    val snapshot = closedTabs.toList()
    val filtered = remember(snapshot, queryTrimmed) {
        val now = System.currentTimeMillis()
        snapshot.filter { tab ->
            now - tab.closedAt <= BrowserStore.CLOSED_TAB_EXPIRY_MS &&
                (queryTrimmed.isEmpty() ||
                    tab.title.contains(queryTrimmed, ignoreCase = true) ||
                    tab.domain.contains(queryTrimmed, ignoreCase = true) ||
                    tab.url.contains(queryTrimmed, ignoreCase = true))
        }.take(BrowserStore.MAX_CLOSED_TABS)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Box(Modifier.weight(1f)) {
                SettingsHeader(stringResource(R.string.tabs_recently_closed_header), onBack)
            }
            if (snapshot.isNotEmpty()) {
                TextButton(
                    onClick = {
                        haptics.reject()
                        onClearAll()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.tabs_recently_closed_clear),
                        style = EyebrowTextStyle,
                        color = colors.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        TabsSearchBar(
            query = searchQuery,
            placeholder = stringResource(R.string.tabs_recently_closed_search_hint),
            onQueryChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 6.dp),
        )
        Text(
            text = stringResource(R.string.tabs_recently_closed_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(
                        if (snapshot.isEmpty()) {
                            R.string.tabs_recently_closed_empty
                        } else {
                            R.string.tabs_search_empty
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                GroupedColumn(filtered.size) { index, shape ->
                    val tab = filtered[index]
                    val tone = colors.tone(index)
                    Surface(
                        onClick = {
                            haptics.confirm()
                            onRestoreTab(tab)
                        },
                        shape = shape,
                        color = colors.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            ShapedIcon(
                                icon = Icons.Filled.History,
                                shape = ExpressiveShapes.cycle(index),
                                container = tone.container,
                                content = tone.content,
                                size = 42.dp,
                                iconSize = 20.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = tab.title.ifBlank { tab.domain.ifBlank { tab.url } },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val ageText = formatClosedAge(
                                    tab.closedAt,
                                    stringResource(R.string.tabs_recently_closed_just_now),
                                )
                                val subtitle = listOf(tab.domain.ifBlank { tab.url }, ageText)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · ")
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatClosedAge(closedAt: Long, justNow: String): String {
    val diff = (System.currentTimeMillis() - closedAt).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    return when {
        minutes < 1L -> justNow
        hours < 1L -> "${minutes}m ago"
        else -> "${hours}h ago"
    }
}
