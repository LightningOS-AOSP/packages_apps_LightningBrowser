/*
 * Copyright (C) 2026 The LightningOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightning.browser

import android.os.Bundle
import android.os.Process
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lightning.browser.ui.root.LightningBrowserRoot
import java.io.File

class IncognitoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        wipeSessionData()
        runCatching { WebView.setDataDirectorySuffix(DATA_SUFFIX) }
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        setContent {
            LightningBrowserRoot(incognito = true)
        }
    }

    override fun onDestroy() {
        val closing = isFinishing
        super.onDestroy()
        if (closing) {
            wipeSessionData()
            Process.killProcess(Process.myPid())
        }
    }

    private fun wipeSessionData() {
        File(applicationInfo.dataDir, "app_webview_$DATA_SUFFIX").deleteRecursively()
        File(cacheDir, "WebView_$DATA_SUFFIX").deleteRecursively()
        File(cacheDir.parentFile, "app_webview_$DATA_SUFFIX").deleteRecursively()
    }

    private companion object {
        const val DATA_SUFFIX = "incognito"
    }
}
