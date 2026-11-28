package com.lightning.browser.ui.browser

import android.app.DownloadManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.settings.DownloadItem
import com.lightning.browser.ui.settings.DownloadState
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableSharedFlow

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

    val completedEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)

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

    fun downloadExists(
        context: Context,
        fileName: String,
        preferredRelativePath: String? = null,
    ): Boolean {
        val clean = sanitize(fileName)
        val candidateDirs = if (!preferredRelativePath.isNullOrBlank()) {
            listOf(preferredRelativePath)
        } else {
            buildSet {
                add(BrowserStore.effectiveDownloadRelativePath(context))
                add(BrowserStore.loadCustomDownloadPath(context))
                BrowserStore.DownloadFolder.entries.forEach { add(it.relativePath) }
            }
        }
        candidateDirs.forEach { rel ->
            val directFile = File(File(Environment.getExternalStorageDirectory(), rel), clean)
            if (directFile.exists()) return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collections = listOf(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            )
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
            collections.forEach { collection ->
                runCatching {
                    resolver.query(collection, projection, selection, arrayOf(clean), null)?.use { cursor ->
                        if (cursor.moveToFirst()) return true
                    }
                }
            }
        }
        return false
    }

    fun nextAvailableName(
        context: Context,
        fileName: String,
        preferredRelativePath: String? = null,
    ): String {
        val clean = sanitize(fileName)
        if (!downloadExists(context, clean, preferredRelativePath)) return clean
        val dot = clean.lastIndexOf('.')
        val base = if (dot > 0) clean.substring(0, dot) else clean
        val ext = if (dot > 0) clean.substring(dot) else ""
        var counter = 1
        while (true) {
            val candidate = "$base ($counter)$ext"
            if (!downloadExists(context, candidate, preferredRelativePath)) return candidate
            counter++
        }
    }

    fun enqueue(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ): Boolean {
        val fileName = fileNameFrom(contentDisposition, url, mimeType)
        return enqueueWithFileName(context, url, userAgent, fileName, mimeType)
    }

    fun extractAllowedRelativePath(treeUri: Uri): String? {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        if (!docId.startsWith("primary:", ignoreCase = true)) return null
        val rel = docId.substringAfter(':').trim('/')
        return rel.takeIf { BrowserStore.isAllowedUserRelativePath(it) }
    }

    fun enqueueWithFileName(
        context: Context,
        url: String,
        userAgent: String?,
        fileName: String,
        mimeType: String?,
        relativePath: String = BrowserStore.effectiveDownloadRelativePath(context),
    ): Boolean {
        if (url.startsWith("data:", ignoreCase = true)) {
            return saveDataOrBlob(context, url, fileName, mimeType, relativePath)
        }
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") {
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasFileAccess(context)) {
            requestFileAccess(context)
            return false
        }
        val cleanName = sanitize(fileName)
        BrowserStore.unhideDownload(context, cleanName)
        val appContext = context.applicationContext
        val dir = File(appContext.filesDir, "downloads").apply { if (!exists()) mkdirs() }
        val id = -(idGen.incrementAndGet())
        val tempFile = File(dir, "dl_${-id}_$cleanName.part")
        val task = HttpDownloadTask(
            id = id,
            url = url,
            userAgent = userAgent,
            fileName = cleanName,
            mimeType = mimeType,
            domain = uri.host.orEmpty(),
            tempFile = tempFile,
            relativePath = relativePath,
        )
        tasks[id] = task
        startTask(appContext, task)
        return true
    }

    private class HttpDownloadTask(
        val id: Long,
        val url: String,
        val userAgent: String?,
        val fileName: String,
        val mimeType: String?,
        val domain: String,
        val tempFile: File,
        val relativePath: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) {
        @Volatile var savedBytes: Long = 0L
        @Volatile var totalBytes: Long = -1L
        @Volatile var state: DownloadState = DownloadState.DOWNLOADING
        @Volatile var failed: Boolean = false
        @Volatile var cancelled: Boolean = false
        @Volatile var connection: HttpURLConnection? = null
    }

    private val idGen = AtomicLong(1000L)
    private val tasks = ConcurrentHashMap<Long, HttpDownloadTask>()
    private val executor = Executors.newCachedThreadPool()

    private fun startTask(appContext: Context, task: HttpDownloadTask) {
        task.state = DownloadState.DOWNLOADING
        task.failed = false
        executor.execute {
            var conn: HttpURLConnection? = null
            try {
                val existing = if (task.tempFile.exists()) task.tempFile.length() else 0L
                task.savedBytes = existing
                conn = (URL(task.url).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    if (!task.userAgent.isNullOrEmpty()) setRequestProperty("User-Agent", task.userAgent)
                    CookieManager.getInstance().getCookie(task.url)?.let { cookie ->
                        setRequestProperty("Cookie", cookie)
                    }
                    if (existing > 0L) {
                        setRequestProperty("Range", "bytes=$existing-")
                    }
                }
                task.connection = conn
                val code = conn.responseCode
                if (code !in 200..299) {
                    task.failed = true
                    task.state = DownloadState.COMPLETE
                    return@execute
                }
                val isPartial = code == HttpURLConnection.HTTP_PARTIAL && existing > 0L
                val contentLen = conn.contentLengthLong
                task.totalBytes = when {
                    isPartial && contentLen > 0L -> existing + contentLen
                    contentLen > 0L -> contentLen
                    else -> task.totalBytes
                }
                if (!isPartial) {
                    task.savedBytes = 0L
                }
                val effectiveMime = task.mimeType?.takeIf {
                    it.isNotBlank() && it != "application/octet-stream"
                } ?: conn.contentType?.substringBefore(';')?.trim()?.ifBlank { null }
                    ?: mimeTypeFromFileName(task.fileName)
                    ?: "application/octet-stream"

                conn.inputStream.use { input ->
                    FileOutputStream(task.tempFile, isPartial).use { out ->
                        val buffer = ByteArray(16 * 1024)
                        while (task.state == DownloadState.DOWNLOADING) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            task.savedBytes += read
                        }
                        out.flush()
                    }
                }
                if (task.state == DownloadState.DOWNLOADING) {
                    val finalSize = task.tempFile.length()
                    val savedUri = saveFileFromTemp(appContext, task.fileName, effectiveMime, task.tempFile, task.relativePath)
                    runCatching { task.tempFile.delete() }
                    if (savedUri != null) {
                        BrowserStore.addDownload(
                            appContext,
                            BrowserStore.DownloadRecord(
                                title = task.fileName,
                                domain = task.domain,
                                sizeBytes = finalSize,
                                timestamp = task.timestamp,
                                relativePath = task.relativePath,
                            ),
                        )
                        tasks.remove(task.id)
                    } else {
                        task.failed = true
                        task.state = DownloadState.COMPLETE
                    }
                }
            } catch (_: Exception) {
                if (task.state == DownloadState.DOWNLOADING) {
                    task.failed = true
                    task.state = DownloadState.COMPLETE
                }
            } finally {
                task.connection = null
                runCatching { conn?.disconnect() }
            }
        }
    }

    private val pendingBlobFiles = ConcurrentHashMap<String, File>()

    fun saveBlobChunk(
        context: Context,
        downloadId: String,
        fileName: String,
        mimeType: String,
        base64Chunk: String,
        isLast: Boolean,
        onStart: ((fileName: String) -> Unit)? = null,
        onComplete: ((fileName: String) -> Unit)? = null,
        onDuplicate: ((existingName: String, suggestedName: String, mimeType: String, tempFile: File) -> Unit)? = null,
    ) {
        runCatching {
            val tempDir = File(context.cacheDir, "downloads").apply { if (!exists()) mkdirs() }
            var isFirst = false
            val tempFile = pendingBlobFiles.getOrPut(downloadId) {
                isFirst = true
                File(tempDir, "blob_${System.currentTimeMillis()}_${downloadId.hashCode()}.tmp")
            }
            if (isFirst && onStart != null) {
                val previewName = sanitize(fileName.trim().ifEmpty { "download" })
                Handler(Looper.getMainLooper()).post { onStart(previewName) }
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
                if (onDuplicate != null && downloadExists(context, safeName)) {
                    val suggested = nextAvailableName(context, safeName)
                    Handler(Looper.getMainLooper()).post {
                        onDuplicate(safeName, suggested, effectiveMime, tempFile)
                    }
                } else {
                    val finalSize = tempFile.length()
                    val savedUri = saveFileFromTemp(context, safeName, effectiveMime, tempFile)
                    tempFile.delete()
                    if (savedUri != null) {
                        BrowserStore.addDownload(
                            context,
                            BrowserStore.DownloadRecord(
                                title = safeName,
                                domain = "",
                                sizeBytes = finalSize,
                                timestamp = System.currentTimeMillis(),
                            ),
                        )
                    }
                    if (onComplete != null) {
                        Handler(Looper.getMainLooper()).post { onComplete(safeName) }
                    }
                }
            }
        }.onFailure {
            pendingBlobFiles.remove(downloadId)?.delete()
        }
    }

    fun completePendingBlob(
        context: Context,
        fileName: String,
        mimeType: String,
        tempFile: File,
        relativePath: String = BrowserStore.effectiveDownloadRelativePath(context),
    ): Boolean {
        val cleanName = sanitize(fileName)
        val finalSize = tempFile.length()
        val saved = runCatching {
            saveFileFromTemp(context, cleanName, mimeType, tempFile, relativePath) != null
        }.getOrDefault(false)
        if (saved) {
            BrowserStore.addDownload(
                context,
                BrowserStore.DownloadRecord(
                    title = cleanName,
                    domain = "",
                    sizeBytes = finalSize,
                    timestamp = System.currentTimeMillis(),
                    relativePath = relativePath,
                ),
            )
        }
        runCatching { tempFile.delete() }
        return saved
    }

    fun saveDataOrBlob(
        context: Context,
        dataUriOrBase64: String,
        fileName: String?,
        mimeType: String?,
        relativePath: String = BrowserStore.effectiveDownloadRelativePath(context),
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

            val saved = saveFileToDownloads(context, safeName, effectiveMime, bytes, relativePath)
            if (saved != null) {
                BrowserStore.addDownload(
                    context,
                    BrowserStore.DownloadRecord(
                        title = safeName,
                        domain = "",
                        sizeBytes = bytes.size.toLong(),
                        timestamp = System.currentTimeMillis(),
                        relativePath = relativePath,
                    ),
                )
                notifyDownloadComplete(context, safeName)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun mediaCollectionFor(relativePath: String, mimeType: String): Uri {
        val lower = relativePath.lowercase(Locale.ROOT)
        return when {
            lower == "download" || lower.startsWith("download/") ->
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            (lower == "pictures" || lower.startsWith("pictures/") || lower == "dcim" || lower.startsWith("dcim/")) &&
                mimeType.startsWith("image/") ->
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            (lower == "movies" || lower.startsWith("movies/")) && mimeType.startsWith("video/") ->
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            (lower == "music" || lower.startsWith("music/") || lower == "podcasts" || lower.startsWith("podcasts/") ||
                lower == "audiobooks" || lower.startsWith("audiobooks/")) && mimeType.startsWith("audio/") ->
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
    }

    private fun saveFileFromTemp(
        context: Context,
        fileName: String,
        mimeType: String,
        tempFile: File,
        relativePath: String = BrowserStore.effectiveDownloadRelativePath(context),
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = mediaCollectionFor(relativePath, mimeType)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
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
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
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
            val downloadsDir = File(Environment.getExternalStorageDirectory(), relativePath)
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
        BrowserStore.unhideDownload(context, fileName)
        completedEvents.tryEmit(fileName)
    }

    fun saveOfflineArchive(
        context: Context,
        title: String,
        domain: String,
        tempFile: File,
        onResult: (String?) -> Unit,
    ) {
        val appContext = context.applicationContext
        val rawBase = title.ifBlank { domain }.ifBlank { "page" }
        val base = sanitize(rawBase)
            .removeSuffix(".mhtml.mht")
            .removeSuffix(".mhtml")
            .removeSuffix(".mht")
            .ifBlank { "page" }
        val candidate = "$base.mht"
        Thread {
            val savedName = runCatching {
                val bytes = tempFile.readBytes()
                val finalName = nextAvailableName(appContext, candidate)
                val uri = saveFileToDownloads(appContext, finalName, "multipart/related", bytes)
                if (uri != null) {
                    BrowserStore.unhideDownload(appContext, finalName)
                    finalName
                } else {
                    null
                }
            }.getOrNull()
            runCatching { tempFile.delete() }
            Handler(Looper.getMainLooper()).post {
                onResult(savedName)
            }
        }.start()
    }

    fun savePdfBytes(
        context: Context,
        title: String,
        domain: String,
        pdfBytes: ByteArray,
        onResult: (String?) -> Unit,
    ) {
        val appContext = context.applicationContext
        val rawBase = title.ifBlank { domain }.ifBlank { "page" }
        val base = sanitize(rawBase).removeSuffix(".pdf").ifBlank { "page" }
        val candidate = "$base.pdf"
        Thread {
            val savedName = runCatching {
                val finalName = nextAvailableName(appContext, candidate)
                val uri = saveFileToDownloads(appContext, finalName, "application/pdf", pdfBytes)
                if (uri != null) {
                    BrowserStore.unhideDownload(appContext, finalName)
                    finalName
                } else {
                    null
                }
            }.getOrNull()
            Handler(Looper.getMainLooper()).post {
                onResult(savedName)
            }
        }.start()
    }

    private fun saveFileToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        data: ByteArray,
        relativePath: String = BrowserStore.effectiveDownloadRelativePath(context),
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = mediaCollectionFor(relativePath, mimeType)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val itemUri = runCatching { resolver.insert(collection, contentValues) }.getOrNull()
            if (itemUri != null) {
                val success = runCatching {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        out.write(data)
                        out.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    true
                }.getOrDefault(false)
                if (success) return itemUri
            }
        }
        return runCatching {
            val downloadsDir = File(Environment.getExternalStorageDirectory(), relativePath)
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
        tasks[downloadId]?.let { task ->
            task.state = DownloadState.PAUSED
            executor.execute { runCatching { task.connection?.disconnect() } }
            return
        }
        if (downloadId < 0) return
        runCatching {
            val values = ContentValues().apply { put(COLUMN_CONTROL, CONTROL_PAUSED) }
            context.contentResolver.update(downloadUri(downloadId), values, null, null)
        }
    }

    fun resume(context: Context, downloadId: Long) {
        tasks[downloadId]?.let { task ->
            if (task.state == DownloadState.PAUSED) {
                startTask(context.applicationContext, task)
            }
            return
        }
        if (downloadId < 0) return
        runCatching {
            val values = ContentValues().apply { put(COLUMN_CONTROL, CONTROL_RUN) }
            context.contentResolver.update(downloadUri(downloadId), values, null, null)
        }
    }

    fun cancel(context: Context, downloadId: Long) {
        tasks[downloadId]?.let { task ->
            task.cancelled = true
            task.state = DownloadState.COMPLETE
            executor.execute {
                runCatching { task.connection?.disconnect() }
                runCatching { task.tempFile.delete() }
            }
            return
        }
        if (downloadId < 0) return
        runCatching {
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager)?.remove(downloadId)
        }
    }

    fun retry(context: Context, downloadId: Long) {
        tasks[downloadId]?.let { task ->
            task.cancelled = false
            task.failed = false
            if (!task.tempFile.exists()) {
                task.savedBytes = 0L
            }
            startTask(context.applicationContext, task)
            return
        }
        if (downloadId < 0) return
        runCatching {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return@runCatching
            DownloadManager::class.java
                .getMethod("restartDownload", LongArray::class.java)
                .invoke(dm, longArrayOf(downloadId))
        }
    }

    private fun resolveDownloadUri(context: Context, item: DownloadItem): Pair<Uri, String>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collections = listOf(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            )
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.MIME_TYPE)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
            collections.forEach { collection ->
                runCatching {
                    resolver.query(collection, projection, selection, arrayOf(item.title), null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(0)
                            val mime = cursor.getString(1)?.ifBlank { null }
                                ?: mimeTypeFromFileName(item.title)
                                ?: "*/*"
                            return ContentUris.withAppendedId(collection, id) to mime
                        }
                    }
                }
            }
        }
        if (item.downloadId >= 0) {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            val uri = runCatching { manager?.getUriForDownloadedFile(item.downloadId) }.getOrNull()
            if (uri != null) {
                val mime = manager?.getMimeTypeForDownloadedFile(item.downloadId)?.ifBlank { null }
                    ?: mimeTypeFromFileName(item.title)
                    ?: "*/*"
                return uri to mime
            }
        }
        val candidateDirs = buildSet {
            add(BrowserStore.effectiveDownloadRelativePath(context))
            add(BrowserStore.loadCustomDownloadPath(context))
            BrowserStore.DownloadFolder.entries.forEach { add(it.relativePath) }
        }
        candidateDirs.forEach { rel ->
            val file = File(File(Environment.getExternalStorageDirectory(), rel), item.title)
            if (file.exists()) {
                val mime = mimeTypeFromFileName(item.title) ?: "*/*"
                val uri = runCatching {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                }.getOrNull() ?: Uri.fromFile(file)
                return uri to mime
            }
        }
        return null
    }

    fun savedPageUri(context: Context, item: DownloadItem): String? =
        resolveDownloadUri(context, item)?.first?.toString()

    fun openDownload(context: Context, item: DownloadItem) {
        val (uri, mime) = resolveDownloadUri(context, item) ?: run {
            locateDownload(context, item)
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, item.title))
        }.onFailure {
            locateDownload(context, item)
        }
    }

    fun locateDownload(context: Context, item: DownloadItem? = null) {
        val relPath = item?.relativePath?.trim()?.trim('/')
        if (!relPath.isNullOrBlank() && !relPath.equals("Download", ignoreCase = true)) {
            val docUri = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:$relPath",
            )
            val dirIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(docUri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (runCatching { context.startActivity(dirIntent) }.isSuccess) return
        }
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" })
            }
        }
    }

    fun shareDownload(context: Context, item: DownloadItem) {
        val (uri, mime) = resolveDownloadUri(context, item) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, item.title))
        }
    }

    fun deleteHistoryOnly(context: Context, item: DownloadItem) {
        tasks.remove(item.downloadId)
        BrowserStore.removeDownload(context, item.title)
        BrowserStore.hideDownload(context, item.title)
    }

    fun deleteWithStorage(context: Context, item: DownloadItem) {
        tasks.remove(item.downloadId)?.let { task ->
            runCatching { task.tempFile.delete() }
        }
        BrowserStore.removeDownload(context, item.title)
        BrowserStore.hideDownload(context, item.title)
        if (item.downloadId >= 0) {
            runCatching {
                (context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager)?.remove(item.downloadId)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            runCatching {
                resolver.delete(collection, "${MediaStore.Downloads.DISPLAY_NAME} = ?", arrayOf(item.title))
            }
        }
        runCatching {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), item.title)
            if (file.exists()) file.delete()
        }
    }

    fun deleteByFileName(context: Context, fileName: String, relativePath: String? = null) {
        if (fileName.isBlank()) return
        BrowserStore.removeDownload(context, fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collections = listOf(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            )
            collections.forEach { uri ->
                runCatching {
                    resolver.delete(uri, "${MediaStore.MediaColumns.DISPLAY_NAME} = ?", arrayOf(fileName))
                }
            }
        }
        val pathsToCheck = buildSet {
            if (!relativePath.isNullOrBlank()) add(relativePath)
            add(BrowserStore.loadCustomDownloadPath(context))
            BrowserStore.DownloadFolder.entries.forEach { add(it.relativePath) }
        }
        pathsToCheck.forEach { rel ->
            runCatching {
                val dir = File(Environment.getExternalStorageDirectory(), rel)
                val file = File(dir, fileName)
                if (file.exists()) file.delete()
            }
        }
    }

    fun resolvePreferenceForDownload(
        context: Context,
        url: String,
        mimeType: String?,
        fileName: String,
    ): BrowserStore.ContentPreference {
        val prefs = BrowserStore.loadContentPreferences(context)
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val ruleKey = when {
            url.startsWith("mailto:", ignoreCase = true) -> "mailto"
            mime == "application/pdf" || ext == "pdf" -> "pdf"
            mime == "image/webp" || ext == "webp" -> "webp"
            mime == "image/avif" || ext == "avif" -> "avif"
            mime == "image/jxl" || ext == "jxl" -> "jxl"
            mime == "image/svg+xml" || ext == "svg" -> "svg"
            mime == "text/xml" || mime == "application/xml" || ext == "xml" -> "xml"
            mime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "gif", "bmp", "heic", "heif", "ico") -> "jpeg"
            else -> null
        }
        if (ruleKey != null) {
            val defaultAction = BrowserStore.defaultContentTypeRules
                .firstOrNull { it.key == ruleKey }?.defaultAction
                ?: BrowserStore.ContentAction.SAVE_FILE
            return prefs[ruleKey] ?: BrowserStore.ContentPreference(defaultAction)
        }
        val action = if (BrowserStore.loadDownloadOtherAsk(context) || BrowserStore.loadDownloadAskBefore(context)) {
            BrowserStore.ContentAction.ALWAYS_ASK
        } else {
            BrowserStore.ContentAction.SAVE_FILE
        }
        return BrowserStore.ContentPreference(action)
    }

    fun cleanupOldPreviewCache(context: Context, maxAgeMs: Long = 24L * 60L * 60L * 1000L) {
        val previewDir = File(context.applicationContext.cacheDir, "preview")
        if (!previewDir.exists()) return
        val cutoff = System.currentTimeMillis() - maxAgeMs
        previewDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                runCatching { file.delete() }
            }
        }
    }

    fun downloadToCacheFile(
        context: Context,
        url: String,
        userAgent: String?,
        fileName: String,
        onComplete: (File?) -> Unit,
    ) {
        val appContext = context.applicationContext
        val cleanName = sanitize(fileName)
        val previewDir = File(appContext.cacheDir, "preview").apply { if (!exists()) mkdirs() }
        val targetFile = File(previewDir, "${System.currentTimeMillis()}_$cleanName")
        val mainHandler = Handler(Looper.getMainLooper())
        executor.execute {
            val result: File? = runCatching {
                if (url.startsWith("data:", ignoreCase = true)) {
                    val comma = url.indexOf(',')
                    if (comma <= 0) return@runCatching null
                    val meta = url.substring(5, comma)
                    val payload = url.substring(comma + 1)
                    val bytes = if (meta.contains(";base64", ignoreCase = true)) {
                        Base64.decode(payload, Base64.DEFAULT)
                    } else {
                        Uri.decode(payload).toByteArray(Charsets.UTF_8)
                    }
                    targetFile.writeBytes(bytes)
                    targetFile
                } else {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        if (!userAgent.isNullOrEmpty()) setRequestProperty("User-Agent", userAgent)
                        CookieManager.getInstance().getCookie(url)?.let { cookie ->
                            setRequestProperty("Cookie", cookie)
                        }
                    }
                    try {
                        if (conn.responseCode !in 200..299) return@runCatching null
                        conn.inputStream.use { input ->
                            FileOutputStream(targetFile).use { out ->
                                input.copyTo(out)
                                out.flush()
                            }
                        }
                        targetFile
                    } finally {
                        conn.disconnect()
                    }
                }
            }.getOrNull()
            if (result == null) runCatching { targetFile.delete() }
            mainHandler.post { onComplete(result) }
        }
    }

    fun saveCachedFileToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        cachedFile: File,
        relativePath: String = BrowserStore.effectiveDownloadRelativePath(context),
        onDone: (Boolean) -> Unit,
    ) {
        val appContext = context.applicationContext
        val cleanName = sanitize(fileName)
        val mainHandler = Handler(Looper.getMainLooper())
        executor.execute {
            val savedUri = saveFileFromTemp(appContext, cleanName, mimeType, cachedFile, relativePath)
            if (savedUri != null) {
                BrowserStore.addDownload(
                    appContext,
                    BrowserStore.DownloadRecord(
                        title = cleanName,
                        domain = "",
                        sizeBytes = cachedFile.length(),
                        timestamp = System.currentTimeMillis(),
                        relativePath = relativePath,
                    ),
                )
            }
            mainHandler.post { onDone(savedUri != null) }
        }
    }

    fun queryAll(context: Context): List<DownloadItem> {
        val items = ArrayList<DownloadItem>()
        val seenTitles = HashSet<String>()
        tasks.values.forEach { task ->
            seenTitles.add(task.fileName)
            val total = task.totalBytes
            val saved = task.savedBytes
            val progress = if (total > 0L) (saved.toFloat() / total).coerceIn(0f, 1f) else 0f
            val detail = when {
                task.cancelled -> "Cancelled"
                task.failed -> "Failed"
                total > 0L -> "${formatSize(saved)} / ${formatSize(total)}"
                saved > 0L -> formatSize(saved)
                else -> "Starting"
            }
            items += DownloadItem(
                title = task.fileName,
                domain = task.domain,
                detail = detail,
                state = task.state,
                progress = progress,
                timestamp = task.timestamp,
                downloadId = task.id,
                relativePath = task.relativePath,
            )
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (manager != null) {
            val flags = DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING or
                DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_SUCCESSFUL or
                DownloadManager.STATUS_FAILED
            runCatching {
                manager.query(DownloadManager.Query().setFilterByStatus(flags))
            }.getOrNull()?.use {
                val idCol = it.getColumnIndex(DownloadManager.COLUMN_ID)
                val titleCol = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val uriCol = it.getColumnIndex(DownloadManager.COLUMN_URI)
                val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val reasonCol = it.getColumnIndex(DownloadManager.COLUMN_REASON)
                val totalCol = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val savedCol = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val stampCol = it.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)
                while (it.moveToNext()) {
                    val status = it.getInt(statusCol)
                    val reason = if (reasonCol >= 0) it.getInt(reasonCol) else 0
                    val total = it.getLong(totalCol)
                    val saved = it.getLong(savedCol)
                    val failed = status == DownloadManager.STATUS_FAILED
                    val title = it.getString(titleCol) ?: "download"
                    val domain = runCatching { Uri.parse(it.getString(uriCol)).host }.getOrNull() ?: ""
                    val state = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL, DownloadManager.STATUS_FAILED -> DownloadState.COMPLETE
                        DownloadManager.STATUS_PAUSED -> DownloadState.PAUSED
                        else -> DownloadState.DOWNLOADING
                    }
                    val detail = when {
                        failed -> "Failed"
                        status == DownloadManager.STATUS_PAUSED && reason == DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
                        status == DownloadManager.STATUS_PAUSED && reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
                        status == DownloadManager.STATUS_PAUSED && reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi"
                        else -> formatSize(total)
                    }
                    seenTitles.add(title)
                    items += DownloadItem(
                        title = title,
                        domain = domain,
                        detail = detail,
                        state = state,
                        progress = if (total > 0) (saved.toFloat() / total).coerceIn(0f, 1f) else 0f,
                        timestamp = it.getLong(stampCol),
                        downloadId = it.getLong(idCol),
                    )
                }
            }
        }
        val missingLabel = context.getString(R.string.download_file_missing)
        BrowserStore.loadDownloads(context).forEach { record ->
            if (!seenTitles.add(record.title)) return@forEach
            val exists = downloadExists(context, record.title, record.relativePath)
            val detail = when {
                !exists -> missingLabel
                record.sizeBytes > 0L -> formatSize(record.sizeBytes)
                else -> ""
            }
            items += DownloadItem(
                title = record.title,
                domain = record.domain,
                detail = detail,
                state = DownloadState.COMPLETE,
                progress = 1f,
                timestamp = record.timestamp,
                downloadId = -1L,
                relativePath = record.relativePath,
            )
        }
        val hidden = BrowserStore.loadHiddenDownloads(context)
        return items
            .filter { it.state != DownloadState.COMPLETE || it.title !in hidden }
            .sortedByDescending { it.timestamp }
    }

    fun querySavedPages(context: Context): List<DownloadItem> {
        val items = ArrayList<DownloadItem>()
        val seenTitles = HashSet<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED,
            )
            val selection = "(${MediaStore.Downloads.DISPLAY_NAME} LIKE ? OR ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?) AND ${MediaStore.Downloads.IS_PENDING} = 0"
            val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC"
            runCatching {
                resolver.query(collection, projection, selection, arrayOf("%.mht", "%.mhtml"), sortOrder)?.use { cursor ->
                    val nameCol = cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                    val dateCol = cursor.getColumnIndex(MediaStore.Downloads.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                        if (name.isNullOrBlank() || !seenTitles.add(name)) continue
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                        val modifiedSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                        items += DownloadItem(
                            title = name,
                            domain = "",
                            detail = if (size > 0L) formatSize(size) else "",
                            state = DownloadState.COMPLETE,
                            progress = 1f,
                            timestamp = modifiedSec * 1000L,
                            downloadId = -1L,
                        )
                    }
                }
            }
        }
        runCatching {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.listFiles { file ->
                file.isFile && (file.name.endsWith(".mht", ignoreCase = true) || file.name.endsWith(".mhtml", ignoreCase = true))
            }
                ?.forEach { file ->
                    if (seenTitles.add(file.name)) {
                        items += DownloadItem(
                            title = file.name,
                            domain = "",
                            detail = formatSize(file.length()),
                            state = DownloadState.COMPLETE,
                            progress = 1f,
                            timestamp = file.lastModified(),
                            downloadId = -1L,
                        )
                    }
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
            "jxl" -> "image/jxl"
            "avif" -> "image/avif"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "svg" -> "image/svg+xml"
            "xml" -> "text/xml"
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
