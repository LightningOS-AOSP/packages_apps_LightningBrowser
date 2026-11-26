package com.lightning.browser.ui.browser

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.lightning.browser.ui.settings.DownloadItem
import com.lightning.browser.ui.settings.DownloadState
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
        if (url.startsWith("data:", ignoreCase = true)) {
            val fileName = fileNameFrom(contentDisposition, url, mimeType)
            return saveDataOrBlob(context, url, fileName, mimeType)
        }
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") {
            return false
        }
        if (!hasFileAccess(context)) {
            requestFileAccess(context)
            return false
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return false
        val fileName = fileNameFrom(contentDisposition, url, mimeType)
        return runCatching {
            val request = DownloadManager.Request(uri)
                .setTitle(fileName)
                .setDescription(url)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            if (!mimeType.isNullOrEmpty()) request.setMimeType(mimeType)
            if (!userAgent.isNullOrEmpty()) request.addRequestHeader("User-Agent", userAgent)
            manager.enqueue(request) != -1L
        }.getOrDefault(false)
    }

    private val pendingBlobFiles = ConcurrentHashMap<String, File>()

    fun saveBlobChunk(
        context: Context,
        downloadId: String,
        fileName: String,
        mimeType: String,
        base64Chunk: String,
        isLast: Boolean,
    ) {
        runCatching {
            val tempDir = File(context.cacheDir, "downloads").apply { if (!exists()) mkdirs() }
            val tempFile = pendingBlobFiles.getOrPut(downloadId) {
                File(tempDir, "blob_${System.currentTimeMillis()}_${downloadId.hashCode()}.tmp")
            }
            if (base64Chunk.isNotEmpty()) {
                val bytes = Base64.decode(base64Chunk, Base64.DEFAULT)
                FileOutputStream(tempFile, true).use { out ->
                    out.write(bytes)
                }
            }
            if (isLast) {
                pendingBlobFiles.remove(downloadId)
                val rawName = fileName.trim().ifEmpty { null } ?: "download_${System.currentTimeMillis()}"
                val extractedMime = mimeType.ifBlank { null }
                val effectiveMime = when {
                    !extractedMime.isNullOrBlank() && extractedMime != "application/octet-stream" && extractedMime != "text/plain" -> extractedMime
                    else -> mimeTypeFromFileName(rawName) ?: extractedMime ?: "application/octet-stream"
                }
                val safeName = ensureExtension(sanitize(rawName), effectiveMime)
                saveFileFromTemp(context, safeName, effectiveMime, tempFile)
                tempFile.delete()
            }
        }.onFailure {
            pendingBlobFiles.remove(downloadId)?.delete()
        }
    }

    fun saveDataOrBlob(
        context: Context,
        dataUriOrBase64: String,
        fileName: String?,
        mimeType: String?,
    ): Boolean {
        return runCatching {
            val base64Data = if (dataUriOrBase64.contains(",")) {
                dataUriOrBase64.substringAfter(",")
            } else {
                dataUriOrBase64
            }
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val extractedMime = mimeTypeFromDataUri(dataUriOrBase64)
            val rawName = fileName?.trim()?.ifEmpty { null } ?: "download_${System.currentTimeMillis()}"
            val effectiveMime = when {
                !extractedMime.isNullOrBlank() && extractedMime != "application/octet-stream" && extractedMime != "text/plain" -> extractedMime
                !mimeType.isNullOrBlank() && mimeType != "application/octet-stream" && mimeType != "text/plain" -> mimeType
                else -> mimeTypeFromFileName(rawName) ?: extractedMime ?: mimeType ?: "application/octet-stream"
            }
            val safeName = ensureExtension(sanitize(rawName), effectiveMime)

            val saved = saveFileToDownloads(context, safeName, effectiveMime, bytes)
            if (saved != null) {
                notifyDownloadComplete(context, safeName)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun saveFileFromTemp(
        context: Context,
        fileName: String,
        mimeType: String,
        tempFile: File,
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val itemUri = runCatching { resolver.insert(collection, contentValues) }.getOrNull()
            if (itemUri != null) {
                val success = runCatching {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    true
                }.getOrDefault(false)
                if (success) {
                    notifyDownloadComplete(context, fileName)
                    return itemUri
                }
            }
        }
        return runCatching {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            var targetFile = File(downloadsDir, fileName)
            var counter = 1
            val baseName = fileName.substringBeforeLast(".")
            val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
            while (targetFile.exists()) {
                targetFile = File(downloadsDir, "$baseName ($counter)$ext")
                counter++
            }
            tempFile.inputStream().use { input ->
                targetFile.outputStream().use { out ->
                    input.copyTo(out)
                }
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType),
                null,
            )
            notifyDownloadComplete(context, targetFile.name)
            Uri.fromFile(targetFile)
        }.getOrNull()
    }

    private fun notifyDownloadComplete(context: Context, fileName: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Downloaded $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFileToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        data: ByteArray,
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val itemUri = runCatching { resolver.insert(collection, contentValues) }.getOrNull()
            if (itemUri != null) {
                val success = runCatching {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        out.write(data)
                        out.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    true
                }.getOrDefault(false)
                if (success) return itemUri
            }
        }
        return runCatching {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            var targetFile = File(downloadsDir, fileName)
            var counter = 1
            val baseName = fileName.substringBeforeLast(".")
            val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
            while (targetFile.exists()) {
                targetFile = File(downloadsDir, "$baseName ($counter)$ext")
                counter++
            }
            targetFile.writeBytes(data)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType),
                null,
            )
            Uri.fromFile(targetFile)
        }.getOrNull()
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

    fun guessExtension(mimeType: String?): String {
        val cleanMime = mimeType?.lowercase(Locale.ROOT)?.substringBefore(";")?.trim() ?: return ""
        return when (cleanMime) {
            "image/png" -> ".png"
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            "image/svg+xml" -> ".svg"
            "application/pdf" -> ".pdf"
            "application/zip", "application/x-zip-compressed" -> ".zip"
            "text/plain" -> ".txt"
            "text/html" -> ".html"
            "text/csv" -> ".csv"
            "application/json" -> ".json"
            "video/mp4" -> ".mp4"
            "audio/mpeg" -> ".mp3"
            "audio/wav", "audio/x-wav" -> ".wav"
            "image/vnd.adobe.photoshop", "image/x-photoshop", "application/x-photoshop", "image/psd" -> ".psd"
            else -> {
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanMime)
                if (!ext.isNullOrEmpty()) ".$ext" else ""
            }
        }
    }

    fun mimeTypeFromFileName(fileName: String): String? {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "psd" -> "image/vnd.adobe.photoshop"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "json" -> "application/json"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
    }

    fun ensureExtension(name: String, mimeType: String?): String {
        val hasExt = name.contains(".") && name.substringAfterLast(".").isNotBlank()
        if (hasExt) {
            return name
        }
        val ext = guessExtension(mimeType)
        return if (ext.isNotEmpty()) "$name$ext" else name
    }

    fun mimeTypeFromDataUri(uri: String): String? {
        if (!uri.startsWith("data:", ignoreCase = true)) return null
        val header = uri.substringAfter("data:").substringBefore(",")
        val mime = header.substringBefore(";").trim()
        return mime.ifEmpty { null }
    }

    fun fileNameFrom(contentDisposition: String?, url: String, mimeType: String? = null): String {
        contentDisposition?.let { disposition ->
            Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]*)\"?").find(disposition)?.let {
                val extracted = sanitize(it.groupValues[1])
                if (extracted.isNotEmpty()) return ensureExtension(extracted, mimeType)
            }
        }
        if (!url.startsWith("data:", ignoreCase = true) && !url.startsWith("blob:", ignoreCase = true)) {
            val segment = runCatching { Uri.parse(url).lastPathSegment }.getOrNull()
            if (!segment.isNullOrEmpty()) {
                return ensureExtension(sanitize(segment), mimeType)
            }
        }
        val defaultName = "download_" + System.currentTimeMillis()
        return ensureExtension(defaultName, mimeType)
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
