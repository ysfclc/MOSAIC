package com.ysfclc.mosaic.core.media

import java.net.URI

/**
 * Accepts only explicit HTTPS stream addresses. This avoids silently enabling cleartext traffic
 * for the whole application and rejects embedded credentials.
 */
object PlayableStreamAddress {
    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty() || value.any(Char::isWhitespace)) return null
        return runCatching {
            val uri = URI(value).normalize()
            if (!uri.scheme.equals("https", ignoreCase = true)) return null
            if (uri.host.isNullOrBlank() || uri.rawUserInfo != null) return null
            if (uri.port !in -1..65535) return null
            uri.toASCIIString()
        }.getOrNull()
    }
}
