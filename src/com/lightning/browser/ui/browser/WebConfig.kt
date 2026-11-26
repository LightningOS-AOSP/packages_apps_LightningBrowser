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

import com.lightning.browser.ui.tabs.BrowserTab

class WebConfig(
    var tab: BrowserTab,
    var adBlocking: Boolean,
    var dnsEndpoint: String?,
    var bypassTrusted: Boolean,
) {
    var userAgent: String? = null
    var fullscreen: FullscreenVideo? = null
    var onProgress: (Int) -> Unit = {}
    var onNavChange: (Boolean, Boolean) -> Unit = { _, _ -> }
    var onScrolled: (Int, Int) -> Unit = { _, _ -> }
    var onUrlChange: (String) -> Unit = {}
    var onPageMeta: (String, String, String) -> Unit = { _, _, _ -> }
    var onDownload: (String, String?, String?, String?) -> Unit = { _, _, _, _ -> }
    var onBlocked: () -> Unit = {}
    var onDnsResolved: () -> Unit = {}
}
