package com.ysfclc.mosaic.adshield

internal object AdShieldRules {
    private val blockedDomains = setOf(
        "adform.net",
        "adnxs.com",
        "adsrvr.org",
        "advertising.com",
        "casalemedia.com",
        "criteo.com",
        "demdex.net",
        "doubleclick.net",
        "exelator.com",
        "googleadservices.com",
        "googlesyndication.com",
        "moatads.com",
        "openx.net",
        "outbrain.com",
        "pubmatic.com",
        "quantserve.com",
        "rubiconproject.com",
        "scorecardresearch.com",
        "serving-sys.com",
        "sharethrough.com",
        "taboola.com",
    )

    fun isBlocked(host: String, allowlist: Set<String> = emptySet()): Boolean {
        val normalized = normalize(host) ?: return false
        if (allowlist.any { matches(normalized, it) }) return false
        return blockedDomains.any { matches(normalized, it) }
    }

    private fun matches(host: String, rule: String): Boolean {
        val normalizedRule = normalize(rule) ?: return false
        return host == normalizedRule || host.endsWith(".$normalizedRule")
    }

    private fun normalize(value: String): String? {
        val normalized = value.trim().trimEnd('.').lowercase()
        if (normalized.isBlank() || normalized.length > 253) return null
        if (normalized.any { it.isWhitespace() || it == '/' || it == ':' }) return null
        return normalized
    }
}
