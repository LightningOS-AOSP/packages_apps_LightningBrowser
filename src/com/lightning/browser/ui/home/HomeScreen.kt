package com.lightning.browser.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningColors
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.tone
import com.lightning.browser.ui.theme.WordmarkTextStyle

@Composable
fun HomeScreen(
    recentPages: List<RecentPage> = emptyList(),
    pinnedSites: List<PinnedSite> = emptyList(),
    onOpenSearch: () -> Unit = {},
    onOpenSite: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onAddPinned: () -> Unit = {},
    onRemovePinned: (PinnedSite) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 12.dp),
    ) {
        HomeHeader(onOpenProfile = onOpenProfile)
        Spacer(Modifier.height(20.dp))
        SearchBar(onOpenSearch = onOpenSearch)
        Spacer(Modifier.height(20.dp))
        QuickAccessRow(pinnedSites, onOpenSite, onAddPinned, onRemovePinned)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.home_recent_header),
            style = EyebrowTextStyle,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        RecentList(recentPages, onOpenSite)
    }
}

@Composable
private fun HomeHeader(onOpenProfile: () -> Unit) {
    val colors = LightningTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        ShapedIcon(
            Icons.Filled.FlashOn,
            ExpressiveShapes.Cookie9,
            colors.primary,
            colors.onPrimary,
            size = 48.dp,
            iconSize = 24.dp,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "LIGHTNING",
            style = WordmarkTextStyle.copy(fontSize = 26.sp),
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = onOpenProfile,
            shape = ExpressiveShapes.Burst8,
            color = colors.secondaryContainer,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.home_profile_avatar),
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchBar(onOpenSearch: () -> Unit) {
    val colors = LightningTheme.colors
    Surface(
        color = colors.surfaceContainerHigh,
        shape = CircleShape,
        onClick = onOpenSearch,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.home_search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Surface(
                color = colors.surfaceContainer,
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.home_search_mic),
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessRow(
    pinnedSites: List<PinnedSite>,
    onOpenSite: (String) -> Unit,
    onAddPinned: () -> Unit,
    onRemovePinned: (PinnedSite) -> Unit,
) {
    val colors = LightningTheme.colors
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(pinnedSites) { i, site ->
            val (container, onContainer) = colors.chipColors(site.tone)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .pointerInput(site) {
                        detectTapGestures(
                            onTap = { onOpenSite(site.domain) },
                            onLongPress = { onRemovePinned(site) },
                        )
                    },
            ) {
                ShapedIcon(
                    site.icon,
                    ExpressiveShapes.cycle(i),
                    container,
                    onContainer,
                    size = 64.dp,
                    iconSize = 28.dp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = site.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp),
            ) {
                Surface(
                    onClick = onAddPinned,
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainerHigh,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.home_pinned_add),
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RecentList(pages: List<RecentPage>, onOpenSite: (String) -> Unit) {
    GroupedColumn(pages.size) { i, shape ->
        RecentRow(pages[i], shape, i, onOpenSite)
    }
}

@Composable
private fun RecentRow(
    page: RecentPage,
    shape: androidx.compose.ui.graphics.Shape,
    index: Int,
    onOpenSite: (String) -> Unit,
) {
    val colors = LightningTheme.colors
    val tone = colors.tone(index)
    Surface(
        onClick = { onOpenSite(page.domain) },
        shape = shape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            ShapedIcon(
                page.icon,
                ExpressiveShapes.cycle(index),
                tone.container,
                tone.content,
                size = 44.dp,
                iconSize = 22.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = page.domain,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun LightningColors.chipColors(tone: ChipTone): Pair<Color, Color> =
    when (tone) {
        ChipTone.PRIMARY -> primaryContainer to onPrimaryContainer
        ChipTone.SECONDARY -> secondaryContainer to onSecondaryContainer
        ChipTone.TERTIARY -> tertiaryContainer to onTertiaryContainer
    }