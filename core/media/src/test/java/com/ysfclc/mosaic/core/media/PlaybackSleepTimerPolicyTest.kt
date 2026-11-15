package com.ysfclc.mosaic.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSleepTimerPolicyTest {
    @Test
    fun computesDeadlineFromTheSelectedDuration() {
        assertEquals(1_000_000L, PlaybackSleepTimerPolicy.deadlineAfterMinutes(15, 100_000L))
    }

    @Test
    fun rejectsNonPositiveDurationsAndBoundsLongTimers() {
        assertEquals(0L, PlaybackSleepTimerPolicy.deadlineAfterMinutes(0, 100_000L))
        assertEquals(0L, PlaybackSleepTimerPolicy.deadlineAfterMinutes(-5, 100_000L))
        assertEquals(86_500_000L, PlaybackSleepTimerPolicy.deadlineAfterMinutes(24 * 60 + 10, 100_000L))
    }

    @Test
    fun remainingTimeNeverBecomesNegative() {
        assertEquals(45_000L, PlaybackSleepTimerPolicy.remainingMs(145_000L, 100_000L))
        assertEquals(0L, PlaybackSleepTimerPolicy.remainingMs(99_000L, 100_000L))
        assertEquals(0L, PlaybackSleepTimerPolicy.remainingMs(0L, 100_000L))
    }

    @Test
    fun expiresOnlyAfterARealDeadlineHasPassed() {
        assertFalse(PlaybackSleepTimerPolicy.isExpired(0L, 100_000L))
        assertFalse(PlaybackSleepTimerPolicy.isExpired(101_000L, 100_000L))
        assertTrue(PlaybackSleepTimerPolicy.isExpired(100_000L, 100_000L))
    }
}
