package com.lightning.browser.ui.home

import androidx.compose.ui.graphics.vector.ImageVector

// Brand colours get mapped into one of the three tonal containers so a
// site's chip keeps its identity without introducing new palette colours.
enum class ChipTone { PRIMARY, SECONDARY, TERTIARY }

data class PinnedSite(
    val title: String,
    val icon: ImageVector,
    val tone: ChipTone,
    val domain: String,
)

data class RecentPage(
    val title: String,
    val domain: String,
    val icon: ImageVector,
    val tone: ChipTone,
)