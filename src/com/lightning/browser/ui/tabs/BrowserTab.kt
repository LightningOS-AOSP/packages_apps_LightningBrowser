package com.lightning.browser.ui.tabs

import java.util.UUID

data class BrowserTab(
    val title: String,
    val domain: String,
    val url: String = "",
    val private: Boolean = false,
    val desktopMode: Boolean = false,
    val id: String = UUID.randomUUID().toString(),
)

data class ClosedTab(
    val title: String,
    val domain: String,
    val url: String,
    val desktopMode: Boolean = false,
    val closedAt: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString(),
)