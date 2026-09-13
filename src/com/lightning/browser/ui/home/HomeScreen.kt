package com.lightning.browser.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.LightningColors
import com.lightning.browser.ui.theme.LightningTheme

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
            .padding(top = 20.dp, bottom = 12.dp),
    ) {
        SearchBar(onOpenSearch = onOpenSearch, onOpenProfile = onOpenProfile)
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
private fun SearchBar(onOpenSearch: () -> Unit, onOpenProfile: () -> Unit) {
    val colors = LightningTheme.colors
    Surface(
        color = colors.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
        onClick = onOpenSearch,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(14.dp))
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(R.string.home_search_mic),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Surface(
                color = colors.secondaryContainer,
                shape = CircleShape,
                onClick = onOpenProfile,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.home_profile_avatar),
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                )
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(pinnedSites) { site ->
            val (container, onContainer) = colors.chipColors(site.tone)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(container)
                    .pointerInput(site) {
                        detectTapGestures(
                            onTap = { onOpenSite(site.domain) },
                            onLongPress = { onRemovePinned(site) },
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = site.icon,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = site.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = onContainer,
                    )
                }
            }
        }
        item {
            Surface(
                onClick = onAddPinned,
                shape = RoundedCornerShape(50),
                color = colors.surfaceContainerHigh,
                modifier = Modifier.size(92.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.home_pinned_add),
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentList(pages: List<RecentPage>, onOpenSite: (String) -> Unit) {
    val colors = LightningTheme.colors
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(8.dp)) {
            pages.forEach { page ->
                RecentRow(page, onOpenSite)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun RecentRow(page: RecentPage, onOpenSite: (String) -> Unit) {
    val colors = LightningTheme.colors
    val (container, onContainer) = colors.chipColors(page.tone)
    Surface(
        onClick = { onOpenSite(page.domain) },
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Surface(
                color = container,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = page.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
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