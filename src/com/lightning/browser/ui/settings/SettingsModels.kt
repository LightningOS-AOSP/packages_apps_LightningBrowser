package com.lightning.browser.ui.settings

import androidx.annotation.StringRes
import com.lightning.browser.R
import java.net.URLEncoder

enum class DnsProvider(
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
) {
    OFF(R.string.dns_provider_off, R.string.dns_provider_off_detail),
    CLOUDFLARE(R.string.dns_provider_cloudflare, R.string.dns_provider_cloudflare_detail),
    GOOGLE(R.string.dns_provider_google, R.string.dns_provider_google_detail),
    QUAD9(R.string.dns_provider_quad9, R.string.dns_provider_quad9_detail),
    CUSTOM(R.string.dns_provider_custom, R.string.dns_provider_custom_detail),
}

enum class SearchEngine(
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    val searchUrlFormat: String,
) {
    GOOGLE(R.string.search_engine_google, R.string.search_engine_google_detail, "https://www.google.com/search?q=%s"),
    DUCKDUCKGO(R.string.search_engine_duckduckgo, R.string.search_engine_duckduckgo_detail, "https://duckduckgo.com/?q=%s"),
    BING(R.string.search_engine_bing, R.string.search_engine_bing_detail, "https://www.bing.com/search?q=%s"),
    BRAVE(R.string.search_engine_brave, R.string.search_engine_brave_detail, "https://search.brave.com/search?q=%s"),
}

fun SearchEngine.searchUrl(query: String): String =
    String.format(searchUrlFormat, URLEncoder.encode(query.trim(), "UTF-8"))