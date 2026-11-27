package com.lightning.browser.ui.tabs

import java.util.UUID

data class BrowserTab(
    val title: String,
    val domain: String,
    val url: String = "",
    val private: Boolean = false,
    val id: String = UUID.randomUUID().toString(),
)