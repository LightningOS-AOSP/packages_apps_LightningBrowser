package com.lightning.browser.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.ui.graphics.vector.ImageVector
import com.lightning.browser.ui.home.ChipTone

data class Bookmark(
    val title: String,
    val domain: String,
    val icon: ImageVector,
    val tone: ChipTone,
    val folder: String = "",
)

data class BookmarkFolder(val name: String, val count: Int)