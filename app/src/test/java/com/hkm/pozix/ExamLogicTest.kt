package com.hkm.pozix

import com.hkm.pozix.viewmodel.calculateExamRemainingMillis
import com.hkm.pozix.viewmodel.isValidExamQuestionIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamLogicTest {
    @Test
    fun activeSessionUsesAbsoluteDeadlineAfterProcessRestart() {
        assertEquals(
            12_000L,
            calculateExamRemainingMillis(
                savedRemainingMillis = 40_000L,
                deadlineEpochMillis = 112_000L,
                nowEpochMillis = 100_000L,
                pausedByUser = false
            )
        )
    }

    @Test
    fun explicitlySavedSessionKeepsPausedRemainingTime() {
        assertEquals(
            40_000L,
            calculateExamRemainingMillis(
                savedRemainingMillis = 40_000L,
                deadlineEpochMillis = 90_000L,
                nowEpochMillis = 100_000L,
                pausedByUser = true
            )
        )
    }

    @Test
    fun expiredActiveSessionReturnsZero() {
        assertEquals(
            0L,
            calculateExamRemainingMillis(
                savedRemainingMillis = 40_000L,
                deadlineEpochMillis = 99_000L,
                nowEpochMillis = 100_000L,
                pausedByUser = false
            )
        )
    }

    @Test
    fun navigationRejectsIndexesOutsideQuestionRange() {
        assertFalse(isValidExamQuestionIndex(-1, 5))
        assertTrue(isValidExamQuestionIndex(0, 5))
        assertTrue(isValidExamQuestionIndex(4, 5))
        assertFalse(isValidExamQuestionIndex(5, 5))
        assertFalse(isValidExamQuestionIndex(0, 0))
    }
}
