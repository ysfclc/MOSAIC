package com.ysfclc.mosaic.ui.browser

import java.net.URI

/**
 * Source-aware request filter for MOSAIC Browser.
 *
 * Known third-party ad/tracker hosts are filtered on provider pages too.
 * The temporary Google ad-infrastructure compatibility exception applies only
 * when the top-level page or request Referer belongs to YouTube. It does not
 * exempt Spotify or unrelated third-party ad/tracker requests.
 */
object AdBlockPolicy {
    private val blockedDomains = setOf(
        "adnxs.com", "adsrvr.org", "adform.net", "advertising.com",
        "casalemedia.com", "contextweb.com", "criteo.com", "exelator.com",
        "media.net", "moatads.com", "openx.net", "outbrain.com",
        "pubmatic.com", "quantserve.com", "rubiconproject.com",
        "scorecardresearch.com", "serving-sys.com", "sharethrough.com",
        "smartadserver.com", "taboola.com", "yieldmo.com", "zedo.com",
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "googletagservices.com",
    )

    private val youtubePageDomains = setOf(
        "youtube.com", "youtu.be", "youtube-nocookie.com",
    )

    private val providerPageDomains = youtubePageDomains + "spotify.com"

    /**
     * Compatibility exception retained from the previous player behavior.
     * It is limited to requests initiated in a YouTube top-level or embedded
     * frame context. Validate on a physical device before narrowing or removing it.
     */
    private val youtubePlayerCompatibilityDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "googletagservices.com",
    )

    fun shouldBlock(requestUrl: String, topLevelHost: String?, referrerUrl: String? = null): Boolean {
        val requestUri = runCatching { URI(requestUrl) }.getOrNull() ?: return false
        if (!requestUri.scheme.equals("http", ignoreCase = true) &&
            !requestUri.scheme.equals("https", ignoreCase = true)
        ) return false
        val requestHost = normalizeHost(requestUri.host) ?: return false
        val contextHosts = listOfNotNull(
            normalizeHost(topLevelHost),
            normalizeHost(referrerUrl?.let { url -> runCatching { URI(url).host }.getOrNull() }),
        )
        val blockedDomain = blockedDomains.firstOrNull { matchesDomain(requestHost, it) } ?: return false

        val isYoutubeContext = contextHosts.any { host -> youtubePageDomains.any { matchesDomain(host, it) } }
        if (isYoutubeContext && youtubePlayerCompatibilityDomains.any { matchesDomain(requestHost, it) }) {
            return false
        }

        return true
    }

    fun isProviderPage(host: String): Boolean {
        val normalized = normalizeHost(host) ?: return false
        return providerPageDomains.any { matchesDomain(normalized, it) }
    }

    private fun normalizeHost(host: String?): String? =
        host?.trim()?.lowercase()?.trimEnd('.')?.removePrefix("www.")?.takeIf { it.isNotBlank() }

    private fun matchesDomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")
}


/** Allows only network navigations; custom schemes stay outside the WebView trust boundary. */
object BrowserNavigationPolicy {
    fun isAllowedNavigation(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val isNetworkScheme = uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)
        return isNetworkScheme && !uri.host.isNullOrBlank()
    }
}
