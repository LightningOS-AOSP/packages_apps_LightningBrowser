package com.lightning.browser.ui.browser

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Patterns
import android.view.textclassifier.TextClassifier

object ClipboardUrlHelper {
    fun extractValidUrl(raw: CharSequence?): String? {
        val text = raw?.toString()?.trim().orEmpty()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val lower = text.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            val host = runCatching { Uri.parse(text).host }.getOrNull()
            return if (!host.isNullOrBlank() && Patterns.WEB_URL.matcher(text).matches()) text else null
        }
        if (text.contains("://") || lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("about:")) {
            return null
        }
        return if (text.contains('.') && Patterns.WEB_URL.matcher(text).matches()) {
            "https://$text"
        } else {
            null
        }
    }

    fun clipSignature(context: Context): Long {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return 0L
        return runCatching { clipboard.primaryClipDescription?.timestamp ?: 0L }.getOrDefault(0L)
    }

    /**
     * Checks clipboard metadata on Android 12+ without reading primaryClip,
     * avoiding the system "pasted from your clipboard" toast until user action.
     */
    fun isClassificationPending(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        return runCatching {
            if (!clipboard.hasPrimaryClip()) return@runCatching false
            val desc = clipboard.primaryClipDescription ?: return@runCatching false
            desc.classificationStatus != ClipDescription.CLASSIFICATION_COMPLETE
        }.getOrDefault(false)
    }

    fun hasClipboardUrlHint(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return readClipboardUrl(context) != null
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        return runCatching {
            if (!clipboard.hasPrimaryClip()) return@runCatching false
            val desc = clipboard.primaryClipDescription ?: return@runCatching false
            when (desc.classificationStatus) {
                ClipDescription.CLASSIFICATION_COMPLETE ->
                    desc.getConfidenceScore(TextClassifier.TYPE_URL) >= 0.5f
                ClipDescription.CLASSIFICATION_NOT_PERFORMED ->
                    (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                        desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) &&
                        readClipboardUrl(context) != null
                else -> false
            }
        }.getOrDefault(false)
    }

    fun readClipboardUrl(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        return runCatching {
            val clip = clipboard.primaryClip ?: return@runCatching null
            if (clip.itemCount <= 0) return@runCatching null
            extractValidUrl(clip.getItemAt(0)?.coerceToText(context))
        }.getOrNull()
    }
}
