package com.lightning.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.lightning.browser.ui.root.LightningBrowserRoot

class MainActivity : ComponentActivity() {

    private val incomingUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            LightningBrowserRoot(initialUrl = incomingUrl.value)
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