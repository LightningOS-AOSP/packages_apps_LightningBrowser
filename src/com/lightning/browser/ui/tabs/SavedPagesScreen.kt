package com.lightning.browser.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lightning.browser.R
import com.lightning.browser.ui.browser.BrowserDownloads
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.settings.DownloadItem
import com.lightning.browser.ui.settings.SettingsHeader
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics
import com.lightning.browser.ui.theme.tone
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SavedPagesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberLightningHaptics()
    var searchQuery by remember { mutableStateOf("") }
    var savedPages by remember { mutableStateOf(emptyList<DownloadItem>()) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(context, refreshKey) {
        savedPages = withContext(Dispatchers.IO) {
            BrowserDownloads.querySavedPages(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val queryTrimmed = searchQuery.trim()
    val filteredSaved = remember(savedPages, queryTrimmed) {
        savedPages.filter { item ->
            queryTrimmed.isEmpty() ||
                savedPageTitle(item.title).contains(queryTrimmed, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsHeader(stringResource(R.string.saved_pages_title), onBack)
        }
        Spacer(Modifier.height(4.dp))
        TabsSearchBar(
            query = searchQuery,
            placeholder = stringResource(R.string.tabs_saved_search_hint),
            onQueryChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
        )
        if (filteredSaved.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(
                        if (savedPages.isEmpty()) R.string.tabs_saved_empty else R.string.tabs_saved_search_empty,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(filteredSaved, key = { _, item -> item.title }) { index, item ->
                    SavedPageCard(
                        item = item,
                        index = index,
                        onOpen = { BrowserDownloads.openDownload(context, item) },
                        onDelete = {
                            haptics.reject()
                            BrowserDownloads.deleteWithStorage(context, item)
                            savedPages = savedPages.filterNot { it.title == item.title }
                        },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(220),
                            fadeOutSpec = tween(180),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPageCard(
    item: DownloadItem,
    index: Int,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val tone = colors.tone(index % 3)
    Surface(
        onClick = onOpen,
        shape = ExpressiveShapes.diagonal(index, big = 32, small = 10),
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        modifier = modifier,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(colors.surfaceContainerHighest),
            ) {
                ShapedIcon(
                    icon = Icons.Filled.OfflinePin,
                    shape = ExpressiveShapes.cycle(index),
                    container = tone.container,
                    content = tone.content,
                    size = 48.dp,
                    iconSize = 24.dp,
                    modifier = Modifier.align(Alignment.Center),
                )
                Surface(
                    onClick = onDelete,
                    color = colors.surface.copy(alpha = 0.85f),
                    shape = ExpressiveShapes.Burst8,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.tabs_delete_saved),
                            tint = colors.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = savedPageTitle(item.title),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.detail.isNotEmpty()) {
                    Text(
                        text = item.detail,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun savedPageTitle(fileName: String): String = fileName
    .removeSuffix(".mhtml.mht")
    .removeSuffix(".mhtml")
    .removeSuffix(".mht")
