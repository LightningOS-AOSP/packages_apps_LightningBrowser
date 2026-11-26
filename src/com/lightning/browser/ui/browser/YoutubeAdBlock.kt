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

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object YoutubeAdBlock {

    private val AD_PATH_PREFIXES = listOf(
        "/api/stats/ads",
        "/pagead/",
        "/ptracking",
        "/pcs/activeview",
    )

    private val AD_HOST_SUFFIXES = listOf(
        "doubleclick.net",
        "2mdn.net",
        "googlesyndication.com",
        "googleadservices.com",
    )

    private val HEAD_TAG = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)

    fun isYoutubeHost(host: String): Boolean =
        host == "youtube.com" || host.endsWith(".youtube.com")

    fun isAdRequest(url: Uri): Boolean {
        val host = url.host ?: return false
        if (AD_HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }) return true
        if (!isYoutubeHost(host)) return false
        val path = url.path ?: return false
        return AD_PATH_PREFIXES.any { path.startsWith(it) }
    }

    fun page(
        url: String,
        userAgent: String?,
        headers: Map<String, String>,
        dnsEndpoint: String?,
    ): WebResourceResponse? {
        val agent = userAgent ?: return null
        val response = (if (dnsEndpoint != null) {
            PinnedHttp.get(url, dnsEndpoint, agent, headers)
        } else {
            PlainHttp.get(url, agent, headers)
        }) ?: return null
        if (response.statusCode !in 200..299) return null
        val contentType = response.headers["content-type"] ?: return null
        if (!contentType.contains("text/html", ignoreCase = true)) return null
        val charsetName = contentType.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim('"')
            ?: "UTF-8"
        val charset = runCatching { Charset.forName(charsetName) }.getOrDefault(Charsets.UTF_8)
        val patched = inject(String(response.body, charset)).toByteArray(charset)
        val outHeaders = response.headers.filterKeys { key ->
            !key.startsWith("content-security-policy")
        }
        return WebResourceResponse(
            "text/html",
            charset.name(),
            response.statusCode,
            "OK",
            outHeaders,
            ByteArrayInputStream(patched),
        )
    }

    private fun inject(html: String): String {
        val tag = "<script>$SCRIPT</script>"
        val head = HEAD_TAG.find(html) ?: return tag + html
        return html.substring(0, head.range.last + 1) + tag + html.substring(head.range.last + 1)
    }

    private const val SCRIPT = """(function(){
if(window.__lightningAds)return;
window.__lightningAds=true;
var KEYS=['adPlacements','playerAds','adSlots'];
function strip(o){
if(!o||typeof o!=='object')return o;
for(var i=0;i<KEYS.length;i++){if(KEYS[i] in o)delete o[KEYS[i]];}
if(o.playerResponse)strip(o.playerResponse);
if(o.response)strip(o.response);
return o;
}
var held;
try{Object.defineProperty(window,'ytInitialPlayerResponse',{configurable:true,get:function(){return held;},set:function(v){held=strip(v);}});}catch(e){}
var parse=JSON.parse;
JSON.parse=function(){return strip(parse.apply(this,arguments));};
var json=Response.prototype.json;
Response.prototype.json=function(){return json.apply(this,arguments).then(strip);};
var style=document.createElement('style');
style.textContent='ytd-ad-slot-renderer,ytd-promoted-sparkles-web-renderer,ytd-display-ad-renderer,ytd-banner-promo-renderer,ytd-statement-banner-renderer,ytd-in-feed-ad-layout-renderer,ytm-promoted-sparkles-web-renderer,ytm-promoted-video-renderer,ytm-ad-slot-renderer,ytm-companion-slot,ad-slot-renderer,#masthead-ad,.ytp-ad-overlay-container,.ytp-ad-image-overlay,.video-ads{display:none!important}';
(document.head||document.documentElement).appendChild(style);
var restore=null;
setInterval(function(){
var player=document.querySelector('.html5-video-player');
if(!player)return;
var video=player.querySelector('video');
if(player.classList.contains('ad-showing')){
if(video){
if(restore===null)restore=video.muted;
video.muted=true;
if(isFinite(video.duration)&&video.duration>0)video.currentTime=video.duration;
}
}else if(restore!==null){
if(video)video.muted=restore;
restore=null;
}
var skip=document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-ad-skip-button-container button');
if(skip)skip.click();
},250);
})();"""
}

object PlainHttp {

    private val DROPPED_HEADERS = setOf(
        "transfer-encoding",
        "content-length",
        "content-encoding",
        "connection",
        "set-cookie",
    )

    fun get(url: String, userAgent: String, headers: Map<String, String>): PinnedHttp.Response? {
        val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull()
            ?: return null
        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", userAgent)
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            val code = connection.responseCode
            if (code in 300..399) return null
            val cookieManager = CookieManager.getInstance()
            connection.headerFields["Set-Cookie"]?.forEach { cookie -> cookieManager.setCookie(url, cookie) }
            val stream = if (code >= 400) connection.errorStream else connection.inputStream
            val body = stream?.use { it.readBytes() } ?: ByteArray(0)
            val out = LinkedHashMap<String, String>()
            connection.headerFields.forEach { (key, values) ->
                if (key != null && values.isNotEmpty() && key.lowercase() !in DROPPED_HEADERS) {
                    out[key.lowercase()] = values.first()
                }
            }
            PinnedHttp.Response(code, out, body)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
