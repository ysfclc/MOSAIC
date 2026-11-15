package com.ysfclc.mosaic.core.media

import android.content.Context

object PlaybackSleepTimerPolicy {
    private const val MAX_TIMER_MINUTES = 24 * 60
    private const val MILLIS_PER_MINUTE = 60_000L

    fun deadlineAfterMinutes(minutes: Int, nowEpochMs: Long): Long {
        if (minutes <= 0) return 0L
        val boundedMinutes = minutes.coerceAtMost(MAX_TIMER_MINUTES)
        return nowEpochMs + boundedMinutes.toLong() * MILLIS_PER_MINUTE
    }

    fun remainingMs(deadlineEpochMs: Long, nowEpochMs: Long): Long =
        if (deadlineEpochMs <= 0L) 0L else (deadlineEpochMs - nowEpochMs).coerceAtLeast(0L)

    fun isExpired(deadlineEpochMs: Long, nowEpochMs: Long): Boolean =
        deadlineEpochMs > 0L && deadlineEpochMs <= nowEpochMs
}

/** Persists a wall-clock deadline so the service can enforce the timer after process recreation. */
class PlaybackSleepTimerStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun deadlineEpochMs(): Long = preferences.getLong(KEY_DEADLINE, 0L)

    fun setMinutes(minutes: Int, nowEpochMs: Long = System.currentTimeMillis()) {
        val deadline = PlaybackSleepTimerPolicy.deadlineAfterMinutes(minutes, nowEpochMs)
        if (deadline <= 0L) {
            clear()
        } else {
            preferences.edit().putLong(KEY_DEADLINE, deadline).apply()
        }
    }

    fun clear() {
        preferences.edit().remove(KEY_DEADLINE).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "mosaic_sleep_timer"
        private const val KEY_DEADLINE = "deadline_epoch_ms"
    }
}
