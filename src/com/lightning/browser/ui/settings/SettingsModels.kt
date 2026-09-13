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