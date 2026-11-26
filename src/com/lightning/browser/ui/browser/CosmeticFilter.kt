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

object CosmeticFilter {

    private val SELECTORS = listOf(
        "ins.adsbygoogle",
        "[id^=\"google_ads_\"]",
        "[id^=\"div-gpt-ad\"]",
        "[id^=\"ad-slot\"]",
        "[id^=\"adslot\"]",
        "[data-ad-slot]",
        "[data-ad-client]",
        "[data-google-query-id]",
        "iframe[src*=\"doubleclick.net\"]",
        "iframe[src*=\"googlesyndication.com\"]",
        "iframe[src*=\"adservice.google\"]",
        "iframe[id^=\"google_ads_iframe\"]",
        "iframe[title=\"Advertisement\"]",
        "iframe[title=\"3rd party ad content\"]",
        ".adsbygoogle",
        ".ad-banner",
        ".ad-container",
        ".ad-wrapper",
        ".ad-slot",
        ".ad-unit",
        ".advert",
        ".advertisement",
        ".adsbox",
        ".sponsored-ad",
        ".banner-ad",
        ".banner_ad",
        ".google-ad",
        ".dfp-ad",
        ".gpt-ad",
        "#ad-banner",
        "#ad-container",
        "#ads",
        "#adbox",
        "#advert",
        "#advertisement",
        "#banner-ad",
        "#google_ads",
        "#sponsored-ads",
    )

    val SCRIPT: String = """(function(){
if(window.__lightningCosmetic)return;
window.__lightningCosmetic=true;
var css='${SELECTORS.joinToString(",")}{display:none!important;visibility:hidden!important;height:0!important;min-height:0!important;margin:0!important;padding:0!important}';
function apply(){
var root=document.head||document.documentElement;
if(!root)return;
var style=document.createElement('style');
style.setAttribute('data-lightning','cosmetic');
style.textContent=css;
root.appendChild(style);
}
apply();
function collapse(){
var nodes=document.querySelectorAll('iframe');
for(var i=0;i<nodes.length;i++){
var f=nodes[i];
var r=f.getBoundingClientRect();
if(r.width>0&&r.height>0&&f.offsetParent&&!f.src&&!f.srcdoc&&f.id&&f.id.indexOf('google_ads')===0){f.style.setProperty('display','none','important');}
}
}
var timer=null;
new MutationObserver(function(){
if(timer)return;
timer=setTimeout(function(){timer=null;collapse();},400);
}).observe(document.documentElement,{childList:true,subtree:true});
})();"""
}
