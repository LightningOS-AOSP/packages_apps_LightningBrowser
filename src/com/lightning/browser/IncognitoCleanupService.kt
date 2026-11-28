package com.lightning.browser

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.browser.BrowserDownloads
import java.io.File

class IncognitoCleanupService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        cleanupNow()
        stopSelf()
        super.onTaskRemoved(rootIntent)
        Process.killProcess(Process.myPid())
    }

    private fun cleanupNow() {
        if (BrowserStore.loadDeletePrivateDownloads(this)) {
            BrowserStore.popAllPrivateDownloads(this).forEach { rec ->
                BrowserDownloads.deleteByFileName(this, rec.fileName, rec.relativePath)
            }
        }
        File(applicationInfo.dataDir, "app_webview_incognito").deleteRecursively()
        File(cacheDir, "WebView_incognito").deleteRecursively()
        File(cacheDir.parentFile, "app_webview_incognito").deleteRecursively()
    }
}
