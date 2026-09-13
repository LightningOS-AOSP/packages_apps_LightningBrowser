package com.lightning.browser.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tag
import androidx.compose.ui.graphics.vector.ImageVector

// Brand colours get mapped into one of the three tonal containers so a
// site's chip keeps its identity without introducing new palette colours.
enum class ChipTone { PRIMARY, SECONDARY, TERTIARY }

data class PinnedSite(
    val title: String,
    val icon: ImageVector,
    val tone: ChipTone,
)

data class RecentPage(
    val title: String,
    val domain: String,
    val icon: ImageVector,
    val tone: ChipTone,
)

val samplePinnedSites = listOf(
    PinnedSite("Code", Icons.Filled.Code, ChipTone.TERTIARY),
    PinnedSite("YouTube", Icons.Filled.PlayCircle, ChipTone.SECONDARY),
    PinnedSite("Chat", Icons.Filled.Tag, ChipTone.PRIMARY),
    PinnedSite("Docs", Icons.Filled.MenuBook, ChipTone.TERTIARY),
    PinnedSite("Weather", Icons.Filled.Air, ChipTone.SECONDARY),
)

val sampleRecentPages = listOf(
    RecentPage("LightningOS", "lightning-web-web.vercel.app", Icons.Filled.FlashOn, ChipTone.PRIMARY),
    RecentPage("Open source", "github.com", Icons.Filled.Code, ChipTone.SECONDARY),
    RecentPage("Weather radar", "windy.com", Icons.Filled.Air, ChipTone.PRIMARY),
)