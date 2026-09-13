package com.lightning.browser.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.ui.graphics.vector.ImageVector
import com.lightning.browser.ui.home.ChipTone

data class Bookmark(
    val title: String,
    val domain: String,
    val icon: ImageVector,
    val tone: ChipTone,
)

data class BookmarkFolder(val name: String, val count: Int)

val sampleBookmarkFolders = listOf(
    BookmarkFolder("Work", 3),
    BookmarkFolder("Inspiration", 5),
)

val sampleBookmarks = listOf(
    Bookmark("LightningOS", "lightning-web-web.vercel.app", Icons.Filled.FlashOn, ChipTone.PRIMARY),
    Bookmark("Kernel docs", "docs.kernel.org", Icons.Filled.Article, ChipTone.TERTIARY),
    Bookmark("GitHub", "github.com", Icons.Filled.Code, ChipTone.SECONDARY),
    Bookmark("Windy", "windy.com", Icons.Filled.Air, ChipTone.PRIMARY),
)