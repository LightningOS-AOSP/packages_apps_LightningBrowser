package com.lightning.browser.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tag
import androidx.compose.ui.graphics.vector.ImageVector
import com.lightning.browser.ui.home.ChipTone
import kotlin.math.abs

// Icon and tone for a site are derived from its domain so bookmarks, history
// and suggestions stay consistent without storing per-site branding.
fun siteIcon(domain: String?): ImageVector {
    val name = domain.orEmpty().lowercase()
    return when {
        name.contains("github") || name.contains("gitlab") || name.contains("codeberg") ||
            name.contains("sourceforge") -> Icons.Filled.Code

        name.contains("youtube") || name.contains("vimeo") || name.contains("twitch") ||
            name.contains("dailymotion") -> Icons.Filled.PlayCircle

        name.contains("telegram") || name.contains("discord") || name.contains("slack") ||
            name.contains("matrix") -> Icons.Filled.Tag

        name.contains("linux") || name.contains("kernel") || name.contains("docs") ||
            name.contains("wikipedia") || name.contains("wiki") -> Icons.Filled.Article

        name.contains("lightning") || name.contains("thunderbolt") -> Icons.Filled.FlashOn

        name.contains("windy") || name.contains("weather") || name.contains("accuweather") ||
            name.contains("openweather") -> Icons.Filled.Air

        else -> Icons.Filled.Public
    }
}

fun siteTone(domain: String?): ChipTone {
    val hash = domain.orEmpty().hashCode().let { if (it == Int.MIN_VALUE) 0 else abs(it) }
    return when (hash % 3) {
        0 -> ChipTone.PRIMARY
        1 -> ChipTone.SECONDARY
        else -> ChipTone.TERTIARY
    }
}