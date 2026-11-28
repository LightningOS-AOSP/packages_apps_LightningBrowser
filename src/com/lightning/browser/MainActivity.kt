package com.lightning.browser

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.browser.BrowserDownloads
import com.lightning.browser.ui.root.LightningBrowserRoot

data class IncomingPdfRequest(
    val uri: Uri,
    val title: String,
    val token: Long,
)

class MainActivity : ComponentActivity() {

    private val incomingUrl = mutableStateOf<String?>(null)
    private val incomingPdf = mutableStateOf<IncomingPdfRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
        WebView.startSafeBrowsing(this, null)
        BrowserDownloads.cleanupOldPreviewCache(this)
        handleIntent(intent)
        setContent {
            LightningBrowserRoot(
                initialUrl = incomingUrl.value,
                initialPdf = incomingPdf.value,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        cleanupOrphanedPrivateDownloads()
    }

    override fun onPause() {
        super.onPause()
        runCatching { CookieManager.getInstance().flush() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { CookieManager.getInstance().flush() }
    }

    private fun cleanupOrphanedPrivateDownloads() {
        if (!BrowserStore.loadDeletePrivateDownloads(this)) return
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val incognitoAlive = am?.runningAppProcesses?.any {
            it.processName == "${packageName}:incognito"
        } == true
        if (!incognitoAlive) {
            BrowserStore.popAllPrivateDownloads(this).forEach { rec ->
                BrowserDownloads.deleteByFileName(this, rec.fileName, rec.relativePath)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val scheme = data.scheme?.lowercase().orEmpty()
        val mime = intent.type ?: runCatching { contentResolver.getType(data) }.getOrNull()
        val isPdf = mime.equals("application/pdf", ignoreCase = true) ||
            data.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true

        if (isPdf && (scheme == "content" || scheme == "file")) {
            val title = resolveDisplayName(data)
            incomingPdf.value = IncomingPdfRequest(
                uri = data,
                title = title,
                token = System.nanoTime(),
            )
        } else if (scheme == "http" || scheme == "https") {
            incomingUrl.value = data.toString()
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "document.pdf"
    }
}
