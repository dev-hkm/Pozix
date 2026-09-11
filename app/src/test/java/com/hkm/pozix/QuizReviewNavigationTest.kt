package com.hkm.pozix

import com.hkm.pozix.data.model.Question
import com.hkm.pozix.data.model.QuizProgress
import com.hkm.pozix.viewmodel.QuizState
import com.hkm.pozix.viewmodel.QuizReviewNavigation
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class QuizReviewNavigationTest {
    private val questions = listOf(
        Question.SingleChoice("First", listOf("A", "B"), 1, "Explanation"),
        Question.TrueFalse("Second", true), Question.TrueFalse("Third", false))
    private val state = QuizState.Playing("id", "Quiz", questions, 2, 1, null, false, false,
        false, 1000L, listOf(0, 1), mapOf(0 to 0, 1 to 0))

    @Test fun reviewRestoresWrongAnswerWithoutChangingScore() {
        val review = QuizReviewNavigation.show(state, 0, true)
        assertTrue(review.isAnswered)
        assertEquals(0, review.selectedAnswerIndex)
        assertFalse(review.isCorrect)
        assertTrue(review.showExplanation)
        assertEquals(state.score, review.score)
        assertEquals(state.selectedAnswers, review.selectedAnswers)
    }
    @Test fun forwardReturnsToUnansweredQuestion() {
        val review = QuizReviewNavigation.show(state, 1, true)
        assertTrue(review.isCorrect)
        assertTrue(review.isAnswered)
        assertEquals(state, QuizReviewNavigation.show(review, 2, true))
    }
    @Test fun invalidNavigationDoesNothing() {
        assertEquals(state, QuizReviewNavigation.show(state, -1, true))
        assertEquals(state, QuizReviewNavigation.show(state, 3, true))
    }
    @Test fun snapshotKeepsShuffledOrderAndAnswersAfterResume() {
        val progress = QuizProgress("id", 2, 1, listOf(0, 1), 1000L, 3,
            selectedAnswers = state.selectedAnswers, questionSnapshot = questions)
        assertEquals(progress, Json.decodeFromString<QuizProgress>(Json.encodeToString(progress)))
    }
}
