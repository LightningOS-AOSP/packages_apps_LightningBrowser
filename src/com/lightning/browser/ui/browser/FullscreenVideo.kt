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

package com.lightning.browser.ui.browser

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FullscreenVideo(private val context: Context) {

    var showing by mutableStateOf(false)
        private set

    private var customView: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null
    private var container: FrameLayout? = null
    private var previousOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    fun show(view: View, viewCallback: WebChromeClient.CustomViewCallback) {
        val activity = findActivity()
        if (activity == null || customView != null) {
            viewCallback.onCustomViewHidden()
            return
        }
        customView = view
        callback = viewCallback
        previousOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val frame = FrameLayout(activity).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                ),
            )
        }
        (activity.window.decorView as ViewGroup).addView(
            frame,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        container = frame
        activity.window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        showing = true
    }

    fun hide() {
        val shown = customView ?: return
        val hiddenCallback = callback
        customView = null
        callback = null
        showing = false
        container?.let { frame ->
            frame.removeView(shown)
            (frame.parent as? ViewGroup)?.removeView(frame)
        }
        container = null
        findActivity()?.let { activity ->
            activity.window.insetsController?.show(WindowInsets.Type.systemBars())
            activity.requestedOrientation = previousOrientation
        }
        hiddenCallback?.onCustomViewHidden()
    }

    private fun findActivity(): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
