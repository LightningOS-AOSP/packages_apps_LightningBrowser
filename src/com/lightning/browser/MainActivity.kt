package com.lightning.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lightning.browser.ui.root.LightningBrowserRoot
import com.lightning.browser.ui.theme.LightningTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LightningTheme {
                LightningBrowserRoot()
            }
        }
    }
}