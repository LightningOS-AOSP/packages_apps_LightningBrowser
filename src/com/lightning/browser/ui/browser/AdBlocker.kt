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

object AdBlocker {

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
}
