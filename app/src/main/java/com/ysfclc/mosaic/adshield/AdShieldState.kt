package com.ysfclc.mosaic.adshield

import android.content.Context

internal object AdShieldState {
    private const val PREFERENCES = "mosaic_ad_shield"
    private const val KEY_RUNNING = "running"
    private const val KEY_BLOCKED_COUNT = "blocked_count"
    private const val KEY_LAST_BLOCKED_DOMAIN = "last_blocked_domain"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_ALLOWLIST = "allowlist"

    fun isRunning(context: Context): Boolean = preferences(context).getBoolean(KEY_RUNNING, false)

    fun setRunning(context: Context, running: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_RUNNING, running)
            .apply()
    }

    fun blockedCount(context: Context): Int = preferences(context).getInt(KEY_BLOCKED_COUNT, 0)

    fun lastBlockedDomain(context: Context): String =
        preferences(context).getString(KEY_LAST_BLOCKED_DOMAIN, "").orEmpty()

    fun lastError(context: Context): String =
        preferences(context).getString(KEY_LAST_ERROR, "").orEmpty()

    fun allowlist(context: Context): Set<String> =
        preferences(context).getStringSet(KEY_ALLOWLIST, emptySet())?.toSet().orEmpty()

    fun setAllowlist(context: Context, domains: Set<String>) {
        val normalized = domains.map { it.trim().trimEnd('.').lowercase() }
            .filter { it.isNotBlank() && it.length <= 253 && !it.any(Char::isWhitespace) }
            .toSet()
        preferences(context).edit().putStringSet(KEY_ALLOWLIST, normalized).apply()
    }

    fun recordBlocked(context: Context, domain: String) {
        val prefs = preferences(context)
        prefs.edit()
            .putInt(KEY_BLOCKED_COUNT, prefs.getInt(KEY_BLOCKED_COUNT, 0) + 1)
            .putString(KEY_LAST_BLOCKED_DOMAIN, domain)
            .apply()
    }

    fun setError(context: Context, error: String?) {
        preferences(context).edit()
            .putString(KEY_LAST_ERROR, error.orEmpty())
            .apply()
    }

    fun resetSession(context: Context) {
        preferences(context).edit()
            .putInt(KEY_BLOCKED_COUNT, 0)
            .putString(KEY_LAST_BLOCKED_DOMAIN, "")
            .putString(KEY_LAST_ERROR, "")
            .putBoolean(KEY_RUNNING, false)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
