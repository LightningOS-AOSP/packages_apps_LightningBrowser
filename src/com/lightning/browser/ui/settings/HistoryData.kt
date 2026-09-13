package com.lightning.browser.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.ui.graphics.vector.ImageVector
import com.lightning.browser.R
import com.lightning.browser.ui.home.ChipTone
import java.util.Calendar

data class HistoryEntry(
    val title: String,
    val domain: String,
    val icon: ImageVector,
    val tone: ChipTone,
    val timestamp: Long,
) {
    @get:StringRes
    val groupRes: Int get() = groupFor(timestamp)
}

val historyGroupOrder = listOf(
    R.string.downloads_group_today,
    R.string.downloads_group_yesterday,
    R.string.history_group_lastweek,
    R.string.history_group_older,
)

@StringRes
private fun groupFor(timestamp: Long): Int {
    if (timestamp <= 0L) return R.string.history_group_older
    val now = Calendar.getInstance()
    now.set(Calendar.HOUR_OF_DAY, 0)
    now.set(Calendar.MINUTE, 0)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 0)
    val startOfToday = now.timeInMillis
    val startOfYesterday = startOfToday - 24 * 60 * 60 * 1000L
    val startOfWeek = startOfToday - 6 * 24 * 60 * 60 * 1000L
    return when {
        timestamp >= startOfToday -> R.string.downloads_group_today
        timestamp >= startOfYesterday -> R.string.downloads_group_yesterday
        timestamp >= startOfWeek -> R.string.history_group_lastweek
        else -> R.string.history_group_older
    }
}