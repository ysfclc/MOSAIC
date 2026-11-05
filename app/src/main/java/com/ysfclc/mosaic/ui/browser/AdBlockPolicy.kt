package com.ysfclc.mosaic.ui.browser

import java.net.URI

/**
 * Conservative request filter for MOSAIC's own browser.
 * It deliberately avoids Google ad infrastructure because those domains can
 * participate in YouTube player delivery. Provider pages/assets are exempt.
 */
object AdBlockPolicy {
    private val blockedDomains = setOf(
        "adnxs.com", "adsrvr.org", "adform.net", "advertising.com",
        "casalemedia.com", "contextweb.com", "criteo.com", "exelator.com",
        "media.net", "moatads.com", "openx.net", "outbrain.com",
        "pubmatic.com", "quantserve.com", "rubiconproject.com",
        "scorecardresearch.com", "serving-sys.com", "sharethrough.com",
        "smartadserver.com", "taboola.com", "yieldmo.com", "zedo.com",
    )

    private val protectedProviderDomains = setOf(
        "youtube.com", "youtu.be", "youtube-nocookie.com", "ytimg.com",
        "googlevideo.com", "spotify.com", "scdn.co", "spotifycdn.com",
    )

    fun shouldBlock(requestUrl: String, topLevelHost: String?): Boolean {
        val requestHost = runCatching { URI(requestUrl).host?.lowercase() }.getOrNull() ?: return false
        val pageHost = topLevelHost?.lowercase().orEmpty()
        if (isProtectedProviderHost(pageHost) || isProtectedProviderHost(requestHost)) return false
        return blockedDomains.any { domain ->
            requestHost == domain || requestHost.endsWith(".$domain")
        }
    }

    fun isProtectedProviderHost(host: String): Boolean {
        val normalized = host.lowercase().removePrefix("www.")
        return protectedProviderDomains.any { domain ->
            normalized == domain || normalized.endsWith(".$domain")
        }
    }
}
