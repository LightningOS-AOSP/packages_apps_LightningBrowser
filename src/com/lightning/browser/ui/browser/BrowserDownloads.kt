package com.lightning.browser.ui.browser

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import com.lightning.browser.ui.settings.DownloadItem
import com.lightning.browser.ui.settings.DownloadState
import java.util.Locale

// Downloads land in the device's internal Download folder via the system
// DownloadManager. "All files access" is what Android calls the file manager
// permission these days and is requested before a download starts.
object BrowserDownloads {

    // DownloadsProvider only lets external apps edit a small whitelist of
    // columns; STATUS is stripped. The "control" column is the supported way
    // for apps to pause/resume their own downloads.
    private const val COLUMN_CONTROL = "control"
    private const val CONTROL_RUN = 0
    private const val CONTROL_PAUSED = 1

    private fun downloadUri(downloadId: Long): Uri =
        Uri.parse("content://downloads/my_downloads/$downloadId")

    fun hasFileAccess(context: Context): Boolean =
        Environment.isExternalStorageManager()

    fun requestFileAccess(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    fun enqueue(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ): Boolean {
        if (!hasFileAccess(context)) {
            requestFileAccess(context)
            return false
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = fileNameFrom(contentDisposition, url)
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription(url)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        if (!mimeType.isNullOrEmpty()) request.setMimeType(mimeType)
        if (!userAgent.isNullOrEmpty()) request.addRequestHeader("User-Agent", userAgent)
        return runCatching { manager.enqueue(request) != -1L }.getOrDefault(false)
    }

    fun pause(context: Context, downloadId: Long) {
        if (downloadId < 0) return
        val values = ContentValues().apply { put(COLUMN_CONTROL, CONTROL_PAUSED) }
        context.contentResolver.update(downloadUri(downloadId), values, null, null)
    }

    fun resume(context: Context, downloadId: Long) {
        if (downloadId < 0) return
        val values = ContentValues().apply { put(COLUMN_CONTROL, CONTROL_RUN) }
        context.contentResolver.update(downloadUri(downloadId), values, null, null)
    }

    fun queryAll(context: Context): List<DownloadItem> {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val flags = DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING or
            DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_SUCCESSFUL or
            DownloadManager.STATUS_FAILED
        val cursor = runCatching {
            manager.query(DownloadManager.Query().setFilterByStatus(flags))
        }.getOrNull() ?: return emptyList()
        val items = ArrayList<DownloadItem>(cursor.count.coerceAtLeast(0))
        cursor.use {
            val idCol = it.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleCol = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val uriCol = it.getColumnIndex(DownloadManager.COLUMN_URI)
            val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val totalCol = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val savedCol = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val stampCol = it.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)
            while (it.moveToNext()) {
                val status = it.getInt(statusCol)
                val total = it.getLong(totalCol)
                val saved = it.getLong(savedCol)
                val failed = status == DownloadManager.STATUS_FAILED
                val domain = runCatching { Uri.parse(it.getString(uriCol)).host }.getOrNull() ?: ""
                val state = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL, DownloadManager.STATUS_FAILED -> DownloadState.COMPLETE
                    DownloadManager.STATUS_PAUSED -> DownloadState.PAUSED
                    else -> DownloadState.DOWNLOADING
                }
                items += DownloadItem(
                    title = it.getString(titleCol) ?: "download",
                    domain = domain,
                    detail = if (failed) "Failed" else formatSize(total),
                    state = state,
                    progress = if (total > 0) (saved.toFloat() / total).coerceIn(0f, 1f) else 0f,
                    timestamp = it.getLong(stampCol),
                    downloadId = it.getLong(idCol),
                )
            }
        }
        return items.sortedByDescending { it.timestamp }
    }

    private fun fileNameFrom(contentDisposition: String?, url: String): String {
        contentDisposition?.let { disposition ->
            Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]*)\"?").find(disposition)?.let {
                return sanitize(it.groupValues[1])
            }
        }
        return sanitize(Uri.parse(url).lastPathSegment ?: "download")
    }

    private fun sanitize(name: String): String {
        val cleaned = name.trim().removeSurrounding("\"").substringAfterLast('/')
        return cleaned.ifEmpty { "download" }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}