package com.ysfclc.mosaic.core.persistence

import java.net.URI

/** Shared policy for durable media references. Ephemeral credentials never enter local persistence. */
object SafeMediaLocatorPolicy {
    private const val MAX_URI_LENGTH = 4096

    fun isSafe(uriValue: String): Boolean {
        if (uriValue.isBlank() || uriValue.length > MAX_URI_LENGTH) return false
        val uri = runCatching { URI(uriValue) }.getOrNull() ?: return false
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return false
        return when (uri.scheme?.lowercase()) {
            "https" -> !uri.host.isNullOrBlank() && (uri.port == -1 || uri.port in 1..65535)
            "content" -> !uri.rawAuthority.isNullOrBlank()
            "file" -> !uri.path.isNullOrBlank()
            else -> false
        }
    }
}
