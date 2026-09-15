package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

/** Structured result artifact sent to the AI review flow. It is not quiz-generation JSON. */
@Serializable
data class QuizReviewPayload(
    val title: String,
    val score: Int,
    val totalQuestions: Int,
    val elapsedTimeMillis: Long,
    val items: List<QuizReviewItem>,
    val reviewId: String = ""
)

@Serializable
data class QuizReviewItem(
    val question: String,
    val options: List<String> = emptyList(),
    val selectedIndex: Int? = null,
    val correctIndex: Int = -1,
    val explanation: String? = null,
    val userTextAnswer: String? = null,
    val correctTextAnswer: String? = null
) {
    val isCorrect: Boolean get() = when {
        userTextAnswer != null && correctTextAnswer != null ->
            com.hkm.pozix.util.ShortAnswerMatcher.isMatch(userTextAnswer, correctTextAnswer)
        selectedIndex != null && correctIndex >= 0 -> selectedIndex == correctIndex
        else -> false
    }
}
