package com.lightning.browser

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.browser.BrowserDownloads
import com.lightning.browser.ui.root.LightningBrowserRoot

class MainActivity : ComponentActivity() {

    private val incomingUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserDownloads.cleanupOldPreviewCache(this)
        handleIntent(intent)
        setContent {
            LightningBrowserRoot(initialUrl = incomingUrl.value)
        }
    }

    override fun onResume() {
        super.onResume()
        cleanupOrphanedPrivateDownloads()
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
        val data = intent?.data
        if (data != null && (data.scheme == "http" || data.scheme == "https")) {
            incomingUrl.value = data.toString()
        }
    }
}