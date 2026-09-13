package com.lightning.browser.ui.settings

import androidx.annotation.StringRes
import com.lightning.browser.R

enum class DownloadState { COMPLETE, DOWNLOADING }

data class DownloadItem(
    val title: String,
    val domain: String,
    val detail: String,
    val state: DownloadState,
    val progress: Float,
)

data class DownloadGroup(
    @StringRes val labelRes: Int,
    val items: List<DownloadItem>,
)

val sampleDownloadGroups = listOf(
    DownloadGroup(
        labelRes = R.string.downloads_group_today,
        items = listOf(
            DownloadItem(
                title = "lightning-130.zip",
                domain = "lightning-web-web.vercel.app",
                detail = "48 MB",
                state = DownloadState.COMPLETE,
                progress = 1f,
            ),
            DownloadItem(
                title = "wallpaper-marble.jpg",
                domain = "unsplash.com",
                detail = "2.4 MB",
                state = DownloadState.DOWNLOADING,
                progress = 0.42f,
            ),
        ),
    ),
    DownloadGroup(
        labelRes = R.string.downloads_group_yesterday,
        items = listOf(
            DownloadItem(
                title = "kernel-patch-v7.diff",
                domain = "github.com",
                detail = "84 KB",
                state = DownloadState.COMPLETE,
                progress = 1f,
            ),
        ),
    ),
)