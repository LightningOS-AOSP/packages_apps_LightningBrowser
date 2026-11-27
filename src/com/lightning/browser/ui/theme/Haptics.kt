package com.lightning.browser.ui.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

val LocalHapticsEnabled = compositionLocalOf { true }

@Immutable
class LightningHaptics(
    private val view: View,
    private val enabled: Boolean,
) {
    fun tick() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun click() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun longPress() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun gestureThreshold() {
        if (!enabled) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_START
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(constant)
    }

    fun confirm() {
        if (!enabled) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
        view.performHapticFeedback(constant)
    }

    fun reject() {
        if (!enabled) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberLightningHaptics(): LightningHaptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { LightningHaptics(view, enabled) }
}
