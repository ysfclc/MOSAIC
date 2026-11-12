package com.ysfclc.mosaic.ui.browser

import java.net.URI

/**
 * Source-aware request filter for MOSAIC Browser.
 *
 * Known third-party ad/tracker hosts are filtered on provider pages too. The
 * only temporary compatibility exception is Google's ad infrastructure on
 * YouTube pages, which can participate in YouTube player delivery. This is
 * deliberately narrower than exempting an entire provider page or its assets.
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
     * YouTube's player may depend on these Google endpoints. Keep this
     * exception scoped to YouTube top-level pages; the same hosts are blocked
     * on ordinary pages and Spotify pages.
     */
    private val youtubePlayerCompatibilityDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "googletagservices.com",
    )

    fun shouldBlock(requestUrl: String, topLevelHost: String?, referrerUrl: String? = null): Boolean {
        val requestHost = normalizeHost(runCatching { URI(requestUrl).host }.getOrNull()) ?: return false
        val contextHosts = listOfNotNull(
            normalizeHost(topLevelHost),
            normalizeHost(runCatching { URI(referrerUrl).host }.getOrNull()),
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
