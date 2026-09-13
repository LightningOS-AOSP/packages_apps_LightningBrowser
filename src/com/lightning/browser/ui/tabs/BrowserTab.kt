package com.lightning.browser.ui.tabs

data class BrowserTab(
    val title: String,
    val domain: String,
    val url: String = "",
    val private: Boolean = false,
)