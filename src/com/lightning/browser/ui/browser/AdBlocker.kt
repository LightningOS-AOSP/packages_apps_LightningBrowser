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

import android.content.Context
import android.net.Uri

object AdBlocker {

    private val AD_PATH_PATTERNS = listOf(
        "/pagead/",
        "/adserver/",
        "/adserve/",
        "/adsense/",
        "/adview/",
        "/adframe",
        "/adx/",
        "/advert/",
        "/adverts/",
        "/banner_ads/",
        "/bannerads/",
        "/ad_banner",
        "/doubleclick/",
        "/show_ads",
        "/adsbygoogle",
        "/prebid",
        "/ads/banner",
        "/ads/serve",
        "/ads/display",
        "/ad-manager/",
    )

    private val SECOND_LEVEL_LABELS = setOf("co", "com", "org", "net", "gov", "edu", "ac")

    @Volatile
    private var loaded: Set<String>? = null

    private fun hosts(context: Context): Set<String> {
        loaded?.let { return it }
        synchronized(this) {
            loaded?.let { return it }
            val set = HashSet<String>()
            runCatching {
                context.assets.open("adblock_hosts.txt").bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val host = line.trim()
                        if (host.isNotEmpty() && !host.startsWith("#")) {
                            set.add(host)
                        }
                    }
                }
            }
            loaded = set
            return set
        }
    }

    fun isAdHost(host: String, context: Context): Boolean {
        val hosts = hosts(context)
        var current = host
        while (current.isNotEmpty()) {
            if (current in hosts) return true
            val dot = current.indexOf('.')
            if (dot == -1) break
            current = current.substring(dot + 1)
        }
        return false
    }

    fun shouldBlock(url: Uri, pageHost: String?, isMainFrame: Boolean, context: Context): Boolean {
        val host = url.host ?: return false
        if (isAdHost(host, context)) return true
        if (isMainFrame || pageHost.isNullOrEmpty()) return false
        if (!isThirdParty(host, pageHost)) return false
        val path = url.path?.lowercase() ?: return false
        return AD_PATH_PATTERNS.any { path.contains(it) }
    }

    fun isThirdParty(host: String, pageHost: String): Boolean =
        registrableDomain(host) != registrableDomain(pageHost)

    private fun registrableDomain(host: String): String {
        val labels = host.lowercase().trimEnd('.').split('.')
        if (labels.size <= 2) return labels.joinToString(".")
        val take = if (labels[labels.size - 2] in SECOND_LEVEL_LABELS && labels.last().length == 2) 3 else 2
        return labels.takeLast(take).joinToString(".")
    }
}
