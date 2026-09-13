package com.lightning.browser.ui.settings

import androidx.annotation.StringRes
import com.lightning.browser.R

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
) {
    GOOGLE(R.string.search_engine_google, R.string.search_engine_google_detail),
    DUCKDUCKGO(R.string.search_engine_duckduckgo, R.string.search_engine_duckduckgo_detail),
    BING(R.string.search_engine_bing, R.string.search_engine_bing_detail),
    BRAVE(R.string.search_engine_brave, R.string.search_engine_brave_detail),
}