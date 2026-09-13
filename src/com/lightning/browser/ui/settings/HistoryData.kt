package com.lightning.browser.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Tag
import androidx.compose.ui.graphics.vector.ImageVector
import com.lightning.browser.R
import com.lightning.browser.ui.home.ChipTone

data class HistoryEntry(
    @StringRes val groupRes: Int,
    val title: String,
    val domain: String,
    val icon: ImageVector,
    val tone: ChipTone,
)

val historyGroupOrder = listOf(
    R.string.downloads_group_today,
    R.string.downloads_group_yesterday,
    R.string.history_group_lastweek,
)

val sampleHistory = listOf(
    HistoryEntry(R.string.downloads_group_today, "LightningOS", "lightning-web-web.vercel.app", Icons.Filled.FlashOn, ChipTone.PRIMARY),
    HistoryEntry(R.string.downloads_group_today, "Windy", "windy.com", Icons.Filled.Air, ChipTone.PRIMARY),
    HistoryEntry(R.string.downloads_group_yesterday, "GitHub", "github.com", Icons.Filled.Code, ChipTone.SECONDARY),
    HistoryEntry(R.string.history_group_lastweek, "Kernel docs", "docs.kernel.org", Icons.Filled.Article, ChipTone.TERTIARY),
)